// Bulk upload EXACT qari images to Appwrite backend.
// Exact set = unique-hash non-placeholder files + verified true owners of
// duplicate faces (score == 1.0) + alias true owners (saad-al-ghamdi,
// yasser-al-dossari). Skips placeholders and wrong-face copies.
// Aliases: mishary-rashid-alafasy->alafasy, maher-al-muaiqly->muaiqly,
// saad-al-ghamdi->ghamdi, saud-al-shuraim->shuraym, yasser-al-dossari->dossari.
// Same image applied to all catalog variants (mp3quran + everyayah).
// DB via node-appwrite SDK (works); Storage via raw REST (SDK hits
// fetch-failed in this env, raw REST verified working).
import dotenv from 'dotenv';
import fs from 'fs';
import path from 'path';
import crypto from 'crypto';
import { Client, Databases, Query } from 'node-appwrite';

dotenv.config();

const EP = (process.env.APPWRITE_ENDPOINT || 'https://fra.cloud.appwrite.io/v1').replace(/\/$/, '');
const PJ = process.env.APPWRITE_PROJECT_ID;
const DB = process.env.APPWRITE_DATABASE_ID;
const KEY = process.env.APPWRITE_API_KEY;
const BUCKET = process.env.APPWRITE_STORAGE_BUCKET_ID || 'covers';
const DRY = process.argv.includes('--dry-run');

const IMGDIR = path.join(process.cwd(), 'reciter_images');
const PLACEHOLDER = '6ab223e499eaf60b797e658f5e0b4bdf';
const ALIAS = {
  'mishary-rashid-alafasy': 'alafasy',
  'maher-al-muaiqly': 'muaiqly',
  'saad-al-ghamdi': 'ghamdi',
  'saud-al-shuraim': 'shuraym',
  'yasser-al-dossari': 'dossari',
};
const SKIP_COPIES = new Set(['saad-al-muqrin', 'yasser-al-mazroyee']);
const KEEP_TRUE_OWNERS = new Set([
  'abdulaziz-al-ahmad',
  'ibrahim-al-akhdar',
  'saleh-al-habdan',
  'abdulmohsen-al-askar',
  'abdulrahman-al-majed',
  'abdullah-ghailan',
  'haitham-al-jadaani',
  'mahmoud-khalil-al-hussary',
  'salman-al-otaibi',
]);

const H = { 'X-Appwrite-Project': PJ, 'X-Appwrite-Key': KEY };

async function storageUp(buf, filename) {
  const fileId = `rec-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`;
  const form = new FormData();
  form.append('fileId', fileId);
  form.append('file', new Blob([buf], { type: 'image/jpeg' }), filename);
  form.append('permissions[]', 'read("any")');
  const res = await fetch(`${EP}/storage/buckets/${BUCKET}/files`, {
    method: 'POST', headers: H, body: form,
  });
  const txt = await res.text();
  if (!res.ok) throw new Error(`upload ${filename} -> ${res.status}: ${txt.slice(0, 200)}`);
  return JSON.parse(txt);
}

async function storageDel(fileId) {
  try {
    await fetch(`${EP}/storage/buckets/${BUCKET}/files/${fileId}`, {
      method: 'DELETE', headers: H,
    });
  } catch (_) {}
}

// Build exact file set
const files = fs.readdirSync(IMGDIR).filter(f => f.endsWith('.jpg'));
const f2h = new Map(files.map(f => [f, crypto.createHash('md5').update(fs.readFileSync(path.join(IMGDIR, f))).digest('hex')]));
const hCount = {};
for (const h of f2h.values()) hCount[h] = (hCount[h] || 0) + 1;

const exactFiles = [];
for (const f of files.sort()) {
  const stem = f.replace(/\.jpg$/, '');
  const h = f2h.get(f);
  if (h === PLACEHOLDER) continue;
  if (SKIP_COPIES.has(stem)) { console.log(`SKIP copy: ${f}`); continue; }
  if (hCount[h] > 1 && !KEEP_TRUE_OWNERS.has(stem)) { console.log(`SKIP duplicate face (x${hCount[h]}): ${f}`); continue; }
  exactFiles.push(f);
}
for (const s of ['saad-al-ghamdi', 'yasser-al-dossari']) {
  const f = `${s}.jpg`;
  if (!exactFiles.includes(f) && files.includes(f)) exactFiles.push(f);
}
exactFiles.sort();
console.log(`\nExact upload set: ${exactFiles.length} files (skipped ${files.length - exactFiles.length})`);

if (DRY) {
  for (const f of exactFiles) console.log(`  ${f} -> ${ALIAS[f.replace(/\.jpg$/, '')] || f.replace(/\.jpg$/, '')}`);
  process.exit(0);
}

const client = new Client().setEndpoint(EP).setProject(PJ).setKey(KEY);
const db = new Databases(client);

const docs = [];
let offset = 0;
for (;;) {
  const r = await db.listDocuments(DB, 'reciters', [Query.limit(100), Query.offset(offset)]);
  docs.push(...r.documents);
  if (r.documents.length < 100) break;
  offset += 100;
}
console.log(`Fetched ${docs.length} reciter docs`);
const bySlug = new Map();
for (const d of docs) {
  if (!bySlug.has(d.slug)) bySlug.set(d.slug, []);
  bySlug.get(d.slug).push(d);
}

let ok = 0;
const missing = [], failed = [];
for (const f of exactFiles) {
  const stem = f.replace(/\.jpg$/, '');
  const target = ALIAS[stem] || stem;
  const targets = bySlug.get(target) || [];
  if (targets.length === 0) { missing.push(`${f} -> ${target} (no doc)`); continue; }
  try {
    const buf = fs.readFileSync(path.join(IMGDIR, f));
    const up = await storageUp(buf, f);
    const imageUrl = `${EP}/storage/buckets/${BUCKET}/files/${up.$id}/view?project=${encodeURIComponent(PJ)}`;
    for (const doc of targets) {
      const oldFile = doc.image_file_id;
      await db.updateDocument(DB, 'reciters', doc.$id, {
        image_file_id: up.$id,
        image_url: imageUrl,
      });
      if (oldFile && oldFile !== up.$id) await storageDel(oldFile);
    }
    ok++;
    console.log(`OK (${targets.length} doc${targets.length > 1 ? 's' : ''}): ${f} -> ${target} [${up.$id}]`);
  } catch (e) {
    failed.push(`${f}: ${e.message}`);
    console.log(`FAIL: ${f}: ${e.message}`);
  }
}

console.log(`\nDone. Uploaded ${ok}/${exactFiles.length} slugs. Missing docs: ${missing.length}. Failed: ${failed.length}.`);
if (missing.length) console.log('MISSING:\n' + missing.join('\n'));
if (failed.length) console.log('FAILED:\n' + failed.join('\n'));
