import React, { useState } from 'react';
import { useAudioPlayer } from '../../context/AudioPlayerContext';
import {
  Play,
  Pause,
  Volume2,
  VolumeX,
  X,
  Disc,
  Radio,
  Sliders,
  CloudRain,
  Waves,
  Moon,
  Bird,
  Gauge,
  Check,
} from 'lucide-react';

// Web Audio API Ambient Mixer Engine
class AmbientMixer {
  private ctx: AudioContext | null = null;
  private ambientGain: GainNode | null = null;
  private nodes: Record<string, { source: AudioNode; gain: GainNode; interval?: any }> = {};
  private activeSounds: Record<string, boolean> = { rain: false, ocean: false, crickets: false, birds: false };
  private volume: number = 0.5;

  private initCtx() {
    if (!this.ctx) {
      const AudioCtx = window.AudioContext || (window as any).webkitAudioContext;
      this.ctx = new AudioCtx();
      this.ambientGain = this.ctx.createGain();
      this.ambientGain.gain.value = this.volume;
      this.ambientGain.connect(this.ctx.destination);
    }
    if (this.ctx.state === 'suspended') {
      this.ctx.resume();
    }
  }

  public setVolume(vol: number) {
    this.volume = vol;
    if (this.ambientGain && this.ctx) {
      this.ambientGain.gain.setValueAtTime(vol, this.ctx.currentTime);
    }
  }

  public toggleSound(sound: 'rain' | 'ocean' | 'crickets' | 'birds', active: boolean) {
    this.initCtx();
    if (!this.ctx || !this.ambientGain) return;

    this.activeSounds[sound] = active;
    if (active) {
      this.startSound(sound);
    } else {
      this.stopSound(sound);
    }
  }

  public isActive(sound: string) {
    return !!this.activeSounds[sound];
  }

  private startSound(sound: 'rain' | 'ocean' | 'crickets' | 'birds') {
    if (!this.ctx || !this.ambientGain) return;
    this.stopSound(sound);

    const ctx = this.ctx;
    const soundGain = ctx.createGain();
    soundGain.gain.value = 0.4;
    soundGain.connect(this.ambientGain);

    if (sound === 'rain') {
      const bufferSize = ctx.sampleRate * 2;
      const buffer = ctx.createBuffer(1, bufferSize, ctx.sampleRate);
      const data = buffer.getChannelData(0);
      for (let i = 0; i < bufferSize; i++) {
        data[i] = Math.random() * 2 - 1;
      }
      const noise = ctx.createBufferSource();
      noise.buffer = buffer;
      noise.loop = true;

      const filter = ctx.createBiquadFilter();
      filter.type = 'lowpass';
      filter.frequency.value = 1200;

      noise.connect(filter);
      filter.connect(soundGain);
      noise.start();
      this.nodes['rain'] = { source: noise, gain: soundGain };
    } else if (sound === 'ocean') {
      const bufferSize = ctx.sampleRate * 4;
      const buffer = ctx.createBuffer(1, bufferSize, ctx.sampleRate);
      const data = buffer.getChannelData(0);
      for (let i = 0; i < bufferSize; i++) {
        data[i] = Math.random() * 2 - 1;
      }
      const noise = ctx.createBufferSource();
      noise.buffer = buffer;
      noise.loop = true;

      const filter = ctx.createBiquadFilter();
      filter.type = 'lowpass';
      filter.frequency.value = 500;

      const lfo = ctx.createOscillator();
      lfo.frequency.value = 0.12; // 8s wave cycle
      const lfoGain = ctx.createGain();
      lfoGain.gain.value = 400;

      lfo.connect(lfoGain);
      lfoGain.connect(filter.frequency);

      noise.connect(filter);
      filter.connect(soundGain);

      noise.start();
      lfo.start();
      this.nodes['ocean'] = { source: noise, gain: soundGain };
    } else if (sound === 'crickets') {
      const osc = ctx.createOscillator();
      osc.type = 'sine';
      osc.frequency.value = 4800;

      const gainNode = ctx.createGain();
      gainNode.gain.value = 0.12;

      osc.connect(gainNode);
      gainNode.connect(soundGain);
      osc.start();
      this.nodes['crickets'] = { source: osc, gain: soundGain };
    } else if (sound === 'birds') {
      const interval = setInterval(() => {
        if (!this.ctx || !this.activeSounds['birds']) return;
        try {
          const bOsc = ctx.createOscillator();
          const bGain = ctx.createGain();
          bOsc.type = 'sine';
          bOsc.frequency.setValueAtTime(2800 + Math.random() * 800, ctx.currentTime);
          bOsc.frequency.exponentialRampToValueAtTime(4200 + Math.random() * 600, ctx.currentTime + 0.12);
          bGain.gain.setValueAtTime(0.08, ctx.currentTime);
          bGain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.2);

          bOsc.connect(bGain);
          bGain.connect(soundGain);
          bOsc.start();
          bOsc.stop(ctx.currentTime + 0.2);
        } catch (e) {
          // ignore
        }
      }, 1800 + Math.random() * 1500);

      const dummy = ctx.createOscillator();
      this.nodes['birds'] = { source: dummy, gain: soundGain, interval };
    }
  }

  private stopSound(sound: string) {
    if (this.nodes[sound]) {
      try {
        if (this.nodes[sound].source && 'stop' in this.nodes[sound].source) {
          (this.nodes[sound].source as any).stop();
        }
        if (this.nodes[sound].interval) {
          clearInterval(this.nodes[sound].interval);
        }
      } catch (e) {
        // ignore
      }
      delete this.nodes[sound];
    }
  }

  public stopAll() {
    Object.keys(this.activeSounds).forEach((s) => {
      this.stopSound(s);
      this.activeSounds[s] = false;
    });
    if (this.ctx && this.ctx.state !== 'closed') {
      this.ctx.close();
      this.ctx = null;
    }
  }
}

// Singleton ambient mixer instance
const ambientMixer = new AmbientMixer();

export const AudioPlayerBar: React.FC = () => {
  const {
    currentTrack,
    isPlaying,
    currentTime,
    duration,
    volume,
    playbackRate,
    pauseTrack,
    resumeTrack,
    setVolume,
    setPlaybackRate,
    seek,
    stopTrack,
  } = useAudioPlayer();

  const [isMuted, setIsMuted] = useState(false);
  const [prevVolume, setPrevVolume] = useState(volume);
  const [isSpeedOpen, setIsSpeedOpen] = useState(false);
  const [isAmbientOpen, setIsAmbientOpen] = useState(false);
  const [ambientVolume, setAmbientVolume] = useState(0.5);
  const [ambientStates, setAmbientStates] = useState({
    rain: false,
    ocean: false,
    crickets: false,
    birds: false,
  });

  const speedOptions = [0.75, 1.0, 1.25, 1.5];

  const handleToggleAmbient = (sound: 'rain' | 'ocean' | 'crickets' | 'birds') => {
    const nextState = !ambientStates[sound];
    setAmbientStates((prev) => ({ ...prev, [sound]: nextState }));
    ambientMixer.toggleSound(sound, nextState);
  };

  const handleAmbientVolumeChange = (vol: number) => {
    setAmbientVolume(vol);
    ambientMixer.setVolume(vol);
  };

  if (!currentTrack) return null;

  const formatTime = (secs: number) => {
    if (isNaN(secs)) return '00:00';
    const mins = Math.floor(secs / 60);
    const remainSecs = Math.floor(secs % 60);
    return `${mins.toString().padStart(2, '0')}:${remainSecs.toString().padStart(2, '0')}`;
  };

  const handleTogglePlay = () => {
    if (isPlaying) {
      pauseTrack();
    } else {
      resumeTrack();
    }
  };

  const handleVolumeToggle = () => {
    if (isMuted) {
      setVolume(prevVolume || 0.8);
      setIsMuted(false);
    } else {
      setPrevVolume(volume);
      setVolume(0);
      setIsMuted(true);
    }
  };

  const progressPercent = duration > 0 ? (currentTime / duration) * 100 : 0;
  const isAnyAmbientActive = Object.values(ambientStates).some(Boolean);

  return (
    <div className="fixed bottom-4 left-64 right-8 z-50 flex items-center justify-between px-6 py-3.5 bg-[#0a0a0c]/90 backdrop-blur-xl border border-white/10 shadow-2xl rounded-2xl text-white transition-all duration-300">
      {/* Track Info & Equalizer */}
      <div className="flex items-center gap-4 min-w-[240px] max-w-[320px]">
        <div className="relative flex items-center justify-center w-11 h-11 rounded-xl bg-gradient-to-br from-emerald-500/20 to-emerald-500/5 border border-emerald-500/30 text-emerald-400 shrink-0 shadow-inner">
          <Disc className={`w-5 h-5 ${isPlaying ? 'animate-spin' : ''}`} style={{ animationDuration: '4s' }} />
          {isPlaying && (
            <span className="absolute -top-1 -right-1 flex h-2.5 w-2.5">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-emerald-500"></span>
            </span>
          )}
        </div>
        <div className="flex flex-col min-w-0">
          <div className="text-sm font-semibold truncate text-white tracking-wide">
            {currentTrack.title}
          </div>
          <div className="text-xs text-emerald-400/90 truncate flex items-center gap-1.5">
            <Radio className="w-3 h-3 text-emerald-400 shrink-0" />
            <span className="truncate">{currentTrack.reciter}</span>
          </div>
        </div>

        {/* 1. Animated Equalizer Visualizer (4-6 pulsating emerald bars) */}
        <div className="flex items-end gap-0.5 h-5 px-1 shrink-0 bg-white/[0.03] rounded-lg border border-white/[0.06]">
          {[1, 2, 3, 4, 5].map((i) => (
            <span
              key={i}
              className={`w-1 rounded-full bg-emerald-400 transition-all duration-200 ${
                isPlaying ? 'animate-pulse' : 'h-1.5 opacity-40'
              }`}
              style={{
                height: isPlaying ? `${Math.max(25, (i * 37) % 100)}%` : '6px',
                animationDuration: `${0.35 + i * 0.12}s`,
              }}
            />
          ))}
        </div>
      </div>

      {/* Controls & Scrubber */}
      <div className="flex-1 flex flex-col items-center max-w-2xl px-6 gap-1.5">
        <div className="flex items-center gap-4">
          <button
            onClick={handleTogglePlay}
            className="flex items-center justify-center w-9 h-9 rounded-full bg-emerald-500 text-slate-950 hover:bg-emerald-400 transition-all shadow-[0_0_15px_rgba(52,211,153,0.4)] active:scale-95 cursor-pointer"
            title={isPlaying ? 'Pause' : 'Play'}
          >
            {isPlaying ? <Pause className="w-4 h-4 fill-current" /> : <Play className="w-4 h-4 fill-current ml-0.5" />}
          </button>
        </div>

        {/* Scrubber Bar */}
        <div className="w-full flex items-center gap-3 text-xs font-mono text-white/50">
          <span className="w-10 text-right">{formatTime(currentTime)}</span>
          <div className="relative flex-1 h-1.5 bg-white/10 rounded-full overflow-hidden group cursor-pointer">
            <div
              className="absolute top-0 left-0 h-full bg-emerald-500 rounded-full group-hover:bg-emerald-400 transition-all"
              style={{ width: `${progressPercent}%` }}
            />
            <input
              type="range"
              min={0}
              max={duration || 100}
              value={currentTime}
              onChange={(e) => seek(parseFloat(e.target.value))}
              className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
            />
          </div>
          <span className="w-10">{formatTime(duration)}</span>
        </div>
      </div>

      {/* Speed Selector, Ambient Mixer, Volume & Close */}
      <div className="flex items-center gap-4 min-w-[300px] justify-end">
        {/* 2. Playback Speed Selector Dropdown */}
        <div className="relative">
          <button
            onClick={() => {
              setIsSpeedOpen(!isSpeedOpen);
              setIsAmbientOpen(false);
            }}
            className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-xs font-mono text-white/80 hover:text-white transition-colors cursor-pointer"
            title="Playback Speed"
          >
            <Gauge className="w-3.5 h-3.5 text-emerald-400" />
            <span>{playbackRate}x</span>
          </button>

          {isSpeedOpen && (
            <div className="absolute bottom-full mb-2 right-0 w-32 bg-[#0a0a0c]/95 backdrop-blur-2xl border border-white/10 shadow-2xl rounded-xl p-1.5 z-50 animate-in fade-in zoom-in-95 duration-150">
              <div className="text-[10px] uppercase font-semibold text-white/40 px-2 py-1 tracking-wider">
                Playback Speed
              </div>
              {speedOptions.map((rate) => (
                <button
                  key={rate}
                  onClick={() => {
                    setPlaybackRate(rate);
                    setIsSpeedOpen(false);
                  }}
                  className={`w-full flex items-center justify-between px-2.5 py-1.5 rounded-lg text-xs font-mono transition-colors cursor-pointer ${
                    playbackRate === rate
                      ? 'bg-emerald-500/20 text-emerald-400 font-semibold border border-emerald-500/30'
                      : 'text-white/70 hover:text-white hover:bg-white/5'
                  }`}
                >
                  <span>{rate}x</span>
                  {playbackRate === rate && <Check className="w-3 h-3 text-emerald-400" />}
                </button>
              ))}
            </div>
          )}
        </div>

        {/* 3. Ambient Sound Mixer Popover */}
        <div className="relative">
          <button
            onClick={() => {
              setIsAmbientOpen(!isAmbientOpen);
              setIsSpeedOpen(false);
            }}
            className={`relative flex items-center justify-center w-9 h-9 rounded-xl border transition-all cursor-pointer ${
              isAnyAmbientActive
                ? 'bg-emerald-500/20 border-emerald-500/40 text-emerald-400 shadow-[0_0_12px_rgba(52,211,153,0.3)]'
                : 'bg-white/5 border-white/10 text-white/70 hover:text-white hover:bg-white/10'
            }`}
            title="Ambient Sound Mixer"
          >
            <Sliders className="w-4 h-4" />
            {isAnyAmbientActive && (
              <span className="absolute -top-1 -right-1 flex h-2 w-2">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
              </span>
            )}
          </button>

          {isAmbientOpen && (
            <div className="absolute bottom-full mb-2 right-0 w-72 bg-[#0a0a0c]/95 backdrop-blur-2xl border border-white/10 shadow-2xl rounded-2xl p-4 z-50 animate-in fade-in zoom-in-95 duration-150">
              <div className="flex items-center justify-between pb-3 mb-3 border-b border-white/10">
                <div className="flex items-center gap-2">
                  <div className="p-1.5 rounded-lg bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                    <CloudRain className="w-4 h-4" />
                  </div>
                  <div>
                    <div className="text-sm font-semibold text-white tracking-wide">Ambient Mixer</div>
                    <div className="text-[10px] text-white/50">Immersive background layers</div>
                  </div>
                </div>
                <button
                  onClick={() => setIsAmbientOpen(false)}
                  className="p-1 rounded-lg text-white/40 hover:text-white hover:bg-white/10 transition-colors"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              </div>

              {/* Presets Grid */}
              <div className="grid grid-cols-2 gap-2 mb-4">
                {/* Rain */}
                <button
                  onClick={() => handleToggleAmbient('rain')}
                  className={`flex items-center gap-2.5 p-2.5 rounded-xl border transition-all cursor-pointer ${
                    ambientStates.rain
                      ? 'bg-emerald-500/20 border-emerald-500/40 text-emerald-400 shadow-sm'
                      : 'bg-white/[0.03] border-white/[0.08] text-white/70 hover:bg-white/[0.06] hover:text-white'
                  }`}
                >
                  <CloudRain className="w-4 h-4 shrink-0" />
                  <span className="text-xs font-medium">Rain</span>
                </button>

                {/* Ocean Waves */}
                <button
                  onClick={() => handleToggleAmbient('ocean')}
                  className={`flex items-center gap-2.5 p-2.5 rounded-xl border transition-all cursor-pointer ${
                    ambientStates.ocean
                      ? 'bg-emerald-500/20 border-emerald-500/40 text-emerald-400 shadow-sm'
                      : 'bg-white/[0.03] border-white/[0.08] text-white/70 hover:bg-white/[0.06] hover:text-white'
                  }`}
                >
                  <Waves className="w-4 h-4 shrink-0" />
                  <span className="text-xs font-medium">Ocean Waves</span>
                </button>

                {/* Night Crickets */}
                <button
                  onClick={() => handleToggleAmbient('crickets')}
                  className={`flex items-center gap-2.5 p-2.5 rounded-xl border transition-all cursor-pointer ${
                    ambientStates.crickets
                      ? 'bg-emerald-500/20 border-emerald-500/40 text-emerald-400 shadow-sm'
                      : 'bg-white/[0.03] border-white/[0.08] text-white/70 hover:bg-white/[0.06] hover:text-white'
                  }`}
                >
                  <Moon className="w-4 h-4 shrink-0" />
                  <span className="text-xs font-medium">Crickets</span>
                </button>

                {/* Birds */}
                <button
                  onClick={() => handleToggleAmbient('birds')}
                  className={`flex items-center gap-2.5 p-2.5 rounded-xl border transition-all cursor-pointer ${
                    ambientStates.birds
                      ? 'bg-emerald-500/20 border-emerald-500/40 text-emerald-400 shadow-sm'
                      : 'bg-white/[0.03] border-white/[0.08] text-white/70 hover:bg-white/[0.06] hover:text-white'
                  }`}
                >
                  <Bird className="w-4 h-4 shrink-0" />
                  <span className="text-xs font-medium">Birds</span>
                </button>
              </div>

              {/* Ambient Volume Slider */}
              <div className="flex flex-col gap-1.5 pt-2 border-t border-white/10">
                <div className="flex items-center justify-between text-xs text-white/70">
                  <span>Ambient Volume</span>
                  <span className="font-mono text-emerald-400">{Math.round(ambientVolume * 100)}%</span>
                </div>
                <div className="relative w-full h-1.5 bg-white/10 rounded-full overflow-hidden group cursor-pointer">
                  <div
                    className="absolute top-0 left-0 h-full bg-emerald-500 rounded-full transition-all"
                    style={{ width: `${ambientVolume * 100}%` }}
                  />
                  <input
                    type="range"
                    min={0}
                    max={1}
                    step={0.01}
                    value={ambientVolume}
                    onChange={(e) => handleAmbientVolumeChange(parseFloat(e.target.value))}
                    className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
                  />
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Volume & Close */}
        <div className="flex items-center gap-2.5">
          <button
            onClick={handleVolumeToggle}
            className="text-white/60 hover:text-white transition-colors cursor-pointer"
            title={isMuted ? 'Unmute' : 'Mute'}
          >
            {volume === 0 || isMuted ? <VolumeX className="w-4 h-4 text-red-400" /> : <Volume2 className="w-4 h-4" />}
          </button>
          <div className="relative w-20 h-1.5 bg-white/10 rounded-full overflow-hidden group cursor-pointer">
            <div
              className="absolute top-0 left-0 h-full bg-white/70 group-hover:bg-emerald-400 rounded-full transition-all"
              style={{ width: `${isMuted ? 0 : volume * 100}%` }}
            />
            <input
              type="range"
              min={0}
              max={1}
              step={0.01}
              value={isMuted ? 0 : volume}
              onChange={(e) => {
                setVolume(parseFloat(e.target.value));
                if (isMuted) setIsMuted(false);
              }}
              className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
            />
          </div>
        </div>

        <div className="h-5 w-[1px] bg-white/10" />

        <button
          onClick={() => {
            ambientMixer.stopAll();
            stopTrack();
          }}
          className="p-1.5 rounded-lg text-white/40 hover:text-white hover:bg-white/10 transition-colors cursor-pointer"
          title="Close player"
        >
          <X className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
};
