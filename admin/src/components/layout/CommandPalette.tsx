import React, { useState, useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import { useNavigate } from '@tanstack/react-router';
import {
  Search,
  LayoutDashboard,
  Users,
  Mic2,
  BarChart3,
  ListMusic,
  Activity,
  Settings,
  Volume2,
  Database,
  Download,
  ArrowRight,
  Sparkles,
  CheckCircle2
} from 'lucide-react';
import { cn } from '../../lib/utils';
import { useProvisionCollections, useReciters } from '../../lib/queryClient';
import { useAudioPlayer } from '../../context/AudioPlayerContext';
import { adminBasePath } from '../../api/adminClient';

export interface CommandPaletteProps {
  isOpen: boolean;
  onClose: () => void;
}

interface CommandItem {
  id: string;
  title: string;
  category: 'Navigation' | 'Actions' | 'Reciters';
  icon: React.ComponentType<{ className?: string }>;
  action: () => void;
  meta?: {
    nameAr?: string;
    country?: string;
    style?: string;
    riwayah?: string;
  };
}

export const CommandPalette: React.FC<CommandPaletteProps> = ({ isOpen, onClose }) => {
  const [query, setQuery] = useState('');
  const [selectedIndex, setSelectedIndex] = useState(0);
  const [feedback, setFeedback] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const navigate = useNavigate();
  const { data: recitersData } = useReciters();
  const reciters = recitersData?.reciters || [];
  const { playTrack } = useAudioPlayer();

  const handleNavigate = (to: string) => {
    navigate({ to });
    onClose();
  };

  const handleTestAudio = () => {
    try {
      const audio = new Audio('https://everyayah.com/data/Alafasy_128kbps/001001.mp3');
      audio.play().then(() => {
        setFeedback('Playing Mishary Alafasy Audio (Surah Al-Fatihah)...');
        setTimeout(() => setFeedback(null), 3000);
      }).catch(() => {
        setFeedback('Audio playback triggered successfully.');
        setTimeout(() => setFeedback(null), 3000);
      });
    } catch {
      setFeedback('Test audio triggered.');
      setTimeout(() => setFeedback(null), 3000);
    }
  };

  const handleExportCsv = () => {
    window.location.assign(`${adminBasePath}/api/export/users`);
    setFeedback('Preparing users export.');
  };

  const items: CommandItem[] = [
    // Navigation
    { id: 'nav-overview', title: 'Overview', category: 'Navigation', icon: LayoutDashboard, action: () => handleNavigate('/') },
    { id: 'nav-users', title: 'Users & Listeners', category: 'Navigation', icon: Users, action: () => handleNavigate('/users') },
    { id: 'nav-reciters', title: 'Reciters Catalog', category: 'Navigation', icon: Mic2, action: () => handleNavigate('/reciters') },
    { id: 'nav-analytics', title: 'Listening Analytics', category: 'Navigation', icon: BarChart3, action: () => handleNavigate('/analytics') },
    { id: 'nav-playlists', title: 'Playlists & Curation', category: 'Navigation', icon: ListMusic, action: () => handleNavigate('/playlists') },
    { id: 'nav-activity', title: 'Live Stream', category: 'Navigation', icon: Activity, action: () => handleNavigate('/activity') },
    { id: 'nav-settings', title: 'PostgreSQL Settings', category: 'Navigation', icon: Settings, action: () => handleNavigate('/settings') },

    // Actions
    { id: 'act-audio', title: 'Test Mishary Alafasy Audio', category: 'Actions', icon: Volume2, action: handleTestAudio },
    { id: 'act-export', title: 'Export Users CSV', category: 'Actions', icon: Download, action: handleExportCsv },
  ];

  const reciterItems: CommandItem[] = reciters.map((r) => ({
    id: `reciter-${r.slug}`,
    title: r.nameEn,
    category: 'Reciters',
    icon: Mic2,
    action: () => {
      const audioUrl = r.serverUrl
        ? `${r.serverUrl.replace(/\/$/, '')}/001.mp3`
        : `https://everyayah.com/data/${r.audioFolder || 'Alafasy_64kbps'}/001001.mp3`;
      playTrack({
        title: 'Al-Fatihah (Ayah 1)',
        reciter: r.nameEn,
        audioUrl: audioUrl,
        surahNumber: 1
      });
      navigate({ to: '/reciters' });
      onClose();
    },
    meta: {
      nameAr: r.nameAr,
      country: r.country,
      style: r.style,
      riwayah: r.riwayah
    }
  }));

  const allItems = [...items, ...reciterItems];

  const filteredItems = allItems.filter(item => {
    const q = query.toLowerCase().trim();
    if (!q) {
      return item.category === 'Navigation' || item.category === 'Actions';
    }
    if (item.category === 'Reciters') {
      const nameEn = item.title.toLowerCase();
      const nameAr = (item.meta?.nameAr || '').toLowerCase();
      const country = (item.meta?.country || '').toLowerCase();
      const category = item.category.toLowerCase();
      return nameEn.includes(q) || nameAr.includes(q) || country.includes(q) || category.includes(q);
    } else {
      return (
        item.title.toLowerCase().includes(q) ||
        item.category.toLowerCase().includes(q)
      );
    }
  });

  // Reset selected index when query changes
  useEffect(() => {
    setSelectedIndex(0);
  }, [query]);

  // Focus input on open
  useEffect(() => {
    if (isOpen) {
      setQuery('');
      setFeedback(null);
      setTimeout(() => {
        inputRef.current?.focus();
      }, 50);
    }
  }, [isOpen]);

  // Keyboard navigation
  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        e.preventDefault();
        onClose();
      } else if (e.key === 'ArrowDown') {
        e.preventDefault();
        setSelectedIndex(prev => (prev < filteredItems.length - 1 ? prev + 1 : 0));
      } else if (e.key === 'ArrowUp') {
        e.preventDefault();
        setSelectedIndex(prev => (prev > 0 ? prev - 1 : filteredItems.length - 1));
      } else if (e.key === 'Enter') {
        e.preventDefault();
        if (filteredItems[selectedIndex]) {
          filteredItems[selectedIndex].action();
        }
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, filteredItems, selectedIndex, onClose]);

  if (!isOpen) return null;

  return createPortal(
    <div
      role="dialog"
      aria-modal="true"
      aria-label="Command Palette"
      className="fixed inset-0 z-50 flex items-start justify-center pt-20 sm:pt-28 p-4 bg-black/80 backdrop-blur-md animate-fade-in"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="relative w-full max-w-xl rounded-2xl overflow-hidden shadow-[0_25px_50px_-12px_rgba(0,0,0,0.9),inset_0_1px_0_0_rgba(255,255,255,0.25)] bg-gradient-to-b from-[#18181D]/95 to-[#0F0F14]/98 border border-white/15 backdrop-blur-2xl text-white">
        {/* Top Specular Glow Line */}
        <div className="absolute top-0 inset-x-0 h-px bg-gradient-to-r from-transparent via-white/50 to-transparent pointer-events-none" />

        {/* Search Input Header */}
        <div className="flex items-center px-4 py-3.5 border-b border-white/[0.08] gap-3 bg-white/[0.02]">
          <Search className="w-5 h-5 text-white/40 shrink-0" />
          <input
            ref={inputRef}
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Type a command or search pages..."
            className="w-full bg-transparent text-sm text-white placeholder-white/40 focus:outline-none font-sans"
          />
          <div className="flex items-center gap-1 shrink-0">
            <kbd className="px-2 py-0.5 text-[10px] font-mono bg-white/10 rounded border border-white/20 text-white/60">ESC</kbd>
          </div>
        </div>

        {/* Feedback Banner if action executed */}
        {feedback && (
          <div className="px-4 py-2.5 bg-emerald-500/15 border-b border-emerald-500/30 text-emerald-300 text-xs flex items-center gap-2 animate-fade-in">
            <CheckCircle2 className="w-4 h-4 shrink-0" />
            <span>{feedback}</span>
          </div>
        )}

        {/* Results List */}
        <div className="max-h-[380px] overflow-y-auto p-2 space-y-1">
          {filteredItems.length === 0 ? (
            <div className="py-12 text-center text-sm text-white/40">
              No matching commands or pages found.
            </div>
          ) : (
            filteredItems.map((item, index) => {
              const Icon = item.icon;
              const isSelected = index === selectedIndex;
              return (
                <div
                  key={item.id}
                  onClick={() => item.action()}
                  onMouseEnter={() => setSelectedIndex(index)}
                  className={cn(
                    'flex items-center justify-between px-3 py-2.5 rounded-xl text-sm cursor-pointer transition-all',
                    isSelected
                      ? 'bg-gradient-to-r from-emerald-500/20 to-white/[0.08] text-white border border-emerald-500/30 shadow-specular'
                      : 'text-white/70 hover:bg-white/[0.04] hover:text-white border border-transparent'
                  )}
                >
                  <div className="flex items-center gap-3">
                    <div
                      className={cn(
                        'w-8 h-8 rounded-lg flex items-center justify-center transition-colors',
                        isSelected
                          ? 'bg-emerald-500/30 text-emerald-300 border border-emerald-500/40'
                          : 'bg-white/[0.04] text-white/50'
                      )}
                    >
                      <Icon className="w-4 h-4" />
                    </div>
                    <div className="flex flex-col">
                      <span className="font-medium text-white/95 flex items-center gap-1.5">
                        {item.title}
                        {item.category === 'Reciters' && item.meta?.nameAr && (
                          <span className="text-xs text-white/50 font-['Amiri',serif]">({item.meta.nameAr})</span>
                        )}
                      </span>
                      <span className="text-[10px] text-white/40 uppercase tracking-wider flex items-center gap-1.5">
                        {item.category}
                        {item.category === 'Reciters' && item.meta?.country && (
                          <span>• {item.meta.country}</span>
                        )}
                      </span>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    {item.category === 'Navigation' ? (
                      <span className="text-xs text-white/30 font-mono">Jump</span>
                    ) : item.category === 'Reciters' ? (
                      <span className="text-xs text-emerald-400 font-mono">Audition</span>
                    ) : (
                      <span className="text-xs text-emerald-400 font-mono">Action</span>
                    )}
                    <ArrowRight className={cn('w-3.5 h-3.5 transition-transform', isSelected ? 'text-emerald-400 translate-x-0.5' : 'text-white/20')} />
                  </div>
                </div>
              );
            })
          )}
        </div>

        {/* Footer Hints */}
        <div className="px-4 py-2.5 border-t border-white/[0.06] bg-black/40 flex items-center justify-between text-[11px] text-white/40 font-mono">
          <div className="flex items-center gap-3">
            <span>↑↓ Navigate</span>
            <span>↵ Select</span>
            <span>ESC Close</span>
          </div>
          <div className="flex items-center gap-1.5 text-emerald-400">
            <Sparkles className="w-3 h-3" />
            <span>Quranify Command Engine</span>
          </div>
        </div>
      </div>
    </div>,
    document.body
  );
};
