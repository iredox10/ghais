import React, { useState, useEffect, useRef } from 'react';
import {
  Activity as ActivityIcon,
  Headphones,
  Heart,
  UserCheck,
  AlarmClock,
  ListMusic,
  Flame,
  Radio,
  Clock,
  Filter,
  Volume2,
  VolumeX,
  Copy,
  Check,
  FileCode,
  Sparkles,
  Zap,
  Info
} from 'lucide-react';
import { useActivity, usePlayback } from '../lib/queryClient';
import { GlassCard } from '../components/ui/GlassCard';
import { Badge } from '../components/ui/Badge';
import { Modal } from '../components/ui/Modal';

type PollRate = 'Paused' | '3s' | '5s' | '10s' | '30s';

const pollIntervalMap: Record<PollRate, number | false> = {
  'Paused': false,
  '3s': 3000,
  '5s': 5000,
  '10s': 10000,
  '30s': 30000,
};

export const Activity: React.FC = () => {
  const [pollRate, setPollRate] = useState<PollRate>('10s');
  const [soundEnabled, setSoundEnabled] = useState<boolean>(true);
  const [simulatedActivities, setSimulatedActivities] = useState<any[]>([]);
  const [inspectingActivity, setInspectingActivity] = useState<any | null>(null);
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const [simulationToast, setSimulationToast] = useState<string | null>(null);
  const [filterType, setFilterType] = useState<string>('all');

  const seenIdsRef = useRef<Set<string>>(new Set());

  const { data, isLoading, isError: activityError } = useActivity({
    refetchInterval: pollIntervalMap[pollRate],
  });

  const { data: playbackData, isLoading: playbackLoading, isError: playbackError } = usePlayback({
    refetchInterval: pollIntervalMap[pollRate],
  });

  const serverActivities = data?.activities || [];
  const allActivities = [...simulatedActivities, ...serverActivities];

  const filtered = filterType === 'all'
    ? allActivities
    : allActivities.filter((a) => a.type === filterType);

  // Play subtle soft audio chime via Web Audio API
  const playChime = () => {
    if (!soundEnabled) return;
    try {
      const AudioCtx = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
      if (!AudioCtx) return;
      const ctx = new AudioCtx();
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(587.33, ctx.currentTime); // D5
      osc.frequency.exponentialRampToValueAtTime(880, ctx.currentTime + 0.15); // A5
      gain.gain.setValueAtTime(0.06, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.0001, ctx.currentTime + 0.3);
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start();
      osc.stop(ctx.currentTime + 0.3);
    } catch {
      // Audio context might be restricted before user interaction
    }
  };

  // Detect new inbound events and trigger sound notification
  useEffect(() => {
    if (!allActivities.length) return;
    const currentIds = allActivities.map(a => a.id);
    const hasNew = currentIds.some(id => !seenIdsRef.current.has(id));
    if (seenIdsRef.current.size > 0 && hasNew && soundEnabled) {
      playChime();
    }
    currentIds.forEach(id => seenIdsRef.current.add(id));
  }, [allActivities, soundEnabled]);

  const handleSimulateEvent = () => {
    const testPool = [
      { user: 'Fatima Zahra', action: 'Streamed Surah Al-Waqi\'ah', reciter: 'Mishary Rashid Alafasy', surah: 'Al-Waqi\'ah', ambient: 'Rain & Thunder', type: 'listen' },
      { user: 'Bilal ibn Rabah', action: 'Favorited Surah Ar-Rahman', reciter: 'Abdul Basit', surah: 'Ar-Rahman', ambient: '-', type: 'like' },
      { user: 'Aishah Tariq', action: 'Configured Recitation Alarm (Fajr)', reciter: 'Abdul Rahman Al-Sudais', surah: 'Al-Baqarah', ambient: 'Gentle Wind', type: 'schedule' },
      { user: 'Zayd ibn Harithah', action: 'Achieved 30-Day Listening Streak', reciter: 'Maher Al-Muaiqly', surah: 'Al-Mulk', ambient: 'Ocean Waves', type: 'streak' },
      { user: 'Khadijah bint Khuwaylid', action: 'Created Curated Playlist', reciter: 'Islam Sobhi', surah: 'Al-Kahf', ambient: 'Birdsong', type: 'playlist' }
    ];
    const template = testPool[Math.floor(Math.random() * testPool.length)];
    const newEvt = {
      id: 'sim_' + Date.now(),
      user: template.user,
      action: template.action,
      reciter: template.reciter,
      surah: template.surah,
      ambient: template.ambient,
      time: 'Just now',
      type: template.type,
      simulated: true,
      payload: {
        eventId: 'ev_' + Math.random().toString(36).substring(2, 9),
        clientTimestamp: new Date().toISOString(),
        device: 'iOS Mobile App (v2.4.1)',
        bitrate: '128kbps',
        network: '5G',
        database: 'ghais',
        collection: template.type === 'like' ? 'likes' : template.type === 'schedule' ? 'schedules' : 'history'
      }
    };

    setSimulatedActivities(prev => [newEvt, ...prev]);
    playChime();
    setSimulationToast(`Simulated inbound ${newEvt.type} event received!`);
    setTimeout(() => setSimulationToast(null), 3500);
  };

  const handleCopyJson = (act: any, e: React.MouseEvent) => {
    e.stopPropagation();
    const jsonStr = JSON.stringify(act, null, 2);
    navigator.clipboard.writeText(jsonStr);
    setCopiedId(act.id);
    setTimeout(() => setCopiedId(null), 2000);
  };

  const getEventIcon = (type: string) => {
    switch (type) {
      case 'listen':
        return <Headphones className="w-4 h-4 text-emerald-400" />;
      case 'like':
        return <Heart className="w-4 h-4 text-rose-400" />;
      case 'follow':
        return <UserCheck className="w-4 h-4 text-cyan-400" />;
      case 'schedule':
        return <AlarmClock className="w-4 h-4 text-purple-400" />;
      case 'streak':
        return <Flame className="w-4 h-4 text-amber-400" />;
      case 'playlist':
        return <ListMusic className="w-4 h-4 text-indigo-400" />;
      default:
        return <ActivityIcon className="w-4 h-4 text-white/50" />;
    }
  };

  return (
    <div className="flex flex-col gap-6 pb-12">
      {/* Toast Notification for Simulation */}
      {simulationToast && (
        <div className="fixed top-6 right-6 z-50 px-4 py-3 rounded-2xl bg-emerald-500/20 border border-emerald-500/40 text-emerald-300 text-xs font-medium shadow-2xl backdrop-blur-xl flex items-center gap-2 animate-fade-in">
          <Sparkles className="w-4 h-4 text-emerald-400 animate-spin" />
          <span>{simulationToast}</span>
        </div>
      )}

      {/* Header Section */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2.5">
            <Radio className="w-6 h-6 text-emerald-400 animate-pulse" />
            Live Recitation Activity Stream
          </h1>
          <p className="text-sm text-white/50 mt-1">
            Real-time feed of recitations, Ayah likes, reciter follows, and schedule alarms.
          </p>
        </div>

        {/* Controls Toolbar */}
        <div className="flex flex-wrap items-center gap-3">
          {/* Simulate Test Inbound Event Button */}
          <button
            onClick={handleSimulateEvent}
            className="chrome-btn px-3.5 py-2 text-xs rounded-xl flex items-center gap-1.5 shadow-specular"
          >
            <Zap className="w-3.5 h-3.5 text-amber-500 fill-amber-500" />
            <span>Simulate / Test Inbound Event</span>
          </button>

          {/* Sound Notification Toggle */}
          <button
            onClick={() => setSoundEnabled(!soundEnabled)}
            className={`px-3 py-2 rounded-xl text-xs font-medium flex items-center gap-1.5 transition-all border ${
              soundEnabled
                ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400'
                : 'bg-white/[0.03] border-white/10 text-white/40 hover:text-white'
            }`}
            title={soundEnabled ? 'Sound Notifications Enabled' : 'Sound Notifications Muted'}
          >
            {soundEnabled ? <Volume2 className="w-3.5 h-3.5" /> : <VolumeX className="w-3.5 h-3.5" />}
            <span className="hidden sm:inline">Sound: {soundEnabled ? 'On' : 'Off'}</span>
          </button>

          {/* Polling Rate Selector */}
          <div className="flex items-center bg-white/[0.03] border border-white/10 rounded-xl p-0.5 text-xs">
            <span className="px-2.5 py-1 text-white/40 font-mono text-[11px] hidden sm:inline">Poll:</span>
            {(['Paused', '3s', '5s', '10s', '30s'] as PollRate[]).map((rate) => (
              <button
                key={rate}
                onClick={() => setPollRate(rate)}
                className={`px-2.5 py-1 rounded-lg font-mono text-[11px] transition-all ${
                  pollRate === rate
                    ? 'bg-gradient-to-b from-white/20 to-white/10 text-white border border-white/20 shadow-specular font-semibold'
                    : 'text-white/50 hover:text-white'
                }`}
              >
                {rate}
              </button>
            ))}
          </div>

          {/* Live Pulse Badge */}
          <div className="flex items-center gap-2 px-3 py-2 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-xs font-mono text-emerald-400">
            <span className={`w-2 h-2 rounded-full bg-emerald-400 ${pollRate === 'Paused' ? '' : 'animate-ping'}`} />
            <span>{pollRate === 'Paused' ? 'Polling Paused' : `Live (${pollRate})`}</span>
          </div>
        </div>
      </div>

      {/* Live Listening Radar Panel */}
      <GlassCard className="p-6 flex flex-col gap-4 relative overflow-hidden">
        <div className="absolute top-0 right-0 w-64 h-64 bg-emerald-500/5 rounded-full blur-3xl pointer-events-none" />
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-xl bg-emerald-500/15 border border-emerald-500/30 flex items-center justify-center">
              <Radio className="w-4 h-4 text-emerald-400 animate-pulse" />
            </div>
            <div>
              <h2 className="text-base font-semibold text-white">Recent Playback Snapshots</h2>
              <p className="text-xs text-white/50">Last synced playback state; this does not indicate whether a listener is currently playing.</p>
            </div>
          </div>
          <div className="flex items-center gap-2 px-3 py-1 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-xs font-mono text-emerald-400">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping" />
            <span>{playbackData?.total ?? 0} snapshots</span>
          </div>
        </div>

        {playbackLoading ? (
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 animate-pulse py-4">
            {[...Array(3)].map((_, i) => (
              <div key={i} className="h-32 rounded-2xl bg-white/5 border border-white/10" />
            ))}
          </div>
        ) : playbackError ? (
          <div className="text-center py-8 text-xs text-rose-300">Playback data is unavailable. Try again later.</div>
        ) : (!playbackData?.sessions || playbackData.sessions.length === 0) ? (
          <div className="text-center py-8 text-xs text-white/40">
            No playback snapshots available.
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {playbackData.sessions.map((session) => {
              const posMin = Math.floor(session.positionSeconds / 60);
              const posSec = session.positionSeconds % 60;
              const durMin = Math.floor(session.durationSeconds / 60);
              const durSec = session.durationSeconds % 60;
              const progressPct = session.durationSeconds > 0 ? Math.min(100, Math.max(0, (session.positionSeconds / session.durationSeconds) * 100)) : null;

              return (
                <div
                  key={session.id}
                  className="flex flex-col justify-between p-4 rounded-2xl bg-white/[0.03] border border-white/10 hover:border-emerald-500/40 hover:bg-white/[0.05] transition-all gap-4 group shadow-specular"
                >
                  {/* User Identity & Avatar & Device Badge */}
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div aria-hidden="true" className="w-10 h-10 rounded-full border border-white/15 bg-white/5 flex items-center justify-center text-white/60">{session.userName.slice(0, 1).toUpperCase()}</div>
                      <div className="flex flex-col">
                        <span className="text-xs font-semibold text-white group-hover:text-emerald-300 transition-colors">
                          {session.userName}
                        </span>
                        <span className="text-[10px] text-white/40 font-mono">
                          ID: {session.userId.substring(0, 8)}...
                        </span>
                        <span className="text-[10px] text-white/40">Last synced: {session.updatedAt}</span>
                      </div>
                    </div>
                    <span className={`px-2.5 py-1 rounded-lg text-[10px] font-mono border ${
                      session.deviceType.toLowerCase().includes('android')
                        ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-300'
                        : 'bg-cyan-500/10 border-cyan-500/30 text-cyan-300'
                    }`}>
                      {session.deviceType}
                    </span>
                  </div>

                  {/* Currently Playing Surah & Reciter */}
                  <div className="flex flex-col gap-1 bg-white/[0.02] p-3 rounded-xl border border-white/[0.06]">
                    <div className="flex items-center gap-2">
                      <Headphones className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                      <span className="text-xs font-bold text-white truncate">
                        {session.surahName}
                      </span>
                    </div>
                    <span className="text-[11px] text-white/60 truncate pl-5">
                      Reciter: <strong className="text-white/80">{session.reciter}</strong>
                    </span>
                  </div>

                  {/* Progress Indicator */}
                  <div className="flex flex-col gap-1.5">
                    <div className="flex items-center justify-between text-[11px] font-mono text-white/50">
                      <span>{posMin}:{posSec.toString().padStart(2, '0')}</span>
                      <span className="text-emerald-400 font-semibold">{progressPct === null ? 'Duration unknown' : `${Math.round(progressPct)}%`}</span>
                      <span>{progressPct === null ? '—' : `${durMin}:${durSec.toString().padStart(2, '0')}`}</span>
                    </div>
                    <div className="w-full h-1.5 bg-white/10 rounded-full overflow-hidden">
                      <div
                        className="h-full bg-gradient-to-r from-emerald-500 to-cyan-400 rounded-full transition-all duration-1000"
                        style={{ width: `${progressPct ?? 0}%` }}
                      />
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </GlassCard>

      {/* Filter Buttons */}
      <div className="flex items-center gap-2 overflow-x-auto pb-1 text-xs">
        {['all', 'listen', 'like', 'follow', 'schedule', 'streak', 'playlist'].map((type) => (
          <button
            key={type}
            onClick={() => setFilterType(type)}
            className={`px-3 py-1.5 rounded-xl capitalize font-medium transition-all ${
              filterType === type
                ? 'bg-gradient-to-b from-white/15 to-white/5 text-white border border-white/20 shadow-specular'
                : 'text-white/50 hover:text-white bg-white/[0.03] hover:bg-white/[0.06] border border-white/[0.06]'
            }`}
          >
            {type === 'all' ? `All Activities (${allActivities.length})` : type}
          </button>
        ))}
      </div>

      {/* Activity Timeline */}
      <GlassCard className="p-6 flex flex-col gap-4">
        {isLoading && serverActivities.length === 0 ? (
          <div className="flex flex-col gap-3 animate-pulse">
            {[...Array(5)].map((_, i) => (
              <div key={i} className="h-16 rounded-xl bg-white/5 border border-white/10" />
            ))}
          </div>
        ) : activityError && serverActivities.length === 0 ? (
          <div className="text-center py-12 text-xs text-rose-300">Activity data is unavailable. Try again later.</div>
        ) : filtered.length === 0 ? (
          <div className="text-center py-12 text-xs text-white/40">
            No recent activity found for filter "{filterType}".
          </div>
        ) : (
          <div className="flex flex-col gap-3">
            {filtered.map((act) => (
              <div
                key={act.id}
                onClick={() => setInspectingActivity(act)}
                className="group flex flex-col sm:flex-row sm:items-center justify-between p-3.5 rounded-xl bg-white/[0.02] border border-white/[0.06] hover:bg-white/[0.05] hover:border-white/15 transition-all gap-3 cursor-pointer"
              >
                <div className="flex items-center gap-3.5">
                  <div className="w-9 h-9 rounded-xl bg-white/[0.05] border border-white/10 flex items-center justify-center shadow-specular shrink-0 group-hover:scale-105 transition-transform">
                    {getEventIcon(act.type)}
                  </div>

                  <div className="flex flex-col">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="text-xs font-semibold text-white">{act.user}</span>
                      <span className="text-xs text-white/70">{act.action}</span>
                      {act.simulated && (
                        <span className="px-1.5 py-0.5 rounded text-[9px] font-mono bg-amber-500/20 border border-amber-500/35 text-amber-300">
                          Simulated
                        </span>
                      )}
                    </div>

                    <div className="flex items-center gap-2 mt-0.5 text-[11px] text-white/40 flex-wrap">
                      {act.reciter && act.reciter !== '-' && (
                        <span>Reciter: <strong className="text-white/60">{act.reciter}</strong></span>
                      )}
                      {act.ambient && act.ambient !== '-' && (
                        <span>• Ambient: <strong className="text-white/60">{act.ambient}</strong></span>
                      )}
                    </div>
                  </div>
                </div>

                <div className="flex items-center gap-3 self-end sm:self-center">
                  <Badge variant={act.type === 'like' ? 'amber' : act.type === 'schedule' ? 'indigo' : 'emerald'} className="text-[10px] font-mono">
                    {act.type}
                  </Badge>

                  <span className="text-xs text-white/40 font-mono flex items-center gap-1">
                    <Clock className="w-3 h-3 text-white/30" />
                    {act.time}
                  </span>

                  {/* Quick Actions */}
                  <div className="flex items-center gap-1 opacity-80 group-hover:opacity-100 transition-opacity">
                    <button
                      onClick={(e) => handleCopyJson(act, e)}
                      title="Copy Event JSON"
                      className="p-1.5 rounded-lg bg-white/5 hover:bg-white/15 text-white/60 hover:text-white border border-white/10 transition-all"
                    >
                      {copiedId === act.id ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                    </button>
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        setInspectingActivity(act);
                      }}
                      title="Inspect Full Metadata Payload"
                      className="p-1.5 rounded-lg bg-white/5 hover:bg-white/15 text-white/60 hover:text-white border border-white/10 transition-all"
                    >
                      <FileCode className="w-3.5 h-3.5 text-cyan-400" />
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </GlassCard>

      {/* Inspect Metadata Slide-out / Modal */}
      <Modal
        isOpen={Boolean(inspectingActivity)}
        onClose={() => setInspectingActivity(null)}
        title={
          <div className="flex items-center gap-2">
            <FileCode className="w-5 h-5 text-emerald-400" />
            <span>Event Payload & Metadata Inspection</span>
          </div>
        }
        size="lg"
      >
        {inspectingActivity && (
          <div className="flex flex-col gap-4">
            <p className="text-xs text-white/60">
              {inspectingActivity.simulated ? 'This is a simulated event.' : 'This is a summary assembled from saved records; it is not a complete device event.'}
            </p>

            <div className="grid grid-cols-2 gap-3 text-xs">
              <div className="p-3 rounded-xl bg-white/[0.03] border border-white/10 flex flex-col gap-1">
                <span className="text-white/40 font-mono text-[10px]">Event ID</span>
                <span className="font-mono text-white/90">{inspectingActivity.id}</span>
              </div>
              <div className="p-3 rounded-xl bg-white/[0.03] border border-white/10 flex flex-col gap-1">
                <span className="text-white/40 font-mono text-[10px]">Event Type</span>
                <span className="font-mono text-emerald-400 capitalize">{inspectingActivity.type}</span>
              </div>
              <div className="p-3 rounded-xl bg-white/[0.03] border border-white/10 flex flex-col gap-1">
                <span className="text-white/40 font-mono text-[10px]">Listener</span>
                <span className="font-medium text-white">{inspectingActivity.user}</span>
              </div>
              <div className="p-3 rounded-xl bg-white/[0.03] border border-white/10 flex flex-col gap-1">
                <span className="text-white/40 font-mono text-[10px]">Timestamp</span>
                <span className="font-mono text-white/80">{inspectingActivity.time}</span>
              </div>
            </div>

            <div className="flex flex-col gap-1.5">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-white/70">Raw JSON Payload</span>
                <button
                  onClick={(e) => handleCopyJson(inspectingActivity, e)}
                  className="px-2.5 py-1 rounded-lg bg-white/5 hover:bg-white/15 text-white/80 text-xs flex items-center gap-1 border border-white/10 transition-all font-mono"
                >
                  <Copy className="w-3 h-3" />
                  <span>Copy JSON</span>
                </button>
              </div>

              <pre className="p-4 rounded-xl bg-black/60 border border-white/10 text-emerald-300 font-mono text-xs overflow-x-auto max-h-72">
                {JSON.stringify(inspectingActivity, null, 2)}
              </pre>
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button
                onClick={() => setInspectingActivity(null)}
                className="px-4 py-2 rounded-xl bg-white/10 hover:bg-white/20 text-white text-xs font-medium transition-all border border-white/15"
              >
                Close Inspector
              </button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};
