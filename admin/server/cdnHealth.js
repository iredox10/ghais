/**
 * Audio CDN & Edge Cluster Health Benchmark Module
 */

export function registerCdnHealth(app) {
  // Probed hosts MUST match the app's REAL audio hosts (verified 2026-09-24
  // against shared/.../domain/model/Models.kt getFullSurahUrl/getAyahAudioUrl):
  // server7 (basit/shur/s_gmd), server8 (afs fallback), server10 (minsh/ajm),
  // server11 (sds/yasser), server12 (maher), server13 (husr), server14 (islam),
  // everyayah.com/data (per-ayah clips), archive.org/download (Omar-Hisham).
  // server6/server9/server16 are NOT emitted by the app and are NOT probed.
  // NOTE: per-host `reciters` counts were removed — they were hardcoded
  // fabrications, not live data. The field stays optional for a future live
  // count (e.g. derived from the reciters collection server_url values).
  const cdnEndpoints = [
    { name: 'Server 7', url: 'https://server7.mp3quran.net' },
    { name: 'Server 8', url: 'https://server8.mp3quran.net' },
    { name: 'Server 10', url: 'https://server10.mp3quran.net' },
    { name: 'Server 11', url: 'https://server11.mp3quran.net' },
    { name: 'Server 12', url: 'https://server12.mp3quran.net' },
    { name: 'Server 13', url: 'https://server13.mp3quran.net' },
    { name: 'Server 14', url: 'https://server14.mp3quran.net' },
    { name: 'EveryAyah', url: 'https://everyayah.com' },
    { name: 'Archive.org', url: 'https://archive.org' }
  ];

  app.get('/api/health/cdn', async (req, res) => {
    const servers = await Promise.all(
      cdnEndpoints.map(async (server) => {
        const startTime = Date.now();
        let pingMs = 0;
        let httpStatus = 0;
        let status = 'down';

        try {
          const controller = new AbortController();
          const timeoutId = setTimeout(() => controller.abort(), 6000);

          let response;
          try {
            response = await fetch(server.url, {
              method: 'HEAD',
              signal: controller.signal,
              headers: { 'User-Agent': 'Ghais-Admin-Monitor/1.0' }
            });
          } catch (_) {
            response = await fetch(server.url, {
              method: 'GET',
              signal: controller.signal,
              headers: { 'User-Agent': 'Ghais-Admin-Monitor/1.0' }
            });
          }

          clearTimeout(timeoutId);
          pingMs = Date.now() - startTime;
          httpStatus = response.status;

          if (httpStatus >= 200 && httpStatus < 400) {
            status = pingMs > 1200 ? 'degraded' : 'healthy';
          } else if (httpStatus >= 400 && httpStatus < 600) {
            status = pingMs > 1500 ? 'degraded' : 'healthy';
          } else {
            status = 'degraded';
          }
        } catch (err) {
          pingMs = Date.now() - startTime;
          status = 'down';
          httpStatus = 0;
        }

        return {
          name: server.name,
          url: server.url,
          pingMs: Math.max(12, pingMs),
          status,
          httpStatus
        };
      })
    );

    res.json({ servers });
  });
}
