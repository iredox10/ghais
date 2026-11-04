import React, { useState } from 'react';
import {
  ListMusic,
  Plus,
  Play,
  Pause,
  Square,
  Share2,
  Lock,
  Globe,
  Sparkles,
  Headphones,
  CheckCircle2,
  Trash2,
  X,
  Music,
  Volume2
} from 'lucide-react';
import { usePlaylists, useCreatePlaylist, useDeletePlaylist, usePlaylistTracks, useQueryClient, adminQueryKeys } from '../lib/queryClient';
import { useAudioPlayer } from '../hooks/useAudioPlayer';
import { GlassCard } from '../components/ui/GlassCard';
import { Badge } from '../components/ui/Badge';
import { Modal } from '../components/ui/Modal';
import { StatCard } from '../components/ui/StatCard';
import type { Playlist, PlaylistTrack } from '../api/types';

// Track view served by GET /api/playlists/:id/tracks. Base keys come from
// PlaylistTrack; the canonical app-schema range (position, ayah_from,
// ayah_to) arrives as position/ayahFrom/ayahTo/ayahRange — reciter + surah
// names are resolved server-side from the catalogs. There is no
// track_json/sequence/surah_name/plays anywhere in the app schema.
type TrackView = PlaylistTrack & {
  position?: number;
  ayahFrom?: number;
  ayahTo?: number;
  ayahRange?: string;
};

const PlaylistInspectorModal: React.FC<{
  playlist: Playlist | null;
  onClose: () => void;
}> = ({ playlist, onClose }) => {
  const { data: tracksData, isLoading: tracksLoading } = usePlaylistTracks(playlist?.id || '', {
    enabled: Boolean(playlist),
  });
  const audioPlayer = useAudioPlayer();

  if (!playlist) return null;

  const tracks = (tracksData?.tracks || []) as TrackView[];

  return (
    <Modal
      isOpen={Boolean(playlist)}
      onClose={() => {
        audioPlayer.stop();
        onClose();
      }}
      title=""
    >
      <div className="flex flex-col gap-6 -m-2 sm:-m-4">
        {/* Header Cover & Info */}
        <div className="relative h-48 w-full overflow-hidden bg-black/60 rounded-t-2xl">
          <img
            src={playlist.coverUrl}
            alt={playlist.title}
            onError={(e) => {
              (e.target as HTMLImageElement).src = 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=400&q=80';
            }}
            className="w-full h-full object-cover opacity-70"
          />
          <div className="absolute inset-0 bg-gradient-to-t from-[#050506] via-[#050506]/50 to-transparent" />
          
          <button
            onClick={() => {
              audioPlayer.stop();
              onClose();
            }}
            className="absolute top-3 right-3 p-2 rounded-xl bg-black/60 hover:bg-white/20 text-white/80 border border-white/10 transition-all backdrop-blur-md cursor-pointer z-10"
          >
            <X className="w-4 h-4" />
          </button>

          <div className="absolute bottom-4 left-6 right-6 flex items-end justify-between">
            <div>
              <div className="flex items-center gap-2 mb-1">
                <Badge variant="emerald" className="text-[10px] backdrop-blur-md">
                  {playlist.isPublic ? 'Public Playlist' : 'Editorial Collection'}
                </Badge>
                <span className="text-xs text-emerald-400 font-mono">
                  {playlist.creator}
                </span>
              </div>
              <h2 className="text-xl font-bold text-white tracking-tight">
                {playlist.title}
              </h2>
            </div>
          </div>
        </div>

        {/* Details & Actions */}
        <div className="px-6 flex flex-col gap-4">
          <p className="text-xs text-white/70 leading-relaxed">
            {playlist.description}
          </p>

          <div className="flex items-center justify-between gap-4 p-3 rounded-xl bg-white/[0.03] border border-white/[0.08]">
            <div className="flex items-center gap-3 text-xs text-white/60 font-mono">
              <span className="flex items-center gap-1">
                <Music className="w-3.5 h-3.5 text-emerald-400" />
                {tracks.length} Recitations
              </span>
              <span>•</span>
              <span className="flex items-center gap-1">
                <Headphones className="w-3.5 h-3.5 text-cyan-400" />
                {tracks.length} recitations • {playlist.isPublic ? 'Public' : 'Private'}
              </span>
            </div>

            <button
              onClick={() => audioPlayer.playAll(tracks)}
              disabled={tracks.length === 0}
              className="chrome-btn px-4 py-2 text-xs rounded-xl flex items-center gap-2 shadow-lg shadow-emerald-500/20 cursor-pointer disabled:opacity-50"
            >
              <Play className="w-3.5 h-3.5 fill-current" />
              Play All / Audition
            </button>
          </div>

          {/* Active Audio Player Banner if playing */}
          {audioPlayer.currentTrack && (
            <div className="flex items-center justify-between p-3 rounded-xl bg-emerald-500/10 border border-emerald-500/30">
              <div className="flex items-center gap-3">
                <div className="w-8 h-8 rounded-lg bg-emerald-500/20 flex items-center justify-center text-emerald-400">
                  <Volume2 className="w-4 h-4 animate-pulse" />
                </div>
                <div>
                  <div className="text-xs font-bold text-white">
                    {audioPlayer.currentTrack.surahName} — {audioPlayer.currentTrack.reciterName}
                  </div>
                  <div className="text-[10px] text-emerald-400 font-mono">
                    Auditioning • {(audioPlayer.currentTrack as TrackView).ayahRange || audioPlayer.currentTrack.ambientPairing}
                  </div>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <button
                  onClick={audioPlayer.togglePlayPause}
                  className="p-2 rounded-lg bg-emerald-500 text-black hover:bg-emerald-400 transition-all cursor-pointer"
                >
                  {audioPlayer.isPlaying ? <Pause className="w-4 h-4 fill-current" /> : <Play className="w-4 h-4 fill-current" />}
                </button>
                <button
                  onClick={audioPlayer.stop}
                  className="p-2 rounded-lg bg-white/10 hover:bg-white/20 text-white transition-all cursor-pointer"
                >
                  <Square className="w-4 h-4 fill-current" />
                </button>
              </div>
            </div>
          )}

          {/* Tracklist Sequencer */}
          <div className="flex flex-col gap-2 pt-2">
            <span className="text-xs font-semibold text-white/80 uppercase tracking-wider">
              Tracklist Sequencer & Ayah Manager
            </span>

            {tracksLoading ? (
              <div className="flex flex-col gap-2 py-6 animate-pulse">
                {[...Array(3)].map((_, i) => (
                  <div key={i} className="h-14 rounded-xl bg-white/5 border border-white/10" />
                ))}
              </div>
            ) : tracks.length === 0 ? (
              <div className="text-center py-8 text-xs text-white/40">
                No tracks found in this playlist.
              </div>
            ) : (
              <div className="flex flex-col gap-2 max-h-72 overflow-y-auto pr-1">
                {tracks.map((track) => {
                  const isCurrent = audioPlayer.currentTrack?.id === track.id;
                  return (
                    <div
                      key={track.id}
                      className={`flex items-center justify-between p-3 rounded-xl border transition-all ${
                        isCurrent 
                          ? 'bg-emerald-500/10 border-emerald-500/40 shadow-md' 
                          : 'bg-white/[0.02] hover:bg-white/[0.05] border-white/[0.06]'
                      }`}
                    >
                      <div className="flex items-center gap-3">
                        <span className="w-6 text-center text-xs font-mono text-white/40">
                          #{track.sequence ?? track.position ?? 0}
                        </span>
                        <button
                          onClick={() => audioPlayer.playTrack(track)}
                          className={`w-8 h-8 rounded-lg flex items-center justify-center transition-all cursor-pointer ${
                            isCurrent && audioPlayer.isPlaying
                              ? 'bg-emerald-500 text-black'
                              : 'bg-white/10 hover:bg-emerald-500 hover:text-black text-white'
                          }`}
                          title="Audition Track"
                        >
                          {isCurrent && audioPlayer.isPlaying ? (
                            <Pause className="w-3.5 h-3.5 fill-current" />
                          ) : (
                            <Play className="w-3.5 h-3.5 fill-current ml-0.5" />
                          )}
                        </button>
                        <div>
                          <div className="text-xs font-bold text-white">
                            {track.surahName} <span className="text-[10px] text-white/40 font-mono">(Surah {track.surahId})</span>
                          </div>
                          <div className="text-[11px] text-emerald-400 font-mono">
                            {track.reciterName}
                          </div>
                        </div>
                      </div>

                      <div className="flex items-center gap-3">
                        <Badge variant="emerald" className="text-[10px]">
                          {track.ayahRange || track.ambientPairing}
                        </Badge>
                        {track.duration ? (
                          <span className="text-xs text-white/50 font-mono w-12 text-right">
                            {track.duration}
                          </span>
                        ) : null}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>

        <div className="flex justify-end gap-3 px-6 py-4 border-t border-white/[0.08] bg-black/20 rounded-b-2xl">
          <button
            type="button"
            onClick={() => {
              audioPlayer.stop();
              onClose();
            }}
            className="px-4 py-2 text-xs rounded-xl bg-white/[0.05] hover:bg-white/[0.1] text-white/80 cursor-pointer"
          >
            Close Inspector
          </button>
        </div>
      </div>
    </Modal>
  );
};

export const Playlists: React.FC = () => {
  const { data, isLoading } = usePlaylists();
  const createPlaylistMutation = useCreatePlaylist();
  const deletePlaylistMutation = useDeletePlaylist();
  const queryClient = useQueryClient();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedPlaylist, setSelectedPlaylist] = useState<Playlist | null>(null);

  // Form State
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [coverUrl, setCoverUrl] = useState('');

  const curatedPlaylists = data?.curated || [];
  const userPlaylistsCount = data?.userPlaylistsCount || 0;
  const publicPlaylistsCount = data?.publicPlaylistsCount || 0;

  const handleCreatePlaylist = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title) return;

    await createPlaylistMutation.mutateAsync({
      title,
      description,
      coverUrl: coverUrl || 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=400&q=80',
    });

    queryClient.invalidateQueries({ queryKey: adminQueryKeys.playlists() });

    setIsModalOpen(false);
    setTitle('');
    setDescription('');
    setCoverUrl('');
  };

  const handleDeletePlaylist = async (id: string) => {
    if (!window.confirm('Are you sure you want to delete this playlist?')) return;
    try {
      await deletePlaylistMutation.mutateAsync(id);
      queryClient.invalidateQueries({ queryKey: adminQueryKeys.playlists() });
    } catch (err) {
      console.error('Failed to delete playlist:', err);
    }
  };

  return (
    <div className="flex flex-col gap-8 pb-12">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2">
            <ListMusic className="w-6 h-6 text-emerald-400" />
            Curated Playlists & Editorial Collections
          </h1>
          <p className="text-sm text-white/50 mt-1">
            Curate thematic recitation playlists for the Explore tab and monitor community creations. Click any playlist to inspect tracks & audition audio.
          </p>
        </div>

        <button
          onClick={() => setIsModalOpen(true)}
          className="chrome-btn px-4 py-2 text-xs rounded-xl flex items-center gap-1.5 self-start cursor-pointer"
        >
          <Plus className="w-4 h-4" />
          Create Curated Playlist
        </button>
      </div>

      {/* Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <StatCard
          title="Editorial Playlists"
          value={curatedPlaylists.length.toString()}
          delta="Featured in Explore"
          deltaType="positive"
          icon={<Sparkles className="w-4 h-4 text-emerald-400" />}
        />
        <StatCard
          title="User Playlists Created"
          value={userPlaylistsCount.toLocaleString()}
          delta="+28 this week"
          deltaType="positive"
          icon={<ListMusic className="w-4 h-4 text-cyan-400" />}
        />
        <StatCard
          title="Public Shared Playlists"
          value={publicPlaylistsCount.toString()}
          delta="Community catalog"
          deltaType="neutral"
          icon={<Globe className="w-4 h-4 text-indigo-400" />}
        />
      </div>

      {/* Editorial Playlists Grid */}
      <div className="flex flex-col gap-4">
        <span className="text-sm font-semibold text-white/80 uppercase tracking-wider">
          Explore Tab Featured Playlists (Click to Inspect)
        </span>

        {isLoading ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 animate-pulse">
            {[...Array(3)].map((_, i) => (
              <div key={i} className="h-64 rounded-2xl bg-white/5 border border-white/10" />
            ))}
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {curatedPlaylists.map((pl) => (
              <GlassCard
                key={pl.id}
                onClick={() => setSelectedPlaylist(pl)}
                className="overflow-hidden flex flex-col group hover:border-white/20 transition-all cursor-pointer"
              >
                {/* Cover Image & Overlay */}
                <div className="h-44 w-full relative overflow-hidden bg-black/40">
                  <img
                    src={pl.coverUrl}
                    alt={pl.title}
                    onError={(e) => {
                      (e.target as HTMLImageElement).src = 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=400&q=80';
                    }}
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500 opacity-80"
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-[#050506] via-[#050506]/40 to-transparent" />
                  
                  <div className="absolute top-3 right-3 flex items-center gap-1.5">
                    <Badge variant="emerald" className="text-[10px] backdrop-blur-md">
                      Featured
                    </Badge>
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        handleDeletePlaylist(pl.id);
                      }}
                      title="Delete Playlist"
                      disabled={deletePlaylistMutation.isPending}
                      className="p-1.5 rounded-lg bg-black/60 hover:bg-rose-500/30 text-white/70 hover:text-rose-400 border border-white/10 transition-all backdrop-blur-md cursor-pointer"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>

                  <div className="absolute bottom-3 left-4 right-4 flex items-end justify-between">
                    <div>
                      <span className="text-xs text-white/60 font-mono flex items-center gap-1">
                        <Headphones className="w-3.5 h-3.5 text-emerald-400" />
                        {pl.tracksCount} recitations • {pl.isPublic ? 'Public' : 'Private'}
                      </span>
                      <h3 className="text-base font-bold text-white tracking-tight mt-0.5">
                        {pl.title}
                      </h3>
                    </div>
                  </div>
                </div>

                {/* Details */}
                <div className="p-4 flex flex-col justify-between flex-1 gap-4">
                  <p className="text-xs text-white/50 line-clamp-2 leading-relaxed">
                    {pl.description}
                  </p>

                  <div className="flex items-center justify-between pt-3 border-t border-white/[0.06] text-xs">
                    <span className="text-white/40 font-mono">
                      {pl.tracksCount} recitations
                    </span>
                    <span className="text-emerald-400 font-mono text-[11px]">
                      {pl.creator}
                    </span>
                  </div>
                </div>
              </GlassCard>
            ))}
          </div>
        )}
      </div>

      {/* Playlist Inspector Drawer / Modal */}
      <PlaylistInspectorModal
        playlist={selectedPlaylist}
        onClose={() => setSelectedPlaylist(null)}
      />

      {/* Create Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Create Curated Editorial Playlist"
      >
        <form onSubmit={handleCreatePlaylist} className="flex flex-col gap-4 pt-2">
          <div className="flex flex-col gap-1.5">
            <label className="text-xs text-white/70 font-medium">Playlist Title</label>
            <input
              type="text"
              required
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="e.g. Tahajjud Reflections"
              className="px-3 py-2 rounded-xl bg-black/40 border border-white/10 text-xs text-white focus:outline-none focus:border-white/30"
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <label className="text-xs text-white/70 font-medium">Description</label>
            <textarea
              rows={3}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Curated recitations paired with gentle rain for late-night listening..."
              className="px-3 py-2 rounded-xl bg-black/40 border border-white/10 text-xs text-white focus:outline-none focus:border-white/30 resize-none"
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <label className="text-xs text-white/70 font-medium">Cover Image URL</label>
            <input
              type="url"
              value={coverUrl}
              onChange={(e) => setCoverUrl(e.target.value)}
              placeholder="https://images.unsplash.com/..."
              className="px-3 py-2 rounded-xl bg-black/40 border border-white/10 text-xs text-white focus:outline-none focus:border-white/30 font-mono"
            />
          </div>

          <div className="flex items-center gap-2 p-3 rounded-xl bg-white/[0.02] border border-white/[0.05]">
            <CheckCircle2 className="w-4 h-4 text-emerald-400" />
            <span className="text-xs text-white/70">
              Immediately published to all app clients via PostgreSQL sync.
            </span>
          </div>

          <div className="flex justify-end gap-3 pt-3 border-t border-white/[0.08]">
            <button
              type="button"
              onClick={() => setIsModalOpen(false)}
              className="px-4 py-2 text-xs rounded-xl bg-white/[0.05] hover:bg-white/[0.1] text-white/80 cursor-pointer"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={createPlaylistMutation.isPending}
              className="chrome-btn px-5 py-2 text-xs rounded-xl cursor-pointer"
            >
              {createPlaylistMutation.isPending ? 'Publishing...' : 'Publish Playlist'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
