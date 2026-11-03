// Small reusable demo fixture for the dedicated local database; never runs on Render.
import 'dotenv/config';
import assert from 'node:assert/strict';
import pg from 'pg';
const base=`http://127.0.0.1:${process.env.PORT ?? 18080}`;
assert.ok(['127.0.0.1','localhost'].includes(new URL(process.env.DATABASE_URL).hostname), 'Local database required');
const email='local@quranify.test';
const password='QuranifyLocal2026!';
let response=await fetch(base+'/v1/auth/register',{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify({email,password,name:'Local Tester'})});
assert.ok(response.ok || response.status===409,await response.text());
const pool=new pg.Pool({connectionString:process.env.DATABASE_URL});
const seeds={
  reciters:[['alafasy',{slug:'alafasy',name_en:'Mishary Rashid Alafasy',name_ar:'مشاري العفاسي',riwayah:'Hafs',country:'Kuwait 🇰🇼',catalog:'mp3quran',server_url:'https://cdn.mp3quran.net/audio/mishary-alafasy/r1/',available_surahs:'',enabled:true}]],
  broadcasts:[['local-welcome',{title:'Local backend is connected',body:'This message came from PostgreSQL on your computer.',audience:'all',urgency:'normal'}]],
  playlists:[['local-daily',{title:'Local daily listening',description:'A short collection served by your local API.',is_public:true,published_at:Date.now(),owner_id:'editorial'}]],
  playlist_items:[['local-daily-1',{playlist_id:'local-daily',reciter_slug:'alafasy',surah_id:1,position:1}],['local-daily-2',{playlist_id:'local-daily',reciter_slug:'alafasy',surah_id:112,position:2}]],
};
try {
  for (const [table,docs] of Object.entries(seeds)) for (const [id,doc] of docs) await pool.query(`INSERT INTO ${table}(id,owner,doc) VALUES($1,NULL,$2) ON CONFLICT(id) DO UPDATE SET doc=EXCLUDED.doc,updated_at=now()`,[id,doc]);
} finally { await pool.end(); }
console.log('Local fixtures ready. Sign in as local@quranify.test / QuranifyLocal2026!');
