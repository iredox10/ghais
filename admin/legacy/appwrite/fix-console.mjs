import fs from 'fs';
import { Client, Databases, Query } from 'node-appwrite';

const env = Object.fromEntries(
  fs.readFileSync('/home/iredox/Desktop/space/quranify-admin/.env','utf8')
    .split('\n').filter(l=>l.includes('=') && !l.trim().startsWith('#'))
    .map(l=>{const i=l.indexOf('=');return [l.slice(0,i).trim(), l.slice(i+1).trim().replace(/^["']|["']$/g,'')];})
);
const c = new Client()
  .setEndpoint(env.APPWRITE_ENDPOINT).setProject(env.APPWRITE_PROJECT_ID).setKey(env.APPWRITE_API_KEY);
const db = new Databases(c);
const DB = env.APPWRITE_DATABASE_ID;
const log = (...a)=>console.log(...a);
async function retry(label, fn, tries=5){
  let last;
  for(let i=1;i<=tries;i++){
    try { return await fn(); }
    catch(e){ last=e; log(`   [${label}] attempt ${i}/${tries} failed: ${e?.message||e}`); await new Promise(r=>setTimeout(r,2000*i)); }
  }
  throw last;
}

const WANT_PERMS = ['read("any")'];
const TARGET = 'reciter_stats';

const cols = await retry('listCollections', () => db.listCollections(DB));
const get = id => cols.collections.find(x=>x.$id===id);

async function waitAttrs(collectionId, keys, tries=20){
  for(let i=0;i<tries;i++){
    const a = await db.listAttributes(DB, collectionId);
    const byKey = Object.fromEntries(a.attributes.map(x=>[x.key,x]));
    const missing = keys.filter(k=>!byKey[k] || byKey[k].status!=='available');
    if(!missing.length) return true;
    if(i===0) log(`   ${collectionId}: waiting on ${missing.join(', ')} (attempt ${i+1})`);
    await new Promise(r=>setTimeout(r,1500));
  }
  return false;
}

// ---- 1. tighten reciter_stats ----
const rs = get(TARGET);
if(!rs){ log(`!! ${TARGET} does not exist`); }
else {
  const cur = rs.permissions || [];
  const want = ['read','create','update','delete'].sort().join(',');
  const have = cur.map(p=>p.split('(')[0]).sort().join(',');
  log(`reciter_stats permissions BEFORE: ${JSON.stringify(cur)}`);
  if(have!==want || cur.length!==WANT_PERMS.length){
    await retry('updateCollection', () => db.updateCollection(DB, TARGET, rs.name, WANT_PERMS, rs.documentSecurity ?? true));
    const after = (await retry('getCollection', () => db.getCollection(DB,TARGET))).permissions;
    log(`reciter_stats permissions AFTER : ${JSON.stringify(after)}`);
  } else {
    log(`reciter_stats already correct: ${JSON.stringify(cur)}`);
  }
}

// ---- 2. create the three missing indexes ----
const WANT = [
  { col:'hifz_mastery', key:'user_surah_ayah_idx', type:'key',  attrs:['user_id','surah_id','ayah_no'] },
  { col:'search_history', key:'user_ranked_idx',      type:'key',  attrs:['user_id','ranked_at_ms'] },
  { col:'search_history', key:'user_query_unique',   type:'unique', attrs:['user_id','query'] },
];
for(const w of WANT){
  const col = get(w.col);
  if(!col){ log(`!! ${w.col} not provisioned yet — run POST /api/settings/provision first`); continue; }
  const idx = (col.indexes||[]).find(i=>i.key===w.key);
  if(idx){ log(`${w.col}.${w.key}: already present (${idx.type})`); continue; }
  const attrs = [...new Set([...w.attrs, ...(w.col==='hifz_mastery'?['daily_goal_count','streak_days','today_reviewed_count','last_review_day']:[])])];
  const ready = await waitAttrs(w.col, attrs);
  if(!ready){ log(`!! ${w.col}: attributes never became available, skipping ${w.key}`); continue; }
  await retry('createIndex:'+w.key, () => db.createIndex(DB, w.col, w.key, w.type, w.attrs));
  log(`${w.col}.${w.key}: CREATED (${w.type} on ${w.attrs.join(',')})`);
}

// ---- 3. report ----
log('\n--- final state ---');
const after = await retry('listCollections2', () => db.listCollections(DB));
for(const id of [TARGET,'hifz_mastery','search_history','khatma_plans','likes','listening_stats']){
  const x = after.collections.find(y=>y.$id===id);
  if(!x){ log(`${id}: MISSING`); continue; }
  log(`${id}: perms=${JSON.stringify(x.permissions)} indexes=[${(x.indexes||[]).map(i=>i.key+':'+i.type).join(', ')}]`);
}
log('total collections: '+after.collections.length);
