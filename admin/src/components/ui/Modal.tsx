import * as React from 'react';
import { createPortal } from 'react-dom';
import { X } from 'lucide-react';
import { cn } from '@/lib/utils';

export type ModalSize = 'sm' | 'md' | 'lg' | 'xl' | 'full';

export interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  title?: React.ReactNode;
  description?: React.ReactNode;
  children?: React.ReactNode;
  size?: ModalSize;
  showCloseButton?: boolean;
  closeOnBackdropClick?: boolean;
  closeOnEscape?: boolean;
  className?: string;
  overlayClassName?: string;
}

const sizeStyles: Record<ModalSize, string> = {
  sm: 'max-w-md',
  md: 'max-w-lg',
  lg: 'max-w-2xl',
  xl: 'max-w-4xl',
  full: 'max-w-[calc(100vw-2rem)] h-[calc(100vh-2rem)]',
};

export const Modal: React.FC<ModalProps> = ({
  isOpen,
  onClose,
  title,
  description,
  children,
  size = 'md',
  showCloseButton = true,
  closeOnBackdropClick = true,
  closeOnEscape = true,
  className,
  overlayClassName,
}) => {
  const [mounted, setMounted] = React.useState(false);

  React.useEffect(() => {
    setMounted(true);
  }, []);

  // Handle escape key
  React.useEffect(() => {
    if (!isOpen || !closeOnEscape) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        e.preventDefault();
        onClose();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, closeOnEscape, onClose]);

  // Lock body scroll
  React.useEffect(() => {
    if (!isOpen) return;
    const originalOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = originalOverflow;
    };
  }, [isOpen]);

  if (!mounted || !isOpen) return null;

  return createPortal(
    <div
      role="dialog"
      aria-modal="true"
      aria-label={typeof title === 'string' ? title : 'Dialog'}
      className={cn(
        'fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 overflow-y-auto animate-fade-in',
        'bg-black/75 backdrop-blur-md',
        overlayClassName
      )}
      onClick={(e) => {
        if (e.target === e.currentTarget && closeOnBackdropClick) {
          onClose();
        }
      }}
    >
      <div
        className={cn(
          'relative w-full rounded-2xl overflow-hidden shadow-[0_25px_50px_-12px_rgba(0,0,0,0.85),inset_0_1px_0_0_rgba(255,255,255,0.22)]',
          'bg-gradient-to-b from-[#17171C]/90 to-[#0F0F14]/95',
          'border border-white/10 border-t-white/25',
          'backdrop-blur-2xl animate-modal-in text-white',
          sizeStyles[size] || sizeStyles.md,
          className
        )}
      >
        {/* Top Specular Glow Line */}
        <div className="absolute top-0 inset-x-0 h-px bg-gradient-to-r from-transparent via-white/40 to-transparent pointer-events-none" />

        {/* Header if title or close button */}
        {(title || showCloseButton) && (
          <div className="flex items-start justify-between p-6 pb-4 border-b border-white/[0.06]">
            <div className="space-y-1 pr-6">
              {title && (
                <h2 className="text-xl font-semibold tracking-tight text-white/95">
                  {title}
                </h2>
              )}
              {description && (
                <p className="text-sm text-white/50 leading-relaxed">
                  {description}
                </p>
              )}
            </div>

            {showCloseButton && (
              <button
                type="button"
                onClick={onClose}
                className="rounded-xl p-1.5 text-white/40 hover:text-white hover:bg-white/10 transition-colors focus:outline-none focus:ring-1 focus:ring-white/20 shrink-0"
                aria-label="Close dialog"
              >
                <X className="w-5 h-5 stroke-[2]" />
              </button>
            )}
          </div>
        )}

        {/* Main Content */}
        {children}
      </div>
    </div>,
    document.body
  );
};

export interface ModalHeaderProps extends React.HTMLAttributes<HTMLDivElement> {}
export const ModalHeader: React.FC<ModalHeaderProps> = ({ className, ...props }) => (
  <div className={cn('p-6 pb-4 border-b border-white/[0.06]', className)} {...props} />
);

export interface ModalTitleProps extends React.HTMLAttributes<HTMLHeadingElement> {}
export const ModalTitle: React.FC<ModalTitleProps> = ({ className, ...props }) => (
  <h2 className={cn('text-xl font-semibold tracking-tight text-white/95', className)} {...props} />
);

export interface ModalDescriptionProps extends React.HTMLAttributes<HTMLParagraphElement> {}
export const ModalDescription: React.FC<ModalDescriptionProps> = ({ className, ...props }) => (
  <p className={cn('text-sm text-white/50 leading-relaxed', className)} {...props} />
);

export interface ModalContentProps extends React.HTMLAttributes<HTMLDivElement> {}
export const ModalContent: React.FC<ModalContentProps> = ({ className, ...props }) => (
  <div className={cn('p-6 space-y-4 text-white/80', className)} {...props} />
);

export interface ModalFooterProps extends React.HTMLAttributes<HTMLDivElement> {}
export const ModalFooter: React.FC<ModalFooterProps> = ({ className, ...props }) => (
  <div
    className={cn(
      'flex items-center justify-end gap-3 p-6 pt-4 border-t border-white/[0.06] bg-white/[0.02]',
      className
    )}
    {...props}
  />
);

export default Modal;
