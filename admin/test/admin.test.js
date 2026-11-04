import { test } from 'node:test';
import assert from 'node:assert/strict';
import express from 'express';
import { adminAuth } from '../server/security.js';
import { createStorage } from '../server/storage.js';
import { setupRecitersSync } from '../server/recitersSync.js';
import { registerPublicImages } from '../server/publicImages.js';
import { Readable } from 'node:stream';

async function listen(app) { const server=app.listen(0,'127.0.0.1'); await new Promise(r=>server.once('listening',r));return {server,url:`http://127.0.0.1:${server.address().port}`}; }

test('admin API requires authentication and rejects cross-origin mutations',async()=>{
  const app=express();app.use(adminAuth({ADMIN_USERNAME:'admin',ADMIN_PASSWORD:'correct-password-123'}));app.get('/data',(_q,r)=>r.json({ok:true}));app.post('/data',(_q,r)=>r.json({ok:true}));const {server,url}=await listen(app);
  try { const auth='Basic '+Buffer.from('admin:correct-password-123').toString('base64');
    assert.equal((await fetch(url+'/data')).status,401);
    assert.equal((await fetch(url+'/data',{headers:{authorization:auth}})).status,200);
    assert.equal((await fetch(url+'/data',{method:'POST',headers:{authorization:auth,origin:'https://attacker.example'}})).status,403);
    assert.equal((await fetch(url+'/data',{method:'POST',headers:{authorization:auth,origin:url}})).status,200);
  } finally { await new Promise(r=>server.close(r)); }
});

test('R2 uploads validate bytes, create unique keys and never delete legacy IDs',async()=>{
  const commands=[],client={send:async command=>{commands.push(command);return {}}};
  const storage=createStorage({R2_ACCOUNT_ID:'test',R2_ACCESS_KEY_ID:'test',R2_SECRET_ACCESS_KEY:'test',R2_BUCKET:'images',R2_PUBLIC_URL:'https://images.example.test'},client);
  await assert.rejects(storage.upload(Buffer.from('not an image'),'image/jpeg'));
  const file=await storage.upload(Buffer.from([0xff,0xd8,0xff,0x01]),'image/jpeg');
  assert.match(file.key,/^reciters\/[a-f0-9-]+\.jpg$/);assert.equal(file.url,'https://images.example.test/'+file.key);
  await storage.delete('old-appwrite-file-id');assert.equal(commands.length,1);
  await storage.delete(file.key);assert.equal(commands.length,2);
  assert.equal(await storage.get('../private'),null);assert.equal(commands.length,2);
  await storage.get(file.key);assert.equal(commands.length,3);
  const proxy=createStorage({R2_ACCOUNT_ID:'test',R2_ACCESS_KEY_ID:'test',R2_SECRET_ACCESS_KEY:'test',R2_BUCKET:'images',ADMIN_PUBLIC_URL:'https://admin.example.test'},client);
  const proxied=await proxy.upload(Buffer.from([0xff,0xd8,0xff,0x01]),'image/jpeg');
  assert.equal(proxied.url, 'https://admin.example.test/media/'+proxied.key);
});

test('portrait reads are public while management remains protected',async()=>{
  const key='reciters/00000000-0000-4000-8000-000000000000.jpg';
  const app=express();registerPublicImages(app,{get:async requested=>requested===key?{Body:Readable.from([Buffer.from([0xff,0xd8,0xff])])}:null});
  app.use(adminAuth({ADMIN_USERNAME:'admin',ADMIN_PASSWORD:'correct-password-123'}));app.post('/api/reciters',(_q,r)=>r.sendStatus(200));
  const {server,url}=await listen(app);
  try {
    const image=await fetch(url+'/media/'+key);assert.equal(image.status,200);assert.equal(image.headers.get('content-type'),'image/jpeg');
    assert.equal((await fetch(url+'/media/reciters/not-a-key.jpg')).status,404);
    assert.equal((await fetch(url+'/api/reciters',{method:'POST'})).status,401);
  } finally {await new Promise(r=>server.close(r));}
});

test('reciter image upload cleans up R2 object when updating PostgreSQL fails',async()=>{
  const deleted=[];const docs=[{$id:'maher',slug:'maher',name_en:'Maher',catalog:'mp3quran'}];
  const database={listDocuments:async()=>({documents:docs}),updateDocument:async()=>{throw Error('db failure')}};
  const storage={upload:async()=>({key:'reciters/test.jpg',url:'https://images.example.test/reciters/test.jpg'}),delete:async key=>deleted.push(key)};
  const app=express();setupRecitersSync(app,database,'ghais',storage);const {server,url}=await listen(app);
  try { const data=new FormData();data.append('image',new Blob([new Uint8Array([0xff,0xd8,0xff,0x01])],{type:'image/jpeg'}),'qari.jpg');const res=await fetch(url+'/api/reciters/maher/image',{method:'POST',body:data});assert.equal(res.status,500);assert.deepEqual(deleted,['reciters/test.jpg']); }
  finally {await new Promise(r=>server.close(r));}
});
