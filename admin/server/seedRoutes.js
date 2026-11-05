import express from 'express';
import { FULL_SURAHS_LIST } from './surahsData.js';

/**
 * Live seed-backfill routes for the EMPTY `surahs` (0 docs) and `moods`
 * (0 docs) collections.
 *
 * Sources:
 * - Surahs: FULL_SURAHS_LIST in ./surahsData.js (114 canonical rows).
 *   Only the 7 canonical `surahs` attrs are persisted
 *   (surah_id, name_en, name_ar, transliteration, ayahs_count,
 *   revelation_type, meaning) — legacy camelCase UI aliases are stripped.
 *   Doc ID: `surah-<surah_id>`.
 * - Moods: AVAILABLE_MOODS in
 *   shared/.../ui/screens/mood/MoodScreen.kt (id -> slug, title, subtitle,
 *   category verbatim). default_reciter_slug is "mishary" for all — the app
 *   plays mood selections via mishary (MoodScreen onMoodSelected). 
 *   default_ambient values are AmbientMixer asset keys
 *   (shared/.../player/AmbientMixer.kt assetKey(): rain/birds/waves/crickets);
 *   "" means no ambient (focus modes). cover_url has no app source and is
 *   left "" (optional attr).
 *
 * Strategy: true upsert — getDocument first; update if it exists, create if
 * it is missing. `seeded` counts successful writes, `skipped` counts docs
 * that could not be written (error detail logged server-side).
 *
 * Wiring: registerSeedRoutes(app, getContext) — same (app, getContext)
 * pattern as exportRoutes.js. Central wiring in server/index.js lands
 * separately; this file registers nothing on import.
 */

export const MOODS_SEED = [
  {
    slug: 'gratitude',
    title: 'Gratitude (Shukr)',
    subtitle: 'Surah Ibrahim • Luqman • Ar-Rahman',
    category: 'Reflection',
    default_reciter_slug: 'mishary',
    default_ambient: 'birds',
    cover_url: '',
  },
  {
    slug: 'anxiety',
    title: 'Anxiety & Relief',
    subtitle: 'Ash-Sharh • Ad-Duha • Yusuf',
    category: 'Comfort',
    default_reciter_slug: 'mishary',
    default_ambient: 'rain',
    cover_url: '',
  },
  {
    slug: 'hifz',
    title: 'Hifz Memorization',
    subtitle: 'Repeat loop mode • Steady pace recitations',
    category: 'Focus',
    default_reciter_slug: 'mishary',
    default_ambient: '',
    cover_url: '',
  },
  {
    slug: 'qiyam',
    title: 'Qiyam al-Layl',
    subtitle: 'Deep night recitations • Al-Mulk • Al-Muzzammil',
    category: 'Night',
    default_reciter_slug: 'mishary',
    default_ambient: 'crickets',
    cover_url: '',
  },
  {
    slug: 'study',
    title: 'Deep Study & Focus',
    subtitle: 'Focus-oriented recitation with calming tempo',
    category: 'Focus',
    default_reciter_slug: 'mishary',
    default_ambient: '',
    cover_url: '',
  },
  {
    slug: 'sleep',
    title: 'Restful Sleep',
    subtitle: 'Calming slow recitations to ease your mind',
    category: 'Night',
    default_reciter_slug: 'mishary',
    default_ambient: 'waves',
    cover_url: '',
  },
];

function surahPayload(s) {
  return {
    surah_id: s.surah_id,
    name_en: s.name_en,
    name_ar: s.name_ar,
    transliteration: s.transliteration,
    ayahs_count: s.ayahs_count,
    revelation_type: s.revelation_type,
    meaning: s.meaning,
  };
}

// Upsert one doc by explicit ID: update when present, create when missing.
// Returns 'seeded' on any successful write, 'skipped' otherwise.
async function upsertById(databases, databaseId, collectionId, docId, data) {
  try {
    let exists = true;
    try {
      await databases.getDocument(databaseId, collectionId, docId);
    } catch (e) {
      if (e && (e.code === 404 || /not found/i.test(e.message || ''))) {
        exists = false;
      } else {
        throw e;
      }
    }
    if (exists) {
      await databases.updateDocument(databaseId, collectionId, docId, data);
    } else {
      await databases.createDocument(databaseId, collectionId, docId, data);
    }
    return 'seeded';
  } catch (e) {
    console.error(`Seed ${collectionId}/${docId} failed:`, e.message);
    return 'skipped';
  }
}

export function registerSeedRoutes(app, getContext) {
  const router = express.Router();

  // POST /api/seed/surahs — upsert all 114 canonical surahs (docId `surah-<id>`)
  router.post('/api/seed/surahs', async (req, res) => {
    try {
      const { databases, databaseId, databaseAvailable } = getContext();
      const total = FULL_SURAHS_LIST.length;
      if (!databaseAvailable || !databases || !databaseId) {
        return res.status(503).json({
          success: false,
          error: 'PostgreSQL is not configured.',
          seeded: 0,
          skipped: total,
          total,
        });
      }

      let seeded = 0;
      let skipped = 0;
      for (const s of FULL_SURAHS_LIST) {
        const outcome = await upsertById(
          databases,
          databaseId,
          'surahs',
          `surah-${s.surah_id}`,
          surahPayload(s)
        );
        if (outcome === 'seeded') seeded++;
        else skipped++;
      }
      res.json({ success: true, seeded, skipped, total });
    } catch (e) {
      console.error('Error in POST /api/seed/surahs:', e);
      res.status(500).json({ success: false, error: e.message, seeded: 0, skipped: 0, total: FULL_SURAHS_LIST.length });
    }
  });

  // POST /api/seed/moods — upsert app mood seeds (docId = slug)
  router.post('/api/seed/moods', async (req, res) => {
    try {
      const { databases, databaseId, databaseAvailable } = getContext();
      const total = MOODS_SEED.length;
      if (!databaseAvailable || !databases || !databaseId) {
        return res.status(503).json({
          success: false,
          error: 'PostgreSQL is not configured.',
          seeded: 0,
          skipped: total,
          total,
        });
      }

      let seeded = 0;
      let skipped = 0;
      for (const m of MOODS_SEED) {
        const outcome = await upsertById(databases, databaseId, 'moods', m.slug, { ...m });
        if (outcome === 'seeded') seeded++;
        else skipped++;
      }
      res.json({ success: true, seeded, skipped, total });
    } catch (e) {
      console.error('Error in POST /api/seed/moods:', e);
      res.status(500).json({ success: false, error: e.message, seeded: 0, skipped: 0, total: MOODS_SEED.length });
    }
  });

  app.use(router);
}

export default registerSeedRoutes;
