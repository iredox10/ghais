import { useState, useRef, useEffect, useCallback } from 'react';
import type { PlaylistTrack } from '../api/types';

export function useAudioPlayer() {
  const [currentTrack, setCurrentTrack] = useState<PlaylistTrack | null>(null);
  const [isPlaying, setIsPlaying] = useState<boolean>(false);
  const [queue, setQueue] = useState<PlaylistTrack[]>([]);
  const audioRef = useRef<HTMLAudioElement | null>(null);

  useEffect(() => {
    audioRef.current = new Audio();
    audioRef.current.onended = () => {
      setIsPlaying(false);
    };
    return () => {
      if (audioRef.current) {
        audioRef.current.pause();
        audioRef.current = null;
      }
    };
  }, []);

  const playTrack = useCallback((track: PlaylistTrack) => {
    if (!audioRef.current) return;
    setCurrentTrack(track);
    if (track.audioUrl) {
      audioRef.current.src = track.audioUrl;
      audioRef.current.play().then(() => {
        setIsPlaying(true);
      }).catch(err => {
        console.error('Audio playback error:', err);
        setIsPlaying(true);
      });
    } else {
      setIsPlaying(true);
    }
  }, []);

  const playAll = useCallback((tracks: PlaylistTrack[]) => {
    if (!tracks || tracks.length === 0) return;
    setQueue(tracks);
    playTrack(tracks[0]);
  }, [playTrack]);

  const togglePlayPause = useCallback(() => {
    if (!audioRef.current || !currentTrack) return;
    if (isPlaying) {
      audioRef.current.pause();
      setIsPlaying(false);
    } else {
      audioRef.current.play().then(() => {
        setIsPlaying(true);
      }).catch(() => {
        setIsPlaying(true);
      });
    }
  }, [isPlaying, currentTrack]);

  const stop = useCallback(() => {
    if (audioRef.current) {
      audioRef.current.pause();
      audioRef.current.currentTime = 0;
    }
    setIsPlaying(false);
    setCurrentTrack(null);
  }, []);

  return {
    currentTrack,
    isPlaying,
    queue,
    playTrack,
    playAll,
    togglePlayPause,
    stop,
  };
}
