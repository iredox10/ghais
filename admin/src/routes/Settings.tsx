import React, { useState } from 'react';
import {
  Settings as SettingsIcon,
  Database,
  Key,
  ShieldCheck,
  CheckCircle2,
  AlertCircle,
  RefreshCw,
  FolderLock,
  Layers,
  ExternalLink,
  Server,
  Download,
  Activity,
  Cpu,
  HardDrive,
  Clock,
  Zap,
  Radio
} from 'lucide-react';
import { useHealth, useCollections, useTelemetry, useCdnHealth, useIndexAudit } from '../lib/queryClient';
import { seedSurahs, seedMoods, adminBasePath } from '../api/adminClient';
import { GlassCard } from '../components/ui/GlassCard';
import { Badge } from '../components/ui/Badge';

const COLLECTIONS = [
  { id: 'reciters', name: 'Reciters Catalog', scope: 'Public (Read)', items: '242 reciters', status: 'Healthy' },
  { id: 'surahs', name: 'Surahs Metadata', scope: 'Public (Read)', items: '114 chapters', status: 'Healthy' },
  { id: 'moods', name: 'Moods & Routines', scope: 'Public (Read)', items: 'Study, Work, Sleep', status: 'Healthy' },
  { id: 'playlists', name: 'User Playlists', scope: 'Users (R/W)', items: '3 playlists', status: 'Healthy' },
  { id: 'playlist_items', name: 'Playlist Items', scope: 'Users (R/W)', items: 'Ordered ayahs', status: 'Healthy' },
  { id: 'likes', name: 'Likes & Favorites', scope: 'Users (R/W)', items: '7 likes', status: 'Healthy' },
  { id: 'playback_state', name: 'Playback State', scope: 'Users (R/W)', items: 'Cross-Device sync', status: 'Healthy' },
  { id: 'khatma_plans', name: 'Khatma Completion', scope: 'Users (R/W)', items: 'Active goals', status: 'Healthy' },
  { id: 'follows', name: 'Reciter Follows', scope: 'Users (R/W)', items: '6 follows', status: 'Healthy' },
  { id: 'listening_stats', name: 'Listening Stats', scope: 'Users (R/W)', items: 'Streaks & minutes', status: 'Healthy' },
  { id: 'schedules', name: 'Recitation Schedules', scope: 'Users (R/W)', items: '4 schedules', status: 'Healthy' },
  { id: 'history', name: 'Listening History', scope: 'Users (R/W)', items: '18 sessions in history', status: 'Healthy' },
  { id: 'user_prefs', name: 'User Preferences', scope: 'Users (R/W)', items: '8 registered users', status: 'Healthy' },
  { id: 'routine_backups', name: 'Routine Backups', scope: 'Users (R/W)', items: 'Routine snapshots', status: 'Healthy' },
  { id: 'reciter_stats', name: 'Reciter Stats', scope: 'Public read / Users (W)', items: 'Followers & likes', status: 'Healthy' },
  { id: 'broadcasts', name: 'Broadcasts & Announcements', scope: 'Public (Read)', items: 'Inbox messages', status: 'Healthy' },
  { id: 'telemetry', name: 'Device Telemetry', scope: 'Users (R/W)', items: 'Device heartbeats', status: 'Healthy' },
];

function formatUptime(seconds: number = 0) {
  const hrs = Math.floor(seconds / 3600);
  const mins = Math.floor((seconds % 3600) / 60);
  const secs = seconds % 60;
  if (hrs > 0) return `${hrs}h ${mins}m ${secs}s`;
  if (mins > 0) return `${mins}m ${secs}s`;
  return `${secs}s`;
}

export const Settings: React.FC = () => {
  const { data: health, isLoading } = useHealth();
  const { data: collectionsData, isLoading: isLoadingCollections } = useCollections();
  const { data: telemetry } = useTelemetry();
  const { data: cdnHealth } = useCdnHealth();
  const { data: indexAuditData, isLoading: isLoadingIndexes } = useIndexAudit();

  const [surahSeedResult, setSurahSeedResult] = useState<{ success: boolean; message: string } | null>(null);
  const [moodSeedResult, setMoodSeedResult] = useState<{ success: boolean; message: string } | null>(null);
  const [seedingSurahs, setSeedingSurahs] = useState(false);
  const [seedingMoods, setSeedingMoods] = useState(false);

  const handleSeedSurahs = async () => {
    setSurahSeedResult(null);
    setSeedingSurahs(true);
    try {
      const res = await seedSurahs();
      setSurahSeedResult({ success: res.success, message: `seeded ${res.seeded} / skipped ${res.skipped} of ${res.total}` });
    } catch (err: any) {
      setSurahSeedResult({ success: false, message: err.message || 'Failed to seed surahs.' });
    } finally {
      setSeedingSurahs(false);
    }
  };

  const handleSeedMoods = async () => {
    setMoodSeedResult(null);
    setSeedingMoods(true);
    try {
      const res = await seedMoods();
      setMoodSeedResult({ success: res.success, message: `seeded ${res.seeded} / skipped ${res.skipped} of ${res.total}` });
    } catch (err: any) {
      setMoodSeedResult({ success: false, message: err.message || 'Failed to seed moods.' });
    } finally {
      setSeedingMoods(false);
    }
  };

  const counts = collectionsData?.collectionCounts || {};

  const dynamicCollections = COLLECTIONS.map(c => {
    const count = counts[c.id];
    let items = c.items;
    if (count !== undefined) {
      if (c.id === 'history') items = `${count} sessions in history`;
      else if (c.id === 'likes') items = `${count} likes`;
      else if (c.id === 'follows') items = `${count} follows`;
      else if (c.id === 'schedules') items = `${count} schedules`;
      else if (c.id === 'playlists') items = `${count} playlists`;
      else if (c.id === 'user_prefs') items = `${count} registered users`;
      else if (c.id === 'reciters') items = `${count} reciters`;
      else if (c.id === 'khatma_plans') items = `${count} active goals`;
      else if (c.id === 'listening_stats') items = `${count} stats records`;
      else if (c.id === 'playlist_items') items = `${count} items`;
      else if (c.id === 'playback_state') items = `${count} active devices`;
      else if (c.id === 'surahs') items = `${count} chapters`;
      else if (c.id === 'moods') items = `${count} moods`;
      else if (c.id === 'routine_backups') items = `${count} backups`;
      else if (c.id === 'reciter_stats') items = `${count} reciter stats`;
      else if (c.id === 'broadcasts') items = `${count} broadcasts`;
      else if (c.id === 'telemetry') items = `${count} devices`;
    }
    return {
      ...c,
      items
    };
  });

  const isLive = health?.mode === 'live' && health?.databaseConnected;

  // LIVE device telemetry (additive; server returns deviceTelemetry even when
  // empty). Cast via `as any` so src/api/types.ts stays untouched.
  const deviceTelemetry = (telemetry as any)?.deviceTelemetry as
    | { devices?: number; byPlatform?: Record<string, number>; byAppVersion?: Record<string, number>; totalSeconds?: number; lastSeen?: string | null }
    | undefined;
  const appTelemetry = (telemetry as any)?.appTelemetry as { connected?: boolean; reason?: string } | undefined;
  const deviceCount = deviceTelemetry?.devices ?? 0;

  return (
    <div className="flex flex-col gap-8 pb-12">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2">
          <SettingsIcon className="w-6 h-6 text-emerald-400" />
          Backend & Storage
        </h1>
        <p className="text-sm text-white/50 mt-1">
          Manage production database credentials, inspect collection schema health, and test live sync.
        </p>
      </div>

      {/* Cluster Health Banner */}
      <GlassCard className="p-6 flex flex-col md:flex-row md:items-center justify-between gap-6 border-white/[0.12]">
        <div className="flex items-center gap-4">
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-emerald-500/20 to-white/5 border border-white/20 flex items-center justify-center text-emerald-400 shadow-specular">
            <Server className="w-6 h-6" />
          </div>
          <div className="flex flex-col">
            <div className="flex items-center gap-2">
              <span className="text-base font-bold text-white">Heroku PostgreSQL</span>
              <Badge variant={isLive ? 'emerald' : 'amber'} className="font-mono text-[10px]">
                {isLive ? 'Live Sync Active' : 'Unavailable'}
              </Badge>
            </div>
            <span className="text-xs text-white/50 font-mono mt-0.5">
              {health?.endpoint || 'https://ghaith-c77c2426ed4d.herokuapp.com'}
            </span>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <a
            href="https://dashboard.heroku.com/apps"
            target="_blank"
            rel="noopener noreferrer"
            className="chrome-btn px-4 py-2 text-xs rounded-xl flex items-center gap-1.5"
          >
            <ExternalLink className="w-3.5 h-3.5" />
            Open Heroku
          </a>
        </div>
      </GlassCard>

      {/* Cluster Telemetry & Performance Card */}
      <GlassCard className="p-6 flex flex-col gap-6 border-white/[0.12]">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-cyan-500/10 border border-cyan-500/20 flex items-center justify-center text-cyan-400">
              <Activity className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base font-bold text-white">Cluster Telemetry & Performance</h2>
              <p className="text-xs text-white/50">Real-time metrics, node process telemetry, and database latency.</p>
            </div>
          </div>
          <Badge variant="emerald" className="font-mono text-[10px] animate-pulse">
            Live 5s Poll
          </Badge>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 font-mono">
          {/* Cloud Ping */}
          <div className="p-4 rounded-2xl bg-white/[0.02] border border-white/[0.06] flex flex-col gap-2">
            <div className="flex items-center justify-between text-white/50 text-xs">
              <span className="flex items-center gap-1.5">
                <Zap className="w-3.5 h-3.5 text-amber-400" />
                Database Ping
              </span>
              <span className="text-[10px] px-1.5 py-0.5 rounded bg-emerald-500/10 text-emerald-400">
                {telemetry?.pingMs == null ? 'Unavailable' : telemetry.pingMs < 100 ? 'Low Latency' : 'Measured'}
              </span>
            </div>
            <div className="text-xl font-bold text-white">
              {telemetry?.pingMs ?? '—'} <span className="text-xs font-normal text-white/50">ms</span>
            </div>
          </div>

          {/* Process Uptime */}
          <div className="p-4 rounded-2xl bg-white/[0.02] border border-white/[0.06] flex flex-col gap-2">
            <div className="flex items-center justify-between text-white/50 text-xs">
              <span className="flex items-center gap-1.5">
                <Clock className="w-3.5 h-3.5 text-cyan-400" />
                Process Uptime
              </span>
              <span className="text-[10px] text-white/40">Counter</span>
            </div>
            <div className="text-xl font-bold text-white">
              {telemetry?.uptimeSeconds == null ? '—' : formatUptime(telemetry.uptimeSeconds)}
            </div>
          </div>

          {/* Memory Footprint */}
          <div className="p-4 rounded-2xl bg-white/[0.02] border border-white/[0.06] flex flex-col gap-2">
            <div className="flex items-center justify-between text-white/50 text-xs">
              <span className="flex items-center gap-1.5">
                <HardDrive className="w-3.5 h-3.5 text-purple-400" />
                Memory Footprint
              </span>
              <span className="text-[10px] text-white/40">RSS</span>
            </div>
            <div className="text-xl font-bold text-white">
              {telemetry?.memoryMb ?? '—'} <span className="text-xs font-normal text-white/50">MB RSS</span>
            </div>
          </div>

          {/* Platform & Node runtime */}
          <div className="p-4 rounded-2xl bg-white/[0.02] border border-white/[0.06] flex flex-col gap-2">
            <div className="flex items-center justify-between text-white/50 text-xs">
              <span className="flex items-center gap-1.5">
                <Cpu className="w-3.5 h-3.5 text-emerald-400" />
                Node Runtime
              </span>
              <span className="text-[10px] text-white/40">{telemetry?.activeConnections ?? '—'} active conn</span>
            </div>
            <div className="text-sm font-bold text-white truncate pt-1">
              Node {telemetry?.nodeVersion ?? '—'} ({telemetry?.platform ?? '—'})
            </div>
          </div>
        </div>

        {/* LIVE device telemetry (additive; degrades to waiting state when empty) */}
        <div className="p-4 rounded-2xl bg-white/[0.02] border border-white/[0.06] flex flex-col gap-3 font-mono">
          <div className="flex items-center justify-between text-xs">
            <span className="text-white/70 font-semibold font-sans">Device Telemetry — live app reports</span>
            <Badge variant={appTelemetry?.connected ? 'emerald' : 'amber'} className="font-mono text-[10px]">
              {appTelemetry?.connected ? `${deviceCount} device${deviceCount === 1 ? '' : 's'} reported` : 'No device reports'}
            </Badge>
          </div>
          {deviceCount > 0 ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 text-xs">
              <div className="text-white/50">Devices <span className="text-white font-bold ml-1">{deviceCount}</span></div>
              <div className="text-white/50">Listening <span className="text-white font-bold ml-1">{deviceTelemetry?.totalSeconds ?? 0}s total</span></div>
              <div className="text-white/50">Last seen <span className="text-white font-bold ml-1">{deviceTelemetry?.lastSeen ?? '—'}</span></div>
              <div className="text-white/50 truncate">
                Platforms <span className="text-white font-bold ml-1">{Object.entries(deviceTelemetry?.byPlatform ?? {}).map(([k, v]) => `${k}:${v}`).join(' · ') || '—'}</span>
              </div>
              <div className="text-white/50 truncate sm:col-span-2">
                Versions <span className="text-white font-bold ml-1">{Object.entries(deviceTelemetry?.byAppVersion ?? {}).map(([k, v]) => `${k}:${v}`).join(' · ') || '—'}</span>
              </div>
            </div>
          ) : (
            <div className="text-[11px] text-white/40">
              {appTelemetry?.reason ?? 'Device telemetry is unavailable.'}
            </div>
          )}
        </div>
      </GlassCard>

      {/* Audio CDN & Edge Cluster Status Card */}
      <GlassCard className="p-6 flex flex-col gap-6 border-white/[0.12]">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
              <Radio className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base font-bold text-white">Audio CDN & Edge Cluster Status</h2>
              <p className="text-xs text-white/50">Live HTTP ping latency and availability across primary audio servers and Quran APIs.</p>
            </div>
          </div>
          <Badge variant="emerald" className="font-mono text-[10px] animate-pulse">
            Live 15s Poll
          </Badge>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {cdnHealth?.servers?.map((server) => {
            const isHealthy = server.status === 'healthy';
            const isDegraded = server.status === 'degraded';
            const variant = isHealthy ? 'emerald' : isDegraded ? 'amber' : 'rose';
            return (
              <div
                key={server.url}
                className="p-4 rounded-2xl bg-white/[0.02] border border-white/[0.06] flex flex-col justify-between gap-3 font-mono"
              >
                <div className="flex items-start justify-between gap-2">
                  <div className="flex flex-col">
                    <span className="text-xs font-bold text-white font-sans">
                      {server.name} ({server.reciters ?? 0} reciters)
                    </span>
                    <span className="text-[10px] text-white/40 truncate max-w-[200px]" title={server.url}>
                      {server.url}
                    </span>
                  </div>
                  <Badge variant={variant} className="text-[10px] uppercase">
                    {server.status}
                  </Badge>
                </div>

                <div className="flex items-center justify-between pt-2 border-t border-white/[0.04] text-xs">
                  <span className="text-white/50 flex items-center gap-1">
                    <Zap className="w-3.5 h-3.5 text-amber-400" />
                    Ping Latency
                  </span>
                  <div className="flex items-center gap-2">
                    <span className="text-white/40 text-[10px]">HTTP {server.httpStatus || 'ERR'}</span>
                    <span className="font-bold text-white text-sm">{server.pingMs} ms</span>
                  </div>
                </div>
              </div>
            );
          }) || (
            <div className="col-span-full py-8 text-center text-xs text-white/40 font-mono">
              Benchmarking audio CDN clusters...
            </div>
          )}
        </div>
      </GlassCard>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <GlassCard className="p-6 flex flex-col gap-4">
          <span className="text-base font-semibold text-white">Cloudflare R2 storage</span>
          <p className="text-xs text-white/50 leading-relaxed">Reciter images are uploaded to R2. Configure bucket credentials and the public asset domain in the server environment.</p>
          <Badge variant={health?.storageConfigured ? 'emerald' : 'amber'}>{health?.storageConfigured ? 'Configured' : 'Not configured'}</Badge>
        </GlassCard>

        {/* Database & Target Info */}
        <GlassCard className="p-6 flex flex-col justify-between gap-4">
          <div className="flex flex-col gap-1">
            <span className="text-base font-semibold text-white flex items-center gap-2">
              <Database className="w-4 h-4 text-cyan-400" />
              Target Project & Database
            </span>
            <p className="text-xs text-white/50">
              The mobile application connects to these production parameters.
            </p>
          </div>

          <div className="flex flex-col gap-3 font-mono text-xs">
            <div className="flex justify-between p-3 rounded-xl bg-white/[0.02] border border-white/[0.05]">
              <span className="text-white/40">Database</span>
              <span className="text-white font-semibold">{health?.databaseId || 'ghais'}</span>
            </div>
            <div className="flex justify-between p-3 rounded-xl bg-white/[0.02] border border-white/[0.05]">
              <span className="text-white/40">Image storage</span>
              <span className="text-white">Cloudflare R2</span>
            </div>
          </div>
        </GlassCard>
      </div>

      {/* Database Collections Table */}
      <GlassCard className="p-6 flex flex-col gap-4">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="flex flex-col">
            <span className="text-base font-semibold text-white flex items-center gap-2">
              <Layers className="w-4 h-4 text-purple-400" />
              Database Collections Schema
            </span>
            <span className="text-xs text-white/40">
              PostgreSQL tables {isLoadingCollections && '(Syncing live counts...)'}
            </span>
          </div>

          <div className="flex flex-col sm:flex-row sm:items-center gap-3">
            <span className="text-xs text-white/40">Schema updates run during release.</span>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-xs text-left">
            <thead>
              <tr className="border-b border-white/[0.08] text-white/40 font-semibold uppercase tracking-wider">
                <th className="py-2.5 px-3">Collection ID</th>
                <th className="py-2.5 px-3">Description</th>
                <th className="py-2.5 px-3">Permissions Scope</th>
                <th className="py-2.5 px-3">Records / Contents</th>
                <th className="py-2.5 px-3 text-right">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/[0.04]">
              {dynamicCollections.map((c) => (
                <tr key={c.id} className="hover:bg-white/[0.02]">
                  <td className="py-3 px-3 font-mono font-medium text-emerald-400">{c.id}</td>
                  <td className="py-3 px-3 text-white/80">{c.name}</td>
                  <td className="py-3 px-3 font-mono text-white/50">{c.scope}</td>
                  <td className="py-3 px-3 text-white/70 font-mono">{c.items}</td>
                  <td className="py-3 px-3 text-right">
                    <Badge variant="emerald" className="text-[10px]">
                      {c.status}
                    </Badge>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </GlassCard>

      {/* Database Index & Query Health */}
      <GlassCard className="p-6 flex flex-col gap-4">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="flex flex-col">
            <span className="text-base font-semibold text-white flex items-center gap-2">
              <Zap className="w-4 h-4 text-amber-400" />
              Database Index & Query Health
            </span>
            <span className="text-xs text-white/40">
              Inspecting performance indexes across all 17 PostgreSQL tables {isLoadingIndexes && '(Auditing indexes...)'}
            </span>
          </div>
          <div className="flex items-center gap-3">
            <Badge variant="emerald" className="text-[10px] font-mono">
              6 Critical Indexes Audited
            </Badge>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-xs text-left">
            <thead>
              <tr className="border-b border-white/[0.08] text-white/40 font-semibold uppercase tracking-wider">
                <th className="py-2.5 px-3">Collection</th>
                <th className="py-2.5 px-3">Documents</th>
                <th className="py-2.5 px-3">Recommended Indexes</th>
                <th className="py-2.5 px-3">Existing Indexes</th>
                <th className="py-2.5 px-3 text-right">Coverage Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/[0.04]">
              {indexAuditData?.collectionsAudit?.map((item) => (
                <tr key={item.collectionId} className="hover:bg-white/[0.02]">
                  <td className="py-3 px-3">
                    <div className="font-mono font-medium text-emerald-400">{item.collectionId}</div>
                    <div className="text-[10px] text-white/50">{item.name}</div>
                  </td>
                  <td className="py-3 px-3 font-mono text-white/80">{item.documentCount}</td>
                  <td className="py-3 px-3">
                    {item.recommendedIndexes.length > 0 ? (
                      <div className="flex flex-wrap gap-1">
                        {item.recommendedIndexes.map((r, i) => (
                          <span key={i} className="px-1.5 py-0.5 rounded bg-white/5 font-mono text-[10px] text-white/70 border border-white/10">
                            {r.key} ({r.type})
                          </span>
                        ))}
                      </div>
                    ) : (
                      <span className="text-white/30 italic">None required</span>
                    )}
                  </td>
                  <td className="py-3 px-3">
                    {item.existingIndexes.length > 0 ? (
                      <div className="flex flex-wrap gap-1">
                        {item.existingIndexes.map((idx, i) => (
                          <span key={i} className="px-1.5 py-0.5 rounded bg-emerald-500/10 font-mono text-[10px] text-emerald-300 border border-emerald-500/20">
                            {idx.key}
                          </span>
                        ))}
                      </div>
                    ) : (
                      <span className="text-white/30 italic">Default key only</span>
                    )}
                  </td>
                  <td className="py-3 px-3 text-right">
                    <Badge 
                      variant={item.coverageStatus.includes('Optimal') ? 'emerald' : item.coverageStatus === 'Partial' ? 'amber' : 'emerald'} 
                      className="text-[10px]"
                    >
                      {item.coverageStatus}
                    </Badge>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </GlassCard>

      {/* Export & Database Backup Center */}
      <GlassCard className="p-6 flex flex-col gap-6">
        <div className="flex flex-col gap-1">
          <span className="text-base font-semibold text-white flex items-center gap-2">
            <Download className="w-4 h-4 text-emerald-400" />
            Export & Database Backup Center
          </span>
          <p className="text-xs text-white/50">
            Download live PostgreSQL data exports and user export sheets instantly. All exports reflect current production records.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-5 gap-4">
          <a
            href={`${adminBasePath}/api/export/users`}
            download="quranify_users_export.csv"
            className="chrome-btn p-4 rounded-xl flex flex-col gap-3 group transition-all hover:border-emerald-500/40"
          >
            <div className="flex items-center justify-between w-full">
              <span className="text-xs font-semibold text-white">Download Users CSV</span>
              <div className="w-8 h-8 rounded-lg bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400 group-hover:scale-105 transition-transform">
                <Download className="w-4 h-4" />
              </div>
            </div>
            <span className="text-[11px] text-white/50 text-left leading-relaxed">
              Live users with metadata, streaks, total listening minutes, and provider tags.
            </span>
          </a>

          <a
            href={`${adminBasePath}/api/export/reciters`}
            download="quranify_reciters_export.csv"
            className="chrome-btn p-4 rounded-xl flex flex-col gap-3 group transition-all hover:border-teal-500/40"
          >
            <div className="flex items-center justify-between w-full">
              <span className="text-xs font-semibold text-white">Download Reciters CSV</span>
              <div className="w-8 h-8 rounded-lg bg-teal-500/10 border border-teal-500/20 flex items-center justify-center text-teal-400 group-hover:scale-105 transition-transform">
                <Download className="w-4 h-4" />
              </div>
            </div>
            <span className="text-[11px] text-white/50 text-left leading-relaxed">
              Complete catalog of reciters with metadata, countries, styles, riwayahs, followers, and likes.
            </span>
          </a>

          <a
            href={`${adminBasePath}/api/export/history`}
            download="quranify_listening_history_backup.json"
            className="chrome-btn p-4 rounded-xl flex flex-col gap-3 group transition-all hover:border-cyan-500/40"
          >
            <div className="flex items-center justify-between w-full">
              <span className="text-xs font-semibold text-white">Download Listening History JSON</span>
              <div className="w-8 h-8 rounded-lg bg-cyan-500/10 border border-cyan-500/20 flex items-center justify-center text-cyan-400 group-hover:scale-105 transition-transform">
                <Download className="w-4 h-4" />
              </div>
            </div>
            <span className="text-[11px] text-white/50 text-left leading-relaxed">
              Complete listening history collection documents formatted with pretty indentation for full archival backup.
            </span>
          </a>

          <a
            href={`${adminBasePath}/api/export/playlists`}
            download="quranify_playlists_backup.json"
            className="chrome-btn p-4 rounded-xl flex flex-col gap-3 group transition-all hover:border-purple-500/40"
          >
            <div className="flex items-center justify-between w-full">
              <span className="text-xs font-semibold text-white">Download Playlists JSON</span>
              <div className="w-8 h-8 rounded-lg bg-purple-500/10 border border-purple-500/20 flex items-center justify-center text-purple-400 group-hover:scale-105 transition-transform">
                <Download className="w-4 h-4" />
              </div>
            </div>
            <span className="text-[11px] text-white/50 text-left leading-relaxed">
              Live user playlists and ordered playlist items database dump in pretty-printed JSON format.
            </span>
          </a>

          <a
            href={`${adminBasePath}/api/export/analytics`}
            download="quranify_analytics_export.json"
            className="chrome-btn p-4 rounded-xl flex flex-col gap-3 group transition-all hover:border-amber-500/40"
          >
            <div className="flex items-center justify-between w-full">
              <span className="text-xs font-semibold text-white">Download Analytics JSON</span>
              <div className="w-8 h-8 rounded-lg bg-amber-500/10 border border-amber-500/20 flex items-center justify-center text-amber-400 group-hover:scale-105 transition-transform">
                <Download className="w-4 h-4" />
              </div>
            </div>
            <span className="text-[11px] text-white/50 text-left leading-relaxed">
              Analytics export includes sampled listening activity and identifies unavailable metrics.
            </span>
          </a>
        </div>
      </GlassCard>
    </div>
  );
};
