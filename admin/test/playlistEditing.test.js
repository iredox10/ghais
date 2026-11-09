import test from 'node:test';
import assert from 'node:assert/strict';
import express from 'express';
import { registerPlaylistEditing } from '../server/playlistEditing.js';

test('editorial track updates validate input and write tracks and metadata in one transaction', async () => {
  const calls=[];
  const client={ query:async (sql,args=[])=>{ calls.push([sql,args]); return sql.startsWith('SELECT owner')?{rowCount:1,rows:[{owner:'editorial'}]}:{rowCount:1,rows:[]}; },release(){} };
  const app=express();app.use(express.json());registerPlaylistEditing(app,{connect:async()=>client});
  const server=await new Promise(resolve=>{const s=app.listen(0,'127.0.0.1',()=>resolve(s));});
  try {
    const url=`http://127.0.0.1:${server.address().port}/api/playlists/editorial-one/tracks`;
    const put=tracks=>fetch(url,{method:'PUT',headers:{'content-type':'application/json'},body:JSON.stringify({tracks})});
    assert.equal((await put([{reciterSlug:'a',surahId:1,ayahFrom:3,ayahTo:1}])).status,400);
    assert.equal(calls.length,0);
    assert.equal((await put([{reciterSlug:'a',surahId:1,ayahFrom:0,ayahTo:0},{reciterSlug:'a',surahId:2,ayahFrom:2,ayahTo:3}])).status,200);
    assert.equal(calls[0][0],'BEGIN');assert.equal(calls.at(-1)[0],'COMMIT');
    assert.deepEqual(JSON.parse(calls.find(([sql])=>sql.startsWith('UPDATE playlists'))[1][1]),{qariSlugs:['a']});
    assert.deepEqual(calls.filter(([sql])=>sql.startsWith('INSERT INTO playlist_items')).map(([,args])=>JSON.parse(args[2]).position),[0,1]);
  } finally {await new Promise(resolve=>server.close(resolve));}
});
