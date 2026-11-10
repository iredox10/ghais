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
          // Partial catalogs are expected: editing one bundled mood must not
          // make the other bundled moods disappear from the admin view.
          const live = new Map(docs.map(doc => [doc.slug, toMood(doc)]));
          const moods = MOODS_SEED.map(seed => live.get(seed.slug) ?? seed);
          moods.push(...docs.filter(doc => !MOODS_SEED.some(seed => seed.slug === doc.slug)).map(toMood));
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

  // The mobile app only knows playback for the six bundled IDs; edit their
  // display copy without creating unsupported, unplayable moods.
  router.patch('/api/moods/:slug', async (req, res) => {
    const seed = MOODS_SEED.find(mood => mood.slug === req.params.slug);
    if (!seed) return res.status(404).json({ error: 'Mood is not available on mobile.' });
    const { title, subtitle, category } = req.body || {};
    if (typeof title !== 'string' || !title.trim() || title.trim().length > 120 ||
        typeof subtitle !== 'string' || !subtitle.trim() || subtitle.trim().length > 240 ||
        !['Reflection', 'Comfort', 'Focus', 'Night'].includes(category)) {
      return res.status(400).json({ error: 'Provide a title, subtitle and supported category.' });
    }
    const { databases, databaseId, databaseAvailable } = getContext();
    if (!databaseAvailable || !databases || !databaseId) return res.status(503).json({ error: 'PostgreSQL is not configured.' });
    try {
      const matches = await databases.listDocuments(databaseId, 'moods', [Query.equal('slug', seed.slug), Query.limit(2)]);
      if (matches.documents.length > 1) return res.status(409).json({ error: 'Duplicate mood records must be resolved first.' });
      const patch = { title: title.trim(), subtitle: subtitle.trim(), category };
      const current = matches.documents[0];
      const doc = current
        ? await databases.updateDocument(databaseId, 'moods', current.$id, patch)
        : await databases.createDocument(databaseId, 'moods', seed.slug, { ...seed, ...patch });
      return res.json({ mood: toMood(doc) });
    } catch (err) {
      console.error('Could not update mood:', err.message);
      return res.status(500).json({ error: 'Could not update mood.' });
    }
  });

  app.use(router);
}

export default registerMoodsRoutes;
