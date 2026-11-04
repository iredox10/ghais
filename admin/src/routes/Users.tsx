import React, { useState } from 'react';
import { ColumnDef } from '@tanstack/react-table';
import {
  Users as UsersIcon,
  Flame,
  Clock,
  BookOpen,
  Calendar,
  Sparkles,
  ExternalLink,
  ShieldCheck,
  Mail,
  AlarmClock,
  ListMusic,
  History,
  Trash2,
  Download
} from 'lucide-react';
import { useUsers, useUserDetails, useDeleteSchedule, adminQueryKeys, useQueryClient } from '../lib/queryClient';
import type { User } from '../api/types';
import { DataTable } from '../components/table/DataTable';
import { ColumnHeader } from '../components/table/ColumnHeader';
import { Badge } from '../components/ui/Badge';
import { Modal } from '../components/ui/Modal';
import { GlassCard } from '../components/ui/GlassCard';

export const Users: React.FC = () => {
  const { data, isLoading } = useUsers();
  const [selectedUserId, setSelectedUserId] = useState<string | null>(null);
  const { data: userDetails, isLoading: isDetailsLoading } = useUserDetails(selectedUserId || '', {
    enabled: !!selectedUserId
  });
  const queryClient = useQueryClient();

  const deleteScheduleMutation = useDeleteSchedule({
    onSuccess: () => {
      if (selectedUserId) {
        queryClient.invalidateQueries({ queryKey: adminQueryKeys.userDetails(selectedUserId) });
      }
      queryClient.invalidateQueries({ queryKey: adminQueryKeys.users() });
    }
  });

  const handleDeleteSchedule = (scheduleId: string) => {
    deleteScheduleMutation.mutate(scheduleId);
  };

  const handleDownloadDossier = (dossierData: any) => {
    const jsonString = `data:text/json;charset=utf-8,${encodeURIComponent(
      JSON.stringify(dossierData, null, 2)
    )}`;
    const downloadAnchor = document.createElement('a');
    downloadAnchor.setAttribute('href', jsonString);
    downloadAnchor.setAttribute('download', `listener_dossier_${dossierData.user?.id || 'export'}.json`);
    document.body.appendChild(downloadAnchor);
    downloadAnchor.click();
    downloadAnchor.remove();
  };

  const columns: ColumnDef<User>[] = [
    {
      accessorKey: 'name',
      header: ({ column }) => <ColumnHeader column={column} title="Listener Name & Account" />,
      cell: ({ row }) => {
        const user = row.original;
        return (
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-white/20 to-white/5 border border-white/10 flex items-center justify-center font-bold text-xs text-white shadow-specular">
              {user.name.charAt(0)}
            </div>
            <div className="flex flex-col">
              <span className="text-xs font-semibold text-white">{user.name}</span>
              <span className="text-[11px] text-white/40 font-mono">{user.email}</span>
            </div>
          </div>
        );
      },
    },
    {
      accessorKey: 'provider',
      header: ({ column }) => <ColumnHeader column={column} title="Auth Source" />,
      cell: ({ row }) => {
        const provider = row.original.provider;
        return provider === 'google' ? (
          <Badge variant="cyan" className="font-mono text-[10px]">
            Google OAuth
          </Badge>
        ) : (
          <Badge variant="ghost" className="font-mono text-[10px]">
            Email / Pass
          </Badge>
        );
      },
    },
    {
      accessorKey: 'totalMinutes',
      header: ({ column }) => <ColumnHeader column={column} title="Total Recitation" />,
      cell: ({ row }) => {
        const mins = row.original.totalMinutes;
        const hrs = Math.floor(mins / 60);
        const remMins = mins % 60;
        return (
          <div className="flex items-center gap-1.5 font-mono text-xs text-emerald-400 font-medium">
            <Clock className="w-3.5 h-3.5 text-emerald-400" />
            <span>{hrs > 0 ? `${hrs}h ${remMins}m` : `${remMins}m`}</span>
          </div>
        );
      },
    },
    {
      accessorKey: 'daysStreak',
      header: ({ column }) => <ColumnHeader column={column} title="Daily Streak" />,
      cell: ({ row }) => {
        const streak = row.original.daysStreak;
        return (
          <div className="flex items-center gap-1.5 font-mono text-xs">
            <Flame className="w-3.5 h-3.5 text-amber-400" />
            <span className="text-white font-semibold">{streak} days</span>
          </div>
        );
      },
    },
    {
      accessorKey: 'uniqueSurahs',
      header: ({ column }) => <ColumnHeader column={column} title="Unique Surahs" />,
      cell: ({ row }) => (
        <span className="text-xs text-white/70 font-mono">
          {row.original.uniqueSurahs} / 114
        </span>
      ),
    },
    {
      accessorKey: 'schedulesCount',
      header: ({ column }) => <ColumnHeader column={column} title="Alarms" />,
      cell: ({ row }) => (
        <Badge variant={row.original.schedulesCount > 0 ? 'indigo' : 'ghost'} className="text-[10px] font-mono">
          {row.original.schedulesCount} scheduled
        </Badge>
      ),
    },
    {
      id: 'actions',
      header: () => <span className="text-right">Profile</span>,
      cell: ({ row }) => {
        const user = row.original;
        return (
          <button
            onClick={() => setSelectedUserId(user.id)}
            className="px-2.5 py-1 rounded-lg text-xs bg-white/[0.05] hover:bg-white/[0.1] text-white/80 hover:text-white border border-white/[0.08] transition-all"
          >
            Inspect
          </button>
        );
      },
    },
  ];

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2">
            <UsersIcon className="w-6 h-6 text-emerald-400" />
            Registered Listeners & Accounts
          </h1>
          <p className="text-sm text-white/50 mt-1">
            Browse all user accounts, listening habits, streaks, and scheduled alarms.
          </p>
        </div>
      </div>

      {/* TanStack Table Card */}
      <GlassCard className="p-6">
        <DataTable
          columns={columns}
          data={data?.users || []}
          isLoading={isLoading}
          searchPlaceholder="Filter listeners by name, email, or ID..."
        />
      </GlassCard>

      {/* User Details Modal */}
      <Modal
        isOpen={!!selectedUserId}
        onClose={() => setSelectedUserId(null)}
        title="Listener Intelligence Profile"
      >
        {isDetailsLoading || !userDetails ? (
          <div className="flex items-center justify-center p-8 text-white/50 text-xs">
            Loading user profile from PostgreSQL database...
          </div>
        ) : (
          <div className="flex flex-col gap-5 pt-2">
            {/* Header Identity */}
            <div className="flex items-center justify-between p-4 rounded-xl bg-white/[0.03] border border-white/[0.06]">
              <div className="flex items-center gap-4">
                <div className="w-12 h-12 rounded-xl bg-gradient-to-tr from-emerald-500/30 to-white/20 border border-white/20 flex items-center justify-center text-base font-bold text-white shadow-specular">
                  {userDetails.user.name.charAt(0)}
                </div>
                <div className="flex flex-col">
                  <span className="text-sm font-bold text-white">{userDetails.user.name}</span>
                  <span className="text-xs text-white/40 font-mono">{userDetails.user.email}</span>
                  <div className="flex items-center gap-2 mt-1">
                    <Badge variant="cyan" className="text-[10px]">
                      {userDetails.user.provider}
                    </Badge>
                    <span className="text-[10px] text-white/30 font-mono">
                      ID: {userDetails.user.id}
                    </span>
                  </div>
                </div>
              </div>
              <button
                onClick={() => handleDownloadDossier(userDetails)}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 font-medium transition-all"
                title="Export Listener Dossier as JSON"
              >
                <Download className="w-3.5 h-3.5" />
                Download Dossier
              </button>
            </div>

            {/* Quick Stats Grid */}
            <div className="grid grid-cols-3 gap-3">
              <div className="p-3 rounded-xl bg-white/[0.02] border border-white/[0.05] flex flex-col">
                <span className="text-[10px] text-white/40 uppercase">Listening Time</span>
                <span className="text-base font-bold text-emerald-400 font-mono mt-1">
                  {userDetails.stats.totalHours} hrs
                </span>
              </div>
              <div className="p-3 rounded-xl bg-white/[0.02] border border-white/[0.05] flex flex-col">
                <span className="text-[10px] text-white/40 uppercase">Current Streak</span>
                <span className="text-base font-bold text-amber-400 font-mono mt-1">
                  {userDetails.stats.streak} days 🔥
                </span>
              </div>
              <div className="p-3 rounded-xl bg-white/[0.02] border border-white/[0.05] flex flex-col">
                <span className="text-[10px] text-white/40 uppercase">Top Reciter</span>
                <span className="text-xs font-semibold text-white truncate mt-1">
                  {userDetails.stats.topReciter}
                </span>
              </div>
            </div>

            {/* Configured Daily Alarms */}
            <div className="flex flex-col gap-2">
              <span className="text-xs font-semibold text-white/80 flex items-center gap-1.5">
                <AlarmClock className="w-3.5 h-3.5 text-purple-400" />
                Configured Recitation Alarms
              </span>
              <div className="flex flex-col gap-2">
                {userDetails.schedules && userDetails.schedules.length > 0 ? (
                  userDetails.schedules.map((sch) => (
                    <div
                      key={sch.id}
                      className="flex items-center justify-between p-3 rounded-xl bg-white/[0.02] border border-white/[0.05] text-xs"
                    >
                      <div className="flex items-center gap-3">
                        <span className="px-2 py-0.5 rounded bg-purple-500/10 text-purple-400 font-mono font-medium border border-purple-500/20">
                          {sch.time}
                        </span>
                        <div className="flex flex-col">
                          <span className="font-medium text-white">{sch.label}</span>
                          <span className="text-[11px] text-white/40">
                            {sch.reciter} • {sch.surahs}
                          </span>
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        <Badge variant={sch.enabled ? 'emerald' : 'ghost'} className="text-[10px]">
                          {sch.enabled ? 'Active' : 'Paused'}
                        </Badge>
                        <button
                          onClick={() => handleDeleteSchedule(sch.id)}
                          disabled={deleteScheduleMutation.isPending}
                          className="p-1.5 rounded-lg bg-white/[0.03] hover:bg-red-500/10 text-white/40 hover:text-red-400 border border-white/[0.06] hover:border-red-500/20 transition-all disabled:opacity-50"
                          title="Delete Alarm Schedule"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>
                  ))
                ) : (
                  <div className="p-3 rounded-xl bg-white/[0.02] border border-white/[0.05] text-center text-xs text-white/40">
                    No active recitation alarms configured.
                  </div>
                )}
              </div>
            </div>

            {/* Custom Playlists */}
            <div className="flex flex-col gap-2">
              <span className="text-xs font-semibold text-white/80 flex items-center gap-1.5">
                <ListMusic className="w-3.5 h-3.5 text-cyan-400" />
                User Created Playlists
              </span>
              <div className="flex flex-col gap-2">
                {userDetails.playlists.map((pl) => (
                  <div
                    key={pl.id}
                    className="flex items-center justify-between p-3 rounded-xl bg-white/[0.02] border border-white/[0.05] text-xs"
                  >
                    <span className="font-medium text-white">{pl.title}</span>
                    <span className="text-white/40 font-mono">{pl.tracks} tracks</span>
                  </div>
                ))}
              </div>
            </div>

            {/* Actual Listening Sessions from PostgreSQL History */}
            <div className="flex flex-col gap-2">
              <span className="text-xs font-semibold text-white/80 flex items-center gap-1.5">
                <History className="w-3.5 h-3.5 text-emerald-400" />
                Listening Sessions (PostgreSQL History)
              </span>
              <div className="flex flex-col gap-2 max-h-60 overflow-y-auto pr-1">
                {userDetails.history && userDetails.history.length > 0 ? (
                  userDetails.history.map((item) => (
                    <div
                      key={item.id}
                      className="flex items-center justify-between p-3 rounded-xl bg-white/[0.02] border border-white/[0.05] text-xs hover:bg-white/[0.04] transition-all"
                    >
                      <div className="flex items-center gap-3">
                        <div className="w-7 h-7 rounded-lg bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center font-mono text-[10px] text-emerald-400">
                          {item.surahNumber || 'Q'}
                        </div>
                        <div className="flex flex-col">
                          <span className="font-medium text-white">{item.surahName}</span>
                          <span className="text-[11px] text-white/40">
                            {item.reciterName}
                          </span>
                        </div>
                      </div>
                      <div className="flex items-center gap-3">
                        <span className="text-[11px] text-emerald-400/90 font-mono">
                          {item.duration}
                        </span>
                        <span className="text-[10px] text-white/30 font-mono">
                          {item.playedTime}
                        </span>
                      </div>
                    </div>
                  ))
                ) : (
                  <div className="p-4 rounded-xl bg-white/[0.02] border border-white/[0.05] text-center text-xs text-white/40">
                    No listening history recorded in PostgreSQL for this user yet.
                  </div>
                )}
              </div>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};
