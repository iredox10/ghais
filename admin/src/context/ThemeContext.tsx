import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';

export type AccentTheme = 'emerald' | 'cyan' | 'gold' | 'amethyst';

export interface AccentConfig {
  id: AccentTheme;
  name: string;
  label: string;
  color: string;
  glow: string;
  dotClass: string;
}

export const ACCENTS: Record<AccentTheme, AccentConfig> = {
  emerald: {
    id: 'emerald',
    name: 'Emerald',
    label: 'Default',
    color: '#10B981',
    glow: 'rgba(16, 185, 129, 0.3)',
    dotClass: 'bg-emerald-500',
  },
  cyan: {
    id: 'cyan',
    name: 'Celestial',
    label: 'Cyan',
    color: '#06B6D4',
    glow: 'rgba(6, 182, 212, 0.3)',
    dotClass: 'bg-cyan-500',
  },
  gold: {
    id: 'gold',
    name: 'Madinah',
    label: 'Gold',
    color: '#F59E0B',
    glow: 'rgba(245, 158, 11, 0.3)',
    dotClass: 'bg-amber-500',
  },
  amethyst: {
    id: 'amethyst',
    name: 'Tahajjud',
    label: 'Amethyst',
    color: '#A855F7',
    glow: 'rgba(168, 85, 247, 0.3)',
    dotClass: 'bg-purple-500',
  },
};

interface ThemeContextType {
  accent: AccentTheme;
  setAccent: (accent: AccentTheme) => void;
  accentConfig: AccentConfig;
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

const STORAGE_KEY = 'quranify-admin-accent';

export const ThemeProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [accent, setAccentState] = useState<AccentTheme>(() => {
    try {
      const saved = localStorage.getItem(STORAGE_KEY) as AccentTheme;
      if (saved && ACCENTS[saved]) {
        return saved;
      }
    } catch (e) {
      console.error('Failed to load theme accent from localStorage:', e);
    }
    return 'emerald';
  });

  const applyAccentToDOM = useCallback((themeName: AccentTheme) => {
    const config = ACCENTS[themeName] || ACCENTS.emerald;
    const root = document.documentElement;
    root.style.setProperty('--accent-primary', config.color);
    root.style.setProperty('--accent-glow', config.glow);
  }, []);

  useEffect(() => {
    applyAccentToDOM(accent);
  }, [accent, applyAccentToDOM]);

  const setAccent = useCallback((newAccent: AccentTheme) => {
    if (!ACCENTS[newAccent]) return;
    setAccentState(newAccent);
    try {
      localStorage.setItem(STORAGE_KEY, newAccent);
    } catch (e) {
      console.error('Failed to save theme accent to localStorage:', e);
    }
    applyAccentToDOM(newAccent);
  }, [applyAccentToDOM]);

  return (
    <ThemeContext.Provider value={{ accent, setAccent, accentConfig: ACCENTS[accent] || ACCENTS.emerald }}>
      {children}
    </ThemeContext.Provider>
  );
};

export const useTheme = () => {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }
  return context;
};
