// Applies src/db/schema.sql idempotently. Render buildCommand runs this.
// Owned later by Unit 2 (schema), but the pattern is final: one file,
// CREATE ... IF NOT EXISTS everywhere so re-runs are safe.
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { readFile } from 'node:fs/promises';
import { pool } from '../db.js';

export async function applySchema(): Promise<void> {
  const dir = path.dirname(fileURLToPath(import.meta.url));
  const sql = await readFile(path.join(dir, 'schema.sql'), 'utf8');
  await pool.query(sql);
}

const thisFile = fileURLToPath(import.meta.url);
const invoked = process.argv[1] ? path.resolve(process.argv[1]) : '';
if (invoked === thisFile) {
  applySchema()
    .then(() => pool.end())
    .catch((e) => {
      console.error(e);
      process.exit(1);
    });
}
