import 'dotenv/config';
import { createDatabase } from './database.js';
const { pool } = createDatabase(process.env.DATABASE_URL);
try {
  for (const name of ['surahs','moods']) {
    await pool.query(`CREATE TABLE IF NOT EXISTS "${name}" (id TEXT PRIMARY KEY, owner TEXT, doc JSONB NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now())`);
    await pool.query(`CREATE INDEX IF NOT EXISTS "${name}_doc_gin" ON "${name}" USING gin(doc)`);
  }
  console.log('Admin PostgreSQL schema ready.');
} finally { await pool.end(); }
