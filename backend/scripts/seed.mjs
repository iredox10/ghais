// Seeds the Render/Postgres backend from the backend/export/*.json dumps
// produced by scripts/export-appwrite.mjs.
//
// Usage: DATABASE_URL=postgres://… node scripts/seed.mjs [collection ...]
// Idempotent: every collection seeds with ON CONFLICT (id) DO UPDATE, so it
// can be re-run against a live service without duplicating data.
// `users` are intentionally NOT seeded (Appwrite password hashes are
// non-portable; users re-register, per the start-clean decision).
import { readFile } from 'node:fs/promises';
import path from 'node:path';
import pg from 'pg';

const { Pool } = pg;

const DATABASE_URL = process.env.DATABASE_URL;
if (!DATABASE_URL) {
  console.error('Set DATABASE_URL first.');
  process.exit(1);
}

const ALL = [
  'charts',
  'broadcasts',
  'playlists',
  'playlist_items',
  'reciters',
  'reciter_stats',
  'likes',
  'playback_state',
  'follows',
  'listening_stats',
  'schedules',
  'history',
  'routine_backups',
  'user_prefs',
  'khatma_plans',
  'play_events',
  'devotion',
  'leagues',
  'friendships',
  'global_charts',
  'achievements',
];

/** Appwrite Document JSON -> (id, owner, doc) insertion triple. */
function rowOf(collection, document) {
  const data = { ...document };
  const id = String(data.$id ?? '');
  delete data.$id;
  delete data.$collectionId;
  delete data.$databaseId;
  delete data.$permissions;
  delete data.$createdAt;
  delete data.$updatedAt;
  const owner =
    data.user_id ?? data.userId ?? data.ownerId ?? (collection === 'reciters' || collection === 'broadcasts' ? null : null);
  const ownerColumn = typeof owner === 'string' && owner ? owner : null;
  // createdAt/updatedAt are preserved as columns.
  const createdAt = document.$createdAt ? new Date(document.$createdAt) : new Date();
  const updatedAt = document.$updatedAt ? new Date(document.$updatedAt) : createdAt;
  return { id, owner: ownerColumn, doc: data, createdAt, updatedAt };
}

const pool = new Pool({ connectionString: DATABASE_URL });
const wanted = process.argv.slice(2);
const targets = wanted.length > 0 ? wanted : ALL;
const exportDir = path.join(path.dirname(new URL(import.meta.url).pathname), '..', 'export');
let failures = 0;

for (const collection of targets) {
  let documents;
  try {
    documents = JSON.parse(await readFile(path.join(exportDir, `${collection}.json`), 'utf8'));
  } catch {
    console.warn(`${collection}: no export file, skipped.`);
    continue;
  }
  if (!Array.isArray(documents) || documents.length === 0) {
    console.log(`${collection}: empty export, skipped.`);
    continue;
  }
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    for (const raw of documents) {
      const { id, owner, doc, createdAt, updatedAt } = rowOf(collection, raw);
      if (!id || id === 'unique()') continue; // server-generated ids already resolved in the export
      await client.query(
        `INSERT INTO ${collection}(id, owner, doc, created_at, updated_at)
         VALUES ($1,$2,$3,$4,$5)
         ON CONFLICT (id) DO UPDATE SET doc = EXCLUDED.doc, owner = COALESCE(${collection}.owner, EXCLUDED.owner),
           created_at = ${collection}.created_at, updated_at = EXCLUDED.updated_at`,
        [id, owner, doc, createdAt, updatedAt],
      );
    }
    await client.query('COMMIT');
    console.log(`${collection}: ${documents.length} docs seeded.`);
  } catch (e) {
    await client.query('ROLLBACK');
    failures++;
    console.error(`${collection}: seed failed: ${e.message}`);
  } finally {
    client.release();
  }
}

await pool.end();
process.exit(failures > 0 ? 1 : 0);
