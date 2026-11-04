import * as React from 'react';
import { X } from 'lucide-react';
import { cn } from '@/lib/utils';

export type BadgeVariant = 'emerald' | 'cyan' | 'indigo' | 'amber' | 'ghost' | 'rose';
export type BadgeSize = 'sm' | 'md' | 'lg';

export interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
  variant?: BadgeVariant;
  size?: BadgeSize;
  dot?: boolean;
  pulse?: boolean;
  icon?: React.ReactNode;
  removable?: boolean;
  onRemove?: () => void;
}

const variantStyles: Record<BadgeVariant, { container: string; dot: string; ping: string }> = {
  emerald: {
    container:
      'bg-emerald-500/10 text-emerald-400 border-emerald-500/25 shadow-[inset_0_1px_0_rgba(16,185,129,0.2)]',
    dot: 'bg-emerald-400',
    ping: 'bg-emerald-400/80',
  },
  cyan: {
    container:
      'bg-cyan-500/10 text-cyan-400 border-cyan-500/25 shadow-[inset_0_1px_0_rgba(6,182,212,0.2)]',
    dot: 'bg-cyan-400',
    ping: 'bg-cyan-400/80',
  },
  indigo: {
    container:
      'bg-indigo-500/10 text-indigo-400 border-indigo-500/25 shadow-[inset_0_1px_0_rgba(99,102,241,0.2)]',
    dot: 'bg-indigo-400',
    ping: 'bg-indigo-400/80',
  },
  amber: {
    container:
      'bg-amber-500/10 text-amber-400 border-amber-500/25 shadow-[inset_0_1px_0_rgba(245,158,11,0.2)]',
    dot: 'bg-amber-400',
    ping: 'bg-amber-400/80',
  },
  ghost: {
    container:
      'bg-white/[0.05] text-white/70 border-white/10 shadow-[inset_0_1px_0_rgba(255,255,255,0.08)]',
    dot: 'bg-white/60',
    ping: 'bg-white/40',
  },
  rose: {
    container:
      'bg-rose-500/10 text-rose-400 border-rose-500/25 shadow-[inset_0_1px_0_rgba(244,63,94,0.2)]',
    dot: 'bg-rose-400',
    ping: 'bg-rose-400/80',
  },
};

const sizeStyles: Record<BadgeSize, string> = {
  sm: 'px-2 py-0.5 text-[11px] gap-1 font-medium',
  md: 'px-2.5 py-1 text-xs gap-1.5 font-medium',
  lg: 'px-3 py-1.5 text-sm gap-2 font-medium',
};

export const Badge = React.forwardRef<HTMLSpanElement, BadgeProps>(
  (
    {
      className,
      variant = 'ghost',
      size = 'md',
      dot = false,
      pulse = true,
      icon,
      removable = false,
      onRemove,
      children,
      ...props
    },
    ref
  ) => {
    const vStyle = variantStyles[variant] || variantStyles.ghost;

    return (
      <span
        ref={ref}
        className={cn(
          'inline-flex items-center justify-center rounded-full border backdrop-blur-md select-none transition-colors duration-150',
          vStyle.container,
          sizeStyles[size] || sizeStyles.md,
          className
        )}
        {...props}
      >
        {dot && (
          <span className="relative flex h-1.5 w-1.5 shrink-0">
            {pulse && (
              <span
                className={cn(
                  'animate-ping absolute inline-flex h-full w-full rounded-full opacity-75',
                  vStyle.ping
                )}
              />
            )}
            <span
              className={cn('relative inline-flex rounded-full h-1.5 w-1.5', vStyle.dot)}
            />
          </span>
        )}

        {icon && <span className="inline-flex shrink-0 items-center">{icon}</span>}

        <span>{children}</span>

        {removable && (
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              onRemove?.();
            }}
            className="ml-0.5 -mr-1 inline-flex items-center justify-center rounded-full p-0.5 hover:bg-white/20 transition-colors focus:outline-none"
            aria-label="Remove badge"
          >
            <X className="w-3 h-3 stroke-[2.5]" />
          </button>
        )}
      </span>
    );
  }
);
Badge.displayName = 'Badge';

export default Badge;
