import test from 'node:test';
import assert from 'node:assert/strict';
import express from 'express';
import registerPlaybackRoutes from '../server/playbackRoutes.js';

async function withServer(context, check) {
  const app = express();
  registerPlaybackRoutes(app, () => context);
  const server = app.listen(0, '127.0.0.1');
  await new Promise(resolve => server.once('listening', resolve));
  try { await check(`http://127.0.0.1:${server.address().port}/api/playback`); }
  finally { await new Promise(resolve => server.close(resolve)); }
}

test('playback exposes valid saved snapshots without inventing a track', async () => {
  const databases = { listDocuments: async () => ({ documents: [
    { $id: 'empty', user_id: 'a', current_ref: '', queue_json: '[]' },
    { $id: 'bad', user_id: 'a', current_ref: 'mishary/2garbage' },
    { $id: 'valid', user_id: 'a', current_ref: 'mishary/2', position_ms: 24000 },
  ] }) };
  const users = { get: async () => ({ name: 'Listener' }) };
  await withServer({ databases, users, databaseAvailable: true }, async url => {
    const response = await fetch(url);
    assert.equal(response.status, 200);
    const body = await response.json();
    assert.equal(body.total, 1);
    assert.equal(body.sessions[0].surahId, 2);
    assert.equal(body.sessions[0].positionSeconds, 24);
    assert.equal(body.sessions[0].durationSeconds, 0);
    assert.equal(body.sessions[0].updatedAt, 'Unknown');
  });
});

test('playback errors are not reported as an empty live session list', async () => {
  const databases = { listDocuments: async () => { throw new Error('offline'); } };
  const users = { list: async () => ({ users: [] }) };
  await withServer({ databases, users, databaseAvailable: true }, async url => {
    const response = await fetch(url);
    assert.equal(response.status, 503);
  });
});
