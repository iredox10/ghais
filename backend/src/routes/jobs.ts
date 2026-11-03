// Server-side jobs (admin-only via ADMIN_TOKEN bearer). Never expose these.
// Render CronJob can hit POST /v1/jobs/charts-rollup daily; the client NEVER
// writes charts — these recomputes replace whatever the old admin panel did.
import type { FastifyInstance, FastifyRequest } from 'fastify';
import { query } from '../db.js';
import { config } from '../config.js';
import { awError, toAppwriteDoc, type Row, type SharedDoc } from '../compat/shape.js';

import { aggregateCharts, type ChartEvent } from '../charts.js';

export default async function jobRoutes(app: FastifyInstance) {
  app.post('/charts-rollup', async (req: FastifyRequest, reply) => {
    const header = req.headers.authorization ?? '';
    const token = header.startsWith('Bearer ') ? header.slice(7) : '';
    if (!config.adminToken || token !== config.adminToken) {
      awError(reply, 403, 'general_forbidden', 'Job token required.');
      return reply;
    }

    const now = Date.now();
    const events = await query<ChartEvent>(
      'SELECT id, owner, doc, created_at FROM play_events WHERE created_at > $1 AND created_at <= $2 ORDER BY created_at, id',
      [new Date(now - 30 * 86_400_000), new Date(now)],
    );
    const chartDoc = aggregateCharts(events.rows, now);
    const { topQaris7d: last7, topQaris30d: last30, trending, generatedAt } = chartDoc;
    await query(
      `INSERT INTO charts(id, owner, doc) VALUES ('global', NULL, $1)
       ON CONFLICT (id) DO UPDATE SET doc = $1, updated_at = now()`,
      [chartDoc],
    );

    // global_charts/<window> — A13 hook windows (BackendSchema.GlobalCharts).
    for (const [window, entries] of [
      ['7d', last7],
      ['30d', last30],
      ['trending', trending],
    ] as const) {
      const doc: SharedDoc = { window, entries, generatedAt };
      await query(
        `INSERT INTO global_charts(id, owner, doc) VALUES ($1, NULL, $2)
         ON CONFLICT (id) DO UPDATE SET doc = $2, updated_at = now()`,
        [window, doc],
      );
    }

    // Public follower/like counts were formerly Appwrite-maintained data.
    // Rebuild from owner-scoped documents; repeated likes by one owner count once.
    await query(`
      WITH slugs AS (
        SELECT doc->>'slug' AS slug FROM reciters
        UNION SELECT doc->>'reciter_slug' FROM follows
        UNION SELECT doc->>'reciter_slug' FROM likes
        UNION SELECT id FROM reciter_stats
      ), followers AS (
        SELECT doc->>'reciter_slug' AS slug, count(DISTINCT owner) AS n FROM follows GROUP BY 1
      ), liked AS (
        SELECT doc->>'reciter_slug' AS slug, count(DISTINCT owner) AS n FROM likes GROUP BY 1
      )
      INSERT INTO reciter_stats(id, owner, doc)
      SELECT s.slug, NULL, jsonb_build_object('slug', s.slug,
        'followers_count', COALESCE(f.n, 0), 'likes_count', COALESCE(l.n, 0),
        'updated_at', $1::bigint)
      FROM slugs s LEFT JOIN followers f ON f.slug = s.slug LEFT JOIN liked l ON l.slug = s.slug
      WHERE s.slug IS NOT NULL AND s.slug <> ''
      ON CONFLICT (id) DO UPDATE SET doc = EXCLUDED.doc, updated_at = now()
    `, [now]);

    app.log.info(`charts-rollup: 7d=${last7.length} 30d=${last30.length} yesterday=${chartDoc.topQarisYesterday.length}`);

    const { rows } = await query<Row>(
      `SELECT id, owner, doc, created_at, updated_at FROM charts WHERE id = 'global'`,
    );
    return { global: toAppwriteDoc(rows[0], 'charts', 'ghais', null), windows: { last7: last7.length, last30: last30.length } };
  });
}
