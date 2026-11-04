import assert from 'node:assert/strict';
import { test, afterEach } from 'node:test';
import { readFileSync } from 'node:fs';
import Fastify from 'fastify';
import { pool } from '../src/db.js';
import { signAccessToken, rotateRefreshToken } from '../src/tokens.js';
import compatRoutes from '../src/routes/compat.js';
import { CATALOG } from '../src/compat/catalog.js';
import { buildList } from '../src/compat/query.js';
import { aggregateCharts, type ChartEvent } from '../src/charts.js';
import { portraitRoutes } from '../src/portraits.js';
import { toAppwriteDoc } from '../src/compat/shape.js';

const originalQuery = pool.query;
afterEach(() => { pool.query = originalQuery; });
const row = (id: string, owner: string | null, doc: Record<string, unknown>) => ({ id, owner, doc, created_at: new Date(), updated_at: new Date() });
const auth = async () => ({ authorization: `Bearer ${await signAccessToken({userId:'alice',email:'a@example.com',name:'Alice'})}` });
async function app() { const server = Fastify(); await server.register(compatRoutes, { prefix:'/v1/databases' }); return server; }
const path = (collection: string, id = '') => `/v1/databases/ghais/collections/${collection}/documents${id ? '/' + id : ''}`;

// Database I/O is substituted here so transport/auth contracts run without a live service.
function database(handler: (sql: string, params: any[]) => any[]) {
  pool.query = (async (sql: string, params: any[] = []) => ({ rows: handler(sql, params), rowCount:1 })) as any;
}

test('reciter catalog supplies available portraits and preserves explicit images', async () => {
  const slug = 'maher-al-muaiqly';
  const fallback = toAppwriteDoc(row('maher', null, { slug }), 'reciters', 'ghais', null);
  assert.match(String(fallback.image_url), /\/v1\/reciter-images\/maher-al-muaiqly\.jpg$/);
  assert.equal(toAppwriteDoc(row('maher-alias', null, { slug: 'muaiqly' }), 'reciters', 'ghais', null).image_url, fallback.image_url);
  const explicit = toAppwriteDoc(row('maher', null, { slug, image_url: 'https://example.com/portrait.jpg' }), 'reciters', 'ghais', null);
  assert.equal(explicit.image_url, 'https://example.com/portrait.jpg');
  const invalid = toAppwriteDoc(row('maher', null, { slug, image_url: 'not-a-url' }), 'reciters', 'ghais', null);
  assert.equal(invalid.image_url, fallback.image_url);
  assert.equal(toAppwriteDoc(row('maher', null, { slug }), 'reciters', 'ghais', new Set(['slug'])).image_url, undefined);
  assert.equal(toAppwriteDoc(row('unknown', null, { slug: 'no-such-reciter' }), 'reciters', 'ghais', null).image_url, undefined);

  const server = Fastify();
  await server.register(portraitRoutes, { prefix: '/v1/reciter-images' });
  try {
    const image = await server.inject({ url: '/v1/reciter-images/maher-al-muaiqly.jpg' });
    assert.equal(image.statusCode, 200);
    assert.match(String(image.headers['content-type']), /^image\/jpeg/);
    assert.deepEqual([...image.rawPayload.subarray(0, 3)], [0xff, 0xd8, 0xff]);
    assert.equal((await server.inject({ url: '/v1/reciter-images/missing.jpg' })).statusCode, 404);
    assert.equal((await server.inject({ url: '/v1/reciter-images/%2e%2e%2fconfig.ts' })).statusCode, 404);
  } finally { await server.close(); }
});

test('every collection is provisioned and covered by export/import', () => {
  const schema = readFileSync(new URL('../src/db/schema.sql', import.meta.url), 'utf8');
  const exporter = readFileSync(new URL('../scripts/export-appwrite.mjs', import.meta.url), 'utf8');
  const importer = readFileSync(new URL('../scripts/seed.mjs', import.meta.url), 'utf8');
  for (const name of Object.keys(CATALOG)) {
    assert.ok(schema.includes(`'${name}'`), `${name}: missing SQL table`);
    assert.ok(exporter.includes(`'${name}'`), `${name}: missing export`);
    assert.ok(importer.includes(`'${name}'`), `${name}: missing import`);
  }
});

test('queries preserve numeric ordering and reject invalid pagination/identifiers', () => {
  const parsed = buildList(CATALOG.search_history, [JSON.stringify({method:'orderDesc',attribute:'ranked_at_ms',values:[]}), JSON.stringify({method:'limit',values:[100]}), JSON.stringify({method:'offset',values:[100]})], 'owner = $1', 'alice');
  assert.match(parsed.orderBy, /::numeric DESC/);
  assert.equal(parsed.limit,100); assert.equal(parsed.offset,100); assert.deepEqual(parsed.params,['alice']);
  for (const [method, values] of [['limit',[-1]],['offset',[0.5]],['limit',['NaN']]]) {
    assert.throws(() => buildList(CATALOG.likes, JSON.stringify({method,values}), null, null));
  }
  assert.throws(() => buildList(CATALOG.likes, JSON.stringify({method:'equal',attribute:"x'); DROP TABLE users; --",values:['a']}), null, null));
});

test('personal writes force authenticated owner and reads cannot cross accounts', async () => {
  const server = await app();
  database((sql, params) => {
    if (sql.startsWith('INSERT')) {
      assert.equal(params[1],'alice'); assert.equal(params[2].user_id,'alice');
      return [row(params[0],params[1],params[2])];
    }
    return [row('fav-bob','bob',{user_id:'bob'})];
  });
  try {
    const headers = await auth();
    const created = await server.inject({method:'POST',url:path('likes'),headers,payload:{documentId:'fav-alice',data:{user_id:'bob',surah_id:1}}});
    assert.equal(created.statusCode,201); assert.equal(created.json().user_id,'alice');
    assert.equal((await server.inject({url:path('likes','fav-bob'),headers})).statusCode,404);
    assert.equal((await server.inject({url:path('likes')})).statusCode,401);
    assert.equal((await server.inject({method:'PATCH',url:path('likes','fav-bob'),headers,payload:{data:{surah_id:2}}})).statusCode,403);
  } finally { await server.close(); }
});

test('catalog documents allow guest reads; public writes and foreign playlist items are rejected', async () => {
  const server = await app();
  database(() => [row('editorial','bob',{title:'Daily'})]);
  try {
    assert.equal((await server.inject({url:path('playlists','editorial')})).statusCode,200);
    assert.equal((await server.inject({method:'POST',url:path('charts'),headers:await auth(),payload:{data:{}}})).statusCode,403);
    assert.equal((await server.inject({method:'POST',url:path('playlist_items'),headers:await auth(),payload:{data:{playlist_id:'editorial'}}})).statusCode,403);
    assert.equal((await server.inject({url:path('constructor')})).statusCode,404);
  } finally { await server.close(); }
});

test('social creation requires real user membership; league memberIds authorize writes', async () => {
  const server = await app();
  database((_sql, params) => [row(params[0],params[1],params[2])]);
  try {
    const headers = await auth();
    assert.equal((await server.inject({method:'POST',url:path('devotion'),headers,payload:{data:{userId:'bob'}}})).statusCode,403);
    assert.equal((await server.inject({method:'POST',url:path('leagues'),headers,payload:{data:{leagueId:'l1',memberIds:['alice']}}})).statusCode,201);
  } finally { await server.close(); }
});

test('refresh tokens are consumed atomically in one database statement', async () => {
  let calls = 0;
  database((sql) => { calls++; assert.match(sql,/DELETE .* RETURNING/); return [{user_id:'alice',expires_at:new Date(Date.now()+10000)}]; });
  assert.deepEqual(await rotateRefreshToken('secret'),{userId:'alice'}); assert.equal(calls,1);
});

test('charts dedupe per owner/surah/UTC day, yesterday is bounded, trending uses velocity', () => {
  const now = Date.parse('2026-10-27T12:00:00Z'); const day = 86400000;
  const event = (id: string, slug: string, days: number, owner = id): ChartEvent => ({ id, owner, doc:{reciterSlug:slug,surahId:1}, created_at:new Date(now-days*day) });
  const events = [event('a','steady',1,'same'),event('duplicate','steady',1,'same'),event('b','surging',0),event('c','steady',2)];
  for (let i=0;i<10;i++) events.push(event(`old${i}`,'steady',10+i));
  events.push(event('future','invalid',-1), event('too-old','invalid',31));
  const charts = aggregateCharts(events,now);
  assert.deepEqual(charts.topQarisYesterday,['steady']);
  assert.deepEqual(charts.topQaris7d,['steady','surging']);
  assert.equal(charts.trending[0],'surging');
  assert.ok(!charts.topQaris30d.includes('invalid'));
});

test('repeated queries[] parameters reach SQL ordering and pagination', async () => {
  const server = await app();
  let listed = false;
  database((sql, params) => {
    if (sql.startsWith('SELECT count')) return [{n:'0'}];
    assert.match(sql, /ranked_at_ms.*::numeric DESC/);
    assert.deepEqual(params.slice(-2),[1,2]);
    listed = true;
    return [];
  });
  const qs = new URLSearchParams();
  for (const q of [{method:'orderDesc',attribute:'ranked_at_ms',values:[]},{method:'limit',values:[1]},{method:'offset',values:[2]}]) qs.append('queries[]',JSON.stringify(q));
  try {
    assert.equal((await server.inject({url:path('search_history')+'?'+qs,headers:await auth()})).statusCode,200);
    assert.ok(listed);
  } finally { await server.close(); }
});
