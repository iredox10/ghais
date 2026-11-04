/**
 * Audio URL & Endpoint Resolver for Quranify Admin and App.
 * Replicates the exact URL resolution logic from quranify/shared/.../Models.kt.
 */

import { FULL_SURAHS_LIST } from './surahsData';

export interface ReciterInfo {
  slug: string;
  serverUrl?: string;
  audioFolder?: string;
  availableSurahList?: string;
  availableSurahs?: string;
  [key: string]: any;
}

/**
 * Pads a surah ID to 3 digits (e.g. 1 -> "001", 114 -> "114").
 */
export function pad3(surahId: number): string {
  return surahId.toString().padStart(3, '0');
}

/**
 * Resolves the full surah audio URL for a given reciter and surah ID.
 * Replicates the exact URL resolution logic from Models.kt:
 * - If serverUrl is present: `${serverUrl.endsWith('/') ? serverUrl : serverUrl + '/'}${pad3(surahId)}.mp3`
 * - If audioFolder is present: `https://everyayah.com/data/${audioFolder}/${pad3(surahId)}001.mp3` or full surah.
 * - Legacy slug fallback map for known reciters.
 */
export function getFullSurahUrl(reciter: ReciterInfo, surahId: number): string {
  const pad = pad3(surahId);

  // 1. If serverUrl is present
  if (reciter.serverUrl && reciter.serverUrl.trim() !== '') {
    const base = reciter.serverUrl.endsWith('/') ? reciter.serverUrl : reciter.serverUrl + '/';
    return `${base}${pad}.mp3`;
  }

  // 2. If audioFolder is present
  if (reciter.audioFolder && reciter.audioFolder.trim() !== '') {
    return `https://everyayah.com/data/${reciter.audioFolder}/${pad}.mp3`;
  }

  // 3. Legacy slug fallback map
  const slug = (reciter.slug || '').toLowerCase().trim().replace(/_/g, '-');
  switch (slug) {
    case 'mishary':
    case 'alafasy':
      return `https://server8.mp3quran.net/afs/${pad}.mp3`;
    case 'al-sudais':
    case 'sudais':
      return `https://server11.mp3quran.net/sds/${pad}.mp3`;
    case 'al-muaiqly':
    case 'muaiqly':
      return `https://server12.mp3quran.net/maher/${pad}.mp3`;
    case 'al-dossari':
    case 'dossari':
      return `https://server11.mp3quran.net/yasser/${pad}.mp3`;
    case 'islam-sobhi':
    case 'islam_sobhi':
      return `https://server14.mp3quran.net/islam/Rewayat-Hafs-A-n-Assem/${pad}.mp3`;
    case 'omar-hisham':
    case 'omar_hisham':
      return `https://archive.org/download/Omar-Hisham/${pad}.mp3`;
    case 'abdul-basit':
    case 'abdul_basit':
    case 'basit':
      return `https://server7.mp3quran.net/basit/Almusshaf-Al-Mojawwad/${pad}.mp3`;
    case 'al-husary':
    case 'husary':
      return `https://server13.mp3quran.net/husr/${pad}.mp3`;
    case 'saud-shuraim':
    case 'shuraim':
      return `https://server7.mp3quran.net/shur/${pad}.mp3`;
    case 'saad-alghamdi':
    case 'alghamdi':
    case 'ghamdi':
      return `https://server7.mp3quran.net/s_gmd/${pad}.mp3`;
    case 'al-minshawi':
    case 'minshawi':
      return `https://server10.mp3quran.net/minsh/${pad}.mp3`;
    case 'ahmed-alajamy':
    case 'alajamy':
    case 'ajamy':
      return `https://server10.mp3quran.net/ajm/${pad}.mp3`;
    default:
      return `https://server8.mp3quran.net/afs/${pad}.mp3`;
  }
}

/**
 * Returns a clean human-readable name for a server URL or endpoint:
 * e.g. "MP3Quran Server 8", "MP3Quran Server 11", "EveryAyah CDN", "Archive.org".
 */
export function getAudioEndpointName(serverUrl?: string): string {
  if (!serverUrl || serverUrl.trim() === '') {
    return 'EveryAyah CDN';
  }
  const url = serverUrl.toLowerCase();
  if (url.includes('archive.org')) {
    return 'Archive.org';
  }
  if (url.includes('everyayah.com')) {
    return 'EveryAyah CDN';
  }
  const match = url.match(/server([0-9]+)\.mp3quran\.net/);
  if (match && match[1]) {
    return `MP3Quran Server ${match[1]}`;
  }
  if (url.includes('mp3quran.net')) {
    return 'MP3Quran CDN';
  }
  return 'Custom Audio Server';
}

/**
 * Resolves the first available Surah and its exact audio stream URL for a reciter.
 */
export function getFirstAvailableSurah(reciter: ReciterInfo): {
  surahId: number;
  surahNameEn: string;
  surahNameAr: string;
  audioUrl: string;
} {
  let surahId = 1;
  const availableList = reciter.availableSurahList || reciter.availableSurahs;

  if (availableList && typeof availableList === 'string' && availableList.trim() !== '') {
    const ids = availableList
      .split(',')
      .map((s) => parseInt(s.trim(), 10))
      .filter((n) => !isNaN(n));
    if (ids.length > 0) {
      surahId = ids[0];
    }
  }

  const surah = FULL_SURAHS_LIST.find((s) => s.id === surahId || (s as any).number === surahId) || {
    id: surahId,
    nameEn: `Surah ${surahId}`,
    nameAr: `سورة ${surahId}`,
  };

  const audioUrl = getFullSurahUrl(reciter, surahId);

  return {
    surahId,
    surahNameEn: surah.nameEn,
    surahNameAr: surah.nameAr,
    audioUrl,
  };
}
