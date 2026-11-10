/** Recent saved records, ordered together instead of concatenating unrelated samples. */
export function registerActivityRoutes(app, pool) {
  app.get('/api/activity', async (_req, res) => {
    try {
      const result = await pool.query(`
        WITH events AS (
          SELECT 'listen' AS type, h.id, h.owner, h.doc,
            CASE WHEN h.doc->>'played_at_ms' ~ '^[0-9]{12,13}$'
              THEN to_timestamp((h.doc->>'played_at_ms')::numeric / 1000)
              ELSE h.created_at END AS event_at
          FROM history h
          UNION ALL
          SELECT 'like', l.id, l.owner, l.doc, l.created_at FROM likes l
          UNION ALL
          SELECT 'schedule', s.id, s.owner, s.doc, s.updated_at FROM schedules s
          WHERE s.doc->>'deleted' IS DISTINCT FROM 'true'
        )
        SELECT e.*, u.name, u.email FROM events e LEFT JOIN users u ON u.id = e.owner
        ORDER BY e.event_at DESC, e.type, e.id DESC LIMIT 50
      `);
      const activities = result.rows.map(row => {
        const doc = row.doc || {};
        let track = {};
        if (row.type === 'listen') { try { track = JSON.parse(doc.track_json || '{}') || {}; } catch {} }
        let schedule = {};
        if (row.type === 'schedule') { try { schedule = JSON.parse(doc.schedule_json || '{}') || {}; } catch {} }
        const surah = row.type === 'listen' ? (track.surahNameEn || track.surahName || track.surah_name || (track.surahId ? `Surah ${track.surahId}` : '—'))
          : row.type === 'like' && doc.surah_id ? `Surah ${doc.surah_id}` : '—';
        const reciter = row.type === 'listen' ? (track.reciterName || track.reciter_name || track.reciterSlug || '—')
          : row.type === 'like' ? (doc.reciter_slug || '—') : (schedule.reciterSlug || '—');
        return { id: `${row.type}:${row.id}`, user: row.name?.trim() || row.email?.split('@')[0] || 'Listener',
          action: row.type === 'listen' ? 'Saved listening history' : row.type === 'like' ? 'Saved a favorite' : 'Saved a recitation schedule',
          reciter, surah, ambient: '—', time: new Date(row.event_at).toISOString(), type: row.type };
      });
      res.json({ activities });
    } catch (error) {
      console.error('Error fetching listener activity:', error.message);
      res.status(503).json({ error: 'Listener activity is unavailable.' });
    }
  });
}
