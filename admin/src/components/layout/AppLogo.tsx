import React from 'react';

interface AppLogoProps {
  collapsed?: boolean;
}

export const AppLogo: React.FC<AppLogoProps> = ({ collapsed = false }) => {
  return (
    <div className="flex items-center gap-3 select-none">
      <div className="relative flex items-center justify-center w-10 h-10 rounded-xl bg-gradient-to-b from-white/15 to-white/5 border border-white/20 shadow-specular overflow-hidden group">
        <div className="absolute inset-0 bg-emerald-500/20 blur-md rounded-full group-hover:bg-emerald-500/30 transition-all" />
        <svg
          viewBox="0 0 100 100"
          className="w-6 h-6 relative z-10 drop-shadow-[0_2px_8px_rgba(16,185,129,0.5)]"
          fill="none"
        >
          <path
            d="M50 18 C50 18, 30 45, 30 62 C30 73 39 82 50 82 C61 82 70 73 70 62 C70 45, 50 18, 50 18 Z"
            fill="url(#logoChrome)"
            stroke="rgba(255,255,255,0.7)"
            strokeWidth="3"
          />
          <path
            d="M50 32 C50 32, 40 52, 40 64 C40 70 44 75 50 75 C47 71 46 66 46 60 C46 50 50 32 50 32 Z"
            fill="#10B981"
          />
          <defs>
            <linearGradient id="logoChrome" x1="20" y1="10" x2="80" y2="90">
              <stop offset="0%" stopColor="#FFFFFF" />
              <stop offset="100%" stopColor="#A1A1AA" />
            </linearGradient>
          </defs>
        </svg>
      </div>

      {!collapsed && (
        <div className="flex flex-col">
          <div className="flex items-center gap-2">
            <span className="text-lg font-bold tracking-tight text-white">Ghais</span>
            <span className="text-xs px-1.5 py-0.5 rounded bg-white/10 text-emerald-400 font-mono font-medium border border-emerald-500/20">
              Admin
            </span>
          </div>
          <span className="text-[11px] text-white/40 tracking-wider font-['Amiri',serif]">
            غَيْث • مركز القيادة
          </span>
        </div>
      )}
    </div>
  );
};
