import test from 'node:test';
import assert from 'node:assert/strict';
import { getListeningAnalytics } from '../server/listeningAnalytics.js';
import pg from 'pg';
import express from 'express';
import registerExportRoutes from '../server/exportRoutes.js';

test('listening analytics groups all UTC hours and excludes invalid dates', async () => {
  const result = await getListeningAnalytics({ query: async () => ({ rows: [
    { hour: 3, count: '2' }, { hour: 11, count: '4' }, { hour: 23, count: '1' },
    { hour: null, count: '3' },
  ] }) });
  assert.equal(result.sampleSize, 7);
  assert.equal(result.skipped, 3);
  assert.equal(result.timezone, 'UTC');
  assert.deepEqual(result.peakHours.map(x => x.count), [2, 4, 0, 0, 0, 1]);
  assert.deepEqual(result.peakHours.map(x => x.index), [50, 100, 0, 0, 0, 25]);
  assert.equal(result.retention, null);
});

test('analytics propagates database errors instead of reporting fabricated zeros', async () => {
  await assert.rejects(getListeningAnalytics({ query: async () => { throw new Error('database offline'); } }), /database offline/);
});

test('analytics export uses the same source and does not claim zero retention', async () => {
  const pool = { query: async () => ({ rows: [{ hour: 8, count: '5' }] }) };
  const app = express();
  registerExportRoutes(app, () => ({ pool, databaseId: 'ghais' }));
  const server = app.listen(0, '127.0.0.1');
  await new Promise(resolve => server.once('listening', resolve));
  try {
    const response = await fetch(`http://127.0.0.1:${server.address().port}/api/export/analytics`);
    assert.equal(response.status, 200);
    const data = await response.json();
    assert.equal(data.sampleSize, 5);
    assert.equal(data.source, 'saved-history');
    assert.equal(data.retention, null);
  } finally { await new Promise(resolve => server.close(resolve)); }
});

test('PostgreSQL counts beyond 200 and safely excludes malformed timestamps', { skip: !process.env.TEST_DATABASE_URL }, async () => {
  const pool = new pg.Pool({ connectionString: process.env.TEST_DATABASE_URL });
  const client = await pool.connect();
  try {
    await client.query('CREATE TEMP TABLE history (doc jsonb NOT NULL)');
    await client.query(`INSERT INTO history(doc)
      SELECT jsonb_build_object('played_at_ms', $1::bigint) FROM generate_series(1, 205)`,
      [Date.UTC(2025, 0, 1, 4)]);
    await client.query("INSERT INTO history(doc) VALUES ('{}'), ('{\"played_at_ms\": \"not-a-date\"}'), ('{\"played_at_ms\": \"999999999999999999999999\"}')");
    const result = await getListeningAnalytics(client);
    assert.equal(result.sampleSize, 205);
    assert.equal(result.skipped, 3);
    assert.equal(result.peakHours[0].count, 205);
  } finally { client.release(); await pool.end(); }
});
