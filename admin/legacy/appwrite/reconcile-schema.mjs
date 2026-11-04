// Reconcile live Appwrite against appwrite/collections.json.
// Additive only: creates missing collections/attributes/indexes, never drops.
// Unlike the server bootstrap it WAITS for each attribute to reach
// `available` before creating an index over it, and re-checks indexes at the
// end — the race that silently loses indexes on a first run.
import fs from 'fs';
import vm from 'vm';
import { Client, Databases } from 'node-appwrite';

const ADMIN = '/home/iredox/Desktop/space/quranify-admin';
const env = Object.fromEntries(
  fs.readFileSync(ADMIN + '/.env', 'utf8').split('\n')
    .filter(l => l.includes('=') && !l.trim().startsWith('#'))
    .map(l => { const i = l.indexOf('='); return [l.slice(0, i).trim(), l.slice(i + 1).trim().replace(/^["']|["']$/g, '')]; })
);
const man = JSON.parse(fs.readFileSync('/home/iredox/Desktop/space/quranify/appwrite/collections.json', 'utf8'));

const c = new Client().setEndpoint(env.APPWRITE_ENDPOINT).setProject(env.APPWRITE_PROJECT_ID).setKey(env.APPWRITE_API_KEY);
const db = new Databases(c);
const DB = env.APPWRITE_DATABASE_ID;
const log = (...a) => console.log(...a);
const sleep = ms => new Promise(r => setTimeout(r, ms));

async function retry(label, fn, tries = 5) {
  let last;
  for (let i = 1; i <= tries; i++) {
    try { return await fn(); }
    catch (e) { last = e; log(`   [${label}] ${i}/${tries} ${e?.message || e}`); await sleep(2000 * i); }
  }
  throw last;
}

const permOf = col => col['$permissions'] || col.permissions || [];
const attrType = a => a.type;
const createAttr = {
  string: (k, a) => db.createStringAttribute(DB, a.__col, k, a.size, !!a.required, a.default ?? undefined),
  integer: (k, a) => db.createIntegerAttribute(DB, a.__col, k, !!a.required, a.default ?? undefined),
  float: (k, a) => db.createFloatAttribute(DB, a.__col, k, !!a.required, a.default ?? undefined),
  boolean: (k, a) => db.createBooleanAttribute(DB, a.__col, k, !!a.required, a.default ?? undefined),
  datetime: (k, a) => db.createDateTimeAttribute(DB, a.__col, k, !!a.required, a.default ?? undefined),
};

async function attrsAvailable(colId, keys, tries = 24) {
  for (let i = 0; i < tries; i++) {
    const a = await retry('listAttributes:' + colId, () => db.listAttributes(DB, colId));
    const by = Object.fromEntries(a.attributes.map(x => [x.key, x]));
    const waiting = keys.filter(k => by[k] && by[k].status !== 'available');
    const absent = keys.filter(k => !by[k]);
    if (!waiting.length && !absent.length) return true;
    if (i === 0) log(`   ${colId}: waiting for ${[...absent, ...waiting.map(k => k + '(processing)')].join(', ')}`);
    await sleep(2000);
  }
  return false;
}

let createdCols = 0, createdAttrs = 0, createdIdx = 0, skipped = [];

for (const col of man.collections) {
  const id = col.$id;
  let live = (await retry('get:' + id, () => db.getCollection(DB, id).catch(e => { if (e.code === 404) return null; throw e; })));

  if (!live) {
    await retry('createCollection:' + id, () => db.createCollection(DB, id, col.name, col.permissions || [], !!col.documentSecurity));
    createdCols++;
    log(`+ collection ${id}`);
    live = await retry('get2:' + id, () => db.getCollection(DB, id));
  }

  // attributes
  const have = new Set((live.attributes || []).map(a => a.key));
  for (const a of col.attributes || []) {
    if (have.has(a.key)) continue;
    // Appwrite forbids a default on a required column; mirror the bootstrap.
    const spec = { ...a, __col: id };
    if (a.required) delete spec.default;
    try {
      await retry('createAttr:' + id + '.' + a.key, () => createAttr[attrType(a)](a.key, spec));
      createdAttrs++;
      log(`+ attr ${id}.${a.key} (${attrType(a)}${a.size ? '/' + a.size : ''})`);
    } catch (e) {
      // a concurrent run may have created it
      const re = await retry('reget:'+id, () => db.getCollection(DB, id).catch(() => null));
      if (re && (re.attributes || []).some(x => x.key === a.key)) continue;
      skipped.push(`${id}.${a.key}: ${e?.message || e}`);
    }
  }

  // indexes — only once every column they touch is available
  const want = col.indexes || [];
  if (!want.length) continue;
  const colsNeeded = [...new Set(want.flatMap(i => i.attributes))];
  if (!await attrsAvailable(id, colsNeeded)) {
    skipped.push(`${id}: attributes never became available, indexes deferred`);
    continue;
  }
  const liveNow = await retry('get:'+id, () => db.getCollection(DB, id));
  const haveIdx = new Set((liveNow.indexes || []).map(i => i.key));
  for (const i of want) {
    if (haveIdx.has(i.key)) continue;
    try {
      await retry('createIndex:' + i.key, () => db.createIndex(DB, id, i.key, i.type, i.attributes));
      createdIdx++;
      log(`+ index ${id}.${i.key} (${i.type} on ${i.attributes.join(',')})`);
    } catch (e) { skipped.push(`${id}.${i.key}: ${e?.message || e}`); }
  }
}

log('\n=== summary ===');
log(`collections created: ${createdCols}   attributes created: ${createdAttrs}   indexes created: ${createdIdx}`);
if (skipped.length) { log('deferred / failed:'); skipped.forEach(s => log('  - ' + s)); }
else log('nothing deferred');

const after = await retry('final', () => db.listCollections(DB));
log(`\nlive collections: ${after.collections.length} / manifest ${man.collections.length}`);
for (const id of ['reciter_stats','hifz_mastery','search_history','khatma_plans','likes','listening_stats','reciters']) {
  const x = after.collections.find(y => y.$id === id);
  if (!x) { log(`${id}: MISSING`); continue; }
  log(`${id}: perms=${JSON.stringify(permOf(x))} attrs=[${(x.attributes||[]).map(a=>a.key).join(',')}] idx=[${(x.indexes||[]).map(i=>i.key).join(',')}]`);
}
