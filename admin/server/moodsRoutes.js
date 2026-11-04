import express from 'express';
import { Query } from './query.js';
import { MOODS_SEED } from './seedRoutes.js';

function toMood(doc) {
  return {
    slug: doc.slug ?? '',
    title: doc.title ?? '',
    subtitle: doc.subtitle ?? '',
    category: doc.category ?? '',
    default_reciter_slug: doc.default_reciter_slug ?? '',
    default_ambient: doc.default_ambient ?? '',
    cover_url: doc.cover_url ?? '',
  };
}

export function registerMoodsRoutes(app, getContext) {
  const router = express.Router();

  // GET /api/moods -> { total, moods: Mood[], source: 'live' | 'static' }
  router.get('/api/moods', async (req, res) => {
    try {
      const { databases, databaseId, databaseAvailable } = getContext();

      // Sandbox (no live client): static seed fallback.
      if (!databaseAvailable || !databases || !databaseId) {
        return res.json({ total: MOODS_SEED.length, moods: [...MOODS_SEED], source: 'static' });
      }

      try {
        const resDoc = await databases.listDocuments(databaseId, 'moods', [Query.limit(50)]);
        const docs = resDoc?.documents ?? [];
        if (docs.length > 0) {
          const moods = docs.map(toMood);
          return res.json({ total: moods.length, moods, source: 'live' });
        }
        // Empty live collection: fall back to seed.
        return res.json({ total: MOODS_SEED.length, moods: [...MOODS_SEED], source: 'static' });
      } catch (err) {
        console.log('ℹ️ Moods collection not reachable, using static seed.');
        return res.json({ total: MOODS_SEED.length, moods: [...MOODS_SEED], source: 'static' });
      }
    } catch (e) {
      console.error('Error in GET /api/moods:', e);
      res.status(500).json({ error: e.message });
    }
  });

  app.use(router);
}

export default registerMoodsRoutes;
