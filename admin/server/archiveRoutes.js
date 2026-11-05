import express from 'express';
import multer from 'multer';
import { Query } from './query.js';
import {
  ARCHIVE_DOWNLOAD_BASE,
  parseArchiveItemId,
  isArchiveAudioFile,
  detectArchiveScheme,
  mapArchiveFilesToSurahs,
  buildArchiveReciterPayload,
  slugifyArchiveName,
} from './archiveParse.js';

const MAX_IMAGE_SIZE = 5 * 1024 * 1024;
const ALLOWED_IMAGE_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp', 'image/avif']);
const ARCHIVE_FETCH_TIMEOUT_MS = 10_000;
const TOTAL_SURAHS = 114;

const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: MAX_IMAGE_SIZE },
  fileFilter: (_req, file, callback) => {
    if (!ALLOWED_IMAGE_TYPES.has(file.mimetype)) {
      callback(new Error('Only JPG, PNG, WebP, and AVIF images are supported.'));
      return;
    }
    callback(null, true);
  },
});

const MP3QURAN_CATALOG = 'mp3quran';
const EVERYAYAH_CATALOG = 'everyayah';
const RECITER_CATALOGS = new Set([MP3QURAN_CATALOG, EVERYAYAH_CATALOG]);

function text(value, fallback = '') {
  return typeof value === 'string' ? value.trim() : fallback;
}

function normalizeCatalog(value, fallback = MP3QURAN_CATALOG) {
  const raw = text(value, fallback).toLowerCase();
  return RECITER_CATALOGS.has(raw) ? raw : fallback;
}

function catalogKeyFor(catalog, slug) {
  return `${catalog}:${slug}`;
}

function documentIdFor(catalog, slug) {
  return catalog === EVERYAYAH_CATALOG ? `everyayah-${slug}` : slug;
}

function normalizeAvailableSurahs(value, fallback = '') {
  const raw = text(value, fallback);
  return raw === '1-114' ? '' : raw;
}

async function listAllDocuments(databases, databaseId, collectionId) {
  const documents = [];
  let offset = 0;
  let hasMore = true;

  while (hasMore) {
    const result = await databases.listDocuments(
      databaseId,
      collectionId,
      [Query.limit(100), Query.offset(offset)]
    );
    documents.push(...(result.documents || []));
    hasMore = (result.documents || []).length === 100;
    if (hasMore) offset += 100;
  }

  return documents;
}

async function findDocument(databases, databaseId, idOrSlug) {
  const documents = await listAllDocuments(databases, databaseId, 'reciters');
  const needle = String(idOrSlug || '');
  return documents.find((document) =>
    document.$id === needle ||
    document.slug === needle ||
    document.catalog_key === needle
  ) || null;
}

function sendError(res, error) {
  const status = Number(error?.statusCode) || 500;
  const message = error?.safe && typeof error?.message === 'string' && error.message
    ? error.message
    : 'Archive request failed.';
  if (status >= 500) {
    console.error('[archive] request failed:', error?.message || error);
    if (error?.response) console.error('[archive] upstream:', JSON.stringify(error.response).slice(0, 500));
  }
  res.status(status).json({ success: false, error: message });
}

function requestError(message, statusCode = 400) {
  const error = new Error(message);
  error.statusCode = statusCode;
  error.safe = true;
  return error;
}

function archiveItemId(value) {
  let parsed;
  try {
    parsed = parseArchiveItemId(value);
  } catch {
    throw requestError('Enter a valid Internet Archive item link or item ID.');
  }

  const itemId = text(
    typeof parsed === 'string'
      ? parsed
      : parsed?.itemId || parsed?.id
  );
  if (!itemId || !/^[a-zA-Z0-9._-]+$/.test(itemId)) {
    throw requestError('Enter a valid Internet Archive item link or item ID.');
  }
  return itemId;
}

function archiveMetadataUrl(itemId) {
  const base = ARCHIVE_DOWNLOAD_BASE.endsWith('/')
    ? ARCHIVE_DOWNLOAD_BASE
    : `${ARCHIVE_DOWNLOAD_BASE}/`;
  return new URL(`../metadata/${encodeURIComponent(itemId)}`, base).toString();
}

function archiveHeaders() {
  return {
    Accept: 'application/json',
    'User-Agent': 'QuranifyAdmin/1.0 (Internet Archive reciter import)',
  };
}

async function fetchArchiveMetadata(itemId) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), ARCHIVE_FETCH_TIMEOUT_MS);
  try {
    let response;
    try {
      response = await fetch(archiveMetadataUrl(itemId), {
        headers: archiveHeaders(),
        signal: controller.signal,
      });
    } catch (error) {
      const message = error?.name === 'AbortError'
        ? 'Internet Archive request timed out.'
        : 'Internet Archive could not be reached.';
      throw requestError(message, 502);
    }

    if (response.status === 404) {
      throw requestError('Internet Archive item was not found.', 404);
    }
    if (!response.ok) {
      throw requestError('Internet Archive could not be reached.', 502);
    }

    try {
      return await response.json();
    } catch {
      throw requestError('Internet Archive returned an invalid metadata response.', 502);
    }
  } finally {
    clearTimeout(timer);
  }
}

function safeMetadata(rawMetadata) {
  const metadata = rawMetadata && typeof rawMetadata === 'object' ? rawMetadata : {};
  return {
    title: text(metadata.title),
    creator: text(metadata.creator),
    description: text(metadata.description),
    subject: text(metadata.subject),
    language: text(metadata.language),
    mediatype: text(metadata.mediatype),
    uploader: text(metadata.uploader),
    addeddate: text(metadata.addeddate),
    publicdate: text(metadata.publicdate),
  };
}

function fileName(file) {
  return text(typeof file === 'string' ? file : file?.name || file?.filename || file?.path);
}

function inspectData(raw, itemId) {
  itemId = archiveItemId(itemId);
  const metadata = safeMetadata(raw?.metadata);
  const files = Array.isArray(raw?.files) ? raw.files : [];
  const scheme = detectArchiveScheme(files);
  const entries = mapArchiveFilesToSurahs(files, itemId);
  const normalizedEntries = Array.isArray(entries) ? entries : [];
  const matchedIds = new Set(
    normalizedEntries
      .map((entry) => Number(entry?.surahId))
      .filter((id) => Number.isInteger(id) && id >= 1 && id <= TOTAL_SURAHS)
  );
  const matched = matchedIds.size;
  const missingIds = Array.from({ length: TOTAL_SURAHS }, (_, index) => index + 1)
    .filter((id) => !matchedIds.has(id));
  const audioCount = files.filter((file) => isArchiveAudioFile(fileName(file))).length;
  const warnings = [];

  if (!audioCount) warnings.push('No supported audio files were found in the item.');
  if (!matched) warnings.push('No surah audio files could be matched.');
  if (scheme === 'unknown') warnings.push('The item uses an unrecognized audio naming scheme.');
  if (matched > 0 && matched < TOTAL_SURAHS) {
    warnings.push(`${missingIds.length} surahs are not matched.`);
  }

  return {
    itemId,
    metadata,
    scheme,
    entries: normalizedEntries,
    matched,
    total: TOTAL_SURAHS,
    missingIds,
    warnings,
    payload: buildArchiveReciterPayload({
      itemId,
      metadata,
      entries: normalizedEntries,
      overrides: {},
    }),
  };
}

function reviewedReciter(value) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw requestError('The reciter field must be a JSON object.');
  }

  return {
    slug: value.slug,
    catalog: value.catalog,
    name_en: value.name_en,
    name_ar: value.name_ar,
    riwayah: value.riwayah,
    style: value.style,
    tempo: value.tempo,
    audio_folder: value.audio_folder,
    country: value.country,
    server_url: value.server_url,
    available_surahs: value.available_surahs,
    full_surah_file_map: value.full_surah_file_map,
    description: value.description,
    is_teacher: value.is_teacher,
    enabled: value.enabled,
    verified: value.verified,
  };
}

function archiveDocumentData(payload, matchedIds) {
  const slug = slugifyArchiveName(payload?.slug);
  const nameEn = text(payload?.name_en);
  const nameAr = text(payload?.name_ar);
  if (!slug || !nameEn || !nameAr) {
    throw requestError('The reviewed reciter name and Arabic name are required.');
  }

  const matchedList = Array.from(matchedIds instanceof Set ? matchedIds : matchedIds || []);
  const catalog = normalizeCatalog(payload?.catalog);
  return {
    slug,
    catalog,
    catalog_key: catalogKeyFor(catalog, slug),
    name_en: nameEn,
    name_ar: nameAr,
    country: text(payload?.country),
    style: text(payload?.style, 'Murattal'),
    riwayah: text(payload?.riwayah, 'Hafs'),
    tempo: text(payload?.tempo, 'medium'),
    audio_folder: text(payload?.audio_folder),
    server_url: text(payload?.server_url),
    available_surahs: normalizeAvailableSurahs(
      payload?.available_surahs,
      matchedList.join(',')
    ),
    full_surah_file_map: text(payload?.full_surah_file_map),
    description: text(payload?.description),
    is_teacher: payload?.is_teacher === true,
    image_url: '',
    image_file_id: '',
    verified: payload?.verified !== false,
    enabled: payload?.enabled !== false,
  };
}

function safeFileName(value) {
  return text(value)
    .replace(/[^\w.\- ]+/g, '_')
    .slice(0, 100) || 'image';
}

function mapReciterDocument(document, metrics = {}) {
  const slug = document.slug || document.$id;
  const catalog = normalizeCatalog(document.catalog);
  return {
    id: document.$id,
    slug,
    catalog,
    catalogKey: document.catalog_key || catalogKeyFor(catalog, slug),
    nameEn: document.name_en || '',
    nameAr: document.name_ar || '',
    country: document.country || '',
    style: document.style || 'Murattal',
    riwayah: document.riwayah || 'Hafs',
    tempo: document.tempo || 'medium',
    audioFolder: document.audio_folder || '',
    serverUrl: document.server_url || '',
    availableSurahs: normalizeAvailableSurahs(document.available_surahs),
    description: document.description || '',
    isTeacher: document.is_teacher === true,
    imageUrl: document.image_url || '',
    imageFileId: document.image_file_id || '',
    followers: metrics.followers || 0,
    likes: metrics.likes || 0,
    verified: document.verified !== false,
    enabled: document.enabled !== false,
  };
}

function contextValue(context, key, fallback) {
  return context?.[key] || fallback;
}

/** Registers Internet Archive inspection, import, and connectivity routes. */
export function registerArchiveRoutes(app, getContext) {
  const router = express.Router();

  router.post('/api/archive/inspect', async (req, res) => {
    try {
      const input = text(req.body?.url) || text(req.body?.itemId);
      if (!input) throw requestError('An Internet Archive URL or item ID is required.');
      const itemId = archiveItemId(input);
      const raw = await fetchArchiveMetadata(itemId);
      const inspected = inspectData({ ...raw, metadata: raw?.metadata, files: raw?.files }, itemId);
      const { payload, ...response } = inspected;
      return res.json({ success: true, ...response, suggested: payload });
    } catch (error) {
      return sendError(res, error);
    }
  });

  router.post('/api/archive/import', upload.single('image'), async (req, res) => {
    try {
      const context = getContext();
      const { databases, databaseId, storage } = context;
      if (!databases || !databaseId) {
        throw requestError('PostgreSQL is not configured.', 503);
      }

      let reviewed;
      try {
        reviewed = JSON.parse(text(req.body?.reciter, '{}'));
      } catch {
        throw requestError('The reciter field must be valid JSON.');
      }

      const inputItemId = text(req.body?.itemId);
      if (!inputItemId) throw requestError('An Internet Archive item ID is required.');
      const itemId = archiveItemId(inputItemId);
      const raw = await fetchArchiveMetadata(itemId);
      const inspected = inspectData({ ...raw, metadata: raw?.metadata, files: raw?.files }, itemId);
      const matchedIds = new Set(
        inspected.entries
          .map((entry) => Number(entry?.surahId))
          .filter((id) => Number.isInteger(id) && id >= 1 && id <= TOTAL_SURAHS)
      );
      if (!matchedIds.size) {
        throw requestError('No matched surah audio was found for this Internet Archive item.');
      }

      const payload = buildArchiveReciterPayload({
        itemId,
        metadata: inspected.metadata,
        entries: inspected.entries,
        overrides: reviewedReciter(reviewed),
      });
      const data = archiveDocumentData(payload, matchedIds);
      const documents = await listAllDocuments(databases, databaseId, 'reciters');
      const duplicate = documents.find((document) =>
        document.catalog_key === data.catalog_key || document.$id === documentIdFor(data.catalog, data.slug)
      );
      if (duplicate) {
        throw requestError('A reciter with this slug already exists in this catalog.', 409);
      }
      const existing = await findDocument(databases, databaseId, data.catalog_key);
      if (existing) {
        throw requestError('A reciter with this slug already exists in this catalog.', 409);
      }

      let uploadedFileId = '';
      if (req.file) {
        if (!storage) {
          throw requestError('Cloudflare R2 is not configured.', 503);
        }
        if (!ALLOWED_IMAGE_TYPES.has(req.file.mimetype) || req.file.size > MAX_IMAGE_SIZE) {
          throw requestError('The image must be a JPG, PNG, WebP, or AVIF no larger than 5 MB.');
        }
        const file = await storage.upload(req.file.buffer, req.file.mimetype);
        uploadedFileId = file.key;
        data.image_file_id = uploadedFileId;
        data.image_url = file.url;
      }

      try {
        const document = await databases.createDocument(
          databaseId,
          'reciters',
          documentIdFor(data.catalog, data.slug),
          data
        );
        return res.status(201).json({ success: true, reciter: mapReciterDocument(document) });
      } catch (error) {
        if (uploadedFileId && storage) {
          await storage.delete(uploadedFileId).catch(() => undefined);
        }
        throw error;
      }
    } catch (error) {
      return sendError(res, error);
    }
  });

  router.get('/api/archive/health', async (_req, res) => {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 5_000);
    let reachable = false;
    try {
      const response = await fetch(ARCHIVE_DOWNLOAD_BASE, {
        method: 'HEAD',
        headers: archiveHeaders(),
        signal: controller.signal,
      });
      reachable = response.status < 500;
    } catch {
      reachable = false;
    } finally {
      clearTimeout(timer);
    }
    return res.json({ success: true, reachable, checkedAt: new Date().toISOString() });
  });

  router.use((error, _req, res, next) => {
    if (res.headersSent) return next(error);
    if (error?.code === 'LIMIT_FILE_SIZE') {
      return res.status(400).json({ success: false, error: 'The image must be 5 MB or smaller.' });
    }
    if (error?.code === 'LIMIT_UNEXPECTED_FILE') {
      return res.status(400).json({ success: false, error: 'Only one image file is allowed.' });
    }
    if (error?.message === 'Only JPG, PNG, WebP, and AVIF images are supported.') {
      return res.status(400).json({ success: false, error: error.message });
    }
    return sendError(res, error);
  });

  app.use(router);
}

export default registerArchiveRoutes;
