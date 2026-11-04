import * as React from 'react';
import { Search, X } from 'lucide-react';
import { cn } from '@/lib/utils';

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  isSearch?: boolean;
  leftIcon?: React.ReactNode;
  rightIcon?: React.ReactNode;
  label?: string;
  error?: string;
  helperText?: string;
  clearable?: boolean;
  onClear?: () => void;
  wrapperClassName?: string;
}

export const Input = React.forwardRef<HTMLInputElement, InputProps>(
  (
    {
      className,
      wrapperClassName,
      isSearch = false,
      leftIcon,
      rightIcon,
      label,
      error,
      helperText,
      clearable = false,
      onClear,
      disabled,
      value,
      type = 'text',
      ...props
    },
    ref
  ) => {
    const hasValue = value !== undefined && value !== null && String(value).length > 0;
    const showClear = clearable && hasValue && !disabled;
    const effectiveLeftIcon = leftIcon || (isSearch ? <Search className="w-4 h-4 text-white/40" /> : null);

    return (
      <div className={cn('w-full flex flex-col space-y-1.5', wrapperClassName)}>
        {label && (
          <label className="text-xs font-medium text-white/70 tracking-wide select-none">
            {label}
          </label>
        )}

        <div className="relative flex items-center w-full">
          {effectiveLeftIcon && (
            <div className="absolute left-3.5 flex items-center pointer-events-none text-white/40 z-10">
              {effectiveLeftIcon}
            </div>
          )}

          <input
            ref={ref}
            type={type}
            disabled={disabled}
            value={value}
            className={cn(
              'w-full h-10 rounded-xl px-3.5 text-sm text-white placeholder:text-white/30',
              'bg-black/50 border border-white/[0.08]',
              'shadow-[inset_0_2px_4px_0_rgba(0,0,0,0.6)] backdrop-blur-md',
              'transition-all duration-200 outline-none',
              'focus:border-white/30 focus:bg-black/70 focus:ring-1 focus:ring-white/20 focus:shadow-[inset_0_2px_4px_0_rgba(0,0,0,0.8),0_0_12px_rgba(255,255,255,0.06)]',
              effectiveLeftIcon && 'pl-10',
              (rightIcon || showClear) && 'pr-10',
              error && 'border-rose-500/50 focus:border-rose-500 focus:ring-rose-500/30 text-rose-100',
              disabled && 'opacity-40 cursor-not-allowed bg-black/30',
              className
            )}
            {...props}
          />

          {showClear && (
            <button
              type="button"
              onClick={onClear}
              className="absolute right-3 p-1 rounded-full text-white/40 hover:text-white/80 hover:bg-white/10 transition-colors focus:outline-none"
              aria-label="Clear input"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          )}

          {!showClear && rightIcon && (
            <div className="absolute right-3.5 flex items-center pointer-events-none text-white/40">
              {rightIcon}
            </div>
          )}
        </div>

        {error && (
          <p className="text-xs text-rose-400 font-medium pl-1 flex items-center gap-1 animate-fade-in">
            {error}
          </p>
        )}

        {!error && helperText && (
          <p className="text-xs text-white/40 pl-1">{helperText}</p>
        )}
      </div>
    );
  }
);
Input.displayName = 'Input';

export default Input;
