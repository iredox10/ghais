import React, { useState } from 'react';
import { ColumnDef } from '@tanstack/react-table';
import {
  BookOpen,
  Play,
  Sparkles,
  Globe,
  Volume2,
  CheckCircle2,
  Compass
} from 'lucide-react';
import { useSurahs } from '../lib/queryClient';
import { useAudioPlayer } from '../context/AudioPlayerContext';
import type { Surah } from '../api/types';
import { DataTable } from '../components/table/DataTable';
import { ColumnHeader } from '../components/table/ColumnHeader';
import { Badge } from '../components/ui/Badge';
import { GlassCard } from '../components/ui/GlassCard';
import { StatCard } from '../components/ui/StatCard';

interface ReciterOption {
  id: string;
  name: string;
  arabicName: string;
  serverUrl: string;
  serverBadge: string;
  slug: string;
}

const RECITERS: ReciterOption[] = [
  { id: 'mishary', name: 'Mishary Rashid Alafasy', arabicName: 'مشاري راشد العفاسي', serverUrl: 'https://server8.mp3quran.net/afs/', serverBadge: 'Mishary [Server 8]', slug: 'mishary' },
  { id: 'al-sudais', name: 'Abdurrahman As-Sudais', arabicName: 'عبد الرحمن السديس', serverUrl: 'https://server11.mp3quran.net/sds/', serverBadge: 'Al-Sudais [Server 11]', slug: 'al-sudais' },
  { id: 'al-zain', name: 'Al-Zain Muhammad Ahmad', arabicName: 'الزين محمد أحمد', serverUrl: 'https://server9.mp3quran.net/alzain/', serverBadge: 'Al-Zain [Sudan Server 9]', slug: 'al-zain' },
  { id: 'al-dokali', name: 'Al-Dokali Muhammad Al-Alem', arabicName: 'الدوكالي محمد العالم', serverUrl: 'https://server7.mp3quran.net/dokali/', serverBadge: 'Al-Dokali [Libya Server 7]', slug: 'al-dokali' },
  { id: 'al-azzazi', name: 'Al-Hussaini Al-Azzazi', arabicName: 'الحسيني العزازي', serverUrl: 'https://server8.mp3quran.net/3zazi/', serverBadge: 'Al-Azzazi [Server 8]', slug: 'al-azzazi' },
  { id: 'al-koushi', name: 'Al-Ouyoun Al-Koushi', arabicName: 'العيون الكوشي', serverUrl: 'https://server11.mp3quran.net/koshi/', serverBadge: 'Al-Koushi [Morocco Server 11]', slug: 'al-koushi' },
  { id: 'al-minshawi', name: 'Mohamed Siddiq Al-Minshawi', arabicName: 'محمد صديق المنشاوي', serverUrl: 'https://server10.mp3quran.net/minsh/', serverBadge: 'Al-Minshawi [Egypt Server 10]', slug: 'al-minshawi' },
];

export const Surahs: React.FC = () => {
  const { data, isLoading } = useSurahs();
  const { playTrack } = useAudioPlayer();
  const [selectedReciterId, setSelectedReciterId] = useState<string>('mishary');

  const surahs = data?.surahs || [];
  const meccanCount = surahs.filter(s => s.revelationType === 'Meccan').length || 86;
  const medinanCount = surahs.filter(s => s.revelationType === 'Medinan').length || 28;
  const totalVerses = surahs.reduce((acc, s) => acc + (s.ayahs || 0), 0) || 6236;

  const selectedReciter = RECITERS.find(r => r.id === selectedReciterId) || RECITERS[0];

  const columns: ColumnDef<Surah>[] = [
    {
      accessorKey: 'number',
      header: ({ column }) => <ColumnHeader column={column} title="No." />,
      cell: ({ row }) => {
        const s = row.original;
        return (
          <div className="w-8 h-8 rounded-xl bg-gradient-to-tr from-emerald-500/20 to-white/5 border border-white/10 flex items-center justify-center font-bold text-xs text-white shadow-specular">
            {s.number || s.id}
          </div>
        );
      },
      size: 70,
    },
    {
      accessorKey: 'nameEn',
      header: ({ column }) => <ColumnHeader column={column} title="Surah Title & Translation" />,
      cell: ({ row }) => {
        const s = row.original;
        return (
          <div className="flex flex-col">
            <span className="text-xs font-semibold text-white flex items-center gap-1.5">
              {s.nameEn}
            </span>
            <span className="text-[11px] text-white/50">{s.translation}</span>
          </div>
        );
      },
    },
    {
      accessorKey: 'nameAr',
      header: ({ column }) => <ColumnHeader column={column} title="Arabic Calligraphy" />,
      cell: ({ row }) => {
        const s = row.original;
        return (
          <span className="text-base font-['Amiri',serif] font-bold text-emerald-300">
            {s.nameAr}
          </span>
        );
      },
    },
    {
      accessorKey: 'revelationType',
      header: ({ column }) => <ColumnHeader column={column} title="Revelation Type" />,
      cell: ({ row }) => {
        const type = row.original.revelationType;
        const isMeccan = type === 'Meccan';
        return (
          <Badge
            variant={isMeccan ? 'emerald' : 'cyan'}
            className="text-[10px] font-medium"
          >
            {type}
          </Badge>
        );
      },
    },
    {
      accessorKey: 'ayahs',
      header: ({ column }) => <ColumnHeader column={column} title="Verses (Ayahs)" />,
      cell: ({ row }) => {
        const ayahs = row.original.ayahs;
        return (
          <span className="font-mono text-xs text-white/80 px-2 py-1 rounded-lg bg-white/[0.04] border border-white/10 inline-block">
            {ayahs} Verses
          </span>
        );
      },
    },
    {
      id: 'actions',
      header: ({ column }) => <ColumnHeader column={column} title="Action / Stream" />,
      cell: ({ row }) => {
        const s = row.original;
        const audioUrl = `${selectedReciter.serverUrl.replace(/\/$/, '')}/${String(s.number || s.id).padStart(3, '0')}.mp3`;

        return (
          <button
            onClick={() => playTrack({
              title: `${s.nameEn} (${s.nameAr})`,
              reciter: selectedReciter.name,
              audioUrl,
              surahNumber: s.number || s.id,
            })}
            className="chrome-btn px-3 py-1.5 text-[11px] rounded-lg flex items-center gap-1.5 bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-300 border border-emerald-500/20 transition-all cursor-pointer"
            title={`Play ${s.nameEn} by ${selectedReciter.name}`}
          >
            <Play className="w-3.5 h-3.5 fill-current text-emerald-400" />
            Play Surah
          </button>
        );
      },
    },
  ];

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2">
            <BookOpen className="w-6 h-6 text-emerald-400" />
            Surahs & Mushaf Explorer
          </h1>
          <p className="text-sm text-white/50 mt-1">
            Explore all 114 Surahs of the Noble Quran with Arabic calligraphy, revelation metadata, and direct audio streaming.
          </p>
          <p className="text-xs text-amber-300/80 mt-2">Admin reference catalog only. The mobile app uses bundled surah data; this view does not publish changes to devices.</p>
        </div>

        {/* Reciter Audio Switcher Dropdown */}
        <div className="flex items-center gap-2 bg-white/[0.04] border border-white/10 px-3 py-2 rounded-xl backdrop-blur-md self-start sm:self-auto">
          <Volume2 className="w-4 h-4 text-emerald-400 shrink-0" />
          <div className="flex flex-col">
            <span className="text-[10px] text-white/40 uppercase tracking-wider font-medium">Reciter Audio Switcher</span>
            <select
              value={selectedReciterId}
              onChange={(e) => setSelectedReciterId(e.target.value)}
              className="bg-transparent text-xs font-semibold text-white focus:outline-none cursor-pointer py-0.5 pr-2"
              title="Select Reciter Server"
            >
              {RECITERS.map((r) => (
                <option key={r.id} value={r.id} className="bg-zinc-900 text-white">
                  {r.name} ({r.serverBadge})
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {/* Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Total Surahs"
          value="114"
          delta="Complete Mushaf"
          deltaType="neutral"
          icon={<BookOpen className="w-4 h-4 text-emerald-400" />}
        />
        <StatCard
          title="Meccan Surahs"
          value={meccanCount.toString()}
          delta="Revealed in Makkah"
          deltaType="positive"
          icon={<Sparkles className="w-4 h-4 text-emerald-400" />}
        />
        <StatCard
          title="Medinan Surahs"
          value={medinanCount.toString()}
          delta="Revealed in Madinah"
          deltaType="neutral"
          icon={<Compass className="w-4 h-4 text-cyan-400" />}
        />
        <StatCard
          title="Total Verses (Ayahs)"
          value={totalVerses.toLocaleString()}
          delta="6,236 Total Ayahs"
          deltaType="positive"
          icon={<Volume2 className="w-4 h-4 text-amber-400" />}
        />
      </div>

      {/* DataTable */}
      <GlassCard className="p-6">
        <DataTable
          columns={columns}
          data={surahs}
          isLoading={isLoading}
          searchPlaceholder="Search surahs by English title, Arabic name, or translation..."
        />
      </GlassCard>
    </div>
  );
};
