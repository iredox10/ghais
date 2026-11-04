// Creates a throwaway account and seeds its cloud rows for every store whose
// restore path has never been exercised against a real backend.
import fs from 'fs';
import { Client, Databases, Account, Query, ID } from 'node-appwrite';
const env = Object.fromEntries(
  fs.readFileSync('.env','utf8').split('\n').filter(l=>l.includes('=')&&!l.trim().startsWith('#'))
    .map(l=>{const i=l.indexOf('=');return [l.slice(0,i).trim(),l.slice(i+1).trim().replace(/^["']|["']$/g,'')];}));
const ENDPOINT = env.APPWRITE_ENDPOINT;
const EMAIL = process.env.TEST_EMAIL, PASS = 'Test!23456789', NAME = 'Sync Probe';
const log=(...a)=>console.log(...a);

// --- 1. create the account over REST (signup needs no key) ---
const uid = 'probe' + Date.now().toString().slice(-9);
const res = await fetch(`${ENDPOINT}/account`, {
  method:'POST',
  headers:{'Content-Type':'application/json','X-Appwrite-Project':env.APPWRITE_PROJECT_ID},
  body: JSON.stringify({ userId: uid, email: EMAIL, password: PASS, name: NAME }),
});
const body = await res.json();
if (!res.ok) throw new Error(`signup failed: ${body.message}`);
let userId = body.$id;
log(`account created: ${EMAIL} -> ${userId}`);

// --- 2. seed cloud docs with the admin key ---
const c = new Client().setEndpoint(env.APPWRITE_ENDPOINT).setProject(env.APPWRITE_PROJECT_ID).setKey(env.APPWRITE_API_KEY);
const db = new Databases(c); const DB = env.APPWRITE_DATABASE_ID;
const put = (col,id,data)=>db.createDocument(DB,col,id,data);

const seeded = {};

// khatma: streak + dates that the old pull fabricated away
seeded.khatma = await put('khatma_plans', ID.unique(), {
  user_id:userId, title:'Probe Khatma', total_days:30, current_surah:18, current_ayah:5,
  percent:0.42, streak:37, total_ayahs_read:2619, start_date_ms:1700000000000, last_progress_ms:1750000000000 });
seeded.khatma_id = seeded.khatma.$id;

// likes: track_json is what preserves textUthmani + durationMs
const track = { reciterSlug:'alafasy', reciterName:'Mishary Rashid Alafasy', surahId:2, surahNameEn:'Al-Baqarah',
  surahNameAr:'البقرة', ayahNo:255, audioUrl:'https://everyayah.com/data/Alafasy_128kbps/002255.mp3',
  textUthmani:'اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ ٱلْحَىُّ ٱلْقَيُّومُ', durationMs:412345 };
seeded.like = await put('likes', ID.unique(), {
  user_id:userId, surah_id:2, ayah_no:255, level:'ayah', reciter_slug:'alafasy',
  track_json: JSON.stringify(track) });
seeded.like_id = seeded.like.$id;

// hifz: per-ayah rows + the sentinel row carrying the scalars
seeded.hifz = [];
for (const [s,a,st] of [[2,255,'MASTERED'],[2,256,'REVIEW_NEEDED'],[18,1,'NEW']]) {
  if (st==='NEW') continue;
  seeded.hifz.push((await put('hifz_mastery', ID.unique(), { user_id:userId, surah_id:s, ayah_no:a, status:st })).$id);
}
seeded.hifz_sentinel = (await put('hifz_mastery', ID.unique(), {
  user_id:userId, surah_id:0, ayah_no:0, status:'NEW',
  daily_goal_count:5, streak_days:12, today_reviewed_count:7, last_review_day:0 })).$id;

// search history
seeded.search = [];
for (const [q,t] of [['baqarah',1750000000000],['mishary',1750000001000],['2:255',1750000002000]]) {
  seeded.search.push((await put('search_history', ID.unique(), { user_id:userId, query:q, ranked_at_ms:t })).$id);
}

// history + stats + surah_plays
seeded.history = (await put('history', ID.unique(), {
  user_id:userId, played_at_ms:1750000500000, position_ms:90000,
  track_json: JSON.stringify({ reciterSlug:'islam-sobhi', reciterName:'Islam Sobhi', surahId:74, surahNameEn:'Al-Muddaththir',
    surahNameAr:'المدثر', ayahNo:0, audioUrl:'https://everyayah.com/data/IslamSobhi_128kbps/074001.mp3',
    textUthmani:'بِٱسْمِ ٱللَّهِ', durationMs:313469 }) })).$id;
seeded.stats = (await put('listening_stats', userId, {
  user_id:userId, total_seconds:98765, days_streak:23, minutes_today:42,
  unique_reciters:7, unique_surahs:11, updated_at:1750000600000,
  surah_plays: JSON.stringify({ "1":"12", "2":"9", "18":"4", "74":"3" }) }));
seeded.follow = (await put('follows', ID.unique(), { user_id:userId, reciter_slug:'islam-sobhi' })).$id;

log('\nseeded doc ids:');
for (const [k,v] of Object.entries(seeded)) log(`  ${k}: ${Array.isArray(v)?v.join(','):v}`);
fs.writeFileSync('/tmp/opencode/seed.json', JSON.stringify({ userId, email:EMAIL, pass:PASS, seeded }, null, 2));
log(`\nTEST_EMAIL=${EMAIL}  TEST_PASSWORD=${PASS}  USER_ID=${userId}`);
