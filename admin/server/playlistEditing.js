import { randomBytes } from 'node:crypto';

/** Replace an editorial playlist's tracklist atomically and derive featured reciters. */
export function registerPlaylistEditing(app, pool) {
  app.put('/api/playlists/:id/tracks', async (req, res) => {
    const tracks = req.body?.tracks;
    if (!Array.isArray(tracks) || tracks.length > 50 || tracks.some(t =>
      !t || typeof t.reciterSlug !== 'string' || !t.reciterSlug.trim() || t.reciterSlug.length > 120 ||
      !Number.isInteger(t.surahId) || t.surahId < 1 || t.surahId > 114 ||
      !Number.isInteger(t.ayahFrom) || !Number.isInteger(t.ayahTo) ||
      t.ayahFrom < 0 || t.ayahTo < 0 || t.ayahFrom > 286 || t.ayahTo > 286 ||
      ((t.ayahFrom === 0) !== (t.ayahTo === 0)) || t.ayahFrom > t.ayahTo
    )) return res.status(400).json({ error: 'Provide up to 50 valid tracks with a reciter, surah, and matching ayah range.' });

    let client;
    try {
      client = await pool.connect();
      await client.query('BEGIN');
      const playlist = await client.query('SELECT owner FROM playlists WHERE id=$1 FOR UPDATE', [req.params.id]);
      if (!playlist.rowCount || playlist.rows[0].owner !== 'editorial') {
        await client.query('ROLLBACK');
        return res.status(404).json({ error: 'Editorial playlist not found.' });
      }
      await client.query("DELETE FROM playlist_items WHERE doc->>'playlist_id'=$1", [req.params.id]);
      for (let i = 0; i < tracks.length; i++) {
        const t = tracks[i];
        const doc = { playlist_id: req.params.id, position: i, reciter_slug: t.reciterSlug.trim(), surah_id: t.surahId, ayah_from: t.ayahFrom, ayah_to: t.ayahTo };
        await client.query('INSERT INTO playlist_items (id, owner, doc) VALUES ($1,$2,$3)', [randomBytes(10).toString('hex'), 'editorial', JSON.stringify(doc)]);
      }
      const qariSlugs = [...new Set(tracks.map(t => t.reciterSlug.trim()))];
      await client.query("UPDATE playlists SET doc=doc || $2::jsonb || CASE WHEN doc ? 'publishedAt' OR doc ? 'published_at' THEN '{}'::jsonb ELSE jsonb_build_object('publishedAt', $3::bigint) END, updated_at=now() WHERE id=$1", [req.params.id, JSON.stringify({ qariSlugs }), Date.now()]);
      await client.query('COMMIT');
      return res.json({ success: true, total: tracks.length });
    } catch (error) {
      if (client) await client.query('ROLLBACK').catch(() => {});
      console.error('Playlist track update failed:', error.message);
      return res.status(500).json({ error: 'Could not save playlist tracks.' });
    } finally { client?.release(); }
  });
}
