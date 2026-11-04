import React, { useState, useEffect } from 'react';
import { Search, Bell, RefreshCw, Palette, Check, Megaphone } from 'lucide-react';
import { useQueryClient } from '@tanstack/react-query';
import { adminQueryKeys } from '../../lib/queryClient';
import { useTheme, ACCENTS, AccentTheme } from '../../context/ThemeContext';

export interface HeaderProps {
  onOpenSearch?: () => void;
  onOpenShortcuts?: () => void;
  onOpenBroadcast?: () => void;
}

export const Header: React.FC<HeaderProps> = ({ onOpenSearch, onOpenShortcuts, onOpenBroadcast }) => {
  const [timeStr, setTimeStr] = useState('');
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [isAccentDropdownOpen, setIsAccentDropdownOpen] = useState(false);
  const queryClient = useQueryClient();
  const { accent, setAccent } = useTheme();

  useEffect(() => {
    const updateTime = () => {
      const now = new Date();
      setTimeStr(
        now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })
      );
    };
    updateTime();
    const interval = setInterval(updateTime, 1000);
    return () => clearInterval(interval);
  }, []);

  const handleRefreshAll = async () => {
    setIsRefreshing(true);
    await queryClient.invalidateQueries({ queryKey: adminQueryKeys.all });
    setTimeout(() => setIsRefreshing(false), 600);
  };

  return (
    <header className="h-16 px-8 border-b border-white/[0.08] bg-[#050506]/70 backdrop-blur-xl flex items-center justify-between sticky top-0 z-20">
      {/* Global Inset Search */}
      <div className="flex items-center gap-3 w-96">
        <div
          onClick={onOpenSearch}
          className="relative w-full cursor-pointer group"
          title="Open Command Palette (⌘K / Ctrl+K)"
        >
          <Search className="w-4 h-4 text-white/40 absolute left-3 top-1/2 -translate-y-1/2 group-hover:text-white/70 transition-colors" />
          <input
            type="text"
            readOnly
            placeholder="Search listeners, reciters, surahs, playlists..."
            className="w-full pl-9 pr-14 py-1.5 text-xs bg-black/40 border border-white/[0.08] rounded-xl text-white/80 placeholder-white/40 focus:outline-none hover:border-white/20 group-hover:border-white/20 transition-all font-sans cursor-pointer"
          />
          <div className="absolute right-2 top-1/2 -translate-y-1/2 flex items-center gap-1">
            <kbd className="px-1.5 py-0.5 text-[10px] font-mono bg-white/10 rounded border border-white/20 text-white/60 group-hover:text-white transition-colors">
              ⌘K
            </kbd>
          </div>
        </div>
      </div>

      {/* Right Controls & Info */}
      <div className="flex items-center gap-4">
        {/* Accent Glow Picker Dropdown */}
        <div className="relative">
          <button
            onClick={() => setIsAccentDropdownOpen(prev => !prev)}
            title="Customize Theme Accent & Glow"
            className="flex items-center gap-2 px-3 py-1.5 rounded-xl bg-white/[0.04] hover:bg-white/[0.08] border border-white/[0.08] text-xs text-white/80 transition-all cursor-pointer"
          >
            <Palette className="w-3.5 h-3.5 text-[var(--accent-primary)]" />
            <span className="hidden sm:inline capitalize">{accent}</span>
            <span className={`w-2 h-2 rounded-full ${ACCENTS[accent]?.dotClass || 'bg-emerald-500'}`} />
          </button>

          {isAccentDropdownOpen && (
            <>
              <div 
                className="fixed inset-0 z-30" 
                onClick={() => setIsAccentDropdownOpen(false)} 
              />
              <div className="absolute right-0 mt-2 w-56 p-2 rounded-2xl bg-[#111114]/95 border border-white/[0.12] shadow-2xl backdrop-blur-2xl z-40 animate-modal-in flex flex-col gap-1">
                <div className="px-3 py-1.5 text-[11px] font-semibold text-white/40 uppercase tracking-wider font-mono border-b border-white/[0.06] mb-1">
                  Theme Accent & Glow
                </div>
                {(Object.keys(ACCENTS) as AccentTheme[]).map((key) => {
                  const item = ACCENTS[key];
                  const isSelected = accent === key;
                  return (
                    <button
                      key={key}
                      onClick={() => {
                        setAccent(key);
                        setIsAccentDropdownOpen(false);
                      }}
                      className={`w-full flex items-center justify-between px-3 py-2 rounded-xl text-xs transition-all cursor-pointer ${
                        isSelected 
                          ? 'bg-white/[0.08] text-white font-medium border border-white/[0.1]' 
                          : 'text-white/70 hover:text-white hover:bg-white/[0.04] border border-transparent'
                      }`}
                    >
                      <div className="flex items-center gap-2.5">
                        <span className={`w-3 h-3 rounded-full ${item.dotClass} shadow-[0_0_10px_currentColor]`} />
                        <div className="flex flex-col text-left">
                          <span className="leading-tight">{item.name}</span>
                          <span className="text-[10px] text-white/40 font-mono">{item.label}</span>
                        </div>
                      </div>
                      {isSelected && (
                        <Check className="w-3.5 h-3.5 text-[var(--accent-primary)]" />
                      )}
                    </button>
                  );
                })}
              </div>
            </>
          )}
        </div>

        {/* Manual Refresh Data */}
        <button
          onClick={handleRefreshAll}
          title="Refresh All Real-time Data"
          className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg text-xs text-white/60 hover:text-white bg-white/[0.04] hover:bg-white/[0.08] border border-white/[0.08] transition-all"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin text-[var(--accent-primary)]' : ''}`} />
          <span className="hidden sm:inline">Sync Data</span>
        </button>

        {/* Live Clock */}
        <div className="hidden md:flex items-center gap-2 px-3 py-1.5 rounded-xl bg-white/[0.03] border border-white/[0.06] text-xs font-mono text-white/70">
          <span className="w-1.5 h-1.5 rounded-full bg-[var(--accent-primary)] animate-pulse" />
          <span>{timeStr || '10:00:00'}</span>
        </div>

        {/* Keyboard Shortcuts Guide Trigger */}
        <button
          onClick={onOpenShortcuts}
          title="Keyboard Shortcuts (?)"
          className="p-2 rounded-xl text-white/60 hover:text-white hover:bg-white/[0.06] border border-white/[0.06] hover:border-white/[0.1] transition-all flex items-center justify-center font-mono text-xs w-9 h-9"
        >
          ?
        </button>

        {/* Broadcast Alert Trigger Button */}
        <button
          onClick={onOpenBroadcast}
          title="Dispatch In-App Broadcast & Push Notification"
          className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-medium text-[var(--accent-primary)] bg-[var(--accent-primary)]/10 hover:bg-[var(--accent-primary)]/20 border border-[var(--accent-primary)]/30 transition-all shadow-sm cursor-pointer"
        >
          <Megaphone className="w-3.5 h-3.5" />
          <span className="hidden md:inline">Broadcast Alert</span>
        </button>

        {/* System Notification */}
        <button className="relative p-2 rounded-xl text-white/60 hover:text-white hover:bg-white/[0.06] border border-transparent hover:border-white/[0.1] transition-all">
          <Bell className="w-4 h-4" />
          <span className="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-[var(--accent-primary)]" />
        </button>

        {/* Admin Profile Pill */}
        <div className="flex items-center gap-2.5 pl-3 border-l border-white/[0.08]">
          <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-[var(--accent-primary)]/30 to-white/20 border border-white/20 flex items-center justify-center text-white text-xs font-semibold shadow-specular">
            AD
          </div>
          <div className="hidden sm:flex flex-col text-left">
            <span className="text-xs font-medium text-white leading-tight">Administrator</span>
            <span className="text-[10px] text-[var(--accent-primary)] font-mono">PostgreSQL Root</span>
          </div>
        </div>
      </div>
    </header>
  );
};
