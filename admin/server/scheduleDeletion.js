function logicalScheduleIds(row) {
  const result = new Set();
  if (typeof row.doc?.schedule_id === 'string' && row.doc.schedule_id.trim()) result.add(row.doc.schedule_id.trim());
  try {
    const parsed = JSON.parse(row.doc?.schedule_json || '{}');
    if (typeof parsed.id === 'string' && parsed.id.trim()) result.add(parsed.id.trim());
  } catch { /* A malformed legacy payload still has its document ID. */ }
  return result;
}

export function registerScheduleDeletion(app, pool) {
  app.delete('/api/schedules/:id', async (req, res) => {
    let client;
    try {
      client = await pool.connect();
      await client.query('BEGIN');
      const found = await client.query('SELECT id, owner, doc FROM schedules WHERE id=$1 FOR UPDATE', [req.params.id]);
      if (found.rowCount) {
        const target = found.rows[0];
        const logicalIds = logicalScheduleIds(target);
        const owner = target.owner || target.doc?.user_id;
        const siblings = owner ? await client.query("SELECT id, owner, doc FROM schedules WHERE owner=$1 OR (owner IS NULL AND doc->>'user_id'=$1) FOR UPDATE", [owner]) : { rows: [target] };
        const ids = siblings.rows.filter(row => row.id === target.id || [...logicalScheduleIds(row)].some(id => logicalIds.has(id))).map(row => row.id);
        await client.query("UPDATE schedules SET doc=doc || $2::jsonb, updated_at=now() WHERE id=ANY($1::text[])", [ids, JSON.stringify({deleted:true,enabled:false,deleted_at_ms:Date.now()})]);
      }
      await client.query('COMMIT');
      res.json({success:true});
    } catch (error) {
      if (client) await client.query('ROLLBACK').catch(() => {});
      console.error('Error deleting schedule:', error.message);
      res.status(500).json({success:false,error:'Could not delete schedule.'});
    } finally { client?.release(); }
  });
}
