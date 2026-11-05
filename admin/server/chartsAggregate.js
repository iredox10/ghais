/**
 * Global charts aggregation (mirrors appwrite/functions/aggregate-charts).
 *
 * Reads the `play_events` collection for the last 30 days, dedupes to one
 * count per ownerId+reciterSlug+surahId per UTC day (anti-gaming), computes:
 *   - topQaris7d        — top-20 reciter slugs, last 7 days. The consumer
 *     app reads this as "Weekly most heard" (its Reciters section was
 *     renamed from yesterday to weekly).
 *   - topQaris30d       — top-20 reciter slugs, last 30 days
 *   - topQarisYesterday — top-20 in yesterday's UTC day. LEGACY: for older
 *     clients only (the app now reads topQaris7d as "Weekly most heard").
 *     Still computed so the stored shape never loses the field.
 *   - trending          — top-20 by velocity (7d share vs 30d baseline)
 * then upserts the single `charts/global` document.
 *
 * Entry points: POST /api/charts/aggregate (manual + cron-callable) and
 * GET /api/charts/yesterday (read-through of the stored window for ops;
 * GET /api/charts/week in server/index.js reads topQaris7d the same way).
 * Run the POST daily after 00:05 UTC so yesterday's window is complete.
 */
import { Query } from './query.js';

const PLAY_EVENTS_COLLECTION = 'play_events';
// Exported so server/index.js can register the GET /api/charts/week
// read-through against the same stored document without duplicating ids.
export const CHARTS_COLLECTION = 'charts';
export const CHARTS_DOCUMENT_ID = 'global';
const TOP_N = 20;
const WINDOW_7D_MS = 7 * 24 * 60 * 60 * 1000;
const WINDOW_30D_MS = 30 * 24 * 60 * 60 * 1000;
const PAGE_LIMIT = 500;

function topN(counts, n) {
  return [...counts.entries()].sort((a, b) => b[1] - a[1]).slice(0, n).map(([slug]) => slug);
}

export async function aggregateCharts(databases, databaseId) {
  const now = new Date();
  const cutoff7 = new Date(now.getTime() - WINDOW_7D_MS);
  const cutoff30 = new Date(now.getTime() - WINDOW_30D_MS);
  const todayStart = new Date(now);
  todayStart.setUTCHours(0, 0, 0, 0);
  const yesterdayStart = new Date(todayStart.getTime() - 24 * 60 * 60 * 1000);

  let scanned = 0;
  let skipped = 0;
  let offset = 0;
  const seen = new Set();
  const count7d = new Map();
  const count30d = new Map();
  const countYesterday = new Map();

  for (;;) {
    const page = await databases.listDocuments(databaseId, PLAY_EVENTS_COLLECTION, [
      Query.greaterThan('$createdAt', cutoff30.toISOString()),
      Query.orderAsc('$createdAt'),
      Query.limit(PAGE_LIMIT),
      Query.offset(offset),
    ]);
    const docs = page.documents || [];
    if (docs.length === 0) break;

    for (const doc of docs) {
      scanned += 1;
      const slug = doc.reciterSlug;
      const surahId = doc.surahId;
      const createdAt = doc.$createdAt ? new Date(doc.$createdAt) : null;
      if (typeof slug !== 'string' || slug.length === 0 || typeof surahId !== 'number' || !createdAt || Number.isNaN(createdAt.getTime())) {
        skipped += 1;
        continue;
      }
      const day = createdAt.toISOString().slice(0, 10);
      const dedupeKey = `${doc.ownerId || doc.$id}|${slug}|${surahId}|${day}`;
      if (seen.has(dedupeKey)) {
        skipped += 1;
        continue;
      }
      seen.add(dedupeKey);
      count30d.set(slug, (count30d.get(slug) || 0) + 1);
      if (createdAt >= cutoff7) count7d.set(slug, (count7d.get(slug) || 0) + 1);
      if (createdAt >= yesterdayStart && createdAt < todayStart) {
        countYesterday.set(slug, (countYesterday.get(slug) || 0) + 1);
      }
    }

    offset += docs.length;
    if (docs.length < PAGE_LIMIT) break;
  }

  const topQaris7d = topN(count7d, TOP_N);
  const topQaris30d = topN(count30d, TOP_N);
  const topQarisYesterday = topN(countYesterday, TOP_N);

  const total7 = [...count7d.values()].reduce((a, b) => a + b, 0);
  const total30 = [...count30d.values()].reduce((a, b) => a + b, 0);
  const trending =
    total7 === 0 || total30 === 0
      ? [...topQaris7d]
      : [...count7d.entries()]
          .map(([slug, c7]) => [slug, c7 / total7 - (count30d.get(slug) || 0) / total30])
          .sort((a, b) => b[1] - a[1])
          .slice(0, TOP_N)
          .map(([slug]) => slug);

  const payload = {
    topQaris7d,
    topQaris30d,
    topQarisYesterday,
    trending,
    generatedAt: now.toISOString(),
  };

  try {
    await databases.updateDocument(databaseId, CHARTS_COLLECTION, CHARTS_DOCUMENT_ID, payload);
  } catch (e) {
    if (e && (e.code === 404 || e.type === 'document_not_found')) {
      await databases.createDocument(databaseId, CHARTS_COLLECTION, CHARTS_DOCUMENT_ID, payload);
    } else {
      throw e;
    }
  }

  return {
    scanned,
    deduped: seen.size,
    slugs7d: count7d.size,
    slugs30d: count30d.size,
    slugsYesterday: countYesterday.size,
    skipped,
    topQaris7d,
    topQaris30d,
    topQarisYesterday,
    trending,
    generatedAt: payload.generatedAt,
  };
}

export function registerChartsAggregate(app, getCtx) {
  app.post('/api/charts/aggregate', async (req, res) => {
    try {
      const token = process.env.MOBILE_API_ADMIN_TOKEN;
      if (!token) return res.status(503).json({ ok: false, error: 'Chart job token is not configured.' });
      const base = (process.env.PUBLIC_API_URL || 'https://ghaith-c77c2426ed4d.herokuapp.com').replace(/\/+$/, '');
      const response = await fetch(`${base}/v1/jobs/charts-rollup`, {
        method: 'POST', headers: { Authorization: `Bearer ${token}` }, signal: AbortSignal.timeout(120000),
      });
      if (!response.ok) return res.status(502).json({ ok: false, error: 'Chart job failed.' });
      return res.json({ ok: true, ...await response.json() });
    } catch (e) {
      console.error('Error in POST /api/charts/aggregate:', e?.message || e);
      return res.status(500).json({ ok: false, error: e?.message || 'aggregation failed' });
    }
  });

  app.get('/api/charts/yesterday', async (req, res) => {
    try {
      const { databases, databaseId } = getCtx();
      if (!databases) {
        return res.status(503).json({ ok: false, error: 'PostgreSQL is not configured.' });
      }
      const doc = await databases.getDocument(databaseId, CHARTS_COLLECTION, CHARTS_DOCUMENT_ID);
      return res.json({
        ok: true,
        topQarisYesterday: Array.isArray(doc.topQarisYesterday) ? doc.topQarisYesterday : [],
        generatedAt: doc.generatedAt || null,
      });
    } catch (e) {
      if (e && (e.code === 404 || e.type === 'document_not_found')) {
        return res.json({ ok: true, topQarisYesterday: [], generatedAt: null });
      }
      console.error('Error in GET /api/charts/yesterday:', e?.message || e);
      return res.status(500).json({ ok: false, error: e?.message || 'lookup failed' });
    }
  });
}
