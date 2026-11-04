import * as React from 'react';
import { cn } from '@/lib/utils';

export interface GlassCardProps extends React.HTMLAttributes<HTMLDivElement> {
  variant?: 'default' | 'soft' | 'inset' | 'elevated';
  glow?: boolean | 'ambient' | 'emerald' | 'cyan' | 'gold' | 'rose';
  interactive?: boolean;
}

const glowStyles: Record<string, string> = {
  true: 'shadow-[0_0_40px_-10px_rgba(255,255,255,0.06)]',
  ambient: 'shadow-[0_0_40px_-10px_rgba(255,255,255,0.06)]',
  emerald: 'shadow-[0_0_40px_-8px_rgba(16,185,129,0.18)] border-emerald-500/20',
  cyan: 'shadow-[0_0_40px_-8px_rgba(6,182,212,0.18)] border-cyan-500/20',
  gold: 'shadow-[0_0_40px_-8px_rgba(245,158,11,0.18)] border-amber-500/20',
  rose: 'shadow-[0_0_40px_-8px_rgba(244,63,94,0.18)] border-rose-500/20',
};

const variantStyles: Record<string, string> = {
  default:
    'bg-card-gradient border border-white/[0.08] border-t-white/20 shadow-[inset_0_1px_0_0_rgba(255,255,255,0.18)] backdrop-blur-xl',
  soft:
    'bg-card-gradient-soft border border-white/[0.05] border-t-white/15 shadow-[inset_0_1px_0_0_rgba(255,255,255,0.10)] backdrop-blur-lg',
  inset:
    'bg-glass-inset border border-white/[0.06] shadow-[inset_0_2px_4px_0_rgba(0,0,0,0.6)] backdrop-blur-md',
  elevated:
    'bg-card-gradient-active border border-white/[0.12] border-t-white/30 shadow-[0_16px_36px_rgba(0,0,0,0.5),inset_0_1px_0_0_rgba(255,255,255,0.25)] backdrop-blur-2xl',
};

export const GlassCard = React.forwardRef<HTMLDivElement, GlassCardProps>(
  ({ className, variant = 'default', glow = false, interactive = false, children, ...props }, ref) => {
    const glowKey = typeof glow === 'boolean' ? (glow ? 'true' : 'none') : glow;
    const glowClass = glowKey !== 'none' ? glowStyles[glowKey] || '' : '';

    return (
      <div
        ref={ref}
        className={cn(
          'relative rounded-2xl transition-all duration-200 text-white',
          variantStyles[variant] || variantStyles.default,
          glowClass,
          interactive &&
            'cursor-pointer hover:border-white/25 hover:border-t-white/35 hover:-translate-y-0.5 hover:shadow-[0_12px_24px_-10px_rgba(0,0,0,0.7),inset_0_1px_0_0_rgba(255,255,255,0.25)]',
          className
        )}
        {...props}
      >
        {children}
      </div>
    );
  }
);
GlassCard.displayName = 'GlassCard';

export interface GlassCardHeaderProps extends React.HTMLAttributes<HTMLDivElement> {}
export const GlassCardHeader = React.forwardRef<HTMLDivElement, GlassCardHeaderProps>(
  ({ className, ...props }, ref) => (
    <div ref={ref} className={cn('flex flex-col space-y-1.5 p-6', className)} {...props} />
  )
);
GlassCardHeader.displayName = 'GlassCardHeader';

export interface GlassCardTitleProps extends React.HTMLAttributes<HTMLHeadingElement> {}
export const GlassCardTitle = React.forwardRef<HTMLHeadingElement, GlassCardTitleProps>(
  ({ className, ...props }, ref) => (
    <h3
      ref={ref}
      className={cn('text-lg font-semibold leading-none tracking-tight text-white/90', className)}
      {...props}
    />
  )
);
GlassCardTitle.displayName = 'GlassCardTitle';

export interface GlassCardDescriptionProps extends React.HTMLAttributes<HTMLParagraphElement> {}
export const GlassCardDescription = React.forwardRef<HTMLParagraphElement, GlassCardDescriptionProps>(
  ({ className, ...props }, ref) => (
    <p ref={ref} className={cn('text-sm text-white/50 leading-relaxed', className)} {...props} />
  )
);
GlassCardDescription.displayName = 'GlassCardDescription';

export interface GlassCardContentProps extends React.HTMLAttributes<HTMLDivElement> {}
export const GlassCardContent = React.forwardRef<HTMLDivElement, GlassCardContentProps>(
  ({ className, ...props }, ref) => <div ref={ref} className={cn('p-6 pt-0', className)} {...props} />
);
GlassCardContent.displayName = 'GlassCardContent';

export interface GlassCardFooterProps extends React.HTMLAttributes<HTMLDivElement> {}
export const GlassCardFooter = React.forwardRef<HTMLDivElement, GlassCardFooterProps>(
  ({ className, ...props }, ref) => (
    <div ref={ref} className={cn('flex items-center p-6 pt-0', className)} {...props} />
  )
);
GlassCardFooter.displayName = 'GlassCardFooter';

export default GlassCard;
