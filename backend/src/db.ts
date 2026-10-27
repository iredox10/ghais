import { Pool, type QueryResultRow } from 'pg';
import { config } from './config.js';

/** Shared pg pool. One pool per process; migrate script closes its own. */
export const pool = new Pool({
  connectionString: config.dbUrl,
  max: 10,
});

/** Small typed query helper. */
export function query<T extends QueryResultRow = QueryResultRow>(
  sql: string,
  params: unknown[] = [],
) {
  return pool.query<T>(sql, params);
}
