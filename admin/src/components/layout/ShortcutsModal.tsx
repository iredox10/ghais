import React, { useEffect } from 'react';
import { createPortal } from 'react-dom';
import { Keyboard, X, Command, Volume2, Compass, Sparkles } from 'lucide-react';
import { cn } from '../../lib/utils';

export interface ShortcutsModalProps {
  isOpen: boolean;
  onClose: () => void;
}

interface ShortcutGroup {
  category: string;
  icon: React.ComponentType<{ className?: string }>;
  shortcuts: {
    keys: string[];
    description: string;
  }[];
}

const SHORTCUT_GROUPS: ShortcutGroup[] = [
  {
    category: 'Global Navigation (G then key)',
    icon: Compass,
    shortcuts: [
      { keys: ['G', 'D'], description: 'Go to Dashboard' },
      { keys: ['G', 'U'], description: 'Go to Users' },
      { keys: ['G', 'R'], description: 'Go to Reciters' },
      { keys: ['G', 'S'], description: 'Go to Surahs' },
      { keys: ['G', 'A'], description: 'Go to Analytics' },
      { keys: ['G', 'P'], description: 'Go to Playlists' },
      { keys: ['G', 'C'], description: 'Go to Settings (Config)' },
    ],
  },
  {
    category: 'Quick Actions & Modals',
    icon: Command,
    shortcuts: [
      { keys: ['⌘K', 'Ctrl+K'], description: 'Command Palette' },
      { keys: ['?'], description: 'Open Keyboard Shortcuts' },
      { keys: ['Esc'], description: 'Close modals' },
    ],
  },
  {
    category: 'Audio & Media Controls',
    icon: Volume2,
    shortcuts: [
      { keys: ['Space'], description: 'Play / Pause Audio' },
      { keys: ['M'], description: 'Mute / Unmute Audio' },
    ],
  },
];

export const ShortcutsModal: React.FC<ShortcutsModalProps> = ({ isOpen, onClose }) => {
  const [mounted, setMounted] = React.useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  // Handle Escape key inside modal
  useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        e.preventDefault();
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  // Lock body scroll when open
  useEffect(() => {
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
      aria-label="Keyboard Shortcuts"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 overflow-y-auto bg-black/75 backdrop-blur-md animate-fade-in"
      onClick={(e) => {
        if (e.target === e.currentTarget) {
          onClose();
        }
      }}
    >
      <div
        className={cn(
          'relative w-full max-w-2xl rounded-2xl overflow-hidden',
          'shadow-[0_25px_50px_-12px_rgba(0,0,0,0.85),inset_0_1px_0_0_rgba(255,255,255,0.22)]',
          'bg-gradient-to-b from-[#17171C]/95 to-[#0F0F14]/98',
          'border border-white/10 border-t-white/25',
          'backdrop-blur-2xl animate-modal-in text-white p-6 sm:p-8'
        )}
      >
        {/* Modal Header */}
        <div className="flex items-center justify-between pb-6 border-b border-white/[0.08]">
          <div className="flex items-center gap-3">
            <div className="flex items-center justify-center w-10 h-10 rounded-xl bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
              <Keyboard className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-lg font-semibold text-white tracking-tight flex items-center gap-2">
                Keyboard Shortcuts
                <span className="px-2 py-0.5 text-[10px] font-mono font-medium bg-white/10 text-emerald-400 rounded-full border border-white/15">
                  Ghais Noir
                </span>
              </h2>
              <p className="text-xs text-white/50">
                Master Ghais Noir admin portal with lightning-fast hotkeys.
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-xl text-white/60 hover:text-white hover:bg-white/[0.08] transition-colors"
            title="Close (Esc)"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Shortcuts List by Category */}
        <div className="py-6 space-y-6 max-h-[60vh] overflow-y-auto pr-1">
          {SHORTCUT_GROUPS.map((group, idx) => {
            const GroupIcon = group.icon;
            return (
              <div key={idx} className="space-y-3">
                <div className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-emerald-400">
                  <GroupIcon className="w-3.5 h-3.5" />
                  <span>{group.category}</span>
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                  {group.shortcuts.map((sc, sIdx) => (
                    <div
                      key={sIdx}
                      className="flex items-center justify-between p-3 rounded-xl bg-white/[0.03] border border-white/[0.06] hover:bg-white/[0.06] hover:border-white/[0.1] transition-all"
                    >
                      <span className="text-xs text-white/80 font-medium">
                        {sc.description}
                      </span>
                      <div className="flex items-center gap-1">
                        {sc.keys.map((key, kIdx) => (
                          <React.Fragment key={kIdx}>
                            {kIdx > 0 && <span className="text-[10px] text-white/30">+</span>}
                            <kbd className="px-2 py-1 text-[11px] font-mono bg-black/50 text-white/90 rounded border border-white/20 shadow-sm">
                              {key}
                            </kbd>
                          </React.Fragment>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            );
          })}
        </div>

        {/* Modal Footer */}
        <div className="pt-6 border-t border-white/[0.08] flex items-center justify-between">
          <div className="flex items-center gap-2 text-xs text-white/40">
            <Sparkles className="w-3.5 h-3.5 text-emerald-400" />
            <span>Press <kbd className="px-1.5 py-0.5 text-[10px] font-mono bg-white/10 rounded border border-white/20 text-white/70">?</kbd> anywhere to open</span>
          </div>
          <button
            onClick={onClose}
            className="px-4 py-2 rounded-xl bg-white/10 hover:bg-white/20 text-white text-xs font-medium border border-white/20 transition-all shadow-sm"
          >
            Close
          </button>
        </div>
      </div>
    </div>,
    document.body
  );
};
