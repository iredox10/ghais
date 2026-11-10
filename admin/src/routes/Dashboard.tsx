import React from 'react';
import {
  Users,
  Headphones,
  Flame,
  Heart,
  UserCheck,
  AlarmClock,
  Sparkles,
  TrendingUp,
  Volume2,
  BookOpen,
  ArrowRight
} from 'lucide-react';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell
} from 'recharts';
import { useOverview } from '../lib/queryClient';
import { GlassCard } from '../components/ui/GlassCard';
import { StatCard } from '../components/ui/StatCard';
import { Badge } from '../components/ui/Badge';
import { Link } from '@tanstack/react-router';

export const Dashboard: React.FC = () => {
  const { data: overview, isLoading, error } = useOverview();

  if (isLoading) {
    return (
      <div className="flex flex-col gap-6 animate-pulse">
        <div className="h-8 w-64 bg-white/10 rounded-lg" />
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
          {[...Array(6)].map((_, i) => (
            <div key={i} className="h-28 bg-white/5 rounded-2xl border border-white/10" />
          ))}
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 h-80 bg-white/5 rounded-2xl border border-white/10" />
          <div className="h-80 bg-white/5 rounded-2xl border border-white/10" />
        </div>
      </div>
    );
  }

  if (error || !overview) {
    return <p role="alert" className="text-sm text-rose-300">Dashboard data is unavailable. Please try again.</p>;
  }

  const kpis = overview?.kpis;
  const listeningTrends = overview?.listeningTrends || [];
  const routineDistribution = overview?.routineDistribution || [];
  const ambientUsage = overview?.ambientUsage || [];
  const topReciters = overview?.topReciters || [];
  const topSurahs = overview?.topSurahs || [];

  return (
    <div className="flex flex-col gap-8 pb-12">
      {/* Top Banner / Welcome */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2.5">
            Executive Command Center
            <span className="text-xs px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 font-mono">
              Live Stream
            </span>
          </h1>
          <p className="text-sm text-white/50 mt-1">
            Account totals and saved listening history from PostgreSQL.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <Link
            to="/activity"
            className="px-3.5 py-2 text-xs rounded-xl bg-white/[0.05] hover:bg-white/[0.08] text-white border border-white/[0.1] shadow-specular transition-all flex items-center gap-2"
          >
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping" />
            Live Event Feed
          </Link>
          <Link
            to="/reciters"
            className="chrome-btn px-4 py-2 text-xs rounded-xl flex items-center gap-1.5"
          >
            <Sparkles className="w-3.5 h-3.5" />
            Manage Reciters
          </Link>
        </div>
      </div>

      {/* KPI Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
        <StatCard
          title="Registered Users"
          value={kpis?.totalUsers.value.toLocaleString() || '0'}
          delta={kpis?.totalUsers.delta}
          deltaType="positive"
          icon={<Users className="w-4 h-4 text-emerald-400" />}
        />
        <StatCard
          title="Listening Hours"
          value={`${kpis?.totalHoursListened.value.toLocaleString() || '0'}h`}
          delta={kpis?.totalHoursListened.delta}
          deltaType="positive"
          icon={<Headphones className="w-4 h-4 text-cyan-400" />}
        />
        <StatCard
          title="Users in History Today"
          value={kpis?.activeListenersToday.value.toLocaleString() || '0'}
          delta={kpis?.activeListenersToday.delta}
          deltaType="positive"
          icon={<Flame className="w-4 h-4 text-amber-400" />}
        />
        <StatCard
          title="Ayah & Surah Likes"
          value={kpis?.totalLikes.value.toLocaleString() || '0'}
          delta={kpis?.totalLikes.delta}
          deltaType="positive"
          icon={<Heart className="w-4 h-4 text-rose-400" />}
        />
        <StatCard
          title="Reciter Follows"
          value={kpis?.totalFollows.value.toLocaleString() || '0'}
          delta={kpis?.totalFollows.delta}
          deltaType="positive"
          icon={<UserCheck className="w-4 h-4 text-indigo-400" />}
        />
        <StatCard
          title="Saved Schedules"
          value={kpis?.activeSchedules.value.toLocaleString() || '0'}
          delta={kpis?.activeSchedules.delta}
          deltaType="positive"
          icon={<AlarmClock className="w-4 h-4 text-purple-400" />}
        />
      </div>

      {/* Main Charts Row */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Recitation Hours Trend (Area Chart) */}
        <GlassCard className="lg:col-span-2 p-6 flex flex-col gap-4">
          <div className="flex items-center justify-between">
            <div className="flex flex-col">
              <span className="text-base font-semibold text-white flex items-center gap-2">
                <TrendingUp className="w-4 h-4 text-emerald-400" />
                Listening History (Last 7 Days)
              </span>
              <span className="text-xs text-white/40">
                Saved history records by UTC day; this is not a complete play log.
              </span>
            </div>
            <div className="flex items-center gap-2 text-xs text-white/50">
              <span className="flex items-center gap-1.5 font-mono">
                <span className="w-2.5 h-2.5 rounded-full bg-emerald-500" />
                Saved records
              </span>
            </div>
          </div>

          <div className="h-64 w-full pt-2">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={listeningTrends}>
                <defs>
                  <linearGradient id="hoursGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#10B981" stopOpacity={0.35} />
                    <stop offset="95%" stopColor="#10B981" stopOpacity={0.0} />
                  </linearGradient>
                </defs>
                <XAxis
                  dataKey="day"
                  stroke="rgba(255,255,255,0.3)"
                  fontSize={11}
                  tickLine={false}
                  axisLine={false}
                />
                <YAxis
                  stroke="rgba(255,255,255,0.3)"
                  fontSize={11}
                  tickLine={false}
                  axisLine={false}
                  allowDecimals={false}
                />
                <Tooltip
                  contentStyle={{
                    backgroundColor: '#0F0F14',
                    border: '1px solid rgba(255,255,255,0.15)',
                    borderRadius: '12px',
                    boxShadow: '0 4px 20px rgba(0,0,0,0.5)',
                    color: '#fff',
                    fontSize: '12px',
                  }}
                  itemStyle={{ color: '#10B981' }}
                />
                <Area
                  type="monotone"
                  dataKey="sessions"
                  name="Saved history records"
                  stroke="#10B981"
                  strokeWidth={2}
                  fillOpacity={1}
                  fill="url(#hoursGrad)"
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </GlassCard>

        {/* Routine Modes Distribution */}
        <GlassCard className="p-6 flex flex-col justify-between gap-4">
          <div className="flex flex-col">
            <span className="text-base font-semibold text-white">Routine Modes Breakdown</span>
            <span className="text-xs text-white/40">
              How listeners engage with routine presets
            </span>
          </div>

          <div className="h-48 w-full flex items-center justify-center">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={routineDistribution}
                  cx="50%"
                  cy="50%"
                  innerRadius={50}
                  outerRadius={75}
                  paddingAngle={4}
                  dataKey="value"
                >
                  {routineDistribution.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} stroke="none" />
                  ))}
                </Pie>
                <Tooltip
                  formatter={(value: any) => [`${value}%`, 'Share']}
                  contentStyle={{
                    backgroundColor: '#0F0F14',
                    border: '1px solid rgba(255,255,255,0.15)',
                    borderRadius: '12px',
                    color: '#fff',
                    fontSize: '12px',
                  }}
                />
              </PieChart>
            </ResponsiveContainer>
          </div>

          <div className="grid grid-cols-2 gap-2 text-xs">
            {routineDistribution.map((r) => (
              <div key={r.name} className="flex items-center gap-2">
                <span className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: r.color }} />
                <span className="text-white/70 truncate">{r.name}</span>
                <span className="text-white/40 font-mono ml-auto">{r.value}%</span>
              </div>
            ))}
          </div>
        </GlassCard>
      </div>

      {/* Second Row: Leaderboards & Ambient Sound Preferences */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Top 5 Reciters Leaderboard */}
        <GlassCard className="p-6 flex flex-col gap-4">
          <div className="flex items-center justify-between">
            <div className="flex flex-col">
              <span className="text-base font-semibold text-white">Top Reciters</span>
              <span className="text-xs text-white/40">Most followed reciters</span>
            </div>
            <Link to="/reciters" className="text-xs text-emerald-400 hover:text-emerald-300 flex items-center gap-1">
              View All <ArrowRight className="w-3 h-3" />
            </Link>
          </div>

          <div className="flex flex-col gap-3 mt-2">
            {topReciters.length === 0 && <p className="text-xs text-white/50">No reciter follows have been recorded.</p>}
            {topReciters.map((reciter, idx) => (
              <div
                key={reciter.slug}
                className="flex items-center justify-between p-2.5 rounded-xl bg-white/[0.02] border border-white/[0.05] hover:bg-white/[0.05] transition-all"
              >
                <div className="flex items-center gap-3">
                  <span className="text-xs font-mono font-bold text-white/30 w-4">{idx + 1}</span>
                  <div className="flex flex-col">
                    <span className="text-xs font-semibold text-white">{reciter.nameEn}</span>
                    <span className="text-[11px] text-white/40 font-['Amiri',serif]">{reciter.nameAr}</span>
                  </div>
                </div>
                <div className="flex flex-col items-end">
                  <span className="text-xs font-mono text-emerald-400 font-medium">
                    {reciter.followers.toLocaleString()}
                  </span>
                  <span className="text-[10px] text-white/30">followers</span>
                </div>
              </div>
            ))}
          </div>
        </GlassCard>

        {/* Most Listened Surahs */}
        <GlassCard className="p-6 flex flex-col gap-4">
          <div className="flex items-center justify-between">
            <div className="flex flex-col">
              <span className="text-base font-semibold text-white">Surahs in Saved History</span>
              <span className="text-xs text-white/40">Counts from saved records, not total plays</span>
            </div>
            <Link to="/analytics" className="text-xs text-cyan-400 hover:text-cyan-300 flex items-center gap-1">
              Deep Dive <ArrowRight className="w-3 h-3" />
            </Link>
          </div>

          <div className="flex flex-col gap-3 mt-2">
            {topSurahs.length === 0 && <p className="text-xs text-white/50">No surahs in saved history yet.</p>}
            {topSurahs.map((surah) => (
              <div
                key={surah.id}
                className="flex items-center justify-between p-2.5 rounded-xl bg-white/[0.02] border border-white/[0.05] hover:bg-white/[0.05] transition-all"
              >
                <div className="flex items-center gap-3">
                  <span className="w-6 h-6 rounded-lg bg-white/5 border border-white/10 flex items-center justify-center text-xs font-mono text-white/70">
                    {surah.id}
                  </span>
                  <div className="flex flex-col">
                    <span className="text-xs font-semibold text-white">{surah.nameEn}</span>
                    <span className="text-[10px] text-white/40">{surah.ayahs} Ayahs</span>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-xs font-['Amiri',serif] text-white/60">{surah.nameAr}</span>
                  <Badge variant="cyan" className="font-mono text-[10px]">
                    {surah.plays.toLocaleString()} saved records
                  </Badge>
                </div>
              </div>
            ))}
          </div>
        </GlassCard>

        {/* Ambient Sound Mixer Usage */}
        <GlassCard className="p-6 flex flex-col gap-4">
          <div className="flex flex-col">
            <span className="text-base font-semibold text-white flex items-center gap-2">
              <Volume2 className="w-4 h-4 text-indigo-400" />
              Ambient Sound Pairings
            </span>
            <span className="text-xs text-white/40">
              Popularity of nature loops mixed with Quran recitation
            </span>
          </div>

          <div className="flex flex-col gap-4 mt-2">
            {ambientUsage.map((amb) => (
              <div key={amb.name} className="flex flex-col gap-1.5">
                <div className="flex justify-between text-xs">
                  <span className="text-white/80 font-medium">{amb.name}</span>
                  <span className="text-white/40 font-mono">{amb.percentage}%</span>
                </div>
                <div className="w-full h-2 rounded-full bg-white/[0.06] overflow-hidden">
                  <div
                    className="h-full rounded-full bg-gradient-to-r from-emerald-500 to-cyan-400 transition-all duration-500"
                    style={{ width: `${amb.percentage}%` }}
                  />
                </div>
              </div>
            ))}
          </div>
        </GlassCard>
      </div>
    </div>
  );
};
