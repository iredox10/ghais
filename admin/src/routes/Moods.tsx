import React from 'react';
import { ColumnDef } from '@tanstack/react-table';
import {
  Smile,
  LayoutGrid,
  Tags,
  Radio,
  Mic2,
} from 'lucide-react';
import { useMoods } from '../lib/queryClient';
import type { Mood } from '../api/types';
import { DataTable } from '../components/table/DataTable';
import { ColumnHeader } from '../components/table/ColumnHeader';
import { Badge } from '../components/ui/Badge';
import { GlassCard } from '../components/ui/GlassCard';
import { StatCard } from '../components/ui/StatCard';

// Contract (shared types + real hook now live):
//   useMoods() -> react-query result with
//     data: MoodsResponse | undefined
// ---------------------------------------------------------------------------

export const Moods: React.FC = () => {
  const { data, isLoading } = useMoods();

  const moods = data?.moods ?? [];
  const total = data?.total ?? moods.length ?? 0;
  const source = data?.source ?? 'static';
  const isLive = source === 'live';

  const categories = React.useMemo(
    () => Array.from(new Set(moods.map((m) => m.category).filter(Boolean))),
    [moods]
  );
  const reciters = React.useMemo(
    () =>
      Array.from(
        new Set(moods.map((m) => m.default_reciter_slug).filter(Boolean))
      ),
    [moods]
  );

  const columns: ColumnDef<Mood>[] = [
    {
      accessorKey: 'slug',
      header: ({ column }) => <ColumnHeader column={column} title="Slug & Cover" />,
      cell: ({ row }) => {
        const m = row.original;
        return (
          <div className="flex items-center gap-2.5 min-w-0">
            {m.cover_url ? (
              <img
                src={m.cover_url}
                alt={m.title}
                loading="lazy"
                className="w-9 h-9 rounded-xl object-cover border border-white/10 bg-white/[0.04] shrink-0"
              />
            ) : (
              <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-emerald-500/20 to-white/5 border border-white/10 flex items-center justify-center font-bold text-xs text-white shadow-specular shrink-0">
                {(m.slug || '?').slice(0, 1).toUpperCase()}
              </div>
            )}
            <span className="font-mono text-xs text-white/80 px-2 py-1 rounded-lg bg-white/[0.04] border border-white/10 inline-block truncate">
              {m.slug}
            </span>
          </div>
        );
      },
    },
    {
      accessorKey: 'title',
      header: ({ column }) => <ColumnHeader column={column} title="Title & Subtitle" />,
      cell: ({ row }) => {
        const m = row.original;
        return (
          <div className="flex flex-col min-w-0">
            <span className="text-xs font-semibold text-white flex items-center gap-1.5 truncate">
              {m.title}
            </span>
            <span className="text-[11px] text-white/50 truncate">{m.subtitle}</span>
          </div>
        );
      },
    },
    {
      accessorKey: 'category',
      header: ({ column }) => <ColumnHeader column={column} title="Category" />,
      cell: ({ row }) => (
        <Badge variant="cyan" className="text-[10px] font-medium">
          {row.original.category}
        </Badge>
      ),
    },
    {
      accessorKey: 'default_reciter_slug',
      header: ({ column }) => (
        <ColumnHeader column={column} title="Default Reciter" />
      ),
      cell: ({ row }) => (
        <span className="font-mono text-xs text-white/80 px-2 py-1 rounded-lg bg-white/[0.04] border border-white/10 inline-block">
          {row.original.default_reciter_slug}
        </span>
      ),
    },
    {
      accessorKey: 'default_ambient',
      header: ({ column }) => <ColumnHeader column={column} title="Ambient" />,
      cell: ({ row }) => (
        <Badge variant="ghost" className="text-[10px] font-medium">
          {row.original.default_ambient}
        </Badge>
      ),
    },
    {
      id: 'source',
      header: ({ column }) => <ColumnHeader column={column} title="Source" />,
      cell: () => (
        <Badge
          variant={isLive ? 'emerald' : 'amber'}
          size="sm"
          dot
          className="text-[10px] font-medium"
        >
          {isLive ? 'live' : 'static'}
        </Badge>
      ),
    },
  ];

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2">
            <Smile className="w-6 h-6 text-emerald-400" />
            Moods & Vibes Explorer
          </h1>
          <p className="text-sm text-white/50 mt-1">
            Browse the 6 curated listening moods — each with its default reciter, ambient pairing, and cover art.
          </p>
        </div>

        {/* Live / static source badge */}
        <div className="flex items-center gap-2 bg-white/[0.04] border border-white/10 px-3 py-2 rounded-xl backdrop-blur-md self-start sm:self-auto">
          <Radio className="w-4 h-4 text-emerald-400 shrink-0" />
          <div className="flex flex-col gap-1">
            <span className="text-[10px] text-white/40 uppercase tracking-wider font-medium">
              Moods API Source
            </span>
            <Badge
              variant={isLive ? 'emerald' : 'amber'}
              size="sm"
              dot
              className="text-[10px] font-medium self-start"
            >
              {isLive ? 'live' : 'static'}
            </Badge>
          </div>
        </div>
      </div>

      {/* Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Total Moods"
          value={total ? String(total) : '6'}
          delta="Curated collection"
          deltaType="neutral"
          icon={<LayoutGrid className="w-4 h-4 text-emerald-400" />}
        />
        <StatCard
          title="Categories"
          value={categories.length ? String(categories.length) : '—'}
          delta={categories.slice(0, 2).join(' • ') || 'Mood groupings'}
          deltaType="neutral"
          icon={<Tags className="w-4 h-4 text-cyan-400" />}
        />
        <StatCard
          title="Default Reciters"
          value={reciters.length ? String(reciters.length) : '—'}
          delta="Paired voices"
          deltaType="neutral"
          icon={<Mic2 className="w-4 h-4 text-amber-400" />}
        />
        <StatCard
          title="API Source"
          value={isLive ? 'Live' : 'Static'}
          delta={isLive ? 'GET /api/moods' : 'Bundled fallback'}
          deltaType={isLive ? 'positive' : 'neutral'}
          icon={<Radio className="w-4 h-4 text-emerald-400" />}
        />
      </div>

      {/* Mood cover cards */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-4">
        {moods.map((m) => (
          <GlassCard key={m.slug} className="p-3 flex flex-col gap-2" interactive>
            {m.cover_url ? (
              <img
                src={m.cover_url}
                alt={m.title}
                loading="lazy"
                className="w-full aspect-square rounded-xl object-cover border border-white/10 bg-white/[0.04]"
              />
            ) : (
              <div className="w-full aspect-square rounded-xl bg-gradient-to-tr from-emerald-500/20 to-white/5 border border-white/10 flex items-center justify-center">
                <Smile className="w-6 h-6 text-emerald-400" />
              </div>
            )}
            <div className="flex flex-col min-w-0 px-0.5 pb-1">
              <span className="text-xs font-semibold text-white truncate">{m.title}</span>
              <span className="text-[11px] text-white/50 truncate">{m.subtitle}</span>
              <span className="font-mono text-[10px] text-white/40 truncate mt-0.5">
                {m.slug}
              </span>
            </div>
          </GlassCard>
        ))}
      </div>

      {/* DataTable */}
      <GlassCard className="p-6">
        <DataTable
          columns={columns}
          data={moods}
          isLoading={isLoading}
          searchPlaceholder="Search moods by slug, title, category, reciter, or ambient..."
        />
      </GlassCard>
    </div>
  );
};
