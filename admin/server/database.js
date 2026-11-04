import pg from 'pg';
import { randomBytes } from 'node:crypto';
import { readFileSync } from 'node:fs';

const COLLECTIONS = new Set(['likes','playback_state','follows','listening_stats','schedules','history','reciter_stats','routine_backups','user_prefs','khatma_plans','broadcasts','play_events','playlists','playlist_items','charts','reciters','devotion','leagues','friendships','global_charts','achievements','hifz_mastery','search_history','telemetry','surahs','moods']);
const NUMERIC = new Set(['played_at_ms','position_ms','ranked_at_ms','position','surah_id','ayah_no','total_seconds','days_streak','minutes_today','unique_surahs','unique_reciters','ms','published_at','publishedAt']);
const notFound = () => Object.assign(new Error('Document not found.'), { code: 404, type: 'document_not_found' });
const table = (name) => { if (!COLLECTIONS.has(name)) throw Object.assign(new Error('Unknown collection.'), { code: 400 }); return `"${name}"`; };
const attr = (name) => {
  if (name === '$id') return 'id';
  if (name === '$createdAt') return 'created_at';
  if (name === '$updatedAt') return 'updated_at';
  if (!/^[a-zA-Z_][a-zA-Z0-9_]*$/.test(name)) throw new Error('Invalid query attribute.');
  return `(doc->>'${name}')`;
};
function parse(queries = []) {
  let limit = 25, offset = 0, order = ''; const params = [], clauses = [];
  for (const raw of queries) {
    const q = typeof raw === 'string' ? JSON.parse(raw) : raw;
    const value = q.values?.[0];
    if (q.method === 'limit' || q.method === 'offset') {
      if (!Number.isSafeInteger(value) || value < (q.method === 'limit' ? 1 : 0) || value > 1000000) throw new Error('Invalid pagination.');
      if (q.method === 'limit') limit = Math.min(value, 1000); else offset = value;
    } else if (q.method === 'orderAsc' || q.method === 'orderDesc') {
      const expression = attr(q.attribute); order = ` ORDER BY ${NUMERIC.has(q.attribute) ? expression+'::numeric' : expression} ${q.method === 'orderAsc' ? 'ASC' : 'DESC'} NULLS LAST`;
    } else if (q.method === 'equal') {
      if (!Array.isArray(q.values) || !q.values.length) throw new Error('Invalid equality query.');
      const placeholders = q.values.map(v => { params.push(String(v)); return `$${params.length}`; });
      clauses.push(`${attr(q.attribute)}::text IN (${placeholders.join(',')})`);
    } else if (q.method === 'greaterThan') {
      params.push(value); clauses.push(`${attr(q.attribute)}${NUMERIC.has(q.attribute) ? '::numeric' : ''} > $${params.length}`);
    } else { throw new Error('Unsupported query.'); }
  }
  return { limit, offset, order, where: clauses.length ? ` WHERE ${clauses.join(' AND ')}` : '', params };
}
const shape = (row, collection) => ({ ...row.doc, $id: row.id, $collectionId: collection, $databaseId: 'ghais', $createdAt: new Date(row.created_at).toISOString(), $updatedAt: new Date(row.updated_at).toISOString() });
const userShape = (row) => ({ $id: row.id, email: row.email, name: row.name, registration: new Date(row.created_at).toISOString(), $createdAt: new Date(row.created_at).toISOString(), status: true, labels: [], prefs: {}, provider: row.provider });

export function createDatabase(connectionString, caPath = process.env.DATABASE_SSL_CA) {
  if (!connectionString) throw new Error('DATABASE_URL is required.');
  const pool = new pg.Pool({ connectionString, ...(caPath ? { ssl: { ca: readFileSync(caPath, 'utf8') } } : {}), max: 5, connectionTimeoutMillis: 10000, statement_timeout: 15000 });
  const database = {
    async listDocuments(_db, collection, queries = []) {
      const t = table(collection), p = parse(queries);
      const [rows, count] = await Promise.all([
        pool.query(`SELECT id,owner,doc,created_at,updated_at FROM ${t}${p.where}${p.order} LIMIT $${p.params.length+1} OFFSET $${p.params.length+2}`, [...p.params,p.limit,p.offset]),
        pool.query(`SELECT count(*)::int AS n FROM ${t}${p.where}`, p.params),
      ]);
      return { total: count.rows[0].n, documents: rows.rows.map(row => shape(row, collection)) };
    },
    async getDocument(_db, collection, id) {
      const r = await pool.query(`SELECT * FROM ${table(collection)} WHERE id=$1`, [id]);
      if (!r.rows.length) throw notFound(); return shape(r.rows[0], collection);
    },
    async createDocument(_db, collection, id, doc) {
      const resolved = !id || id === 'unique()' ? randomBytes(10).toString('hex') : id;
      let owner = doc.user_id ?? doc.userId ?? doc.owner_id ?? doc.ownerId ?? null;
      if (collection === 'playlist_items' && doc.playlist_id) owner = (await pool.query('SELECT owner FROM playlists WHERE id=$1',[doc.playlist_id])).rows[0]?.owner ?? null;
      const r = await pool.query(`INSERT INTO ${table(collection)}(id,owner,doc) VALUES($1,$2,$3) RETURNING *`, [resolved,owner,doc]); return shape(r.rows[0], collection);
    },
    async updateDocument(_db, collection, id, patch) {
      const r = await pool.query(`UPDATE ${table(collection)} SET doc=doc || $2::jsonb,updated_at=now() WHERE id=$1 RETURNING *`, [id,JSON.stringify(patch)]);
      if (!r.rows.length) throw notFound(); return shape(r.rows[0],collection);
    },
    async deleteDocument(_db, collection, id) { const r=await pool.query(`DELETE FROM ${table(collection)} WHERE id=$1`,[id]); if (!r.rowCount) throw notFound(); },
    async listIndexes(_db, collection) { const r=await pool.query('SELECT indexname FROM pg_indexes WHERE schemaname=current_schema() AND tablename=$1',[table(collection).slice(1,-1)]); return { indexes:r.rows.map(x=>({key:x.indexname})) }; },
  };
  const users = {
    async list(queries = []) { const p=parse(queries); if(p.where) throw new Error('User filters unsupported.'); const [r,c]=await Promise.all([pool.query(`SELECT id,email,name,provider,created_at FROM users${p.order} LIMIT $1 OFFSET $2`,[p.limit,p.offset]),pool.query('SELECT count(*)::int AS n FROM users')]);return { users:r.rows.map(userShape), total:c.rows[0].n }; },
    async get(id) { const r=await pool.query('SELECT id,email,name,provider,created_at FROM users WHERE id=$1',[id]);if(!r.rows.length)throw notFound();return userShape(r.rows[0]); },
  };
  return { pool, database, users };
}
