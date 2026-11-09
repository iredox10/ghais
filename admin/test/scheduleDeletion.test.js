import test from 'node:test';
import assert from 'node:assert/strict';
import express from 'express';
import {registerScheduleDeletion} from '../server/scheduleDeletion.js';

test('schedule deletion tombstones duplicate logical IDs for one owner',async()=>{
  const calls=[];const target={id:'a',owner:'u',doc:{schedule_id:'alarm'}};
  const client={query:async(sql,args=[])=>{calls.push([sql,args]);if(sql.startsWith('SELECT id, owner, doc FROM schedules WHERE id='))return {rowCount:1,rows:[target]};if(sql.startsWith('SELECT id, owner, doc FROM schedules WHERE owner='))return {rows:[target,{id:'b',owner:'u',doc:{schedule_json:'{"id":"alarm"}'}},{id:'c',owner:'u',doc:{schedule_id:'other'}}]};return {rows:[]};},release(){}};
  const app=express();registerScheduleDeletion(app,{connect:async()=>client});const server=await new Promise(resolve=>{const s=app.listen(0,'127.0.0.1',()=>resolve(s));});
  try {const response=await fetch(`http://127.0.0.1:${server.address().port}/api/schedules/a`,{method:'DELETE'});assert.equal(response.status,200);const update=calls.find(([sql])=>sql.startsWith('UPDATE schedules'));assert.deepEqual(update[1][0],['a','b']);const tombstone=JSON.parse(update[1][1]);assert.equal(tombstone.deleted,true);assert.equal(tombstone.enabled,false);assert.ok(tombstone.deleted_at_ms>0);assert.equal(calls.at(-1)[0],'COMMIT');} finally {await new Promise(resolve=>server.close(resolve));}
});
