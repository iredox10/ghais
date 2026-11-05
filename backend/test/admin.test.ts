import assert from 'node:assert/strict';
import { test } from 'node:test';
import Fastify from 'fastify';
import express from 'express';
import { mountAdmin } from '../src/admin.js';

test('admin is mounted under /admin while the mobile API remains available', async () => {
  const admin = express();
  admin.get('/media/example.jpg', (_req, res) => { res.type('image/jpeg').send(Buffer.from([0xff,0xd8,0xff])); });
  admin.use((req, res, next) => { if (req.headers.authorization === 'Basic test') next(); else res.status(401).end(); });
  admin.use(express.json());
  admin.get('/', (_req, res) => { res.send('admin'); });
  admin.post('/api/check', (req, res) => { res.json(req.body); });
  const app = Fastify();
  await mountAdmin(app, admin);
  app.get('/v1/check', async () => ({ mobile: true }));
  const base = await app.listen({ host:'127.0.0.1', port:0 });
  try {
    assert.equal((await fetch(base+'/admin/')).status,401);
    assert.equal(await (await fetch(base+'/admin/', {headers:{authorization:'Basic test'}})).text(),'admin');
    assert.equal((await fetch(base+'/admin/media/example.jpg')).status,200);
    const posted=await fetch(base+'/admin/api/check',{method:'POST',headers:{authorization:'Basic test','content-type':'application/json'},body:JSON.stringify({value:42})});
    assert.equal(posted.status,200);assert.equal((await posted.json() as {value:number}).value,42);
    assert.deepEqual(await (await fetch(base+'/v1/check')).json(),{mobile:true});
  } finally { await app.close(); }
});
