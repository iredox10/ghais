import * as React from 'react';
import { LucideIcon, TrendingUp, TrendingDown, Minus } from 'lucide-react';
import { cn } from '@/lib/utils';
import { GlassCard, GlassCardProps } from './GlassCard';

export type DeltaType =
  | number
  | string
  | {
      value: number | string;
      label?: string;
      isPositive?: boolean;
      neutral?: boolean;
    };

export interface StatCardProps extends Omit<GlassCardProps, 'title'> {
  title: string;
  value: string | number;
  delta?: DeltaType;
  deltaLabel?: string;
  icon?: LucideIcon | React.ComponentType<{ className?: string }> | React.ReactNode;
  iconColor?: 'emerald' | 'cyan' | 'gold' | 'silver' | 'indigo' | 'rose';
  description?: string;
  deltaType?: 'positive' | 'negative' | 'neutral' | string;
  sparkline?: React.ReactNode;
  loading?: boolean;
}

const iconColorMap = {
  emerald: {
    well: 'bg-emerald-950/30 border-emerald-500/20 text-emerald-400 shadow-[inset_0_2px_4px_rgba(0,0,0,0.6),inset_0_-1px_1px_rgba(16,185,129,0.2)]',
    aura: 'from-emerald-500/15 to-transparent',
    dot: 'bg-emerald-400',
  },
  cyan: {
    well: 'bg-cyan-950/30 border-cyan-500/20 text-cyan-400 shadow-[inset_0_2px_4px_rgba(0,0,0,0.6),inset_0_-1px_1px_rgba(6,182,212,0.2)]',
    aura: 'from-cyan-500/15 to-transparent',
    dot: 'bg-cyan-400',
  },
  gold: {
    well: 'bg-amber-950/30 border-amber-500/20 text-amber-400 shadow-[inset_0_2px_4px_rgba(0,0,0,0.6),inset_0_-1px_1px_rgba(245,158,11,0.2)]',
    aura: 'from-amber-500/15 to-transparent',
    dot: 'bg-amber-400',
  },
  silver: {
    well: 'bg-white/[0.05] border-white/10 text-white/90 shadow-[inset_0_2px_4px_rgba(0,0,0,0.6),inset_0_-1px_1px_rgba(255,255,255,0.15)]',
    aura: 'from-white/10 to-transparent',
    dot: 'bg-white',
  },
  indigo: {
    well: 'bg-indigo-950/30 border-indigo-500/20 text-indigo-400 shadow-[inset_0_2px_4px_rgba(0,0,0,0.6),inset_0_-1px_1px_rgba(99,102,241,0.2)]',
    aura: 'from-indigo-500/15 to-transparent',
    dot: 'bg-indigo-400',
  },
  rose: {
    well: 'bg-rose-950/30 border-rose-500/20 text-rose-400 shadow-[inset_0_2px_4px_rgba(0,0,0,0.6),inset_0_-1px_1px_rgba(244,63,94,0.2)]',
    aura: 'from-rose-500/15 to-transparent',
    dot: 'bg-rose-400',
  },
};

function parseDelta(delta?: DeltaType) {
  if (delta === undefined || delta === null) return null;

  if (typeof delta === 'object' && 'value' in delta) {
    const valStr = String(delta.value);
    const num = parseFloat(valStr.replace(/[^\d.-]/g, ''));
    const isPositive =
      delta.isPositive !== undefined
        ? delta.isPositive
        : !isNaN(num)
        ? num > 0
        : valStr.startsWith('+');
    const isNeutral = delta.neutral || (!isNaN(num) && num === 0);
    return {
      formatted: isNeutral
        ? `${valStr}`
        : isPositive && !valStr.startsWith('+')
        ? `+${valStr}`
        : `${valStr}`,
      isPositive,
      isNeutral,
      label: delta.label,
    };
  }

  const str = String(delta);
  const num = parseFloat(str.replace(/[^\d.-]/g, ''));
  const isNeutral = !isNaN(num) && num === 0;
  const isPositive = !isNaN(num) ? num > 0 : str.startsWith('+');
  const formatted =
    typeof delta === 'number'
      ? `${delta > 0 ? '+' : ''}${delta}%`
      : isPositive && !str.startsWith('+')
      ? `+${str}`
      : str;

  return {
    formatted,
    isPositive,
    isNeutral,
    label: undefined,
  };
}

export const StatCard = React.forwardRef<HTMLDivElement, StatCardProps>(
  (
    {
      title,
      value,
      delta,
      deltaLabel,
      deltaType,
      icon: Icon,
      iconColor = 'emerald',
      description,
      sparkline,
      loading = false,
      glow,
      className,
      ...props
    },
    ref
  ) => {
    const theme = iconColorMap[iconColor] || iconColorMap.emerald;
    const deltaInfo = parseDelta(delta);
    const resolvedGlow = glow ?? (iconColor === 'emerald' ? 'emerald' : iconColor === 'cyan' ? 'cyan' : 'ambient');

    return (
      <GlassCard
        ref={ref}
        glow={resolvedGlow}
        className={cn('p-6 relative overflow-hidden group', className)}
        {...props}
      >
        {/* Subtle Ambient Radial Backlight */}
        <div
          className={cn(
            'absolute -top-10 -right-10 w-36 h-36 rounded-full blur-3xl opacity-20 pointer-events-none bg-gradient-to-br transition-opacity duration-300 group-hover:opacity-30',
            theme.aura
          )}
        />

        {/* Header Row: Title & Claymorphic Icon Well */}
        <div className="flex items-start justify-between gap-4">
          <div className="space-y-1">
            <span className="text-xs font-semibold uppercase tracking-wider text-white/50 block">
              {title}
            </span>
          </div>

          {/* Claymorphic Well for Icon */}
          {Icon && (
            <div
              className={cn(
                'relative flex items-center justify-center w-11 h-11 rounded-xl border transition-transform duration-200 group-hover:scale-105 shrink-0',
                theme.well
              )}
            >
              {typeof Icon === 'function' ? (
                <Icon className="w-5 h-5 stroke-[2.25]" />
              ) : (
                Icon
              )}
            </div>
          )}
        </div>

        {/* Metric Value & Trend Row */}
        <div className="mt-4 flex flex-col space-y-2">
          {loading ? (
            <div className="space-y-2 animate-pulse">
              <div className="h-8 w-28 bg-white/10 rounded-md" />
              <div className="h-4 w-20 bg-white/5 rounded-md" />
            </div>
          ) : (
            <>
              <div className="flex items-baseline gap-2">
                <span className="text-3xl font-bold tracking-tight text-white/95 font-['Plus_Jakarta_Sans',sans-serif]">
                  {value}
                </span>
              </div>

              {/* Delta Trend Badge & Additional Context */}
              {(deltaInfo || description || sparkline) && (
                <div className="flex items-center justify-between pt-1 gap-2 flex-wrap">
                  {deltaInfo && (
                    <div className="flex items-center gap-1.5">
                      <span
                        className={cn(
                          'inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-semibold border backdrop-blur-md transition-colors',
                          deltaInfo.isNeutral
                            ? 'bg-white/[0.04] text-white/60 border-white/10'
                            : deltaInfo.isPositive
                            ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20 shadow-[inset_0_1px_0_rgba(16,185,129,0.2)]'
                            : 'bg-rose-500/10 text-rose-400 border-rose-500/20 shadow-[inset_0_1px_0_rgba(244,63,94,0.2)]'
                        )}
                      >
                        {deltaInfo.isNeutral ? (
                          <Minus className="w-3 h-3 stroke-[2.5]" />
                        ) : deltaInfo.isPositive ? (
                          <TrendingUp className="w-3 h-3 stroke-[2.5]" />
                        ) : (
                          <TrendingDown className="w-3 h-3 stroke-[2.5]" />
                        )}
                        <span>{deltaInfo.formatted}</span>
                      </span>

                      {(deltaInfo.label || deltaLabel) && (
                        <span className="text-xs text-white/40">
                          {deltaInfo.label || deltaLabel}
                        </span>
                      )}
                    </div>
                  )}

                  {description && !deltaInfo && (
                    <span className="text-xs text-white/40">{description}</span>
                  )}

                  {sparkline && <div className="ml-auto">{sparkline}</div>}
                </div>
              )}
            </>
          )}
        </div>
      </GlassCard>
    );
  }
);
StatCard.displayName = 'StatCard';

export default StatCard;
