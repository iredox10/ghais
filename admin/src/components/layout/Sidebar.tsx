import React from 'react';
import { Link, useRouterState } from '@tanstack/react-router';
import {
  LayoutDashboard,
  Users,
  Mic2,
  BookOpen,
  BarChart3,
  ListMusic,
  Smile,
  Activity,
  Settings,
  Database,
  Radio,
  ExternalLink
} from 'lucide-react';
import { AppLogo } from './AppLogo';
import { useHealth } from '../../lib/queryClient';

const NAV_ITEMS = [
  { label: 'Overview', href: '/', icon: LayoutDashboard },
  { label: 'Users & Listeners', href: '/users', icon: Users },
  { label: 'Reciters Catalog', href: '/reciters', icon: Mic2 },
  { label: 'Surahs & Mushaf', href: '/surahs', icon: BookOpen },
  { label: 'Listening Analytics', href: '/analytics', icon: BarChart3 },
  { label: 'Playlists & Curation', href: '/playlists', icon: ListMusic },
  { label: 'Moods & Vibes', href: '/moods', icon: Smile },
  { label: 'Live Stream', href: '/activity', icon: Activity },
  { label: 'PostgreSQL Config', href: '/settings', icon: Settings },
];

export const Sidebar: React.FC = () => {
  const routerState = useRouterState();
  const currentPath = routerState.location.pathname;
  const { data: health } = useHealth();

  const isLive = health?.mode === 'live' && health?.databaseConnected;

  return (
    <aside className="w-64 h-screen sticky top-0 flex flex-col justify-between p-4 border-r border-white/[0.08] bg-[#07070A]/80 backdrop-blur-2xl z-30 select-none">
      <div className="flex flex-col gap-6">
        {/* Logo Section */}
        <div className="px-2 pt-2">
          <AppLogo />
        </div>

        {/* Navigation Section */}
        <nav className="flex flex-col gap-1.5">
          <div className="px-3 pb-2 text-[10px] font-semibold tracking-wider text-white/40 uppercase">
            Platform Command
          </div>
          {NAV_ITEMS.map((item) => {
            const Icon = item.icon;
            const isActive = item.href === '/' ? currentPath === '/' : currentPath.startsWith(item.href);

            return (
              <Link
                key={item.href}
                to={item.href}
                className={`flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all group ${
                  isActive
                    ? 'bg-gradient-to-b from-white/15 to-white/5 text-white border border-white/20 shadow-specular'
                    : 'text-white/60 hover:text-white hover:bg-white/[0.04]'
                }`}
              >
                <div
                  className={`flex items-center justify-center w-7 h-7 rounded-lg transition-colors ${
                    isActive
                      ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                      : 'bg-white/[0.03] text-white/50 group-hover:text-white group-hover:bg-white/[0.08]'
                  }`}
                >
                  <Icon className="w-4 h-4" />
                </div>
                <span>{item.label}</span>
                {item.href === '/activity' && (
                  <span className="ml-auto flex h-2 w-2 relative">
                    <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                    <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
                  </span>
                )}
              </Link>
            );
          })}
        </nav>
      </div>

      {/* Footer / System Status */}
      <div className="flex flex-col gap-3 pt-4 border-t border-white/[0.08]">
        {/* PostgreSQL Status Card */}
        <div className="p-3 rounded-xl bg-white/[0.03] border border-white/[0.06] flex flex-col gap-2">
          <div className="flex items-center justify-between">
            <span className="text-xs text-white/50 flex items-center gap-1.5 font-medium">
              <Database className="w-3.5 h-3.5 text-white/60" />
              PostgreSQL DB
            </span>
            <div className="flex items-center gap-1.5">
              <span
                className={`w-2 h-2 rounded-full ${
                  isLive ? 'bg-emerald-400 shadow-[0_0_8px_rgba(52,211,153,0.8)]' : 'bg-amber-400 shadow-[0_0_8px_rgba(251,191,36,0.6)]'
                }`}
              />
              <span className="text-[11px] text-white/80 font-mono">
                {isLive ? 'Live' : 'Unavailable'}
              </span>
            </div>
          </div>
          <div className="text-[11px] text-white/40 truncate font-mono">
            {health?.projectId || 'tikitakatalk'} • {health?.databaseId || 'ghais'}
          </div>
        </div>

        {/* Live Audio Sync Badge */}
        <div className="flex items-center justify-between px-2 text-xs text-white/40">
          <span className="flex items-center gap-1.5">
            <Radio className="w-3 h-3 text-emerald-400 animate-pulse" />
            EveryAyah & CDN
          </span>
          <span className="font-mono text-[10px] text-white/30">v1.2.0</span>
        </div>
      </div>
    </aside>
  );
};
