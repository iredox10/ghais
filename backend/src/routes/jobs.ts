// Server-side jobs (admin-only via ADMIN_TOKEN bearer). Never expose these.
// Render CronJob can hit POST /v1/jobs/charts-rollup daily; the client NEVER
// writes charts — these recomputes replace whatever the old admin panel did.
import type { FastifyInstance, FastifyRequest } from 'fastify';
import { query } from '../db.js';
import { config } from '../config.js';
import { awError, toAppwriteDoc, type Row, type SharedDoc } from '../compat/shape.js';

/** Top reciter slugs by play-event count since [sinceMs], distinct-owner weight. */
async function topSlugs(sinceMs: number, limit: number): Promise<string[]> {
  const { rows } = await query<{ slug: string; n: string }>(
    `SELECT doc->>'reciterSlug' AS slug, count(*) AS n
       FROM play_events
      WHERE doc->>'reciterSlug' IS NOT NULL
        AND (doc->>'ms')::bigint >= $1
        AND (doc->>'ms')::bigint <= $2
      GROUP BY 1
      ORDER BY n DESC
      LIMIT $3`,
    [sinceMs, Date.now(), limit],
  );
  return rows.map((r) => r.slug).filter(Boolean);
}

const DAY = 86_400_000;
const TOP = 20;

export default async function jobRoutes(app: FastifyInstance) {
  app.post('/charts-rollup', async (req: FastifyRequest, reply) => {
    const header = req.headers.authorization ?? '';
    const token = header.startsWith('Bearer ') ? header.slice(7) : '';
    if (!config.adminToken || token !== config.adminToken) {
      awError(reply, 403, 'general_forbidden', 'Job token required.');
      return reply;
    }

    const now = Date.now();
    const last7 = await topSlugs(now - 7 * DAY, TOP);
    const last30 = await topSlugs(now - 30 * DAY, TOP);
    const yesterday = await topSlugs(now - 2 * DAY, TOP);

    const generatedAt = new Date(now).toISOString();

    // charts/global — the single doc `ChartsRepository` reads. Both the
    // camelCase keys the newer reader uses and its snake_case fallbacks.
    const chartDoc: SharedDoc = {
      topQaris7d: last7,
      topQaris30d: last30,
      topQarisYesterday: yesterday,
      trending: last7,
      top_qaris_7d: last7,
      top_qaris_30d: last30,
      top_qaris_yesterday: yesterday,
      generatedAt,
    };
    await query(
      `INSERT INTO charts(id, owner, doc) VALUES ('global', NULL, $1)
       ON CONFLICT (id) DO UPDATE SET doc = $1, updated_at = now()`,
      [chartDoc],
    );

    // global_charts/<window> — A13 hook windows (BackendSchema.GlobalCharts).
    for (const [window, entries] of [
      ['7d', last7],
      ['30d', last30],
      ['trending', last7],
    ] as const) {
      const doc: SharedDoc = { window, entries, generatedAt };
      await query(
        `INSERT INTO global_charts(id, owner, doc) VALUES ($1, NULL, $2)
         ON CONFLICT (id) DO UPDATE SET doc = $2, updated_at = now()`,
        [window, doc],
      );
    }

    app.log.info(`charts-rollup: 7d=${last7.length} 30d=${last30.length} yesterday=${yesterday.length}`);

    const { rows } = await query<Row>(
      `SELECT id, owner, doc, created_at, updated_at FROM charts WHERE id = 'global'`,
    );
    return { global: toAppwriteDoc(rows[0], 'charts', 'ghais', null), windows: { last7: last7.length, last30: last30.length } };
  });
}
