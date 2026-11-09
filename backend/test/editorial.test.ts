import assert from 'node:assert/strict';
import { test } from 'node:test';
import Fastify from 'fastify';
import { pool } from '../src/db.js';
import { editorialRoutes } from '../src/routes/editorial.js';

test('public mood catalog exposes only display fields', async () => {
  const original = pool.query;
  pool.query = (async () => ({ rows: [{ doc: { slug:'sleep', title:'Sleep', subtitle:'Rest', category:'Night', secret:'hidden' } }] })) as typeof pool.query;
  const app = Fastify();
  try {
    await app.register(editorialRoutes, { prefix: '/v1/editorial' });
    const response = await app.inject({ url: '/v1/editorial/moods' });
    assert.equal(response.statusCode, 200);
    assert.deepEqual(response.json(), { moods: [{ slug:'sleep', title:'Sleep', subtitle:'Rest', category:'Night' }] });
  } finally { pool.query = original; await app.close(); }
});
