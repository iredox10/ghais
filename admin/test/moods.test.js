import test from 'node:test';
import assert from 'node:assert/strict';
import express from 'express';
import registerMoodsRoutes from '../server/moodsRoutes.js';
import registerSeedRoutes from '../server/seedRoutes.js';

test('mood editing preserves playback metadata, validates mobile constraints and supports partial catalogs', async () => {
  const docs = [];
  const db = {
    listDocuments: async (_db, _collection, queries) => { const equal = queries.map(JSON.parse).find(q => q.method === 'equal'); return { documents: equal ? docs.filter(d => d.slug === equal.values[0]) : docs }; },
    createDocument: async (_db, _collection, id, doc) => { const item = { $id: id, ...doc }; docs.push(item); return item; },
    updateDocument: async (_db, _collection, id, patch) => { const item = docs.find(d => d.$id === id); Object.assign(item, patch); return item; },
  };
  const app = express(); app.use(express.json());
  registerMoodsRoutes(app, () => ({ databases: db, databaseId: 'ghais', databaseAvailable: true }));
  const server = await new Promise(resolve => { const s = app.listen(0, '127.0.0.1', () => resolve(s)); });
  try {
    const base = `http://127.0.0.1:${server.address().port}/api/moods`;
    const patch = (slug, body) => fetch(`${base}/${slug}`, { method:'PATCH', headers:{'content-type':'application/json'}, body:JSON.stringify(body) });
    assert.equal((await patch('unknown', { title:'A', subtitle:'B', category:'Focus' })).status, 404);
    assert.equal((await patch('study', { title:'A', subtitle:'B', category:'Unsupported' })).status, 400);
    assert.equal((await patch('study', { title:'Study well', subtitle:'A short description', category:'Focus' })).status, 200);
    assert.equal(docs[0].default_reciter_slug, 'mishary');
    assert.equal((await (await fetch(base)).json()).moods.length, 6);
    assert.equal((await patch('study', { title:'New title', subtitle:'Another description', category:'Night' })).status, 200);
    assert.equal(docs.length, 1); assert.equal(docs[0].title, 'New title');
    docs.push({ ...docs[0], $id: 'duplicate' });
    assert.equal((await patch('study', { title:'A', subtitle:'B', category:'Focus' })).status, 409);
  } finally { server.close(); }
});

test('seeding leaves edited moods unchanged', async () => {
  const existing = { $id: 'legacy-study', slug: 'study', title: 'Edited title' };
  const docs = [existing];
  const db = {
    listDocuments: async (_db, _collection, queries) => { const equal = queries.map(JSON.parse).find(q => q.method === 'equal'); return { documents: docs.filter(d => d.slug === equal.values[0]) }; },
    getDocument: async (_db, _collection, id) => { const item = docs.find(d => d.$id === id); if (!item) throw Object.assign(new Error('missing'), { code: 404 }); return item; },
    createDocument: async (_db, _collection, id, doc) => { docs.push({ $id:id, ...doc }); },
    updateDocument: async () => { throw new Error('Seed must not update existing moods'); },
  };
  const app = express(); app.use(express.json());
  registerSeedRoutes(app, () => ({ databases:db, databaseId:'ghais', databaseAvailable:true }));
  const server = await new Promise(resolve => { const s = app.listen(0, '127.0.0.1', () => resolve(s)); });
  try {
    const response = await fetch(`http://127.0.0.1:${server.address().port}/api/seed/moods`, { method:'POST' });
    assert.equal(response.status, 200);
    assert.equal(existing.title, 'Edited title');
    assert.equal(docs.filter(d => d.slug === 'study').length, 1);
    assert.equal(docs.length, 6);
  } finally { server.close(); }
});
