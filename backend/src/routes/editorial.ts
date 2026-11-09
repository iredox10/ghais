import type { FastifyInstance } from 'fastify';
import { query } from '../db.js';

/** Only expose the display fields that the app understands. Playback remains
 * defined by its bundled catalog until the editorial schema carries mappings. */
export async function editorialRoutes(app: FastifyInstance) {
  app.get('/moods', async () => {
    const { rows } = await query<{ doc: Record<string, unknown> }>(
      'SELECT doc FROM moods ORDER BY id LIMIT 100',
    );
    return { moods: rows.map(({ doc }) => ({
      slug: doc.slug, title: doc.title, subtitle: doc.subtitle, category: doc.category,
    })) };
  });
}
