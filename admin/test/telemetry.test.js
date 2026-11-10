import test from 'node:test';
import assert from 'node:assert/strict';
import express from 'express';
import { registerTelemetry } from '../server/telemetry.js';

test('telemetry does not fabricate latency or report missing data as connected', async () => {
  const app = express();
  registerTelemetry(app, { getDatabases: () => null, getDatabaseId: () => null });
  const server = app.listen(0, '127.0.0.1');
  await new Promise(resolve => server.once('listening', resolve));
  try {
    const response = await fetch(`http://127.0.0.1:${server.address().port}/api/telemetry`);
    const data = await response.json();
    assert.equal(data.pingMs, null);
    assert.equal(data.dbReachable, false);
    assert.equal(data.deviceTelemetry.available, false);
    assert.equal(data.appTelemetry.connected, false);
  } finally { await new Promise(resolve => server.close(resolve)); }
});
