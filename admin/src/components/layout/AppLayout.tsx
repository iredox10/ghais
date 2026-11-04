import React, { useState, useEffect } from 'react';
import { Outlet, useNavigate } from '@tanstack/react-router';
import { Sidebar } from './Sidebar';
import { Header } from './Header';
import { CommandPalette } from './CommandPalette';
import { ShortcutsModal } from './ShortcutsModal';
import { BroadcastModal } from '../broadcast/BroadcastModal';
import { AudioPlayerProvider, useAudioPlayer } from '../../context/AudioPlayerContext';
import { ThemeProvider } from '../../context/ThemeContext';
import { AudioPlayerBar } from '../audio/AudioPlayerBar';

const AppInnerLayout: React.FC = () => {
  const [isCommandPaletteOpen, setIsCommandPaletteOpen] = useState(false);
  const [isShortcutsModalOpen, setIsShortcutsModalOpen] = useState(false);
  const [isBroadcastModalOpen, setIsBroadcastModalOpen] = useState(false);
  const navigate = useNavigate();
  const audio = useAudioPlayer();

  useEffect(() => {
    let gPressed = false;
    let gTimeout: NodeJS.Timeout | null = null;

    const handleKeyDown = (e: KeyboardEvent) => {
      const target = e.target as HTMLElement;
      const isInput = ['INPUT', 'TEXTAREA', 'SELECT'].includes(target?.tagName);

      // Cmd+K / Ctrl+K
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault();
        setIsCommandPaletteOpen(prev => !prev);
        return;
      }

      // Escape -> close modals
      if (e.key === 'Escape') {
        setIsCommandPaletteOpen(false);
        setIsShortcutsModalOpen(false);
        return;
      }

      // Do not trigger single letter shortcuts if typing in an input
      if (isInput) return;

      // ? key (Shift + / or key === '?')
      if (e.key === '?' || (e.shiftKey && e.key === '/')) {
        e.preventDefault();
        setIsShortcutsModalOpen(prev => !prev);
        return;
      }

      // Space -> Play / Pause Audio
      if (e.key === ' ') {
        if (audio.currentTrack) {
          e.preventDefault();
          if (audio.isPlaying) {
            audio.pauseTrack();
          } else {
            audio.resumeTrack();
          }
        }
        return;
      }

      // M -> Mute / Unmute Audio
      if (e.key.toLowerCase() === 'm') {
        e.preventDefault();
        if (audio.volume > 0) {
          (audio as any)._prevVolume = audio.volume;
          audio.setVolume(0);
        } else {
          audio.setVolume((audio as any)._prevVolume || 0.8);
        }
        return;
      }

      // G then key navigation sequence
      if (e.key.toLowerCase() === 'g' && !gPressed) {
        gPressed = true;
        if (gTimeout) clearTimeout(gTimeout);
        gTimeout = setTimeout(() => {
          gPressed = false;
        }, 1200);
        return;
      }

      if (gPressed) {
        gPressed = false;
        if (gTimeout) clearTimeout(gTimeout);
        const key = e.key.toLowerCase();
        switch (key) {
          case 'd':
            e.preventDefault();
            navigate({ to: '/' });
            break;
          case 'u':
            e.preventDefault();
            navigate({ to: '/users' });
            break;
          case 'r':
            e.preventDefault();
            navigate({ to: '/reciters' });
            break;
          case 's':
            e.preventDefault();
            navigate({ to: '/surahs' });
            break;
          case 'm':
            e.preventDefault();
            navigate({ to: '/moods' });
            break;
          case 'a':
            e.preventDefault();
            navigate({ to: '/analytics' });
            break;
          case 'p':
            e.preventDefault();
            navigate({ to: '/playlists' });
            break;
          case 'c':
            e.preventDefault();
            navigate({ to: '/settings' });
            break;
          default:
            break;
        }
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      if (gTimeout) clearTimeout(gTimeout);
    };
  }, [navigate, audio]);

  return (
    <div className="flex min-h-screen bg-[#050506] text-white relative">
      {/* Sidebar */}
      <Sidebar />

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0 pb-28">
        <Header
          onOpenSearch={() => setIsCommandPaletteOpen(true)}
          onOpenShortcuts={() => setIsShortcutsModalOpen(true)}
          onOpenBroadcast={() => setIsBroadcastModalOpen(true)}
        />

        <main className="flex-1 p-8 overflow-y-auto">
          {/* Subtle Ambient Radial Glow */}
          <div 
            className="pointer-events-none fixed top-0 right-1/4 w-[600px] h-[300px] blur-[140px] rounded-full transition-all duration-500" 
            style={{ background: 'var(--accent-glow)' }}
          />
          
          <Outlet />
        </main>
      </div>

      {/* Global Floating Audio Player Bar */}
      <AudioPlayerBar />

      {/* Global Command Palette */}
      <CommandPalette
        isOpen={isCommandPaletteOpen}
        onClose={() => setIsCommandPaletteOpen(false)}
      />

      {/* Global Keyboard Shortcuts Modal */}
      <ShortcutsModal
        isOpen={isShortcutsModalOpen}
        onClose={() => setIsShortcutsModalOpen(false)}
      />

      {/* Global Broadcast Modal */}
      <BroadcastModal
        isOpen={isBroadcastModalOpen}
        onClose={() => setIsBroadcastModalOpen(false)}
      />
    </div>
  );
};

export const AppLayout: React.FC = () => {
  return (
    <ThemeProvider>
      <AudioPlayerProvider>
        <AppInnerLayout />
      </AudioPlayerProvider>
    </ThemeProvider>
  );
};
