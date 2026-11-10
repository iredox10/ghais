import { Query } from './query.js';

export function registerTelemetry(app, { getDatabases, getDatabaseId }) {
  let activeConnections = 0;

  // Track active HTTP requests/connections (settled-once: 'finish' and
  // 'close' can both fire for one response, so guard against double-count).
  app.use((req, res, next) => {
    activeConnections++;
    let settled = false;
    const release = () => {
      if (settled) return;
      settled = true;
      activeConnections = Math.max(0, activeConnections - 1);
    };
    res.on('finish', release);
    res.on('close', release);
    next();
  });

  // The device section is a sample of saved mobile reports; it is not a count
  // of devices currently connected. Server process metrics are independent.
  app.get('/api/telemetry', async (req, res) => {
    const startTime = Date.now();
    let pingMs = null;
    let dbReachable = false;
    let mode = 'sandbox';

    const databases = getDatabases();
    const databaseId = getDatabaseId();

    if (databases && databaseId) {
      try {
        await databases.listDocuments(databaseId, 'reciters', [Query.limit(1)]);
        dbReachable = true;
        mode = 'live';
        pingMs = Date.now() - startTime;
      } catch (err) {
        pingMs = Date.now() - startTime;
      }
    }

    const memUsage = process.memoryUsage();
    const memoryMb = Math.round(memUsage.rss / (1024 * 1024));
    const uptimeSeconds = Math.round(process.uptime());
    const nodeVersion = process.version;
    const platform = process.platform;

    // Most recent 200 device reports. Reports may be old or absent.
    let deviceTelemetry = {
      available: false,
      devices: 0,
      byPlatform: {},
      byAppVersion: {},
      totalSeconds: 0,
      lastSeen: null
    };
    if (databases && databaseId) {
      try {
        const telRes = await databases.listDocuments(databaseId, 'telemetry', [Query.orderDesc('$updatedAt'), Query.limit(200)]);
        const docs = Array.isArray(telRes?.documents) ? telRes.documents : [];
        const byPlatform = {};
        const byAppVersion = {};
        let totalSeconds = 0;
        let lastSeenTime = 0;
        let lastSeen = null;
        for (const doc of docs) {
          const p = (typeof doc.platform === 'string' && doc.platform.trim() ? doc.platform.trim().toLowerCase() : 'unknown');
          byPlatform[p] = (byPlatform[p] || 0) + 1;
          const v = (typeof doc.app_version === 'string' && doc.app_version.trim() ? doc.app_version.trim() : 'unknown');
          byAppVersion[v] = (byAppVersion[v] || 0) + 1;
          const secs = Number(doc.total_seconds);
          if (Number.isFinite(secs) && secs > 0) totalSeconds += Math.floor(secs);
          const raw = doc.updated_at ?? doc.$updatedAt ?? null;
          let t = 0;
          if (typeof raw === 'number' && Number.isFinite(raw)) t = raw > 1e12 ? raw : (raw > 1e10 ? raw : raw * 1000);
          else if (typeof raw === 'string' && raw.trim()) {
            const parsed = Date.parse(raw.trim());
            if (Number.isFinite(parsed)) t = parsed;
          }
          if (t > lastSeenTime) {
            lastSeenTime = t;
            lastSeen = new Date(t).toISOString();
          }
        }
        deviceTelemetry = {
          available: true,
          devices: docs.length,
          byPlatform,
          byAppVersion,
          totalSeconds,
          lastSeen
        };
      } catch (_) {
        // Collection empty / missing / no permission yet — stay degraded.
      }
    }

    const appTelemetry = deviceTelemetry.devices > 0
      ? {
          connected: true,
          reason: `live: ${deviceTelemetry.devices} device(s) reporting via telemetry collection (limit 200)`
        }
      : !deviceTelemetry.available ? {
          connected: false,
          reason: 'Device telemetry could not be retrieved.'
        } : {
          connected: false,
          reason: 'No device telemetry is available yet; server metrics remain available.'
        };

    res.json({
      pingMs,
      uptimeSeconds,
      memoryMb,
      nodeVersion,
      platform,
      activeConnections,
      mode,
      dbReachable,
      source: 'server-self',
      appTelemetry,
      deviceTelemetry
    });
  });
}
