import test from 'node:test';
import assert from 'node:assert/strict';
import express from 'express';
import { registerActivityRoutes } from '../server/activityRoutes.js';

async function withServer(pool, fn) {
  const app=express(); registerActivityRoutes(app,pool);
  const server=await new Promise(resolve=>{const s=app.listen(0,'127.0.0.1',()=>resolve(s));});
  try { await fn(`http://127.0.0.1:${server.address().port}`); } finally { await new Promise(resolve=>server.close(resolve)); }
}

test('activity uses real user and saved record fields without fabricated schedule content', async () => {
  let sql;
  const pool={query:async q=>{sql=q;return {rows:[
    {type:'schedule',id:'s1',name:'Ada',email:'a@example.test',event_at:new Date('2026-01-01T00:00:00Z'),doc:{schedule_json:'{"reciterSlug":"mishary"}'}},
    {type:'listen',id:'h1',name:'',email:'a@example.test',event_at:new Date('2025-12-31T00:00:00Z'),doc:{track_json:'{"surahNameEn":"Al-Fatiha"}'}},
    {type:'follow',id:'f1',name:'Ada',event_at:new Date('2025-12-30T00:00:00Z'),doc:{reciter_slug:'alafasy'}},
    {type:'playlist',id:'p1',name:'Ada',event_at:new Date('2025-12-29T00:00:00Z'),doc:{title:'Evening'}},
  ]};}};
  await withServer(pool,async url=>{const response=await fetch(`${url}/api/activity`);assert.equal(response.status,200);const {activities}=await response.json();
    assert.equal(activities[0].reciter,'mishary');assert.equal(activities[0].surah,'—');assert.equal(activities[0].user,'Ada');
    assert.equal(activities[1].user,'a');assert.equal(activities[1].surah,'Al-Fatiha');assert.equal(activities[1].time,'2025-12-31T00:00:00.000Z');
    assert.equal(activities[2].reciter,'alafasy');assert.equal(activities[2].action,'Followed a reciter');
    assert.equal(activities[3].action,'Created playlist: Evening');assert.equal(activities[3].reciter,'—');
    assert.match(sql,/LEFT JOIN users/);assert.match(sql,/ORDER BY e.event_at DESC/);assert.match(sql,/LIMIT 50/);
    assert.match(sql,/FROM follows f/);assert.match(sql,/EXISTS \(SELECT 1 FROM users listener WHERE listener.id = p.owner\)/);
  });
});
test('activity reports storage failure', async()=>{await withServer({query:async()=>{throw new Error('offline');}},async url=>assert.equal((await fetch(`${url}/api/activity`)).status,503));});
