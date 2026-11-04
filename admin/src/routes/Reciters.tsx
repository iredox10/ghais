import React, { useEffect, useState } from 'react';
import { ColumnDef } from '@tanstack/react-table';
import {
  Mic2,
  Globe,
  Plus,
  CheckCircle2,
  ExternalLink,
  Play,
  RefreshCw,
  Download,
  Pencil,
  Upload,
  Image as ImageIcon,
  Sparkles,
  Loader2,
} from 'lucide-react';
import {
  useReciters,
  useCreateReciter,
  useUpdateReciter,
  useUploadReciterImage,
  useSyncRecitersCatalog,
} from '../lib/queryClient';
import { countryFlag, countryOptions, countryWithFlag, splitCountryWithFlag } from '../lib/countries';
import { ArchiveImportModal } from '../components/reciters/ArchiveImportModal';
import { useAudioPlayer } from '../context/AudioPlayerContext';
import type { CreateReciterInput, Reciter } from '../api/types';
import { adminBasePath } from '../api/adminClient';
import { getFullSurahUrl, getFirstAvailableSurah } from '../lib/audioUrlResolver';import { DataTable } from '../components/table/DataTable';
import { ColumnHeader } from '../components/table/ColumnHeader';
import { Badge } from '../components/ui/Badge';
import { Modal } from '../components/ui/Modal';
import { GlassCard } from '../components/ui/GlassCard';
import { StatCard } from '../components/ui/StatCard';

const emptyForm = {
  slug: '',
  catalog: 'mp3quran',
  nameEn: '',
  nameAr: '',
  country: '',
  style: 'Murattal',
  riwayah: 'Hafs',
  tempo: 'medium',
  audioFolder: '',
  serverUrl: '',
  // Blank = all 114 (app's availableSurahList format: comma-separated IDs).
  availableSurahs: '',
  description: '',
  isTeacher: false,
  verified: true,
  enabled: true,
};

// Server shapes carry catalog identity (catalog/catalog_key in PostgreSQL);
// the shared Reciter type predates those columns, so widen locally.
type AdminReciter = Reciter & { catalog?: string; catalogKey?: string };

const inputClassName = 'px-3 py-2 rounded-xl bg-black/40 border border-white/10 text-xs text-white focus:outline-none focus:border-white/30';
const selectClassName = `${inputClassName} bg-black/40`;

export const Reciters: React.FC = () => {
  const { data, isLoading, error, refetch } = useReciters();
  const createReciterMutation = useCreateReciter();
  const updateReciterMutation = useUpdateReciter();
  const uploadImageMutation = useUploadReciterImage();
  const syncCatalogMutation = useSyncRecitersCatalog();
  const { playTrack } = useAudioPlayer();
  const [isEditorOpen, setIsEditorOpen] = useState(false);
  const [editingReciter, setEditingReciter] = useState<AdminReciter | null>(null);
  const [isArchiveImportOpen, setIsArchiveImportOpen] = useState(false);
  const [selectedRiwayah, setSelectedRiwayah] = useState('All');
  const [inspectedReciter, setInspectedReciter] = useState<AdminReciter | null>(null);
  const [isInspectModalOpen, setIsInspectModalOpen] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [imagePreview, setImagePreview] = useState('');
  const [formError, setFormError] = useState('');

  const reciters: AdminReciter[] = (data?.reciters as AdminReciter[] | undefined) || [];
  const totalCountries = new Set(reciters.map((reciter) => reciter.country).filter(Boolean)).size;
  const totalRiwayahs = new Set(reciters.map((reciter) => reciter.riwayah).filter(Boolean)).size;
  const isSaving =
    createReciterMutation.isPending ||
    updateReciterMutation.isPending ||
    uploadImageMutation.isPending;

  useEffect(() => {
    return () => {
      if (imagePreview.startsWith('blob:')) URL.revokeObjectURL(imagePreview);
    };
  }, [imagePreview]);

  const filteredReciters = reciters.filter((reciter) => {
    if (selectedRiwayah === 'All') return true;
    const riwayahValue = (reciter.riwayah || '').toLowerCase();
    const styleValue = (reciter.style || '').toLowerCase();
    const selectedValue = selectedRiwayah.toLowerCase();
    if (selectedRiwayah === "Mu'allim" || selectedRiwayah === 'Muallim') {
      return riwayahValue.includes('mu') || styleValue.includes('teacher') || styleValue.includes('muallim');
    }
    return riwayahValue.includes(selectedValue) || styleValue.includes(selectedValue);
  });

  const resetForm = () => {
    setForm(emptyForm);
    setImageFile(null);
    setImagePreview('');
    setFormError('');
    setEditingReciter(null);
  };

  const openCreateModal = () => {
    resetForm();
    setIsEditorOpen(true);
  };

  const openEditModal = (reciter: AdminReciter) => {
    setEditingReciter(reciter);
    setForm({
      slug: reciter.slug,
      catalog: reciter.catalog || 'mp3quran',
      nameEn: reciter.nameEn,
      nameAr: reciter.nameAr,
      country: splitCountryWithFlag(reciter.country || '').name,
      style: reciter.style || 'Murattal',
      riwayah: reciter.riwayah || 'Hafs',
      tempo: reciter.tempo || 'medium',
      audioFolder: reciter.audioFolder || '',
      serverUrl: reciter.serverUrl || '',
      // Legacy "1-114" range strings are not parseable by the app; edit as blank (= all).
      availableSurahs: !reciter.availableSurahs || reciter.availableSurahs === '1-114' ? '' : reciter.availableSurahs,
      description: reciter.description || '',
      isTeacher: reciter.isTeacher,
      verified: reciter.verified,
      enabled: reciter.enabled,
    });
    setImageFile(null);
    setImagePreview(reciter.imageUrl || '');
    setFormError('');
    setIsEditorOpen(true);
  };

  const updateForm = <K extends keyof typeof form>(key: K, value: (typeof form)[K]) => {
    setForm((current) => ({ ...current, [key]: value }));
  };

  const handleImageChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;
    if (imagePreview.startsWith('blob:')) URL.revokeObjectURL(imagePreview);
    setImageFile(file);
    setImagePreview(URL.createObjectURL(file));
  };

  const handleSaveReciter = async (event: React.FormEvent) => {
    event.preventDefault();
    setFormError('');

    if (!form.slug || !form.nameEn || !form.nameAr) return;

    // Send the form verbatim (including catalog). Leave audioFolder empty for
    // mp3quran rows — they stream via serverUrl and the app falls back to its
    // per-slug EveryAyah folder when empty. Never fabricate "<slug>_64kbps":
    // those folders don't exist on the CDN and would break per-ayah audio.
    // Blank availableSurahs = all 114 (comma-separated IDs otherwise).
    const payload: CreateReciterInput & { catalog: string } = {
      ...form,
      country: countryWithFlag(form.country),
    };

    try {
      const result = editingReciter
        ? await updateReciterMutation.mutateAsync({ id: editingReciter.id, data: payload })
        : await createReciterMutation.mutateAsync(payload);

      if (imageFile) {
        await uploadImageMutation.mutateAsync({ id: result.reciter.id, file: imageFile });
      }

      resetForm();
      setIsEditorOpen(false);
    } catch (saveError) {
      setFormError((saveError as Error).message || 'Unable to save reciter.');
    }
  };

  const columns: ColumnDef<AdminReciter>[] = [
    {
      accessorKey: 'nameEn',
      header: ({ column }) => <ColumnHeader column={column} title="Reciter Name" />,
      cell: ({ row }) => {
        const reciter = row.original;
        return (
          <div className="flex items-center gap-3">
            {reciter.imageUrl ? (
              <img
                src={reciter.imageUrl}
                alt={reciter.nameEn}
                className="w-9 h-9 rounded-xl object-cover border border-white/15"
              />
            ) : (
              <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-emerald-500/20 to-white/5 border border-white/10 flex items-center justify-center font-bold text-xs text-white shadow-specular">
                <Mic2 className="w-4 h-4 text-emerald-400" />
              </div>
            )}
            <div className="flex flex-col">
              <span className="text-xs font-semibold text-white flex items-center gap-1.5">
                {reciter.nameEn}
                {reciter.verified && <span title="Verified Reciter"><CheckCircle2 className="w-3 h-3 text-cyan-400" /></span>}
                <span title={`Catalog key: ${reciter.catalogKey || `${reciter.catalog || 'mp3quran'}:${reciter.slug}`}`} className="text-[9px] font-mono font-normal text-white/40 px-1.5 py-0.5 rounded-md bg-white/[0.04] border border-white/10">
                  {reciter.catalog || 'mp3quran'}
                </span>
              </span>
              <span className="text-[11px] text-white/50 font-['Amiri',serif]">{reciter.nameAr}</span>
            </div>
          </div>
        );
      },
    },
    {
      accessorKey: 'country',
      header: ({ column }) => <ColumnHeader column={column} title="Country & Flag" />,
      cell: ({ row }) => (
        <Badge variant="ghost" className="text-xs font-medium text-white/90 border-white/10 bg-white/[0.03] flex items-center gap-1.5 w-fit">
          <Globe className="w-3 h-3 text-emerald-400" />
          {row.original.country || 'Not set'}
        </Badge>
      ),
    },
    {
      accessorKey: 'riwayah',
      header: ({ column }) => <ColumnHeader column={column} title="Riwayah & Style" />,
      cell: ({ row }) => {
        const reciter = row.original;
        return (
          <div className="flex flex-col gap-1 items-start">
            <Badge variant="emerald" className="text-[10px] font-mono">
              {reciter.riwayah || 'Hafs'}
            </Badge>
            <span className="text-[11px] text-white/60">{reciter.style || 'Murattal'}</span>
          </div>
        );
      },
    },
    {
      accessorKey: 'serverUrl',
      header: ({ column }) => <ColumnHeader column={column} title="Audio Endpoint" />,
      cell: ({ row }) => {
        const serverUrl = row.original.serverUrl || '';
        let endpoint = 'EveryAyah';
        let variant: 'emerald' | 'cyan' | 'amber' | 'indigo' | 'ghost' = 'emerald';
        if (serverUrl.includes('server8')) { endpoint = 'Server 8'; variant = 'emerald'; }
        else if (serverUrl.includes('server11')) { endpoint = 'Server 11'; variant = 'cyan'; }
        else if (serverUrl.includes('server16')) { endpoint = 'Server 16'; variant = 'indigo'; }
        else if (serverUrl.includes('server7')) { endpoint = 'Server 7'; variant = 'amber'; }
        else if (serverUrl.includes('server10')) { endpoint = 'Server 10'; variant = 'emerald'; }
        else if (serverUrl.includes('server12')) { endpoint = 'Server 12'; variant = 'cyan'; }
        else if (serverUrl.includes('server13')) { endpoint = 'Server 13'; variant = 'indigo'; }
        else if (serverUrl.includes('server14')) { endpoint = 'Server 14'; variant = 'amber'; }
        else if (serverUrl.includes('archive.org')) { endpoint = 'Archive.org'; variant = 'ghost'; }

        return <Badge variant={variant} className="text-[10px] font-mono">{endpoint}</Badge>;
      },
    },
    {
      id: 'surahs',
      header: ({ column }) => <ColumnHeader column={column} title="Surahs Available" />,
      cell: ({ row }) => {
        const value = row.original.availableSurahs || '';
        const isAll = value === '' || value === '1-114';
        return (
          <span className="font-mono text-xs text-white/80 px-2 py-1 rounded-lg bg-white/[0.04] border border-white/10 inline-block">
            {isAll ? 'All 114' : value}
          </span>
        );
      },
    },
    {
      id: 'actions',
      header: ({ column }) => <ColumnHeader column={column} title="Actions" />,
      cell: ({ row }) => {
        const reciter = row.original;
        const { surahId, surahNameEn, surahNameAr } = getFirstAvailableSurah(reciter);
        const audioUrl = getFullSurahUrl(reciter, surahId);

        return (
          <div className="flex items-center gap-2">
            <button
              onClick={() => playTrack({
                title: `${surahNameEn} (${surahNameAr})`,
                reciter: reciter.nameEn,
                audioUrl,
                surahNumber: surahId,
              })}
              className="chrome-btn px-2.5 py-1.5 text-[11px] rounded-lg flex items-center gap-1 bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-300 border border-emerald-500/20 transition-all cursor-pointer"
              title={`Test Stream ${surahNameEn} for ${reciter.nameEn}`}
            >
              <Play className="w-3.5 h-3.5 fill-current text-emerald-400" />
              Audition
            </button>
            <button
              onClick={() => {
                setInspectedReciter(reciter);
                setIsInspectModalOpen(true);
              }}
              className="px-2.5 py-1.5 text-[11px] rounded-lg flex items-center gap-1 bg-white/[0.05] hover:bg-white/[0.1] text-white/80 hover:text-white border border-white/10 transition-all cursor-pointer"
              title={`Inspect details for ${reciter.nameEn}`}
            >
              <ExternalLink className="w-3.5 h-3.5" />
              Inspect
            </button>
            <button
              onClick={() => openEditModal(reciter)}
              className="px-2.5 py-1.5 text-[11px] rounded-lg flex items-center gap-1 bg-white/[0.05] hover:bg-white/[0.1] text-white/80 hover:text-white border border-white/10 transition-all cursor-pointer"
              title={`Edit ${reciter.nameEn}`}
            >
              <Pencil className="w-3.5 h-3.5" />
              Edit
            </button>
          </div>
        );
      },
    },
  ];

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2">
            <Mic2 className="w-6 h-6 text-emerald-400" />
            Reciters Directory & Performance
          </h1>
          <p className="text-sm text-white/50 mt-1">
            Live PostgreSQL catalog for Quran reciters, profile images, and audio metadata.
          </p>
        </div>

        <div className="flex items-center gap-3 self-start">
          <button
            onClick={() => syncCatalogMutation.mutateAsync()}
            disabled={syncCatalogMutation.isPending}
            className="chrome-btn px-4 py-2 text-xs rounded-xl flex items-center gap-1.5"
          >
            <RefreshCw className={`w-4 h-4 ${syncCatalogMutation.isPending ? 'animate-spin' : ''}`} />
            {syncCatalogMutation.isPending ? 'Syncing Catalog...' : 'Sync Catalog'}
          </button>

          <button
            onClick={() => setIsArchiveImportOpen(true)}
            className="chrome-btn px-4 py-2 text-xs rounded-xl flex items-center gap-1.5"
          >
            <Upload className="w-4 h-4 text-cyan-400" />
            Import from Internet Archive
          </button>

          <a
            href={`${adminBasePath}/api/export/reciters`}
            download="quranify_reciters_export.csv"
            className="chrome-btn px-4 py-2 text-xs rounded-xl flex items-center gap-1.5"
            title="Download Reciters CSV export"
          >
            <Download className="w-4 h-4 text-emerald-400" />
            Export CSV
          </a>

          <button
            onClick={openCreateModal}
            className="chrome-btn px-4 py-2 text-xs rounded-xl flex items-center gap-1.5"
          >
            <Plus className="w-4 h-4" />
            Add Reciter
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Total Reciters Indexed"
          value={(data?.total ?? reciters.length).toLocaleString()}
          delta="Live PostgreSQL documents"
          deltaType="positive"
          icon={<Mic2 className="w-4 h-4 text-emerald-400" />}
        />
        <StatCard
          title="Countries Represented"
          value={totalCountries.toString()}
          delta="International origins"
          deltaType="neutral"
          icon={<Globe className="w-4 h-4 text-amber-400" />}
        />
        <StatCard
          title="Riwayahs Covered"
          value={totalRiwayahs.toString()}
          delta="Hafs, Warsh, Qaloon, Al-Duri..."
          deltaType="positive"
          icon={<Sparkles className="w-4 h-4 text-indigo-400" />}
        />
        <StatCard
          title="Profile Images"
          value={reciters.filter((reciter) => reciter.imageUrl).length.toString()}
          delta="Uploaded to Cloudflare R2"
          deltaType="positive"
          icon={<ImageIcon className="w-4 h-4 text-cyan-400" />}
        />
      </div>

      <GlassCard className="p-6 flex flex-col gap-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="text-xs text-white/60 font-medium mr-1">Filter Riwayah:</span>
            {['All', 'Hafs', 'Warsh', 'Qaloon', 'Al-Duri', "Mu'allim"].map((chip) => {
              const isActive = selectedRiwayah === chip;
              return (
                <button
                  key={chip}
                  onClick={() => setSelectedRiwayah(chip)}
                  className={`px-3 py-1.5 rounded-xl text-xs font-medium transition-all ${
                    isActive
                      ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40 shadow-specular'
                      : 'bg-white/[0.03] text-white/70 border border-white/10 hover:bg-white/[0.08] hover:text-white'
                  }`}
                >
                  {chip}
                </button>
              );
            })}
          </div>
          <span className="text-xs text-white/50 font-mono">
            Showing {filteredReciters.length} of {reciters.length} reciters
          </span>
        </div>

        {error ? (
          <div className="rounded-xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-200">
            Unable to load the live reciter catalog. Check the PostgreSQL connection or run catalog sync.
          </div>
        ) : (
          <DataTable
            columns={columns}
            data={filteredReciters}
            isLoading={isLoading}
            searchPlaceholder="Search reciters by name, country, or slug..."
          />
        )}
      </GlassCard>

      <Modal
        isOpen={isInspectModalOpen}
        onClose={() => setIsInspectModalOpen(false)}
        title="Reciter Detailed Inspection"
        size="lg"
      >
        {inspectedReciter && (
          <div className="flex flex-col gap-6 py-2">
            <div className="flex items-center gap-4 p-4 rounded-xl bg-white/[0.03] border border-white/10">
              {inspectedReciter.imageUrl ? (
                <img
                  src={inspectedReciter.imageUrl}
                  alt={inspectedReciter.nameEn}
                  className="w-14 h-14 rounded-2xl object-cover border border-white/15"
                />
              ) : (
                <div className="w-14 h-14 rounded-2xl bg-gradient-to-tr from-emerald-500/20 to-white/5 border border-white/10 flex items-center justify-center">
                  <Mic2 className="w-7 h-7 text-emerald-400" />
                </div>
              )}
              <div className="flex flex-col">
                <span className="text-base font-bold text-white flex items-center gap-2">
                  {inspectedReciter.nameEn}
                  {inspectedReciter.verified && <span title="Verified Reciter"><CheckCircle2 className="w-4 h-4 text-cyan-400" /></span>}
                </span>
                <span className="text-sm text-white/60 font-['Amiri',serif]">{inspectedReciter.nameAr}</span>
                <span className="text-xs text-white/40 font-mono mt-1">Slug: {inspectedReciter.slug}</span>
                <span className="text-xs text-white/40 font-mono">Catalog: {inspectedReciter.catalog || 'mp3quran'} · Key: {inspectedReciter.catalogKey || `${inspectedReciter.catalog || 'mp3quran'}:${inspectedReciter.slug}`}</span>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="p-3 rounded-xl bg-black/40 border border-white/10 flex flex-col gap-1">
                <span className="text-[11px] text-white/50">Country & Origin</span>
                <span className="text-xs font-semibold text-white flex items-center gap-1.5">
                  <Globe className="w-3.5 h-3.5 text-emerald-400" />
                  {inspectedReciter.country || 'Not set'}
                </span>
              </div>
              <div className="p-3 rounded-xl bg-black/40 border border-white/10 flex flex-col gap-1">
                <span className="text-[11px] text-white/50">Riwayah & Style</span>
                <span className="text-xs font-semibold text-white">
                  {inspectedReciter.riwayah} ({inspectedReciter.style})
                </span>
              </div>
              <div className="p-3 rounded-xl bg-black/40 border border-white/10 flex flex-col gap-1">
                <span className="text-[11px] text-white/50">Audio Folder / Path</span>
                <span className="text-xs font-mono text-emerald-300 truncate">
                  {inspectedReciter.audioFolder || 'N/A'}
                </span>
              </div>
              <div className="p-3 rounded-xl bg-black/40 border border-white/10 flex flex-col gap-1">
                <span className="text-[11px] text-white/50">Server Stream URL</span>
                <span className="text-xs font-mono text-cyan-300 truncate" title={inspectedReciter.serverUrl}>
                  {inspectedReciter.serverUrl || 'Default EveryAyah CDN'}
                </span>
              </div>
              <div className="p-3 rounded-xl bg-black/40 border border-white/10 flex flex-col gap-1">
                <span className="text-[11px] text-white/50">Available Surahs</span>
                <span className="text-xs font-mono text-white">{!inspectedReciter.availableSurahs || inspectedReciter.availableSurahs === '1-114' ? 'All 114 (blank)' : inspectedReciter.availableSurahs}</span>
              </div>
              <div className="p-3 rounded-xl bg-black/40 border border-white/10 flex flex-col gap-1">
                <span className="text-[11px] text-white/50">Description</span>
                <span className="text-xs text-white/80">{inspectedReciter.description || 'No description'}</span>
              </div>
            </div>

            <div className="flex justify-end gap-3 pt-3 border-t border-white/[0.08]">
              <button
                onClick={() => {
                  const { surahId, surahNameEn, surahNameAr, audioUrl } = getFirstAvailableSurah(inspectedReciter);
                  playTrack({
                    title: `${surahNameEn} (${surahNameAr})`,
                    reciter: inspectedReciter.nameEn,
                    audioUrl,
                    surahNumber: surahId,
                  });
                }}
                className="chrome-btn px-4 py-2 text-xs rounded-xl flex items-center gap-1.5"
              >
                <Play className="w-4 h-4 fill-current text-emerald-400" />
                Audition Stream
              </button>
              <button
                onClick={() => {
                  setIsInspectModalOpen(false);
                  openEditModal(inspectedReciter);
                }}
                className="px-4 py-2 text-xs rounded-xl bg-white/[0.05] hover:bg-white/[0.1] text-white/80 flex items-center gap-1.5"
              >
                <Pencil className="w-4 h-4" />
                Edit
              </button>
              <button
                onClick={() => setIsInspectModalOpen(false)}
                className="px-4 py-2 text-xs rounded-xl bg-white/[0.05] hover:bg-white/[0.1] text-white/80"
              >
                Close
              </button>
            </div>
          </div>
        )}
      </Modal>

      <Modal
        isOpen={isEditorOpen}
        onClose={() => {
          if (!isSaving) {
            resetForm();
            setIsEditorOpen(false);
          }
        }}
        title={editingReciter ? 'Edit Reciter' : 'Add Reciter'}
        size="lg"
      >
        <form onSubmit={handleSaveReciter} className="flex flex-col gap-5 px-6 pb-6">
          {formError && (
            <div className="rounded-xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-xs text-rose-200">
              {formError}
            </div>
          )}

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="flex flex-col gap-1.5">
              <label className="text-xs text-white/70 font-medium">English Name</label>
              <input required value={form.nameEn} onChange={(event) => updateForm('nameEn', event.target.value)} placeholder="e.g. Idris Abkar" className={inputClassName} />
            </div>
            <div className="flex flex-col gap-1.5">
              <label className="text-xs text-white/70 font-medium">Arabic Name</label>
              <input required value={form.nameAr} onChange={(event) => updateForm('nameAr', event.target.value)} placeholder="e.g. إدريس أبكر" className={`${inputClassName} font-['Amiri',serif]`} />
            </div>
            <div className="flex flex-col gap-1.5">
              <label className="text-xs text-white/70 font-medium">Slug Key</label>
              <input required value={form.slug} onChange={(event) => updateForm('slug', slugInput(event.target.value))} placeholder="e.g. idris-abkar" className={`${inputClassName} font-mono`} />
            </div>
            <div className="flex flex-col gap-1.5">
              <label className="text-xs text-white/70 font-medium">Catalog</label>
              <select value={form.catalog} onChange={(event) => updateForm('catalog', event.target.value)} className={selectClassName}>
                <option value="mp3quran">mp3quran (full-surah streams)</option>
                <option value="everyayah">everyayah (ayah-by-ayah)</option>
              </select>
            </div>
            <div className="flex flex-col gap-1.5">
              <label className="text-xs text-white/70 font-medium">Country</label>
              <div className="flex items-center gap-2">
                <input
                  list="country-options"
                  value={form.country}
                  onChange={(event) => updateForm('country', event.target.value)}
                  placeholder="e.g. Yemen"
                  className={`${inputClassName} min-w-0 flex-1`}
                />
                {countryFlag(form.country) && (
                  <span className="shrink-0 rounded-xl border border-white/10 bg-white/[0.04] px-3 py-2 text-sm" title="Country flag">
                    {countryFlag(form.country)}
                  </span>
                )}
              </div>
              <datalist id="country-options">
                {countryOptions().map(({ name, flag }) => (
                  <option key={name} value={name}>
                    {flag} {name}
                  </option>
                ))}
              </datalist>
            </div>
            <div className="flex flex-col gap-1.5">
              <label className="text-xs text-white/70 font-medium">Recitation Style</label>
              <select value={form.style} onChange={(event) => updateForm('style', event.target.value)} className={selectClassName}>
                <option value="Murattal">Murattal</option>
                <option value="Mujawwad">Mujawwad</option>
                <option value="Muallim">Muallim</option>
              </select>
            </div>
            <div className="flex flex-col gap-1.5">
              <label className="text-xs text-white/70 font-medium">Riwayah</label>
              <select value={form.riwayah} onChange={(event) => updateForm('riwayah', event.target.value)} className={selectClassName}>
                <option value="Hafs">Hafs A'n Assem</option>
                <option value="Warsh">Warsh A'n Nafi'</option>
                <option value="Qaloon">Qaloon A'n Nafi'</option>
                <option value="Al-Douri">Al-Douri A'n Abi Amr</option>
              </select>
            </div>
            <div className="flex flex-col gap-1.5">
              <label className="text-xs text-white/70 font-medium">Tempo</label>
              <select value={form.tempo} onChange={(event) => updateForm('tempo', event.target.value)} className={selectClassName}>
                <option value="slow">Slow</option>
                <option value="medium">Medium</option>
                <option value="fast">Fast</option>
              </select>
            </div>
            <div className="flex flex-col gap-1.5">
              <label className="text-xs text-white/70 font-medium">Available Surahs</label>
              <input value={form.availableSurahs} onChange={(event) => updateForm('availableSurahs', event.target.value)} placeholder="Blank = all 114, or 1,2,3,..." className={`${inputClassName} font-mono`} />
            </div>
            <div className="flex flex-col gap-1.5">
              <label className="text-xs text-white/70 font-medium">EveryAyah Folder</label>
              <input value={form.audioFolder} onChange={(event) => updateForm('audioFolder', event.target.value)} placeholder="e.g. Idris_Abkar_64kbps" className={`${inputClassName} font-mono`} />
            </div>
            <div className="flex flex-col gap-1.5 sm:col-span-2">
              <label className="text-xs text-white/70 font-medium">Full-Surah Stream URL</label>
              <input value={form.serverUrl} onChange={(event) => updateForm('serverUrl', event.target.value)} placeholder="https://server8.mp3quran.net/afs/" className={`${inputClassName} font-mono`} />
            </div>
            <div className="flex flex-col gap-1.5 sm:col-span-2">
              <label className="text-xs text-white/70 font-medium">Description</label>
              <textarea value={form.description} onChange={(event) => updateForm('description', event.target.value)} placeholder="Short editorial description" rows={3} className={`${inputClassName} resize-y`} />
            </div>
          </div>

          <div className="flex flex-col gap-3 rounded-xl border border-white/10 bg-white/[0.03] p-4">
            <div className="flex items-center justify-between gap-4">
              <div>
                <p className="text-xs font-medium text-white">Profile image</p>
                <p className="text-[11px] text-white/45 mt-1">JPG, PNG, WebP, or AVIF. Maximum 5 MB.</p>
              </div>
              <label className="chrome-btn px-3 py-2 text-xs rounded-xl flex items-center gap-1.5 cursor-pointer">
                {uploadImageMutation.isPending ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Upload className="w-3.5 h-3.5" />}
                {imageFile ? 'Replace image' : 'Upload image'}
                <input type="file" accept="image/jpeg,image/png,image/webp,image/avif" onChange={handleImageChange} className="hidden" />
              </label>
            </div>
            <div className="flex items-center gap-3">
              {imagePreview ? (
                <img src={imagePreview} alt="Reciter preview" className="w-16 h-16 rounded-xl object-cover border border-white/15" />
              ) : (
                <div className="w-16 h-16 rounded-xl bg-black/40 border border-white/10 flex items-center justify-center">
                  <ImageIcon className="w-6 h-6 text-white/30" />
                </div>
              )}
              <div className="text-xs text-white/55 min-w-0">
                <p className="truncate">{imageFile?.name || (imagePreview ? 'Current PostgreSQL image' : 'No image selected')}</p>
                {imageFile && <p className="text-[11px] text-white/35 mt-1">The new image uploads after saving the reciter.</p>}
              </div>
            </div>
          </div>

          <div className="flex flex-wrap gap-5">
            <label className="flex items-center gap-2 text-xs text-white/70">
              <input type="checkbox" checked={form.verified} onChange={(event) => updateForm('verified', event.target.checked)} className="accent-emerald-400" />
              Verified
            </label>
            <label className="flex items-center gap-2 text-xs text-white/70">
              <input type="checkbox" checked={form.enabled} onChange={(event) => updateForm('enabled', event.target.checked)} className="accent-emerald-400" />
              Enabled
            </label>
            <label className="flex items-center gap-2 text-xs text-white/70">
              <input type="checkbox" checked={form.isTeacher} onChange={(event) => updateForm('isTeacher', event.target.checked)} className="accent-emerald-400" />
              Teacher / educational
            </label>
          </div>

          <div className="flex justify-end gap-3 pt-3 border-t border-white/[0.08]">
            <button
              type="button"
              onClick={() => {
                resetForm();
                setIsEditorOpen(false);
              }}
              disabled={isSaving}
              className="px-4 py-2 text-xs rounded-xl bg-white/[0.05] hover:bg-white/[0.1] text-white/80"
            >
              Cancel
            </button>
            <button type="submit" disabled={isSaving} className="chrome-btn px-5 py-2 text-xs rounded-xl flex items-center gap-1.5">
              {isSaving && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
              {isSaving ? 'Saving...' : editingReciter ? 'Save Changes' : 'Save Reciter'}
            </button>
          </div>
        </form>
      </Modal>

      <ArchiveImportModal
        isOpen={isArchiveImportOpen}
        onClose={() => setIsArchiveImportOpen(false)}
        onImported={() => {
          void refetch();
        }}
      />
    </div>
  );
};

function slugInput(value: string) {
  return value.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '');
}
