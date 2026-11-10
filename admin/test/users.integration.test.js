import test from 'node:test';
import assert from 'node:assert/strict';
import express from 'express';
import { randomBytes } from 'node:crypto';
import { createDatabase } from '../server/database.js';
import { registerUsersRoutes } from '../server/usersRoutes.js';
import { registerActivityRoutes } from '../server/activityRoutes.js';

const url=process.env.TEST_DATABASE_URL;
test('listener pages, scoped counts and activity work against PostgreSQL', {skip:!url}, async () => {
  const parsed = new URL(url);
  assert.ok(['127.0.0.1','localhost','[::1]'].includes(parsed.hostname), 'integration DB must be local');
  const {pool,database,users}=createDatabase(url);
  const tag=`test${randomBytes(5).toString('hex')}`;
  const id=`${tag}1`;
  const app=express();
  registerUsersRoutes(app,{pool,databases:database,databaseId:'ghais',userStore:users,listHistoryDocs:async()=>({documents:[]}),historyTs:()=>0,parseTrack:()=>({}),trackSurahId:()=>1,trackSurahName:()=>'',trackReciterName:()=>''});
  registerActivityRoutes(app,pool);
  const server=await new Promise(resolve=>{const s=app.listen(0,'127.0.0.1',()=>resolve(s));});
  try {
    await pool.query("INSERT INTO users (id,email,name) SELECT $1 || n, $1 || n || '@example.test', 'Listener ' || n FROM generate_series(1,101) AS n",[tag]);
    await pool.query("INSERT INTO listening_stats (id,owner,doc) VALUES($1,$1,$2)",[id,JSON.stringify({user_id:id,total_seconds:600})]);
    await pool.query("INSERT INTO history (id,owner,doc) VALUES($1,$2,$3)",[`${tag}h`,id,JSON.stringify({user_id:id,played_at_ms:1704067200000})]);
    await pool.query("INSERT INTO schedules (id,owner,doc) VALUES($1,$2,$3),($4,$2,$5)",[`${tag}s1`,id,JSON.stringify({user_id:id,schedule_id:'one'}),`${tag}s2`,JSON.stringify({user_id:id,schedule_id:'two',deleted:true})]);
    await pool.query("INSERT INTO playlists (id,owner,doc) VALUES($1,$2,$3)",[`${tag}p`,id,JSON.stringify({owner_id:id,title:'Routine'})]);
    await pool.query("INSERT INTO playlists (id,owner,doc) VALUES($1,'editorial',$2)",[`${tag}editorial`,JSON.stringify({owner_id:'editorial',title:'Editorial'})]);
    await pool.query("INSERT INTO follows (id,owner,doc) VALUES($1,$2,$3)",[`${tag}f`,id,JSON.stringify({user_id:id,reciter_slug:'alafasy'})]);
    await pool.query("INSERT INTO playlist_items (id,owner,doc) VALUES($1,$2,$3)",[`${tag}i`,id,JSON.stringify({playlist_id:`${tag}p`})]);
    const base=`http://127.0.0.1:${server.address().port}`;
    let response=await fetch(`${base}/api/users?search=${tag}&limit=50&offset=50`);assert.equal(response.status,200);let data=await response.json();assert.equal(data.total,101);assert.equal(data.users.length,50);
    response=await fetch(`${base}/api/users?search=${id}%40example.test`); data=await response.json();assert.equal(data.total,1);assert.equal(data.users[0].schedulesCount,1);assert.equal(data.users[0].playlistsCount,1);assert.equal(data.users[0].totalMinutes,10);assert.equal(data.users[0].lastActive,'2024-01-01T00:00:00.000Z');
    response=await fetch(`${base}/api/users/${id}`);assert.equal(response.status,200);data=await response.json();assert.equal(data.playlists[0].tracks,1);assert.equal(data.schedules.length,1);
    response=await fetch(`${base}/api/activity`);assert.equal(response.status,200);data=await response.json();
    assert.equal(data.activities.filter(a=>a.id.startsWith(`schedule:${tag}`)).length,1);
    assert.equal(data.activities.find(a=>a.id===`follow:${tag}f`).reciter,'alafasy');
    assert.equal(data.activities.find(a=>a.id===`playlist:${tag}p`).action,'Created playlist: Routine');
    assert.equal(data.activities.some(a=>a.id===`playlist:${tag}editorial`),false);
  } finally {
    await pool.query('DELETE FROM schedules WHERE owner LIKE $1',[`${tag}%`]);
    await pool.query('DELETE FROM history WHERE owner LIKE $1',[`${tag}%`]);
    await pool.query('DELETE FROM playlist_items WHERE owner LIKE $1',[`${tag}%`]);
    await pool.query('DELETE FROM follows WHERE owner LIKE $1',[`${tag}%`]);
    await pool.query('DELETE FROM playlists WHERE owner LIKE $1 OR id = $2',[`${tag}%`,`${tag}editorial`]);
    await pool.query('DELETE FROM listening_stats WHERE owner LIKE $1',[`${tag}%`]);
    await pool.query('DELETE FROM users WHERE id LIKE $1',[`${tag}%`]);
    await new Promise(resolve=>server.close(resolve));await pool.end();
  }
});
