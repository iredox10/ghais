/**
 * aggregate-charts — Quranify global charts aggregation.
 *
 * Runtime: node-22 (same Node.js server-side language as scripts/seed_appwrite.js;
 * there are no pre-existing functions in this repo, so this sets the convention:
 * CommonJS + node-appwrite, Databases queried via Query helpers).
 * Schedule: daily, 03:00 UTC (cron "0 3 * * *", registered via the CLI — see report).
 *
 * Reads the `play_events` collection for the last 30 days, dedupes to one count
 * per ownerId+reciterSlug+surahId per UTC day (anti-gaming), computes:
 *   - topQaris7d  — top-20 reciter slugs by deduped count, last 7 days
 *   - topQaris30d — top-20 reciter slugs by deduped count, last 30 days
 *   - topQarisYesterday — top-20 by deduped count in yesterday's UTC day
 *     (global "Yesterday's most heard", all listeners)
 *   - trending    — top-20 by velocity: 7d share of plays vs 30d baseline share
 * then upserts the single `charts/global` document with generatedAt = now.
 *
 * Auth: runs with APPWRITE_API_KEY (server key), which bypasses the collection
 * permissions — required because play_events grants clients create-only and
 * charts grants clients read-only (see appwrite/collections.json notes).
 *
 * Required function variables (Console > Functions > aggregate-charts > Settings):
 *   APPWRITE_ENDPOINT   e.g. https://fra.cloud.appwrite.io/v1
 *   APPWRITE_PROJECT_ID e.g. tikitakatalk
 *   APPWRITE_DATABASE_ID e.g. ghais
 *   APPWRITE_API_KEY    server API key with databases.read + databases.write
 */

const { Client, Databases, Query } = require('node-appwrite');

const PLAY_EVENTS_COLLECTION = 'play_events';
const CHARTS_COLLECTION = 'charts';
const CHARTS_DOCUMENT_ID = 'global';
const TOP_N = 20;
const WINDOW_7D_MS = 7 * 24 * 60 * 60 * 1000;
const WINDOW_30D_MS = 30 * 24 * 60 * 60 * 1000;
const PAGE_LIMIT = 500;

function topN(counts, n) {
  return [...counts.entries()].sort((a, b) => b[1] - a[1]).slice(0, n).map(([slug]) => slug);
}

module.exports = async ({ res, log, error }) => {
  const endpoint = process.env.APPWRITE_ENDPOINT;
  const projectId = process.env.APPWRITE_PROJECT_ID;
  const databaseId = process.env.APPWRITE_DATABASE_ID || 'ghais';
  const apiKey = process.env.APPWRITE_API_KEY;

  if (!endpoint || !projectId || !apiKey) {
    const msg = 'Missing APPWRITE_ENDPOINT / APPWRITE_PROJECT_ID / APPWRITE_API_KEY variables.';
    error(msg);
    return res.json({ ok: false, error: msg }, 500);
  }

  const client = new Client().setEndpoint(endpoint).setProject(projectId).setKey(apiKey);
  const databases = new Databases(client);

  const now = new Date();
  const cutoff7 = new Date(now.getTime() - WINDOW_7D_MS);
  const cutoff30 = new Date(now.getTime() - WINDOW_30D_MS);
  // Yesterday's UTC window [00:00, 00:00): powers the global
  // "Yesterday's most heard" strip (all listeners, not per-user).
  const todayStart = new Date(now);
  todayStart.setUTCHours(0, 0, 0, 0);
  const yesterdayStart = new Date(todayStart.getTime() - 24 * 60 * 60 * 1000);

  // Page the last 30 days of events, oldest first. $createdAt is a system
  // attribute (no declared index possible), so range-page on it directly.
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
      // Dedupe: one count per owner + reciter + surah per UTC day.
      // Anonymous events have no ownerId, so fall back to the document $id
      // (each anonymous event still counts once — it cannot collide).
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

  // Trending by velocity: 7d share of plays minus 30d baseline share.
  // A reciter surging this week scores positive; steady giants score ~0.
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
    log(`charts/global updated: scanned=${scanned} kept30d=${seen.size} slugs7d=${count7d.size} slugs30d=${count30d.size} skipped=${skipped}`);
  } catch (e) {
    if (e && (e.code === 404 || e.type === 'document_not_found')) {
      await databases.createDocument(databaseId, CHARTS_COLLECTION, CHARTS_DOCUMENT_ID, payload);
      log(`charts/global created: scanned=${scanned} kept30d=${seen.size} slugs7d=${count7d.size} slugs30d=${count30d.size} skipped=${skipped}`);
    } else {
      throw e;
    }
  }

  return res.json({
    ok: true,
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
  });
};
