import React, { useEffect, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  AlertCircle,
  ArrowLeft,
  Check,
  CheckCircle2,
  ExternalLink,
  FileAudio,
  Globe,
  Image as ImageIcon,
  Info,
  Link2,
  Search,
  Sparkles,
  Upload,
} from 'lucide-react';
import { inspectArchive, importArchive, adminBasePath } from '../../api/adminClient';
import type {
  ArchiveEntry,
  ArchiveInspectResponse,
  ArchiveReciterDraft,
  ArchiveScheme,
  Reciter,
} from '../../api/types';
import { countryFlag, countryOptions, countryWithFlag } from '../../lib/countries';
import { Badge } from '../ui/Badge';
import { Button } from '../ui/Button';
import { GlassCard } from '../ui/GlassCard';
import { Input } from '../ui/Input';
import { Modal, ModalFooter } from '../ui/Modal';

export interface ArchiveImportModalProps {
  isOpen: boolean;
  onClose: () => void;
  onImported?: (reciter: Reciter) => void;
}

const emptyDraft: ArchiveReciterDraft = {
  slug: '',
  catalog: 'archive',
  catalog_key: '',
  name_en: '',
  name_ar: '',
  riwayah: 'Hafs',
  style: 'Murattal',
  tempo: 'medium',
  audio_folder: '',
  country: '',
  server_url: '',
  available_surahs: '',
  description: '',
  is_teacher: false,
  enabled: true,
  verified: true,
};

const schemeExplanations: Record<ArchiveScheme, string> = {
  canonical: 'Canonical layout detected. Filenames use the expected surah numbering and names, so the app can build direct archive paths.',
  'numbered-name': 'Numbered-name layout detected. Filenames include a surah number and name, which gives the importer a reliable match.',
  'name-only': 'Name-only layout detected. Files are matched by surah name, so review the mapping carefully for unusual or repeated names.',
  unknown: 'No reliable filename pattern was detected. Review the mapping before importing this item.',
};

const useInspectArchive = () => useMutation({ mutationFn: inspectArchive });

const useImportArchive = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: importArchive,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'reciters'] });
      queryClient.invalidateQueries({ queryKey: ['admin', 'overview'] });
    },
  });
};

const safeErrorMessage = (error: unknown, fallback: string) => {
  if (typeof error === 'string' && error.trim()) return error.trim();
  if (error && typeof error === 'object' && 'message' in error) {
    const message = (error as { message?: unknown }).message;
    if (typeof message === 'string' && message.trim()) return message.trim();
  }
  return fallback;
};

const errorStatus = (error: unknown) => {
  if (error && typeof error === 'object' && 'status' in error) {
    const status = (error as { status?: unknown }).status;
    return typeof status === 'number' ? status : undefined;
  }
  return undefined;
};

const normalizeDraft = (suggested?: Partial<ArchiveReciterDraft>): ArchiveReciterDraft => ({
  ...emptyDraft,
  ...suggested,
  is_teacher: suggested?.is_teacher ?? false,
  enabled: suggested?.enabled ?? true,
  verified: suggested?.verified ?? true,
});

const getCountryOption = (option: unknown) => {
  if (typeof option === 'string') return { value: option, label: option };
  if (!option || typeof option !== 'object') return null;

  const record = option as { value?: unknown; name?: unknown; label?: unknown };
  const value = record.value ?? record.name;
  if (typeof value !== 'string') return null;

  return { value, label: typeof record.label === 'string' ? record.label : value };
};

const updateDraftField = <K extends keyof ArchiveReciterDraft>(
  draft: ArchiveReciterDraft,
  key: K,
  value: ArchiveReciterDraft[K],
) => ({ ...draft, [key]: value });

/** Guides an Internet Archive item through inspection, review, and PostgreSQL import. */
export const ArchiveImportModal: React.FC<ArchiveImportModalProps> = ({ isOpen, onClose, onImported }) => {
  const [step, setStep] = useState<1 | 2 | 3>(1);
  const [url, setUrl] = useState('');
  const [inspection, setInspection] = useState<ArchiveInspectResponse | null>(null);
  const [draft, setDraft] = useState<ArchiveReciterDraft | null>(null);
  const [image, setImage] = useState<File | null>(null);
  const [imagePreview, setImagePreview] = useState('');
  const [inspectError, setInspectError] = useState('');
  const [importError, setImportError] = useState('');
  const [isDuplicate, setIsDuplicate] = useState(false);
  const [importedReciter, setImportedReciter] = useState<Reciter | null>(null);
  const inspectMutation = useInspectArchive();
  const importMutation = useImportArchive();

  useEffect(() => {
    if (!isOpen) return;
    setStep(1);
    setUrl('');
    setInspection(null);
    setDraft(null);
    setImage(null);
    setImagePreview('');
    setInspectError('');
    setImportError('');
    setIsDuplicate(false);
    setImportedReciter(null);
  }, [isOpen]);

  useEffect(() => {
    return () => {
      if (imagePreview.startsWith('blob:')) URL.revokeObjectURL(imagePreview);
    };
  }, [imagePreview]);

  const resetFlow = () => {
    setStep(1);
    setUrl('');
    setInspection(null);
    setDraft(null);
    setImage(null);
    setImagePreview('');
    setInspectError('');
    setImportError('');
    setIsDuplicate(false);
    setImportedReciter(null);
  };

  const handleClose = () => {
    resetFlow();
    onClose();
  };

  const handleInspect = async () => {
    if (!url.trim()) {
      setInspectError('Paste an archive.org link or item ID first.');
      return;
    }

    setInspectError('');
    setImportError('');
    setIsDuplicate(false);

    try {
      const result = await inspectMutation.mutateAsync({ url: url.trim() });
      setInspection(result);
      setDraft(normalizeDraft(result.suggested));
      setStep(2);
    } catch (error) {
      setInspection(null);
      setDraft(null);
      setInspectError(safeErrorMessage(error, 'Unable to inspect this Internet Archive item.'));
    }
  };

  const handleImageChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0] || null;
    setImage(file);
    setImagePreview(file ? URL.createObjectURL(file) : '');
  };

  const updateDraft = <K extends keyof ArchiveReciterDraft>(key: K, value: ArchiveReciterDraft[K]) => {
    setDraft((current) => (current ? updateDraftField(current, key, value) : current));
  };

  const handleImport = async () => {
    if (!inspection || !draft) return;

    setImportError('');
    setIsDuplicate(false);
    setStep(3);

    try {
      const result = await importMutation.mutateAsync({
        itemId: inspection.itemId,
        reciter: draft,
        image,
      });
      setImportedReciter(result.reciter);
      onImported?.(result.reciter);
    } catch (error) {
      const duplicate = errorStatus(error) === 409;
      setIsDuplicate(duplicate);
      setImportError(safeErrorMessage(error, 'Unable to import this Internet Archive item.'));
    }
  };

  const goBack = () => {
    setImportError('');
    setIsDuplicate(false);
    setStep((current) => (current === 3 ? 2 : 1));
  };

  const metadata = inspection?.metadata;
  const entries: ArchiveEntry[] = inspection?.entries || [];
  const missingIds = inspection?.missingIds || [];
  const matched = inspection?.matched || 0;
  const stepLabels = ['Inspect', 'Review', 'Import'];

  return (
    <Modal
      isOpen={isOpen}
      onClose={handleClose}
      title="Import from Internet Archive"
      description="Inspect an archive.org item, verify its surah mapping, then create the reciter in PostgreSQL."
      size="full"
      closeOnEscape
      closeOnBackdropClick
      className="flex flex-col"
    >
      <div className="min-h-0 flex-1 overflow-y-auto px-6 pb-6">
        <div className="grid grid-cols-3 gap-2 mb-6">
          {stepLabels.map((label, index) => {
            const stepNumber = (index + 1) as 1 | 2 | 3;
            const isCurrent = step === stepNumber;
            const isComplete = step > stepNumber;
            return (
              <div
                key={label}
                className={`flex items-center gap-2 rounded-xl border px-3 py-2.5 ${
                  isCurrent
                    ? 'border-emerald-500/30 bg-emerald-500/10'
                    : isComplete
                    ? 'border-emerald-500/20 bg-emerald-500/[0.06]'
                    : 'border-white/[0.08] bg-white/[0.02]'
                }`}
              >
                <span
                  className={`flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-[10px] font-mono ${
                    isCurrent
                      ? 'bg-emerald-500 text-black'
                      : isComplete
                      ? 'bg-emerald-500/20 text-emerald-300'
                      : 'bg-white/[0.08] text-white/40'
                  }`}
                >
                  {isComplete ? <Check className="h-3.5 w-3.5" /> : stepNumber}
                </span>
                <span className={`text-xs ${isCurrent ? 'font-semibold text-white' : 'text-white/45'}`}>{label}</span>
              </div>
            );
          })}
        </div>

        {step === 1 && (
          <div className="mx-auto max-w-3xl space-y-5 py-4">
            <div className="text-center space-y-2">
              <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400">
                <Link2 className="h-6 w-6" />
              </div>
              <h3 className="text-lg font-semibold text-white">Start with an Internet Archive item</h3>
              <p className="mx-auto max-w-xl text-sm leading-relaxed text-white/45">
                Paste an archive.org details link such as <span className="font-mono text-white/60">archive.org/details/item-id</span>, or paste the bare item ID.
              </p>
            </div>

            <GlassCard variant="soft" className="p-5">
              <div className="space-y-4">
                <Input
                  label="Archive.org URL or item ID"
                  value={url}
                  onChange={(event) => setUrl(event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter') void handleInspect();
                  }}
                  placeholder="archive.org/details/your-item-id"
                  helperText="The inspector will read the item metadata and compare its audio files with the 114-surah catalog."
                  error={inspectError || undefined}
                  autoFocus
                  leftIcon={<Search className="h-4 w-4" />}
                />
                <Button
                  variant="emerald"
                  size="md"
                  className="w-full sm:w-auto"
                  onClick={() => void handleInspect()}
                  loading={inspectMutation.isPending}
                  loadingText="Inspecting item..."
                  leftIcon={<Search className="h-4 w-4" />}
                >
                  Inspect
                </Button>
              </div>
            </GlassCard>

            {inspectError && (
              <div className="flex items-start gap-2.5 rounded-xl border border-rose-500/30 bg-rose-500/10 p-3.5 text-xs text-rose-200">
                <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
                <span>{inspectError}</span>
              </div>
            )}
          </div>
        )}

        {step >= 2 && inspection && draft && (
          <div className="space-y-5">
            {step === 2 && (
              <>
                <GlassCard variant="soft" className="p-5">
                  <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                    <div className="min-w-0 space-y-2">
                      <div className="flex flex-wrap items-center gap-2">
                        <h3 className="text-base font-semibold text-white">{metadata?.title || 'Untitled Internet Archive item'}</h3>
                        <Badge variant="ghost" size="sm" icon={<FileAudio className="h-3 w-3" />}>
                          {inspection.itemId}
                        </Badge>
                      </div>
                      <div className="flex flex-wrap items-center gap-3 text-xs text-white/50">
                        <span className="flex items-center gap-1.5"><Globe className="h-3.5 w-3.5 text-emerald-400" />{metadata?.creator || 'Creator not provided'}</span>
                        <span>{metadata?.language || 'Language not provided'}</span>
                      </div>
                    </div>
                    <Badge variant="cyan" size="sm">{inspection.scheme}</Badge>
                  </div>
                  <div className="mt-4 rounded-xl border border-white/[0.06] bg-black/20 p-3 text-xs leading-relaxed text-white/60">
                    {metadata?.description || 'No description provided by the archive item.'}
                  </div>
                </GlassCard>

                <div className="grid gap-4 md:grid-cols-2">
                  <GlassCard variant="inset" className="space-y-3 p-4">
                    <div className="flex items-center justify-between gap-3">
                      <span className="text-xs font-medium text-white/65">Filename scheme</span>
                      <Badge variant="cyan" size="sm">{inspection.scheme}</Badge>
                    </div>
                    <p className="text-xs leading-relaxed text-white/50">{schemeExplanations[inspection.scheme] || 'The archive layout could not be classified.'}</p>
                    <div className="flex flex-wrap gap-2 text-[10px] text-white/40">
                      <span className="rounded-lg border border-white/10 bg-white/[0.03] px-2 py-1 font-mono">{inspection.total || 0} archive files</span>
                      <span className="rounded-lg border border-white/10 bg-white/[0.03] px-2 py-1 font-mono">Archive.org</span>
                    </div>
                  </GlassCard>

                  <GlassCard variant="inset" className="space-y-3 p-4">
                    <div className="flex items-center justify-between gap-3">
                      <span className="text-xs font-medium text-white/65">Coverage</span>
                      <Badge variant={matched === 114 ? 'emerald' : 'amber'} size="sm">{matched} / 114</Badge>
                    </div>
                    <div className="flex items-end gap-2">
                      <span className="text-3xl font-semibold tracking-tight text-white">{matched}</span>
                      <span className="pb-1 text-xs text-white/40">matched surahs</span>
                    </div>
                    <p className="text-xs text-white/50">
                      {missingIds.length > 0 ? `${missingIds.length} surah${missingIds.length === 1 ? '' : 's'} missing: ${missingIds.join(', ')}` : 'All 114 surahs were matched.'}
                    </p>
                  </GlassCard>
                </div>

                {inspection.warnings?.length > 0 && (
                  <GlassCard variant="inset" className="space-y-3 border-amber-500/20 p-4">
                    <div className="flex items-center gap-2 text-xs font-semibold text-amber-200"><AlertCircle className="h-4 w-4" />Inspector warnings</div>
                    <ul className="space-y-1.5 text-xs text-amber-100/70">
                      {inspection.warnings.map((warning, index) => <li key={`${warning}-${index}`}>• {warning}</li>)}
                    </ul>
                  </GlassCard>
                )}

                <GlassCard variant="inset" className="overflow-hidden p-0">
                  <div className="flex flex-col gap-1 border-b border-white/[0.08] px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
                    <div>
                      <h3 className="text-sm font-semibold text-white">Surah mapping</h3>
                      <p className="text-xs text-white/40">Review every file before creating the reciter.</p>
                    </div>
                    <Badge variant="ghost" size="sm">{entries.length} matched files</Badge>
                  </div>
                  {entries.length > 0 ? (
                    <div className="max-h-72 overflow-auto">
                      <table className="w-full min-w-[680px] border-collapse text-left text-xs">
                        <thead className="sticky top-0 bg-[#141419] text-[10px] uppercase tracking-[0.16em] text-white/35">
                          <tr>
                            <th className="px-4 py-2.5 font-medium">ID</th>
                            <th className="px-4 py-2.5 font-medium">Canonical surah</th>
                            <th className="px-4 py-2.5 font-medium">Archive filename</th>
                            <th className="px-4 py-2.5 font-medium">Matched by</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-white/[0.06] text-white/65">
                          {entries.map((entry) => (
                            <tr key={`${entry.surahId}-${entry.fileName}`} className="transition-colors hover:bg-white/[0.03]">
                              <td className="px-4 py-2.5 font-mono text-emerald-300">{entry.surahId}</td>
                              <td className="px-4 py-2.5">
                                <div className="font-medium text-white/80">{entry.surahNameEn || 'Unnamed surah'}</div>
                                <div className="font-['Amiri',serif] text-[11px] text-white/40">{entry.surahNameAr || '—'}</div>
                              </td>
                              <td className="max-w-[280px] px-4 py-2.5 font-mono text-[11px] text-white/50"><span className="break-all">{entry.fileName || '—'}</span></td>
                              <td className="px-4 py-2.5"><Badge variant="ghost" size="sm">{entry.matchedBy || 'unknown'}</Badge></td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  ) : (
                    <div className="p-6 text-center text-xs text-white/40">No archive files were matched to the surah catalog.</div>
                  )}
                </GlassCard>

                <GlassCard variant="default" className="space-y-5 p-5">
                  <div className="flex items-center gap-2">
                    <Sparkles className="h-4 w-4 text-emerald-400" />
                    <div>
                      <h3 className="text-sm font-semibold text-white">Reciter profile</h3>
                      <p className="text-xs text-white/40">Suggested values are prefilled. Edit anything before importing.</p>
                    </div>
                  </div>

                  <div className="grid gap-4 md:grid-cols-2">
                    <Input label="English name" value={draft.name_en || ''} onChange={(event) => updateDraft('name_en', event.target.value)} placeholder="Reciter name in English" />
                    <Input label="Arabic name" value={draft.name_ar || ''} onChange={(event) => updateDraft('name_ar', event.target.value)} placeholder="اسم القارئ" dir="rtl" className="text-right font-['Amiri',serif]" />
                    <div>
                      <Input
                        label="Country"
                        list="archive-country-options"
                        value={draft.country || ''}
                        onChange={(event) => updateDraft('country', event.target.value)}
                        placeholder="Start typing a country"
                        leftIcon={draft.country ? <span>{countryFlag(draft.country)}</span> : undefined}
                        helperText={draft.country ? countryWithFlag(draft.country) : 'The matching flag appears automatically as you type.'}
                      />
                      <datalist id="archive-country-options">
                        {countryOptions().map((option) => (
                          <option key={option.name} value={option.name}>{countryWithFlag(option.name)}</option>
                        ))}
                      </datalist>
                    </div>
                    <Input label="Style" value={draft.style || ''} onChange={(event) => updateDraft('style', event.target.value)} placeholder="Murattal" />
                    <Input label="Riwayah" value={draft.riwayah || ''} onChange={(event) => updateDraft('riwayah', event.target.value)} placeholder="Hafs" />
                    <Input label="Tempo" value={draft.tempo || ''} onChange={(event) => updateDraft('tempo', event.target.value)} placeholder="medium" />
                  </div>

                  <div className="space-y-1.5">
                    <label htmlFor="archive-description" className="text-xs font-medium text-white/70">Description</label>
                    <textarea
                      id="archive-description"
                      rows={4}
                      value={draft.description || ''}
                      onChange={(event) => updateDraft('description', event.target.value)}
                      placeholder="Describe the recitation collection"
                      className="w-full resize-none rounded-xl border border-white/[0.08] bg-black/50 px-3.5 py-2.5 text-sm text-white outline-none transition-all placeholder:text-white/30 focus:border-white/30 focus:bg-black/70 focus:ring-1 focus:ring-white/20"
                    />
                  </div>

                  <div className="grid gap-4 sm:grid-cols-2">
                    <div className="space-y-1.5">
                      <label htmlFor="archive-server-url" className="text-xs font-medium text-white/70">Derived server_url</label>
                      <input id="archive-server-url" readOnly value={draft.server_url || ''} placeholder="No server URL provided" className="h-10 w-full rounded-xl border border-cyan-500/15 bg-cyan-500/[0.04] px-3.5 font-mono text-xs text-cyan-100/70 outline-none" />
                    </div>
                    <div className="space-y-1.5">
                      <label htmlFor="archive-available-surahs" className="text-xs font-medium text-white/70">Derived available_surahs</label>
                      <input id="archive-available-surahs" readOnly value={draft.available_surahs || ''} placeholder="Blank means all 114" className="h-10 w-full rounded-xl border border-cyan-500/15 bg-cyan-500/[0.04] px-3.5 font-mono text-xs text-cyan-100/70 outline-none" />
                    </div>
                  </div>

                  <div className="grid gap-3 sm:grid-cols-3">
                    {([
                      ['is_teacher', 'Teacher profile'],
                      ['verified', 'Verified'],
                      ['enabled', 'Enabled'],
                    ] as const).map(([key, label]) => (
                      <label key={key} className="flex cursor-pointer items-center gap-2.5 rounded-xl border border-white/[0.08] bg-white/[0.03] px-3 py-2.5 text-xs text-white/70 transition-colors hover:bg-white/[0.06]">
                        <input
                          type="checkbox"
                          checked={Boolean(draft[key])}
                          onChange={(event) => updateDraft(key, event.target.checked)}
                          className="h-4 w-4 rounded border-white/20 bg-black/40 accent-emerald-500"
                        />
                        {label}
                      </label>
                    ))}
                  </div>

                  <div className="space-y-3 border-t border-white/[0.08] pt-5">
                    <div className="flex items-center gap-2 text-xs font-semibold text-white/75"><ImageIcon className="h-4 w-4 text-cyan-400" />Reciter photo</div>
                    <div className="flex flex-col gap-4 sm:flex-row sm:items-center">
                      {imagePreview ? (
                        <img src={imagePreview} alt="Selected reciter preview" className="h-20 w-20 rounded-2xl border border-white/15 object-cover shadow-lg" />
                      ) : (
                        <div className="flex h-20 w-20 items-center justify-center rounded-2xl border border-dashed border-white/15 bg-white/[0.03] text-white/30"><ImageIcon className="h-7 w-7" /></div>
                      )}
                      <div className="flex flex-1 flex-col gap-2">
                        <label htmlFor="archive-photo" className="flex cursor-pointer items-center justify-center gap-2 rounded-xl border border-white/10 bg-white/[0.07] px-4 py-2.5 text-xs font-medium text-white transition-colors hover:bg-white/[0.12] hover:border-white/20">
                          <Upload className="h-4 w-4 text-emerald-400" />
                          {image ? 'Change photo' : 'Choose photo'}
                        </label>
                        <input id="archive-photo" type="file" accept="image/*" onChange={handleImageChange} className="sr-only" />
                        <span className="text-[11px] text-white/35">Optional. JPG, PNG, or WebP.</span>
                      </div>
                    </div>
                  </div>
                </GlassCard>
              </>
            )}

            {step === 3 && (
              <div className="mx-auto max-w-3xl space-y-5 py-4">
                {importedReciter ? (
                  <GlassCard variant="elevated" glow="emerald" className="space-y-5 p-7 text-center">
                    <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-emerald-500/15 text-emerald-300"><CheckCircle2 className="h-8 w-8" /></div>
                    <div className="space-y-2">
                      <Badge variant="emerald" size="sm" dot>Import complete</Badge>
                      <h3 className="text-xl font-semibold text-white">Reciter added to PostgreSQL</h3>
                      <p className="text-sm text-white/60">{importedReciter.nameEn}</p>
                      <p className="font-mono text-xs text-emerald-300">Slug: {importedReciter.slug}</p>
                    </div>
                    <div className="flex flex-col justify-center gap-3 sm:flex-row">
                      <a href={`${adminBasePath}/reciters`} className="chrome-btn inline-flex h-10 items-center justify-center gap-2 rounded-xl px-4 text-sm font-semibold text-zinc-950">
                        <ExternalLink className="h-4 w-4" />Open Reciters
                      </a>
                      <Button variant="emerald" onClick={handleClose}>Close</Button>
                    </div>
                  </GlassCard>
                ) : (
                  <>
                    <GlassCard variant="elevated" glow="emerald" className="space-y-5 p-6 text-center">
                      <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-emerald-500/10 text-emerald-400"><Upload className="h-6 w-6" /></div>
                      <div className="space-y-2">
                        <h3 className="text-lg font-semibold text-white">Ready to import this reciter?</h3>
                        <p className="text-sm text-white/45">The app will create a new reciter using the reviewed profile and archive audio mapping.</p>
                      </div>
                      <div className="grid gap-3 text-left sm:grid-cols-3">
                        <div className="rounded-xl border border-white/[0.08] bg-black/20 p-3"><div className="text-[10px] uppercase tracking-[0.16em] text-white/35">Reciter</div><div className="mt-1 truncate text-xs text-white/75">{draft.name_en || 'Unnamed reciter'}</div></div>
                        <div className="rounded-xl border border-white/[0.08] bg-black/20 p-3"><div className="text-[10px] uppercase tracking-[0.16em] text-white/35">Coverage</div><div className="mt-1 font-mono text-xs text-emerald-300">{matched} / 114 surahs</div></div>
                        <div className="rounded-xl border border-white/[0.08] bg-black/20 p-3"><div className="text-[10px] uppercase tracking-[0.16em] text-white/35">Archive</div><div className="mt-1 truncate font-mono text-xs text-cyan-300">{inspection.itemId}</div></div>
                      </div>
                    </GlassCard>

                    {importError && (
                      <div className={`flex items-start gap-2.5 rounded-xl border p-3.5 text-xs ${isDuplicate ? 'border-amber-500/30 bg-amber-500/10 text-amber-100' : 'border-rose-500/30 bg-rose-500/10 text-rose-200'}`}>
                        {isDuplicate ? <Info className="mt-0.5 h-4 w-4 shrink-0" /> : <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />}
                        <div className="space-y-1">
                          {isDuplicate && <div className="font-semibold">Reciter already exists</div>}
                          <div>{importError}</div>
                        </div>
                      </div>
                    )}
                  </>
                )}
              </div>
            )}
          </div>
        )}
      </div>

      <ModalFooter className="flex-wrap">
        {step === 1 && <Button variant="ghost" onClick={handleClose}>Cancel</Button>}
        {step > 1 && <Button variant="glass" onClick={goBack} leftIcon={<ArrowLeft className="h-4 w-4" />}>Back</Button>}
        {step === 2 && <Button variant="emerald" onClick={() => { setImportError(''); setIsDuplicate(false); setStep(3); }} rightIcon={<ArrowLeft className="h-4 w-4 rotate-180" />}>Continue to Import</Button>}
        {step === 3 && !importedReciter && <Button variant="emerald" onClick={() => void handleImport()} loading={importMutation.isPending} loadingText="Importing..." leftIcon={!importMutation.isPending ? <Upload className="h-4 w-4" /> : undefined}>Import to PostgreSQL</Button>}
      </ModalFooter>
    </Modal>
  );
};
