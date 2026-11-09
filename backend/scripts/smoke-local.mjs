// Real HTTP/PostgreSQL checks. Local targets only; test accounts are removed afterward.
import 'dotenv/config';
import assert from 'node:assert/strict';
import { randomBytes } from 'node:crypto';
import pg from 'pg';
const base = process.env.LOCAL_API_URL ?? `http://127.0.0.1:${process.env.PORT ?? 18080}`;
for (const url of [base, process.env.DATABASE_URL]) assert.ok(['localhost','127.0.0.1','[::1]'].includes(new URL(url).hostname), 'Smoke tests require local API and DB');
const pool = new pg.Pool({connectionString:process.env.DATABASE_URL});
const tag = `smoke-${randomBytes(5).toString('hex')}`;
const users = [];
let checks = 0;
const baseline = (await pool.query(`SELECT
  (SELECT count(DISTINCT owner) FROM likes WHERE doc->>'reciter_slug'='alafasy') AS likes,
  (SELECT count(DISTINCT owner) FROM follows WHERE doc->>'reciter_slug'='alafasy') AS follows`)).rows[0];
async function req(method,path,body,token,expected=200) {
  const r = await fetch(base+path,{method,headers:{...(body?{'content-type':'application/json'}:{}),...(token?{authorization:`Bearer ${token}`}:{})},body:body?JSON.stringify(body):undefined,signal:AbortSignal.timeout(20000)});
  const text = await r.text(); assert.equal(r.status,expected,`${method} ${path}: ${text}`); checks++; return text?JSON.parse(text):null;
}
const doc = (c,id='') => `/v1/databases/ghais/collections/${c}/documents${id?'/'+id:''}`;
try {
  await req('GET','/healthz');
  for (const suffix of ['a','b']) users.push(await req('POST','/v1/auth/register',{email:`${tag}-${suffix}@example.com`,password:'SmokePassword2026!',name:`Smoke ${suffix}`}));
  const [a,b]=users;
  await req('POST','/v1/auth/register',{email:a.email,password:'SmokePassword2026!',name:'Duplicate'},null,409);
  await req('POST','/v1/auth/login',{email:a.email,password:'wrong-password'},null,401);
  await req('POST','/v1/auth/login',{email:a.email,password:'SmokePassword2026!'});
  assert.equal((await req('GET','/v1/auth/me',null,a.accessToken)).userId,a.userId);
  await req('GET',doc('likes'),null,null,401);
  const like=await req('POST',doc('likes'),{documentId:tag,data:{user_id:b.userId,surah_id:1,ayah_no:0,reciter_slug:'alafasy'}},a.accessToken,201);
  assert.equal(like.user_id,a.userId);
  await req('GET',doc('likes',tag),null,b.accessToken,404);
  await req('PATCH',doc('likes',tag),{data:{level:'like'}},b.accessToken,403);
  assert.equal((await req('GET',doc('likes'),null,b.accessToken)).total,0);
  await req('POST',doc('likes'),{documentId:tag,data:{}},a.accessToken,409);
  await req('PATCH',doc('likes',tag),{data:{level:'love'}},a.accessToken);
  for (const c of ['playback_state','follows','listening_stats','schedules','history','routine_backups','user_prefs','khatma_plans','hifz_mastery','telemetry']) {
    await req('POST',doc(c),{documentId:tag,data:{user_id:a.userId,reciter_slug:'alafasy'}},a.accessToken,201);
    await req('GET',doc(c,tag),null,a.accessToken);
  }
  for (const rank of [9,10]) await req('POST',doc('search_history'),{documentId:`${tag}-${rank}`,data:{query:`query ${rank}`,ranked_at_ms:rank}},a.accessToken,201);
  const qs=new URLSearchParams();
  for (const q of [{method:'orderDesc',attribute:'ranked_at_ms',values:[]},{method:'limit',values:[1]},{method:'offset',values:[0]}]) qs.append('queries[]',JSON.stringify(q));
  assert.equal((await req('GET',doc('search_history')+'?'+qs,null,a.accessToken)).documents[0].ranked_at_ms,10);
  await req('POST',doc('playlists'),{documentId:tag,data:{owner_id:b.userId,title:'Test routine',is_public:true}},a.accessToken,201);
  await req('GET',doc('playlists',tag));
  const item={documentId:tag,data:{playlist_id:tag,position:1,surah_id:1,reciter_slug:'alafasy'}};
  await req('POST',doc('playlist_items'),item,b.accessToken,403);
  await req('POST',doc('playlist_items'),item,a.accessToken,201);
  await req('GET',doc('playlist_items',tag));
  const privateId=`${tag}-private`;
  await req('POST',doc('playlists'),{documentId:privateId,data:{title:'Private',is_public:false}},a.accessToken,201);
  await req('POST',doc('playlist_items'),{documentId:privateId,data:{playlist_id:privateId,position:0}},a.accessToken,201);
  for (const collection of ['playlists','playlist_items']) {
    await req('GET',doc(collection,privateId),null,null,404);
    await req('GET',doc(collection,privateId),null,b.accessToken,404);
    await req('GET',doc(collection,privateId),null,a.accessToken);
    const filter=new URLSearchParams();
    filter.append('queries[]',JSON.stringify({method:'equal',attribute:'$id',values:[privateId]}));
    assert.equal((await req('GET',doc(collection)+'?'+filter)).total,0);
    assert.equal((await req('GET',doc(collection)+'?'+filter,null,b.accessToken)).total,0);
    assert.equal((await req('GET',doc(collection)+'?'+filter,null,a.accessToken)).total,1);
  }
  await req('PATCH',doc('playlists',privateId),{data:{is_public:true}},a.accessToken);
  await req('GET',doc('playlist_items',privateId));
  await req('PATCH',doc('playlists',privateId),{data:{is_public:false}},a.accessToken);
  await req('GET',doc('playlist_items',privateId),null,null,404);
  await req('POST',doc('devotion'),{documentId:tag,data:{userId:b.userId}},a.accessToken,403);
  await req('POST',doc('devotion'),{documentId:tag,data:{userId:a.userId,streakCount:1}},a.accessToken,201);
  await req('POST',doc('leagues'),{documentId:tag,data:{memberIds:[a.userId,b.userId],leagueId:tag}},a.accessToken,201);
  await req('PATCH',doc('leagues',tag),{data:{tier:'bronze'}},b.accessToken);
  await req('POST',doc('friendships'),{documentId:tag,data:{aId:a.userId,bId:b.userId,pairKey:tag}},a.accessToken,201);
  await req('POST',doc('achievements'),{documentId:tag,data:{userId:a.userId,achievementId:'first',unlockedMs:Date.now()}},a.accessToken,201);
  for (const c of ['charts','reciters','reciter_stats','broadcasts','global_charts']) {
    await req('GET',doc(c)); await req('POST',doc(c),{documentId:tag,data:{}},a.accessToken,403);
  }
  for (let i=0;i<2;i++) await req('POST',doc('play_events'),{documentId:`${tag}-${i}`,data:{ownerId:a.userId,reciterSlug:'alafasy',surahId:1,ms:35000}},a.accessToken,201);
  await req('POST','/v1/jobs/charts-rollup',{},a.accessToken,403);
  const charts=await req('POST','/v1/jobs/charts-rollup',{},process.env.ADMIN_TOKEN);
  assert.ok(charts.global.topQaris7d.includes('alafasy'));
  const counts=await req('GET',doc('reciter_stats','alafasy'));
  assert.equal(counts.followers_count,Number(baseline.follows)+1); assert.equal(counts.likes_count,Number(baseline.likes)+1);
  const rotations=await Promise.all([0,1].map(()=>fetch(base+'/v1/auth/refresh',{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify({refreshToken:a.refreshToken})})));
  assert.deepEqual(rotations.map(r=>r.status).sort(),[200,401]); checks+=2;
  const rotated=await rotations.find(r=>r.status===200).json();
  await req('GET','/v1/auth/me',null,rotated.accessToken);
  await req('DELETE','/v1/auth/refresh',null,rotated.accessToken,204);
  await req('POST','/v1/auth/refresh',{refreshToken:rotated.refreshToken},null,401);
  await req('DELETE',doc('likes',tag),null,a.accessToken,204);
  console.log(`PASS: ${checks} real HTTP/PostgreSQL checks; all 24 document collections covered.`);
} finally {
  for (const user of users) {
    const tables=await pool.query("SELECT tablename FROM pg_tables WHERE schemaname='public' AND tablename NOT IN ('users','refresh_tokens')");
    for (const {tablename} of tables.rows) { assert.match(tablename,/^[a-z_]+$/); await pool.query(`DELETE FROM ${tablename} WHERE owner=$1`,[user.userId]); }
    await pool.query('DELETE FROM users WHERE id=$1',[user.userId]);
  }
  await pool.end();
  await fetch(base+'/v1/jobs/charts-rollup',{method:'POST',headers:{authorization:`Bearer ${process.env.ADMIN_TOKEN}`}});
}
