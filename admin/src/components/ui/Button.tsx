import * as React from 'react';
import { Loader2 } from 'lucide-react';
import { cn } from '@/lib/utils';

export type ButtonVariant = 'chrome' | 'glass' | 'danger' | 'ghost' | 'emerald';
export type ButtonSize = 'sm' | 'md' | 'lg' | 'icon-sm' | 'icon' | 'icon-lg';

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
  loading?: boolean;
  loadingText?: string;
  leftIcon?: React.ReactNode;
  rightIcon?: React.ReactNode;
}

const variantStyles: Record<ButtonVariant, string> = {
  chrome:
    'bg-gradient-to-b from-[#F5F5F7] to-[#C7C7CC] text-zinc-950 font-semibold shadow-[0_1px_3px_rgba(0,0,0,0.5),inset_0_1px_0_rgba(255,255,255,0.9),inset_0_-1px_0_rgba(0,0,0,0.2)] hover:from-white hover:to-[#D6D6DC] hover:shadow-[0_2px_12px_rgba(255,255,255,0.22),inset_0_1px_0_rgba(255,255,255,1)] active:scale-[0.98] active:translate-y-px active:shadow-[0_1px_2px_rgba(0,0,0,0.4)] border-0',
  glass:
    'bg-white/[0.07] hover:bg-white/[0.12] active:bg-white/[0.04] active:scale-[0.99] text-white border border-white/10 hover:border-white/20 border-t-white/25 shadow-[inset_0_1px_0_0_rgba(255,255,255,0.18)] backdrop-blur-md font-medium',
  danger:
    'bg-rose-500/15 hover:bg-rose-500/25 active:bg-rose-500/10 active:scale-[0.99] text-rose-300 hover:text-rose-200 border border-rose-500/30 hover:border-rose-500/50 border-t-rose-400/40 shadow-[inset_0_1px_0_0_rgba(244,63,94,0.25)] backdrop-blur-md font-medium',
  ghost:
    'bg-transparent hover:bg-white/[0.06] active:bg-white/[0.03] active:scale-[0.99] text-white/75 hover:text-white border border-transparent hover:border-white/10 font-medium',
  emerald:
    'bg-emerald-500/15 hover:bg-emerald-500/25 active:bg-emerald-500/10 active:scale-[0.99] text-emerald-300 hover:text-emerald-200 border border-emerald-500/30 hover:border-emerald-500/50 border-t-emerald-400/40 shadow-[inset_0_1px_0_0_rgba(16,185,129,0.25)] backdrop-blur-md font-medium',
};

const sizeStyles: Record<ButtonSize, string> = {
  sm: 'h-8 px-3 text-xs rounded-lg gap-1.5',
  md: 'h-10 px-4 text-sm rounded-xl gap-2',
  lg: 'h-12 px-6 text-base rounded-xl gap-2.5',
  'icon-sm': 'h-8 w-8 p-0 rounded-lg justify-center',
  icon: 'h-10 w-10 p-0 rounded-xl justify-center',
  'icon-lg': 'h-12 w-12 p-0 rounded-xl justify-center',
};

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      className,
      variant = 'glass',
      size = 'md',
      loading = false,
      loadingText,
      leftIcon,
      rightIcon,
      disabled,
      children,
      type = 'button',
      ...props
    },
    ref
  ) => {
    const isDisabled = disabled || loading;

    return (
      <button
        ref={ref}
        type={type}
        disabled={isDisabled}
        className={cn(
          'inline-flex items-center justify-center select-none whitespace-nowrap transition-all duration-150',
          'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/40 focus-visible:ring-offset-2 focus-visible:ring-offset-[#050506]',
          variantStyles[variant] || variantStyles.glass,
          sizeStyles[size] || sizeStyles.md,
          isDisabled && 'opacity-40 cursor-not-allowed pointer-events-none active:scale-100 active:translate-y-0 shadow-none',
          className
        )}
        {...props}
      >
        {loading ? (
          <>
            <Loader2 className="w-4 h-4 animate-spin shrink-0" />
            {loadingText && <span>{loadingText}</span>}
            {!loadingText && children && <span>{children}</span>}
          </>
        ) : (
          <>
            {leftIcon && <span className="inline-flex shrink-0 items-center">{leftIcon}</span>}
            {children && <span>{children}</span>}
            {rightIcon && <span className="inline-flex shrink-0 items-center">{rightIcon}</span>}
          </>
        )}
      </button>
    );
  }
);
Button.displayName = 'Button';

export default Button;
