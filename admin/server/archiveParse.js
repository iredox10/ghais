import { FULL_SURAHS_LIST } from './surahsData.js';

export const ARCHIVE_DOWNLOAD_BASE = 'https://archive.org/download';
export const TOTAL_SURAHS = FULL_SURAHS_LIST.length;

const AUDIO_EXTENSIONS = new Set(['mp3', 'm4a', 'mp4', 'aac', 'ogg', 'oga', 'opus', 'flac', 'wav', 'aiff', 'aif', 'wma']);
const PREFERRED_EXTENSIONS = ['mp3', 'm4a', 'mp4', 'aac'];
const ITEM_ID_PATTERN = /^[A-Za-z0-9][A-Za-z0-9._~-]*$/;
const ITEM_PATH_PREFIXES = ['details', 'download', 'metadata', 'stream', 'embed'];
const PARTIAL_FILE_PATTERN = /(?:aya(?:h|s|t)?|verse|verses|part)\s*[._-]?\d/i;
const ARABIC_NAME_PATTERN = /[\u0600-\u06FF]/;

const SURAHS = FULL_SURAHS_LIST.map((surah) => ({
  id: Number(surah.id || surah.surah_id),
  nameEn: String(surah.name_en || surah.nameEn || ''),
  nameAr: String(surah.name_ar || surah.nameAr || ''),
})).filter((surah) => Number.isInteger(surah.id) && surah.id >= 1 && surah.id <= TOTAL_SURAHS);

const SURAH_BY_ID = new Map(SURAHS.map((surah) => [surah.id, surah]));
const FILE_NAME_PATTERN = /^\d{1,3}$/;
const NAME_PREFIX_PATTERN = /^(?:surah|surat|chapter|ch|سورة)\s*[._-]?/i;

function collapse(value) {
  return String(value || '').replace(/[_\s]+/g, ' ').trim();
}

function stripLeadingArticle(value) {
  return value.replace(/^(?:al|ad|as|ash|an|ar|at|az|us)\s+/i, '');
}

function normalizeSurahName(value) {
  return String(value || '')
    .toLowerCase()
    .replace(/[\u064B-\u065F\u0670]/g, '')
    .replace(/['’`ʻ]/g, '')
    .replace(/[^a-z0-9]+/g, '')
    .trim();
}

const NAME_ALIASES = new Map();
const registerAlias = (alias, surahId) => {
  const key = normalizeSurahName(alias);
  if (key.length < 3 || NAME_ALIASES.has(key)) return;
  NAME_ALIASES.set(key, surahId);
};

for (const surah of SURAHS) {
  const bare = stripLeadingArticle(surah.nameEn);
  registerAlias(surah.nameEn, surah.id);
  registerAlias(bare, surah.id);
  registerAlias(bare.replace(/\s+/g, ''), surah.id);
  registerAlias(surah.nameEn.replace(/\s+/g, ''), surah.id);
}
registerAlias('fatihah', 1);
registerAlias('fatiha', 1);
registerAlias('baqara', 2);
registerAlias('baqarah', 2);
registerAlias('imran', 3);
registerAlias('nisa', 4);
registerAlias('maida', 5);
registerAlias('anam', 6);
registerAlias('araf', 7);
registerAlias('infitar', 8);
registerAlias('tawba', 9);
registerAlias('tawbah', 9);
registerAlias('yunus', 10);
registerAlias('hud', 11);
registerAlias('hijr', 12);
registerAlias('rad', 13);
registerAlias('rahman', 55);
registerAlias('waqiah', 56);
registerAlias('wagia', 56);
registerAlias('muzzammil', 73);
registerAlias('muzammil', 73);
registerAlias('kawthar', 108);
registerAlias('nas', 114);

function trimTrailingArtifact(value) {
  return value
    .replace(PARTIAL_FILE_PATTERN, ' ')
    .replace(/(?:[\s._-]+(?:aali?y(?:a|h|s|at)?|verse|verses|part|ruku|rukoo|para|juz|quarter|half)\b.*)$/i, ' ')
    .replace(/[\s._-]+$/g, '')
    .trim();
}

function matchByName(value) {
  const candidate = normalizeSurahName(trimTrailingArtifact(value));
  if (candidate.length < 3) return null;
  if (NAME_ALIASES.has(candidate)) return NAME_ALIASES.get(candidate);

  let match = null;
  for (const [alias, surahId] of NAME_ALIASES) {
    if (!candidate.includes(alias)) continue;
    if (!match || alias.length > match.length) match = { alias, surahId };
  }
  return match?.surahId ?? null;
}

function fileNameOf(file) {
  if (typeof file === 'string') return collapse(file);
  if (!file || typeof file !== 'object') return '';
  return collapse(file.name || file.filename || file.path || file.file || '');
}

function filePathOf(file) {
  if (file && typeof file === 'object' && typeof file.path === 'string' && file.path.trim()) {
    return collapse(file.path);
  }
  return fileNameOf(file);
}

function extensionOf(value) {
  const match = /\.([A-Za-z0-9]+)$/.exec(String(value || ''));
  return match ? match[1].toLowerCase() : '';
}

function folderOf(filePath) {
  const normalized = String(filePath || '').replace(/^\/+/, '');
  const index = normalized.lastIndexOf('/');
  return index === -1 ? '' : normalized.slice(0, index);
}

function leadingSurahNumber(value) {
  const match = /^(\d{1,3})(.*)$/.exec(String(value || ''));
  if (!match) return null;
  const id = Number(match[1]);
  if (!Number.isInteger(id) || id < 1 || id > TOTAL_SURAHS) return null;
  return { id, remainder: match[2] };
}

function explicitSurahToken(value) {
  const match = /(?<![0-9a-z])(?:surah|surat|chapter|ch)[\s._-]*([0-9]{1,3})(?![\d])/i.exec(String(value || ''));
  if (!match) return null;
  const id = Number(match[1]);
  return Number.isInteger(id) && id >= 1 && id <= TOTAL_SURAHS ? id : null;
}

function folderSurahNumber(filePath) {
  const segments = folderOf(filePath).split('/').filter(Boolean);
  for (let index = segments.length - 1; index >= 0; index -= 1) {
    const cleaned = segments[index].replace(/^surah[\s._-]?/i, '');
    if (!/^\d{1,3}$/.test(cleaned)) continue;
    const id = Number(cleaned);
    if (Number.isInteger(id) && id >= 1 && id <= TOTAL_SURAHS) return id;
  }
  return null;
}

function rankEntry(candidate) {
  const extensionRank = PREFERRED_EXTENSIONS.indexOf(candidate.extension);
  const completeness = candidate.isPartial ? 2 : 0;
  return completeness * 10 + (extensionRank === -1 ? PREFERRED_EXTENSIONS.length : extensionRank);
}

function audioCandidates(file) {
  const filePath = filePathOf(file);
  const base = fileNameOf(file).replace(/\.[A-Za-z0-9]+$/, '');
  const leading = leadingSurahNumber(base);
  return [
    { value: leading?.id ?? null, matchedBy: 'number' },
    { value: explicitSurahToken(base), matchedBy: 'number' },
    { value: folderSurahNumber(filePath), matchedBy: 'folder-number' },
    { value: matchByName(base.replace(NAME_PREFIX_PATTERN, '')), matchedBy: 'name' },
  ];
}

function entryForFile(file, itemId) {
  const filePath = filePathOf(file);
  const name = fileNameOf(file);
  if (!name || !isArchiveAudioFile(name)) return null;

  const base = name.replace(/\.[A-Za-z0-9]+$/, '');
  const candidates = audioCandidates(file);
  const numbered = candidates.find((candidate) => typeof candidate.value === 'number');
  const named = candidates.find((candidate) => candidate.matchedBy === 'name' && typeof candidate.value === 'number');
  const selected = numbered || named;
  if (!selected || !SURAH_BY_ID.has(selected.value)) return null;

  const surah = SURAH_BY_ID.get(selected.value);
  return {
    surahId: surah.id,
    surahNameEn: surah.nameEn,
    surahNameAr: surah.nameAr,
    fileName: name,
    filePath,
    url: `${ARCHIVE_DOWNLOAD_BASE}/${encodeURIComponent(itemId)}/${filePath
      .split('/')
      .map((segment) => encodeURIComponent(segment))
      .join('/')}`,
    sizeBytes: Number(file && typeof file === 'object' ? file.size : 0) || 0,
    matchedBy: selected.matchedBy,
    extension: extensionOf(name),
    isPartial: PARTIAL_FILE_PATTERN.test(base) || /\d+\s*-\s*\d+/.test(base),
    hasNumber: Boolean(numbered),
  };
}

function isCanonicalName(base) {
  return FILE_NAME_PATTERN.test(base);
}

function schemeForEntries(entries, audioCount) {
  if (!entries.length || !audioCount) return 'unknown';
  const canonical = entries.filter((entry) => isCanonicalName(entry.fileName.replace(/\.[A-Za-z0-9]+$/, ''))).length;
  if (canonical >= Math.max(1, Math.ceil(entries.length * 0.6))) return 'canonical';
  if (entries.some((entry) => entry.matchedBy === 'number' || entry.matchedBy === 'folder-number')) return 'numbered-name';
  if (entries.some((entry) => entry.matchedBy === 'name')) return 'name-only';
  return 'unknown';
}

function commonFolder(entries) {
  const folders = new Set(entries.map((entry) => folderOf(entry.filePath)));
  if (folders.size !== 1) return '';
  const [folder] = [...folders];
  return folder.replace(/^\/+|\/+$/g, '');
}

function isCanonicalStem(fileName) {
  const base = fileName.replace(/\.[A-Za-z0-9]+$/, '');
  return /^\d{1,3}$/.test(base);
}

function buildFileMap(entries) {
  const needsMap = entries.some((entry) => {
    const stem = String(entry?.fileName || '').replace(/\.[A-Za-z0-9]+$/, '');
    return !/^\d{3}$/.test(stem);
  });
  if (!needsMap) return '';

  const map = {};
  for (const entry of entries) {
    const surahId = Number(entry?.surahId);
    if (!Number.isInteger(surahId) || surahId < 1 || surahId > TOTAL_SURAHS) continue;
    const filePath = String(entry?.filePath || '').trim();
    if (filePath) map[String(surahId)] = filePath;
  }
  return Object.keys(map).length ? JSON.stringify(map) : '';
}

export function parseArchiveItemId(value) {
  const raw = String(value ?? '').trim();
  if (!raw) throw new Error('An Internet Archive URL or item ID is required.');

  let candidate = raw;
  if (/^(?:www\.)?archive\.org\//i.test(candidate)) candidate = `https://${candidate}`;
  if (/^https?:\/\//i.test(candidate)) {
    let url;
    try {
      url = new URL(candidate);
    } catch {
      throw new Error('Enter a valid Internet Archive item link or item ID.');
    }
    const host = url.hostname.toLowerCase().replace(/^www\./, '');
    if (!/(^|\.)archive\.org$/.test(host)) {
      throw new Error('Only archive.org item links are supported.');
    }
    const segments = url.pathname.split('/').filter(Boolean);
    const prefixIndex = segments.findIndex((segment) => ITEM_PATH_PREFIXES.includes(segment.toLowerCase()));
    if (prefixIndex === -1) {
      candidate = segments[segments.length - 1] || '';
    } else {
      const prefix = segments[prefixIndex].toLowerCase();
      const tail = segments.slice(prefixIndex + 1);
      candidate = prefix === 'download' || prefix === 'metadata' || prefix === 'stream' ? tail[0] || '' : tail[tail.length - 1] || '';
    }
  } else {
    candidate = candidate.replace(/^[./]+/, '').split(/[?#]/)[0].replace(/\/+$/, '');
  }

  candidate = decodeURIComponent(candidate).trim();
  if (!candidate || candidate.toLowerCase().includes('/') || !ITEM_ID_PATTERN.test(candidate)) {
    throw new Error('Enter a valid Internet Archive item link or item ID.');
  }
  if (ITEM_PATH_PREFIXES.includes(candidate.toLowerCase())) {
    throw new Error('Enter a valid Internet Archive item link or item ID.');
  }
  return candidate;
}

export function isArchiveAudioFile(name) {
  const fileName = fileNameOf(name);
  if (!fileName) return false;
  if (/^__ia_thumb|__ia_thumb\.jpg$|itemimage/i.test(fileName)) return false;
  return AUDIO_EXTENSIONS.has(extensionOf(fileName));
}

export function detectArchiveScheme(files) {
  const list = Array.isArray(files) ? files : [];
  const audioFiles = list.filter((file) => isArchiveAudioFile(fileNameOf(file)));
  const entries = [];
  for (const file of audioFiles) {
    const entry = entryForFile(file, 'scheme-probe');
    if (entry) entries.push(entry);
  }
  return schemeForEntries(entries, audioFiles.length);
}

export function mapArchiveFilesToSurahs(files, itemId = '') {
  const list = Array.isArray(files) ? files : [];
  const bestById = new Map();

  for (const file of list) {
    const candidate = entryForFile(file, itemId);
    if (!candidate) continue;
    const existing = bestById.get(candidate.surahId);
    if (!existing) {
      bestById.set(candidate.surahId, candidate);
      continue;
    }

    const candidateCanonical = isCanonicalName(candidate.fileName.replace(/\.[A-Za-z0-9]+$/, ''));
    const existingCanonical = isCanonicalName(existing.fileName.replace(/\.[A-Za-z0-9]+$/, ''));
    if (candidateCanonical !== existingCanonical) {
      if (candidateCanonical) bestById.set(candidate.surahId, candidate);
      continue;
    }
    if (rankEntry(candidate) < rankEntry(existing)) bestById.set(candidate.surahId, candidate);
  }

  return [...bestById.values()]
    .sort((left, right) => left.surahId - right.surahId)
    .map(({ extension, isPartial, hasNumber, ...entry }) => entry);
}

export function slugifyArchiveName(value) {
  return String(value ?? '')
    .normalize('NFKD')
    .replace(/[\u0300-\u036F]/g, '')
    .replace(/['’`ʻ]/g, '')
    .replace(/[^A-Za-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .toLowerCase()
    .slice(0, 60) || 'archive-reciter';
}

function deriveNameEn(metadata, itemId) {
  const creator = collapse(metadata?.creator);
  if (creator) return creator;
  const title = collapse(metadata?.title);
  if (title) {
    return title.replace(/[\r\n]+/g, ' ').replace(/\s*\b(full|complete|quran|recitation|reciter)\b.*$/i, '').trim() || title;
  }
  return itemId;
}

function deriveDescription(metadata) {
  const description = String(metadata?.description || '')
    .replace(/<[^>]*>/g, ' ')
    .replace(/&nbsp;/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
  return description.slice(0, 500);
}

export function buildArchiveReciterPayload({ itemId, metadata, entries, overrides } = {}) {
  const safeItemId = String(itemId || '').trim();
  const list = Array.isArray(entries) ? entries : [];
  const surahIds = list.map((entry) => Number(entry?.surahId)).filter((id) => Number.isInteger(id) && id >= 1 && id <= TOTAL_SURAHS);
  const uniqueIds = [...new Set(surahIds)].sort((left, right) => left - right);
  const folder = commonFolder(list);
  const nameEn = deriveNameEn(metadata, safeItemId);
  const base = {
    slug: slugifyArchiveName(nameEn),
    catalog: 'mp3quran',
    catalog_key: `mp3quran:${slugifyArchiveName(nameEn)}`,
    name_en: nameEn,
    name_ar: ARABIC_NAME_PATTERN.test(nameEn) ? nameEn : nameEn,
    riwayah: 'Hafs',
    style: 'Murattal',
    tempo: 'medium',
    audio_folder: folder,
    country: '',
    server_url: safeItemId ? `${ARCHIVE_DOWNLOAD_BASE}/${encodeURIComponent(safeItemId)}/` : ARCHIVE_DOWNLOAD_BASE,
    available_surahs: uniqueIds.join(','),
    full_surah_file_map: buildFileMap(list),
    description: deriveDescription(metadata),
    is_teacher: false,
    enabled: true,
    verified: true,
  };

  const review = overrides && typeof overrides === 'object' && !Array.isArray(overrides) ? overrides : {};
  const merged = { ...base };
  for (const [key, value] of Object.entries(review)) {
    if (!(key in base)) continue;
    if (typeof base[key] === 'boolean') {
      if (typeof value === 'boolean') merged[key] = value;
      continue;
    }
    if (typeof value === 'string' && value.trim()) merged[key] = value.trim();
  }

  merged.slug = slugifyArchiveName(merged.slug || merged.name_en);
  merged.catalog_key = `${merged.catalog}:${merged.slug}`;
  if (!safeItemId) merged.server_url = `${ARCHIVE_DOWNLOAD_BASE}/`;
  return merged;
}

export default {
  ARCHIVE_DOWNLOAD_BASE,
  parseArchiveItemId,
  isArchiveAudioFile,
  detectArchiveScheme,
  mapArchiveFilesToSurahs,
  buildArchiveReciterPayload,
  slugifyArchiveName,
};
