import test from 'node:test';
import assert from 'node:assert/strict';
import express from 'express';
import registerBroadcastRoutes from '../server/broadcastRoutes.js';

test('broadcasts are public and reject unsupported targeting', async () => {
  let saved;
  const app = express();
  app.use(express.json());
  registerBroadcastRoutes(app, () => ({ databaseId: 'ghais', databases: {
    createDocument: async (_db, _collection, _id, data) => { saved = data; return { $id: 'one', $createdAt: new Date().toISOString() }; },
  } }));
  const server = await new Promise(resolve => { const s = app.listen(0, '127.0.0.1', () => resolve(s)); });
  try {
    const url = `http://127.0.0.1:${server.address().port}/api/broadcast`;
    const post = body => fetch(url, { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify(body) });
    assert.equal((await post({ title: 'Title', body: 'Body', targetTopic: 'Daily Khatma Streaks' })).status, 400);
    assert.equal(saved, undefined);
    assert.equal((await post({ title: 'Title', body: 'Body', targetTopic: 'All Listeners' })).status, 201);
    assert.equal(saved.audience, 'all');
  } finally { server.close(); }
});
