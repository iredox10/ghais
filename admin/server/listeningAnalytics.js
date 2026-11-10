// History is a bounded per-device/user snapshot, not a complete event log.
// Aggregate all saved entries rather than claiming a 200-row sample is global.
export async function getListeningAnalytics(pool) {
  if (!pool) throw new Error('PostgreSQL is not configured.');
  const result = await pool.query(`
    WITH parsed AS (
      SELECT CASE WHEN doc->>'played_at_ms' ~ '^[0-9]{13}$'
        THEN (doc->>'played_at_ms')::bigint ELSE NULL END AS ms
      FROM history
    )
    SELECT CASE WHEN ms BETWEEN 946684800000 AND 4102444800000
      THEN EXTRACT(HOUR FROM to_timestamp(ms / 1000.0) AT TIME ZONE 'UTC')::int
      ELSE NULL END AS hour, count(*)::bigint AS count
    FROM parsed GROUP BY 1
  `);
  const buckets = [0, 0, 0, 0, 0, 0];
  let skipped = 0;
  for (const row of result.rows) {
    const count = Number(row.count);
    if (row.hour === null) { skipped += count; continue; }
    const hour = Number(row.hour);
    const bucket = hour >= 3 && hour <= 5 ? 0 : hour >= 6 && hour <= 11 ? 1 :
      hour >= 12 && hour <= 15 ? 2 : hour >= 16 && hour <= 19 ? 3 :
      hour >= 20 && hour <= 22 ? 4 : 5;
    buckets[bucket] += count;
  }
  const labels = ['03:00–05:59', '06:00–11:59', '12:00–15:59', '16:00–19:59', '20:00–22:59', '23:00–02:59'];
  const maximum = Math.max(0, ...buckets);
  return {
    source: 'saved-history', timezone: 'UTC',
    sampleSize: buckets.reduce((sum, count) => sum + count, 0), skipped,
    peakHours: labels.map((hour, i) => ({ hour, count: buckets[i], index: maximum ? Math.round(100 * buckets[i] / maximum) : 0 })),
    retention: null, deviceDistribution: [], reciterStyleDistribution: [],
  };
}
