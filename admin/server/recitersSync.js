import multer from 'multer';
import { Query } from './query.js';
import { RECITERS_CATALOG } from './recitersData.js';
import { portraitFor } from './portraits.js';

const MAX_IMAGE_SIZE = 5 * 1024 * 1024;
const ALLOWED_IMAGE_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp', 'image/avif']);
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

function text(value, fallback = '') {
  return typeof value === 'string' ? value.trim() : fallback;
}

function slugify(value) {
  return text(value)
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
}

const MP3QURAN_CATALOG = 'mp3quran';
const EVERYAYAH_CATALOG = 'everyayah';
const RECITER_CATALOGS = new Set([MP3QURAN_CATALOG, EVERYAYAH_CATALOG]);

/**
 * Normalizes to the app's ReciterCatalog wire values (see
 * ReciterCatalog.wire in quranify Models.kt). Unknown/missing values fall
 * back to mp3quran, matching ReciterCloudCache.catalogOf().
 */
function normalizeCatalog(value, fallback = MP3QURAN_CATALOG) {
  const raw = text(value, fallback).toLowerCase();
  return RECITER_CATALOGS.has(raw) ? raw : fallback;
}

/** Stable catalog-scoped identity "<catalog>:<slug>" — mirrors Reciter.catalogKey(). */
function catalogKeyFor(catalog, slug) {
  return `${catalog}:${slug}`;
}

/**
 * Appwrite row IDs: legacy bare slugs for mp3quran rows and
 * "everyayah-<slug>" for everyayah variants (':' is illegal in row IDs).
 * See the catalog_key note in quranify appwrite/collections.json.
 */
function documentIdFor(catalog, slug) {
  return catalog === EVERYAYAH_CATALOG ? `everyayah-${slug}` : slug;
}

/**
 * The app's availableSurahList is a comma-separated surah-ID list (see
 * Reciter.getAvailableSurahIds/isSurahAvailable); blank means all 114.
 * The legacy "1-114" range string is not parseable (toIntOrNull fails), so
 * normalize it to "" instead of persisting it.
 */
function normalizeAvailableSurahs(value, fallback = '') {
  const raw = text(value, fallback);
  if (raw === '1-114') return '';
  return raw;
}

function listAllDocuments(databases, databaseId, collectionId) {
  return (async () => {
    const documents = [];
    let offset = 0;
    let hasMore = true;

    while (hasMore) {
      const result = await databases.listDocuments(
        databaseId,
        collectionId,
        [Query.limit(100), Query.offset(offset)]
      );
      documents.push(...result.documents);
      hasMore = result.documents.length === 100;
      if (hasMore) offset += 100;
    }

    return documents;
  })();
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
    imageUrl: portraitFor(slug, document.image_url),
    imageFileId: document.image_file_id || '',
    followers: metrics.followers || 0,
    likes: metrics.likes || 0,
    verified: document.verified !== false,
    enabled: document.enabled !== false,
  };
}

function mapStaticReciter(reciter) {
  const catalog = normalizeCatalog(reciter.catalog);
  return {
    id: documentIdFor(catalog, reciter.slug),
    slug: reciter.slug,
    catalog,
    catalogKey: reciter.catalogKey || catalogKeyFor(catalog, reciter.slug),
    nameEn: reciter.nameEn || '',
    nameAr: reciter.nameAr || '',
    country: reciter.country || '',
    style: reciter.style || 'Murattal',
    riwayah: reciter.riwayah || 'Hafs',
    tempo: reciter.tempo || 'medium',
    audioFolder: reciter.audioFolder || '',
    serverUrl: reciter.serverUrl || '',
    availableSurahs: normalizeAvailableSurahs(reciter.availableSurahs || reciter.availableSurahList),
    description: reciter.description || '',
    isTeacher: reciter.isTeacher === true,
    imageUrl: reciter.imageUrl || reciter.photoUrl || '',
    imageFileId: '',
    followers: 0,
    likes: 0,
    verified: reciter.verified !== false,
    enabled: true,
  };
}

function applyFilters(reciters, query) {
  const { search, country, riwayah, style, server } = query;
  let filtered = reciters;

  if (search) {
    const value = String(search).toLowerCase();
    filtered = filtered.filter((reciter) =>
      [reciter.nameEn, reciter.nameAr, reciter.slug, reciter.country]
        .some((field) => field && field.toLowerCase().includes(value))
    );
  }

  if (country) {
    const value = String(country).toLowerCase();
    filtered = filtered.filter((reciter) => reciter.country.toLowerCase().includes(value));
  }

  if (riwayah) {
    const value = String(riwayah).toLowerCase();
    filtered = filtered.filter((reciter) => reciter.riwayah.toLowerCase().includes(value));
  }

  if (style) {
    const value = String(style).toLowerCase();
    filtered = filtered.filter(
      (reciter) => reciter.style.toLowerCase().includes(value) || reciter.riwayah.toLowerCase().includes(value)
    );
  }

  if (server) {
    const value = String(server).toLowerCase();
    filtered = filtered.filter(
      (reciter) => reciter.serverUrl.toLowerCase().includes(value) || reciter.audioFolder.toLowerCase().includes(value)
    );
  }

  return filtered;
}

function documentData(input, existing = {}) {
  const nextSlug = slugify(input.slug || existing.slug);
  const nameEn = text(input.nameEn, existing.name_en || '');
  const nameAr = text(input.nameAr, existing.name_ar || '');

  if (!nextSlug || !nameEn || !nameAr) {
    const error = new Error('slug, nameEn, and nameAr are required.');
    error.statusCode = 400;
    throw error;
  }

  // Catalog identity mirrors Reciter.catalogKey(): writers must always set
  // catalog (Appwrite forbids defaults on required columns); catalog_key is
  // derived so it can never drift from "<catalog>:<slug>".
  const catalog = normalizeCatalog(input.catalog ?? existing.catalog);
  return {
    slug: nextSlug,
    catalog,
    catalog_key: catalogKeyFor(catalog, nextSlug),
    name_en: nameEn,
    name_ar: nameAr,
    country: text(input.country, existing.country || ''),
    style: text(input.style, existing.style || 'Murattal'),
    riwayah: text(input.riwayah, existing.riwayah || 'Hafs'),
    tempo: text(input.tempo, existing.tempo || 'medium'),
    audio_folder: text(input.audioFolder, existing.audio_folder || ''),
    server_url: text(input.serverUrl, existing.server_url || ''),
    available_surahs: normalizeAvailableSurahs(input.availableSurahs, existing.available_surahs || ''),
    description: text(input.description, existing.description || ''),
    is_teacher: input.isTeacher === undefined ? existing.is_teacher === true : input.isTeacher === true,
    image_url: text(input.imageUrl, existing.image_url || ''),
    image_file_id: text(input.imageFileId, existing.image_file_id || ''),
    verified: input.verified === undefined ? existing.verified !== false : input.verified === true,
    enabled: input.enabled === undefined ? existing.enabled !== false : input.enabled === true,
  };
}

async function getMetrics(databases, databaseId) {
  const metrics = { followsBySlug: {}, likesBySlug: {} };
  if (!databases || !databaseId) return metrics;

  const [follows, likes] = await Promise.all([
    databases.listDocuments(databaseId, 'follows', [Query.limit(500)]).catch(() => ({ documents: [] })),
    databases.listDocuments(databaseId, 'likes', [Query.limit(500)]).catch(() => ({ documents: [] })),
  ]);

  follows.documents.forEach((document) => {
    if (document.reciter_slug) {
      metrics.followsBySlug[document.reciter_slug] = (metrics.followsBySlug[document.reciter_slug] || 0) + 1;
    }
  });

  likes.documents.forEach((document) => {
    if (document.reciter_slug) {
      metrics.likesBySlug[document.reciter_slug] = (metrics.likesBySlug[document.reciter_slug] || 0) + 1;
    }
  });

  return metrics;
}

async function listReciters(databases, databaseId) {
  if (!databases || !databaseId) {
    return RECITERS_CATALOG.map(mapStaticReciter);
  }

  const [documents, metrics] = await Promise.all([
    listAllDocuments(databases, databaseId, 'reciters'),
    getMetrics(databases, databaseId),
  ]);

  return documents.map((document) => mapReciterDocument(document, {
    followers: metrics.followsBySlug[document.slug] || 0,
    likes: metrics.likesBySlug[document.slug] || 0,
  }));
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

function findByCatalogKey(documents, catalogKey, exceptId = null) {
  return documents.find((document) =>
    document.catalog_key === catalogKey && document.$id !== exceptId
  ) || null;
}

function sendError(res, error) {
  const status = Number(error.statusCode) || 500;
  res.status(status).json({ error: error.message || 'Reciter request failed.' });
}

export function setupRecitersSync(
  app,
  databases,
  databaseId,
  storage
) {
  app.get('/api/reciters', async (req, res) => {
    try {
      const reciters = await listReciters(databases, databaseId);
      res.json({ total: reciters.length, reciters: applyFilters(reciters, req.query) });
    } catch (error) {
      sendError(res, error);
    }
  });

  app.get('/api/reciters/full', async (req, res) => {
    try {
      const reciters = await listReciters(databases, databaseId);
      res.json({ total: reciters.length, reciters });
    } catch (error) {
      sendError(res, error);
    }
  });

  app.post('/api/reciters', async (req, res) => {
    if (!databases || !databaseId) {
      res.status(503).json({ error: 'PostgreSQL is not configured.' });
      return;
    }

    try {
      const data = documentData(req.body);
      // Slug alone is NOT unique across catalogs (mp3quran + everyayah
      // variants coexist), so dedupe on catalog_key. Also guard the row ID
      // (bare slug vs "everyayah-<slug>") before Appwrite throws on it.
      const documents = await listAllDocuments(databases, databaseId, 'reciters');
      const docId = documentIdFor(data.catalog, data.slug);
      if (findByCatalogKey(documents, data.catalog_key) || documents.some((document) => document.$id === docId)) {
        res.status(409).json({ error: 'A reciter with this slug already exists in this catalog.' });
        return;
      }

      const document = await databases.createDocument(databaseId, 'reciters', docId, data);
      res.status(201).json({ success: true, reciter: mapReciterDocument(document) });
    } catch (error) {
      sendError(res, error);
    }
  });

  app.patch('/api/reciters/:id', async (req, res) => {
    if (!databases || !databaseId) {
      res.status(503).json({ error: 'PostgreSQL is not configured.' });
      return;
    }

    try {
      const existing = await findDocument(databases, databaseId, req.params.id);
      if (!existing) {
        res.status(404).json({ error: 'Reciter not found.' });
        return;
      }

      const data = documentData(req.body, existing);
      if (data.catalog_key !== existing.catalog_key) {
        const documents = await listAllDocuments(databases, databaseId, 'reciters');
        const duplicate = findByCatalogKey(documents, data.catalog_key, existing.$id);
        if (duplicate) {
          res.status(409).json({ error: 'A reciter with this slug already exists in this catalog.' });
          return;
        }
      }

      const document = await databases.updateDocument(databaseId, 'reciters', existing.$id, data);
      res.json({ success: true, reciter: mapReciterDocument(document) });
    } catch (error) {
      sendError(res, error);
    }
  });

  app.delete('/api/reciters/:id', async (req, res) => {
    if (!databases || !databaseId) {
      res.status(503).json({ error: 'PostgreSQL is not configured.' });
      return;
    }

    try {
      const existing = await findDocument(databases, databaseId, req.params.id);
      if (!existing) {
        res.status(404).json({ error: 'Reciter not found.' });
        return;
      }

      await databases.deleteDocument(databaseId, 'reciters', existing.$id);
      if (storage && existing.image_file_id) {
        await storage.delete(existing.image_file_id).catch(() => undefined);
      }
      res.json({ success: true, id: existing.$id });
    } catch (error) {
      sendError(res, error);
    }
  });

  app.post('/api/reciters/:id/image', upload.single('image'), async (req, res) => {
    if (!databases || !databaseId || !storage) {
      res.status(503).json({ error: 'Cloudflare R2 is not configured.' });
      return;
    }
    if (!req.file) {
      res.status(400).json({ error: 'An image file is required.' });
      return;
    }

    try {
      const existing = await findDocument(databases, databaseId, req.params.id);
      if (!existing) {
        res.status(404).json({ error: 'Reciter not found.' });
        return;
      }

      const file = await storage.upload(req.file.buffer, req.file.mimetype);
      let document;
      try {
        document = await databases.updateDocument(databaseId, 'reciters', existing.$id, {
          image_file_id: file.key,
          image_url: file.url,
        });
      } catch (error) {
        await storage.delete(file.key).catch(() => undefined);
        throw error;
      }

      if (existing.image_file_id) {
        await storage.delete(existing.image_file_id).catch(() => undefined);
      }

      res.json({ success: true, imageUrl: file.url, imageFileId: file.key, reciter: mapReciterDocument(document) });
    } catch (error) {
      sendError(res, error);
    }
  });

  app.post('/api/reciters/sync-catalog', async (req, res) => {
    if (!databases || !databaseId) {
      res.status(503).json({ error: 'PostgreSQL is not configured.' });
      return;
    }

    try {
      let syncedCount = 0;
      const total = RECITERS_CATALOG.length;
      const existingDocuments = await listAllDocuments(databases, databaseId, 'reciters');
      // Key by catalog_key (never bare slug): mp3quran + everyayah variants
      // coexist, so same-slug rows in the other catalog must not be touched.
      const existingByKey = new Map(existingDocuments.map((document) => [
        document.catalog_key || catalogKeyFor(normalizeCatalog(document.catalog), document.slug),
        document,
      ]));

      for (const reciter of RECITERS_CATALOG) {
        const catalog = normalizeCatalog(reciter.catalog);
        const existing = existingByKey.get(catalogKeyFor(catalog, reciter.slug));
        const data = documentData({
          slug: reciter.slug,
          catalog,
          nameEn: reciter.nameEn,
          nameAr: reciter.nameAr,
          country: reciter.country,
          style: reciter.style,
          riwayah: reciter.riwayah,
          tempo: reciter.tempo,
          audioFolder: reciter.audioFolder,
          serverUrl: reciter.serverUrl,
          availableSurahs: reciter.availableSurahList || reciter.availableSurahs || '',
          description: reciter.description,
          isTeacher: reciter.isTeacher,
          verified: reciter.verified,
          enabled: true,
          imageUrl: reciter.imageUrl,
        }, existing || {});

        if (!existing) {
          await databases.createDocument(databaseId, 'reciters', documentIdFor(catalog, data.slug), data);
        } else {
          await databases.updateDocument(databaseId, 'reciters', existing.$id, data);
        }
        syncedCount++;
      }

      res.json({ success: true, total, synced: syncedCount, message: `Successfully synchronized ${syncedCount} reciters out of ${total} catalog reciters.` });
    } catch (error) {
      sendError(res, error);
    }
  });
}
