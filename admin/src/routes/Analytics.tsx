import React from 'react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell
} from 'recharts';
import {
  BarChart3,
  Moon,
  Sun,
  Smartphone,
  Repeat,
  Sparkles,
  BookOpen,
  Compass,
  CheckCheck
} from 'lucide-react';
import { useAnalytics, useTelemetry } from '../lib/queryClient';
import { GlassCard } from '../components/ui/GlassCard';
import { Badge } from '../components/ui/Badge';
import { StatCard } from '../components/ui/StatCard';

const COLORS = ['#10B981', '#06B6D4', '#6366F1', '#F59E0B', '#EC4899'];

export const Analytics: React.FC = () => {
  const { data: analytics, isLoading, isError, refetch } = useAnalytics();
  const { data: telemetry } = useTelemetry();

  if (isError && !analytics) {
    return <div role="alert" className="text-sm text-rose-300">Listening analytics could not be loaded. <button type="button" onClick={() => refetch()} className="underline">Try again</button></div>;
  }
  if (isLoading || !analytics) {
    return (
      <div className="flex flex-col gap-6 animate-pulse">
        <div className="h-8 w-64 bg-white/10 rounded-lg" />
        <div className="h-72 bg-white/5 rounded-2xl border border-white/10" />
      </div>
    );
  }

  const peakHours = analytics.peakHours || [];
  const peakIndex = Math.max(0, ...peakHours.map((hour) => hour.index));
  const reciterStyles = (analytics.reciterStyleDistribution || []).map((item: any) => ({
    name: item.name || item.style || '',
    percentage: item.percentage,
  }));
  const deviceCounts = telemetry?.deviceTelemetry?.byPlatform ?? {};
  const deviceTotal = Object.values(deviceCounts).reduce((sum, count) => sum + count, 0);
  const devices = Object.entries(deviceCounts).map(([name, count]) => ({
    name, value: deviceTotal ? Math.round(count * 100 / deviceTotal) : 0,
  }));

  return (
    <div className="flex flex-col gap-8 pb-12">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2">
          <BarChart3 className="w-6 h-6 text-emerald-400" />
          Listening Analytics & Quran Deep-Dive
        </h1>
        <p className="text-sm text-white/50 mt-1">
          Patterns from saved listening history and reported devices.
        </p>
      </div>

      {/* 24-Hour Listening Cycle / Peak Hours */}
      <GlassCard className="p-6 flex flex-col gap-4">
        <div className="flex items-center justify-between">
          <div className="flex flex-col">
            <span className="text-base font-semibold text-white flex items-center gap-2">
              <Sun className="w-4 h-4 text-amber-400" />
              24-Hour Recitation Activity Peaks
            </span>
            <span className="text-xs text-white/40">
              {analytics.sampleSize.toLocaleString()} saved history entries, grouped by UTC hour. Saved history is not a complete listening log.
              {analytics.skipped > 0 && ` ${analytics.skipped.toLocaleString()} entries had no valid timestamp.`}
            </span>
          </div>
        </div>

        {analytics.sampleSize === 0 ? <p className="text-sm text-white/50">No dated listening history is available yet.</p> : <div className="h-64 w-full pt-4">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={peakHours}>
              <XAxis
                dataKey="hour"
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
                tickFormatter={(val) => `${val}%`}
              />
              <Tooltip
                formatter={(val: any) => [`${val}% Intensity`, 'Activity Index']}
                contentStyle={{
                  backgroundColor: '#0F0F14',
                  border: '1px solid rgba(255,255,255,0.15)',
                  borderRadius: '12px',
                  color: '#fff',
                  fontSize: '12px',
                }}
              />
              <Bar dataKey="index" fill="#10B981" radius={[6, 6, 0, 0]}>
                {peakHours.map((_, index) => (
                  <Cell
                    key={`cell-${index}`}
                    fill={peakIndex > 0 && peakHours[index].index === peakIndex ? '#10B981' : 'rgba(255,255,255,0.2)'}
                  />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>}
      </GlassCard>

      {/* Retention & Routine Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <GlassCard className="p-6 flex flex-col gap-3">
          <span className="text-base font-semibold text-white">Listener retention</span>
          <p className="text-xs text-white/50">Retention rates are not available from the data collected yet.</p>
        </GlassCard>

        {/* Recitation Style Preference */}
        <GlassCard className="p-6 flex flex-col justify-between gap-4">
          <div className="flex flex-col">
            <span className="text-base font-semibold text-white">Recitation Styles</span>
            <span className="text-xs text-white/40">Murattal vs Mujawwad vs Muallim</span>
          </div>

          <div className="h-44 w-full flex items-center justify-center">
            {reciterStyles.length === 0 ? <p className="text-xs text-white/50">Style breakdown is not available yet.</p> :
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={reciterStyles}
                  cx="50%"
                  cy="50%"
                  innerRadius={45}
                  outerRadius={68}
                  paddingAngle={5}
                  nameKey="name"
                  dataKey="percentage"
                >
                  {reciterStyles.map((entry, index) => (
                    <Cell key={`style-${index}`} fill={COLORS[index % COLORS.length]} stroke="none" />
                  ))}
                </Pie>
                <Tooltip
                  formatter={(val: any) => [`${val}%`, 'Share']}
                  contentStyle={{
                    backgroundColor: '#0F0F14',
                    border: '1px solid rgba(255,255,255,0.15)',
                    borderRadius: '12px',
                    color: '#fff',
                    fontSize: '12px',
                  }}
                />
              </PieChart>
            </ResponsiveContainer>}
          </div>

          <div className="flex flex-col gap-2">
            {reciterStyles.map((item, idx) => (
              <div key={item.name} className="flex items-center justify-between text-xs">
                <div className="flex items-center gap-2">
                  <span className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: COLORS[idx % COLORS.length] }} />
                  <span className="text-white/80">{item.name}</span>
                </div>
                <span className="text-white/40 font-mono">{item.percentage}%</span>
              </div>
            ))}
          </div>
        </GlassCard>

        {/* Platform & OS Breakdown */}
        <GlassCard className="p-6 flex flex-col justify-between gap-4">
          <div className="flex flex-col">
            <span className="text-base font-semibold text-white flex items-center gap-2">
              <Smartphone className="w-4 h-4 text-cyan-400" />
              Device Distribution
            </span>
            <span className="text-xs text-white/40">Platforms among up to 200 reporting devices; not all installations</span>
          </div>

          <div className="flex flex-col gap-4 my-auto">
            {devices.length === 0 && <p className="text-xs text-white/50">No device telemetry available.</p>}
            {devices.map((dev) => (
              <div key={dev.name} className="flex flex-col gap-1.5">
                <div className="flex justify-between text-xs">
                  <span className="text-white/80 font-medium">{dev.name}</span>
                  <span className="text-white/40 font-mono">{dev.value}%</span>
                </div>
                <div className="w-full h-2 rounded-full bg-white/[0.06] overflow-hidden">
                  <div
                    className="h-full rounded-full bg-cyan-400"
                    style={{ width: `${dev.value}%` }}
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
