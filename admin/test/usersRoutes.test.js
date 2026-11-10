import test from 'node:test';
import assert from 'node:assert/strict';
import express from 'express';
import { registerUsersRoutes } from '../server/usersRoutes.js';

async function withServer(ctx, run) {
  const app = express(); registerUsersRoutes(app, ctx);
  const server = await new Promise(resolve => { const s=app.listen(0, '127.0.0.1', () => resolve(s)); });
  try { return await run(`http://127.0.0.1:${server.address().port}`); }
  finally { await new Promise(resolve => server.close(resolve)); }
}
const base = { databaseId: 'ghais', historyTs: h => h.played_at_ms || 0, parseTrack: () => ({}), trackSurahId: () => 1, trackSurahName: () => 'Surah', trackReciterName: () => 'Reciter', listHistoryDocs: async () => ({documents: []}) };

test('listener list supports bounded pagination and searches without fabricated activity', async () => {
  const calls = [];
  const pool = { query: async (sql, args) => {
    calls.push({sql,args});
    if (sql.startsWith('SELECT COUNT')) return {rows:[{total:101}]};
    return {rows:[{id:'u1',name:'',email:'listener@example.test',provider:'email',created_at:new Date(),stats:{total_seconds:120},schedules_count:2,playlists_count:3,last_active:null}]};
  }};
  await withServer({...base,pool}, async url => {
    const response = await fetch(`${url}/api/users?offset=50&limit=50&search=%25`);
    assert.equal(response.status, 200);
    const data = await response.json();
    assert.equal(data.total,101); assert.equal(data.users[0].name,'listener');
    assert.equal(data.users[0].totalMinutes,2); assert.equal(data.users[0].lastActive,'');
    assert.equal(calls.find(c=>c.args.length===3).args[0],'%\\%%');
    assert.equal((await fetch(`${url}/api/users?limit=101`)).status,400);
  });
});

test('profile loads every schedule page and returns accurate playlist track counts', async () => {
  const queries=[];
  const databases={listDocuments:async (_db, collection, q) => {
    queries.push({collection,q});
    if (collection==='listening_stats') return {documents:[{total_seconds:120,days_streak:3}]};
    if (collection==='schedules') return {documents:[{$id:'s',schedule_id:'s',enabled:true,schedule_json:'{"hour":9,"minute":30}'},{$id:'gone',deleted:true}]};
    return {documents:[{$id:'p',title:'Routine'}]};
  }};
  const pool={query:async()=>({rows:[{id:'p',count:4}]})};
  const userStore={get:async()=>({name:'Ada',email:'ada@example.test',$createdAt:'2026-01-01T00:00:00Z'})};
  await withServer({...base,pool,databases,userStore}, async url => {
    const response=await fetch(`${url}/api/users/u1`); assert.equal(response.status,200);
    const result=await response.json();
    assert.equal(result.schedules.length,1); assert.equal(result.schedules[0].time,'9:30 AM');
    assert.equal(result.playlists[0].tracks,4); assert.equal(result.user.lastActive,'');
    assert.equal(queries.find(q=>q.collection==='schedules').q.some(q=>{ const parsed=JSON.parse(q); return parsed.method==='limit' && parsed.values[0]===1000; }),true);
  });
});

test('profile reports database errors rather than inventing empty data', async () => {
  const userStore={get:async()=>({name:'Ada'})};
  const databases={listDocuments:async()=>{throw new Error('db unavailable');}};
  const pool={query:async()=>({rows:[]})};
  await withServer({...base,pool,databases,userStore}, async url => {
    assert.equal((await fetch(`${url}/api/users/u1`)).status,500);
  });
});
