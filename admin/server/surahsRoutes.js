import express from 'express';
import { Query } from './query.js';
import { SURAHS_CATALOG } from './surahsData.js';
import { RECITERS_CATALOG } from './recitersData.js';

// Map a live `surahs` doc (7 canonical attrs only) to the full catalog item
// shape: canonical keys + legacy camelCase aliases kept for the admin UI.
function toCatalogItem(doc) {
  const surah_id = doc.surah_id;
  const name_en = doc.name_en;
  const name_ar = doc.name_ar;
  const transliteration = doc.transliteration;
  const ayahs_count = doc.ayahs_count;
  const revelation_type = doc.revelation_type;
  const meaning = doc.meaning;
  return {
    surah_id,
    name_en,
    name_ar,
    transliteration,
    ayahs_count,
    revelation_type,
    meaning,
    id: surah_id,
    number: surah_id,
    nameEn: name_en,
    nameAr: name_ar,
    translation: transliteration,
    revelationType: revelation_type,
    type: revelation_type,
    ayahs: ayahs_count
  };
}

function buildRecitersWithStreams(surahId) {
  const paddedId = String(surahId).padStart(3, '0');
  return RECITERS_CATALOG.map(reciter => {
    const baseUrl = reciter.serverUrl.endsWith('/') ? reciter.serverUrl : reciter.serverUrl + '/';
    return {
      slug: reciter.slug,
      nameEn: reciter.nameEn,
      nameAr: reciter.nameAr,
      country: reciter.country,
      style: reciter.style,
      riwayah: reciter.riwayah,
      streamUrl: `${baseUrl}${paddedId}.mp3`
    };
  });
}

export function registerSurahsRoutes(app, getContext) {
  const router = express.Router();

  // GET /api/surahs: Returns { total: 114, surahs, source }
  // Tries the live `surahs` collection first; falls back to the static
  // catalog (sandbox behavior) on any failure or when no client is wired.
  router.get('/api/surahs', async (req, res) => {
    try {
      try {
        const ctx = typeof getContext === 'function' ? getContext() : null;
        const { databases, databaseId, databaseAvailable } = ctx || {};
        if (databaseAvailable && databases && databaseId) {
          const live = await databases.listDocuments(databaseId, 'surahs', [Query.limit(120)]);
          const docs = live.documents || [];
          if (docs.length > 0) {
            const surahs = docs.map(toCatalogItem).sort((a, b) => a.surah_id - b.surah_id);
            return res.json({
              total: surahs.length,
              surahs,
              source: 'live'
            });
          }
        }
      } catch (liveErr) {
        console.error('Live GET /api/surahs failed, falling back to static:', liveErr.message);
      }
      res.json({
        total: SURAHS_CATALOG.length,
        surahs: SURAHS_CATALOG,
        source: 'static'
      });
    } catch (e) {
      console.error('Error in GET /api/surahs:', e);
      res.status(500).json({ error: e.message });
    }
  });

  // GET /api/surahs/:id: Returns Surah details with stream URLs for top reciters.
  // Tries the live `surahs` collection (Query.equal on surah_id) first,
  // falls back to the static catalog; 404 only when missing in both.
  // Canonical shape mirrors the `surahs` collection (surah_id, name_en,
  // name_ar, transliteration, ayahs_count, revelation_type, meaning);
  // legacy camelCase aliases are kept for the admin UI.
  router.get('/api/surahs/:id', async (req, res) => {
    try {
      const surahId = parseInt(req.params.id, 10);

      try {
        const ctx = typeof getContext === 'function' ? getContext() : null;
        const { databases, databaseId, databaseAvailable } = ctx || {};
        if (databaseAvailable && databases && databaseId && Number.isInteger(surahId)) {
          const live = await databases.listDocuments(databaseId, 'surahs', [
            Query.equal('surah_id', surahId),
            Query.limit(1)
          ]);
          const doc = (live.documents || [])[0];
          if (doc) {
            const surah = toCatalogItem(doc);
            return res.json({
              ...surah,
              reciters: buildRecitersWithStreams(surah.surah_id ?? surah.id),
              source: 'live'
            });
          }
        }
      } catch (liveErr) {
        console.error(`Live GET /api/surahs/${req.params.id} failed, falling back to static:`, liveErr.message);
      }

      const surah = SURAHS_CATALOG.find(s => s.surah_id === surahId || s.id === surahId);

      if (!surah) {
        return res.status(404).json({ error: `Surah with id ${req.params.id} not found` });
      }

      const recitersWithStreams = buildRecitersWithStreams(surah.surah_id ?? surah.id);

      res.json({
        ...surah,
        reciters: recitersWithStreams,
        source: 'static'
      });
    } catch (e) {
      console.error(`Error in GET /api/surahs/${req.params.id}:`, e);
      res.status(500).json({ error: e.message });
    }
  });

  app.use(router);
}

export default registerSurahsRoutes;
