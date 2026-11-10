import { Query } from './query.js';

// These routes use bounded SQL pagination for the directory and scoped queries
// for each profile; counts must never depend on a global sample of documents.
export function registerUsersRoutes(app, { pool, databases, databaseId, userStore, listHistoryDocs, historyTs, parseTrack }) {
  app.get('/api/users', async (req, res) => {
    const limit = Number(req.query.limit ?? 50), offset = Number(req.query.offset ?? 0);
    const search = typeof req.query.search === 'string' ? req.query.search.trim() : '';
    if (!Number.isSafeInteger(limit) || limit < 1 || limit > 100 || !Number.isSafeInteger(offset) || offset < 0 || search.length > 200) {
      return res.status(400).json({ error: 'Invalid pagination or search.' });
    }
    try {
      const pattern = `%${search.replace(/[\\%_]/g, '\\$&')}%`;
      const filter = "($1 = '%%' OR u.name ILIKE $1 OR u.email ILIKE $1 OR u.id ILIKE $1)";
      const [count, result] = await Promise.all([
        pool.query(`SELECT COUNT(*)::int AS total FROM users u WHERE ${filter}`, [pattern]),
        pool.query(`SELECT u.id, u.name, u.email, u.provider, u.created_at,
          st.doc AS stats,
          (SELECT COUNT(DISTINCT COALESCE(NULLIF(s.doc->>'schedule_id',''), s.id))::int FROM schedules s WHERE s.owner = u.id AND s.doc->>'deleted' IS DISTINCT FROM 'true') AS schedules_count,
          (SELECT COUNT(*)::int FROM playlists p WHERE p.owner = u.id) AS playlists_count,
          (SELECT MAX(h.created_at) FROM history h WHERE h.owner = u.id) AS last_active
          FROM users u
          LEFT JOIN LATERAL (SELECT doc FROM listening_stats WHERE owner = u.id ORDER BY updated_at DESC, id DESC LIMIT 1) st ON true
          WHERE ${filter} ORDER BY u.created_at DESC, u.id DESC LIMIT $2 OFFSET $3`, [pattern, limit, offset]),
      ]);
      const users = result.rows.map(row => userSummary(row));
      return res.json({ total: count.rows[0].total, users, limit, offset });
    } catch (error) {
      console.error('Error fetching users:', error.message);
      return res.status(500).json({ error: 'Could not load listeners.' });
    }
  });

  app.get('/api/users/:id', async (req, res) => {
    try {
      const id = req.params.id;
      let record;
      try { record = await userStore.get(id); }
      catch (error) { if (error.code === 404) return res.status(404).json({ error: 'User not found.' }); throw error; }
      const [stRes, schRes, plRes, histRes, trackCounts] = await Promise.all([
        databases.listDocuments(databaseId, 'listening_stats', [Query.equal('user_id', id), Query.orderDesc('$updatedAt'), Query.limit(1)]),
        allForUser(databases, databaseId, 'schedules', 'user_id', id),
        allForUser(databases, databaseId, 'playlists', 'owner_id', id),
        listHistoryDocs([Query.equal('user_id', id), Query.limit(25)]),
        pool.query("SELECT i.doc->>'playlist_id' AS id, COUNT(*)::int AS count FROM playlist_items i JOIN playlists p ON p.id = i.doc->>'playlist_id' WHERE p.owner = $1 GROUP BY 1", [id]),
      ]);
      const st = stRes.documents[0] || {};
      const counts = new Map(trackCounts.rows.map(row => [row.id, row.count]));
      const activeSchedules = new Map();
      for (const schedule of schRes) {
        if (schedule.deleted === true) continue;
        const key = schedule.schedule_id || schedule.$id;
        activeSchedules.set(key, schedule);
      }
      const schedules = [...activeSchedules.values()].map(d => {
        let parsed = {};
        try { parsed = JSON.parse(d.schedule_json || '{}') || {}; } catch {}
        const h = Number.isInteger(parsed.hour) ? Math.min(23, Math.max(0, parsed.hour)) : null;
        const m = Number.isInteger(parsed.minute) ? Math.min(59, Math.max(0, parsed.minute)) : null;
        const time = h === null || m === null ? 'Unknown time' : `${h % 12 || 12}:${String(m).padStart(2, '0')} ${h >= 12 ? 'PM' : 'AM'}`;
        const from = Number.isInteger(parsed.fromSurah) ? parsed.fromSurah : null;
        const to = Number.isInteger(parsed.toSurah) ? parsed.toSurah : null;
        return { id: d.$id, scheduleId: d.schedule_id || parsed.id || d.$id, time,
          label: parsed.durationMin == null ? 'Recitation Alarm (full range)' : `Recitation Alarm (${parsed.durationMin} mins)`,
          reciter: parsed.reciterSlug || 'Unknown reciter', surahs: from && to ? `Surah ${from} → ${to}` : 'Surah range unavailable', enabled: d.enabled ?? parsed.enabled ?? true };
      });
      const playlists = plRes.map(p => ({ id: p.$id, title: p.title || 'Untitled playlist', tracks: counts.get(p.$id) ?? 0, createdAt: p.$createdAt || '' }));
      const history = histRes.documents.map(h => {
        const tr = parseTrack(h.track_json), durationMs = Number(tr.durationMs) || 0;
        const mins = Math.floor(durationMs / 60000), secs = Math.floor((durationMs % 60000) / 1000);
        const rawSurahId = Number(tr.surahId ?? tr.surah_id ?? tr.surahID);
        const surahId = Number.isInteger(rawSurahId) && rawSurahId >= 1 && rawSurahId <= 114 ? rawSurahId : null;
        return { id: h.$id, surahName: tr.surahNameEn || tr.surahName || tr.surah_name || (surahId ? `Surah ${surahId}` : 'Unknown surah'), reciterName: tr.reciterName || tr.reciter_name || tr.reciterSlug || 'Unknown reciter',
          duration: durationMs > 0 ? (mins > 0 ? `${mins}m ${secs}s` : `${secs}s`) : 'Duration unavailable',
          playedTime: historyTs(h) ? new Date(historyTs(h)).toLocaleString() : 'Time unavailable', surahNumber: surahId };
      });
      const totalMinutes = Math.round(numeric(st.total_seconds) / 60);
      return res.json({ user: { id, name: record.name || record.email?.split('@')[0] || 'Listener', email: record.email || '', provider: record.provider || 'email',
        createdAt: record.$createdAt, totalMinutes, daysStreak: numeric(st.days_streak), uniqueSurahs: numeric(st.unique_surahs), schedulesCount: schedules.length, playlistsCount: playlists.length,
        lastActive: histRes.documents.length && historyTs(histRes.documents[0]) ? new Date(historyTs(histRes.documents[0])).toISOString() : '' },
        stats: { totalHours: Math.round(totalMinutes / 60), streak: numeric(st.days_streak), topReciter: '—', favoriteSurah: '—', preferredAmbient: '—' }, schedules, playlists, history });
    } catch (error) {
      console.error('Error fetching listener profile:', error.message);
      return res.status(500).json({ error: 'Could not load listener profile.' });
    }
  });
}

function numeric(value) { const n = Number(value); return Number.isFinite(n) && n > 0 ? n : 0; }
function userSummary(row) {
  const st = row.stats || {};
  return { id: row.id, name: row.name?.trim() || row.email?.split('@')[0] || 'Listener', email: row.email || '',
    provider: row.provider || 'email', createdAt: row.created_at, totalMinutes: Math.round(numeric(st.total_seconds) / 60),
    daysStreak: numeric(st.days_streak), uniqueSurahs: numeric(st.unique_surahs), schedulesCount: row.schedules_count,
    playlistsCount: row.playlists_count, lastActive: row.last_active ? new Date(row.last_active).toISOString() : '' };
}
async function allForUser(databases, databaseId, collection, attribute, id) {
  const out = [], pageSize = 1000;
  for (let offset = 0; ; offset += pageSize) {
    const page = await databases.listDocuments(databaseId, collection, [Query.equal(attribute, id), Query.limit(pageSize), Query.offset(offset), Query.orderAsc('$id')]);
    out.push(...page.documents);
    if (page.documents.length < pageSize) return out;
  }
}
