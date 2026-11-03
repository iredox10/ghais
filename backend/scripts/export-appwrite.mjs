// One-shot export from Appwrite Cloud -> backend/export/<collection>.json.
//
// Prereqs in env (operator shell; the CLIENT never holds these):
//   APPWRITE_ENDPOINT=https://fra.cloud.appwrite.io/v1
//   APPWRITE_PROJECT_ID=tikitakatalk
//   APPWRITE_API_KEY=<server key with databases.read>
// Usage: node scripts/export-appwrite.mjs [collection ...]   (default: all)
import { mkdir, writeFile } from 'node:fs/promises';
import path from 'node:path';

const ENDPOINT = process.env.APPWRITE_ENDPOINT;
const PROJECT = process.env.APPWRITE_PROJECT_ID;
const API_KEY = process.env.APPWRITE_API_KEY;
if (!ENDPOINT || !PROJECT || !API_KEY) {
  console.error('Set APPWRITE_ENDPOINT, APPWRITE_PROJECT_ID, APPWRITE_API_KEY first.');
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
  'hifz_mastery',
  'search_history',
  'telemetry',
];

const pages = 100;
const outDir = path.join(path.dirname(new URL(import.meta.url).pathname), '..', 'export');
await mkdir(outDir, { recursive: true });

async function pull(collection) {
  const documents = [];
  let offset = 0;
  for (;;) {
    const queries = [
      JSON.stringify({ method: 'limit', values: [pages] }),
      JSON.stringify({ method: 'offset', values: [offset] }),
    ];
    const params = new URLSearchParams();
    queries.forEach(q => params.append('queries[]', q));
    const url = `${ENDPOINT}/databases/ghais/collections/${collection}/documents?${params}`;
    const res = await fetch(url, {
      headers: { 'X-Appwrite-Project': PROJECT, 'X-Appwrite-Key': API_KEY },
    });
    if (!res.ok) {
      throw new Error(`${collection}: HTTP ${res.status} ${await res.text()}`);
    }
    const body = await res.json();
    const docs = body.documents ?? [];
    documents.push(...docs);
    offset += docs.length;
    if (docs.length < pages || offset >= body.total) break;
  }
  const file = path.join(outDir, `${collection}.json`);
  await writeFile(file, JSON.stringify(documents, null, 2));
  console.log(`${collection}: ${documents.length} docs -> ${file}`);
}

const wanted = process.argv.slice(2);
const targets = wanted.length > 0 ? wanted : ALL;
for (const collection of targets) {
  if (!ALL.includes(collection)) throw new Error(`Unknown collection: ${collection}`);
}
let failures = 0;
for (const collection of targets) {
  try {
    await pull(collection);
  } catch (e) {
    failures++;
    console.error(e.message);
  }
}
process.exit(failures > 0 ? 1 : 0);
