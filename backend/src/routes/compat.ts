// Appwrite document REST compat shim.
//
// The app's repositories speak raw Appwrite REST (SDK `Databases` + hand-rolled
// Ktor), so this layer re-implements the document surface over the Postgres
// tables with the SAME paths/shapes:
//   GET    /v1/databases/{db}/collections/{c}/documents        -> {total, documents}
//   POST   /v1/databases/{db}/collections/{c}/documents        -> Document (201)
//   GET    /v1/databases/{db}/collections/{c}/documents/{id}   -> Document
//   PATCH  /v1/databases/{db}/collections/{c}/documents/{id}   -> Document
//   DELETE /v1/databases/{db}/collections/{c}/documents/{id}   -> 204
// Errors carry Appwrite's {code,type,message} body (the SDK keys on `code`).
// Duplicate ids on create return 409 so the app's create->catch->update
// upsert pattern keeps working unchanged.
//
// Identity: regular `Authorization: Bearer <access token>` (NOT Appwrite
// session cookies); X-Appwrite-Project is accepted and ignored.
import type { FastifyInstance, FastifyReply, FastifyRequest } from 'fastify';
import { randomBytes } from 'node:crypto';
import { query, pool } from '../db.js';
import { mergeListeningStats } from '../stats.js';
import { verifyAccessToken } from '../tokens.js';
import { CATALOG, DATABASE_ID, type CollectionDef } from '../compat/catalog.js';
import { awError, sanitizeData, toAppwriteDoc, type Row, type SharedDoc } from '../compat/shape.js';
import { buildList } from '../compat/query.js';

type Req<R extends Record<string, unknown> = Record<string, unknown>> = FastifyRequest<R>;

const genId = (): string => randomBytes(10).toString('hex'); // 20 hex chars, Appwrite-length

async function bearerUserId(req: Req): Promise<string | null> {
  const header = req.headers.authorization ?? '';
  const token = header.startsWith('Bearer ') ? header.slice(7) : '';
  if (!token) return null;
  const claims = await verifyAccessToken(token);
  return claims?.userId ?? null;
}

function notFound(reply: FastifyReply, message: string): void {
  awError(reply, 404, 'document_not_found', message);
}

function cannotWrite(reply: FastifyReply, message: string): void {
  awError(reply, 403, 'general_forbidden', message);
}

// The visibility of an item is inherited from its parent playlist. Missing
// parents and absent/malformed public flags are private by default.
function catalogReadFilter(collection: string): string {
  const visible = "(p.doc->'is_public' = 'true'::jsonb OR p.owner = $1)";
  return collection === 'playlists'
    ? `(doc->'is_public' = 'true'::jsonb OR owner = $1)`
    : `EXISTS (SELECT 1 FROM playlists p WHERE p.id = playlist_items.doc->>'playlist_id' AND ${visible})`;
}

/** Ad-hoc social write authorization: own docs, or referenced members. */
function socialCanWrite(def: CollectionDef, row: Row, userId: string): boolean {
  if (row.owner === userId) return true;
  const doc = row.doc as SharedDoc;
  for (const attr of def.ownerAttrs) {
    const v = doc[attr];
    if (typeof v === 'string' && v === userId) return true;
    if (Array.isArray(v) && v.includes(userId)) return true;
  }
  return false;
}

export default async function compatRoutes(app: FastifyInstance) {
  const assertDb = (db: string): boolean => db === DATABASE_ID;

  // ------------------------------------------------------------ list
  app.get('/:db/collections/:collection/documents', async (req: Req, reply) => {
    const { db, collection } = req.params as Record<string, string>;
    if (!assertDb(db)) { notFound(reply, `Database '${db}' not found.`); return reply; }
    const defs = Object.hasOwn(CATALOG, collection) ? CATALOG[collection] : undefined;
    if (!defs) { notFound(reply, `Collection '${collection}' not found.`); return reply; }

    let userId: string | null = null;
    // 'public' and 'catalog' reads are anonymous-friendly (editorial pulls
    // run signed-out); 'user' and 'social' require a bearer token.
    if (defs.access === 'user' || defs.access === 'social') {
      userId = await bearerUserId(req);
      if (!userId) { awError(reply, 401, 'user_unauthorized', 'Unauthorized.'); return reply; }
    } else if (defs.access === 'catalog') {
      userId = await bearerUserId(req);
    }

    const qs = req.query as Record<string, unknown>;
    // Normal (fastify-parsed) `queries` may be a single param or array; also
    // accept indexed `queries[0]=...` keys straight from the raw querystring.
    const queryValues: unknown[] = [];
    if (Array.isArray(qs.queries)) queryValues.push(...qs.queries);
    else if (typeof qs.queries === 'string') queryValues.push(qs.queries);
    else if (qs.queries !== undefined) queryValues.push(qs.queries);
    for (const [key, value] of Object.entries(qs)) {
      if (/^queries\[\d*\]$/.test(key)) {
        if (typeof value === 'string') queryValues.push(value);
        else if (Array.isArray(value)) queryValues.push(...value);
      }
    }

    const whereOwner = defs.access === 'user' ? 'owner = $1' : defs.access === 'catalog' ? catalogReadFilter(collection) : null;
    let list;
    try {
      list = buildList(defs, queryValues, whereOwner, userId);
    } catch {
      awError(reply, 400, 'general_argument', 'Malformed queries.');
      return reply;
    }
    if (list.limit > 1000) list.limit = 1000;

    try {
      const countSql = `SELECT count(*)::bigint AS n FROM ${collection} ${list.where}`;
      const rows = await query<Row>(
        `SELECT id, owner, doc, created_at, updated_at FROM ${collection} ${list.where}${list.orderBy} LIMIT $${list.params.length + 1} OFFSET $${list.params.length + 2}`,
        [...list.params, list.limit, list.offset],
      );
      const total = (await query<{ n: string }>(countSql, list.params)).rows[0]?.n ?? '0';
      const documents = rows.rows.map((r) => toAppwriteDoc(r, collection, db, list.select));
      return reply.send({ total: Number(total), documents });
    } catch (e) {
      app.log.warn({ e, collection }, 'compat list failed');
      awError(reply, 500, 'server_error', 'Query failed.');
      return reply;
    }
  });

  // ---------------------------------------------------------- create
  app.post('/:db/collections/:collection/documents', async (req: Req, reply) => {
    const { db, collection } = req.params as Record<string, string>;
    if (!assertDb(db)) { notFound(reply, `Database '${db}' not found.`); return reply; }
    const defs = Object.hasOwn(CATALOG, collection) ? CATALOG[collection] : undefined;
    if (!defs) { notFound(reply, `Collection '${collection}' not found.`); return reply; }
    if (defs.access === 'public') {
      cannotWrite(reply, `'${collection}' is read-only for clients.`); return reply;
    }
    const userId = await bearerUserId(req);
    if (!userId) { awError(reply, 401, 'user_unauthorized', 'Unauthorized.'); return reply; }

    const body = (req.body ?? {}) as { documentId?: unknown; data?: unknown };
    const rawId = typeof body.documentId === 'string' ? body.documentId.trim() : '';
    const id = !rawId || rawId === 'unique()' ? genId() : rawId;
    if (id.length > 256) { awError(reply, 400, 'document_invalid_structure', 'Document id too long (maximum 256 characters).'); return reply; }
    let data = sanitizeData(body.data);
    if (collection === 'listening_stats') { delete data.counter_state; data = mergeListeningStats({}, data); }
    if (defs.access === 'user' || collection === 'playlists') {
      for (const attr of defs.ownerAttrs) data[attr] = userId;
    }
    if (defs.access === 'social' && !socialCanWrite(defs, { owner: null, doc: data } as Row, userId)) {
      cannotWrite(reply, 'You must be a document member.'); return reply;
    }
    if (collection === 'playlist_items') {
      const parent = await query<Row>('SELECT owner FROM playlists WHERE id = $1', [data.playlist_id]);
      if (parent.rows[0]?.owner !== userId) {
        cannotWrite(reply, 'You do not own this playlist.'); return reply;
      }
    }

    try {
      const { rows } = await query<Row>(
        `INSERT INTO ${collection}(id, owner, doc) VALUES ($1,$2,$3)
         RETURNING id, owner, doc, created_at, updated_at`,
        [id, userId, data],
      );
      return reply.code(201).send(toAppwriteDoc(rows[0], collection, db, null));
    } catch (e) {
      const msg = String((e as { message?: string }).message ?? e);
      if (/duplicate key|unique constraint/i.test(msg)) {
        awError(reply, 409, 'document_already_exists', `Document '${id}' already exists in '${collection}'.`);
        return reply;
      }
      app.log.warn({ e, collection }, 'compat create failed');
      awError(reply, 500, 'server_error', 'Create failed.');
      return reply;
    }
  });

  // ------------------------------------------------------------ read
  app.get('/:db/collections/:collection/documents/:id', async (req: Req, reply) => {
    const { db, collection, id } = req.params as Record<string, string>;
    if (!assertDb(db)) { notFound(reply, `Database '${db}' not found.`); return reply; }
    const defs = Object.hasOwn(CATALOG, collection) ? CATALOG[collection] : undefined;
    if (!defs) { notFound(reply, `Collection '${collection}' not found.`); return reply; }

    let userId: string | null = null;
    if (defs.access === 'user' || defs.access === 'social') {
      userId = await bearerUserId(req);
      if (!userId) { awError(reply, 401, 'user_unauthorized', 'Unauthorized.'); return reply; }
    } else if (defs.access === 'catalog') {
      userId = await bearerUserId(req);
    }

    const { rows } = await query<Row>(
      `SELECT id, owner, doc, created_at, updated_at FROM ${collection} WHERE id = $1${defs.access === 'catalog' ? ` AND ${catalogReadFilter(collection).replaceAll('$1', '$2')}` : ''}`,
      defs.access === 'catalog' ? [id, userId] : [id],
    );
    const row = rows[0];
    if (!row || (defs.access === 'user' && row.owner !== userId)) {
      notFound(reply, `Document '${id}' not found in '${collection}'.`);
      return reply;
    }
    return reply.send(toAppwriteDoc(row, collection, db, null));
  });

  // ---------------------------------------------------------- update
  app.patch('/:db/collections/:collection/documents/:id', async (req: Req, reply) => {
    const { db, collection, id } = req.params as Record<string, string>;
    if (!assertDb(db)) { notFound(reply, `Database '${db}' not found.`); return reply; }
    const defs = Object.hasOwn(CATALOG, collection) ? CATALOG[collection] : undefined;
    if (!defs) { notFound(reply, `Collection '${collection}' not found.`); return reply; }
    if (defs.access === 'public') {
      cannotWrite(reply, `'${collection}' is read-only for clients.`); return reply;
    }
    const userId = await bearerUserId(req);
    if (!userId) { awError(reply, 401, 'user_unauthorized', 'Unauthorized.'); return reply; }

    const body = (req.body ?? {}) as { data?: unknown };
    const patch = sanitizeData(body.data);
    if (collection === 'listening_stats') delete patch.counter_state;
    if (defs.access === 'user' || collection === 'playlists') {
      for (const attr of defs.ownerAttrs) patch[attr] = userId;
    }
    if (collection === 'playlist_items' && patch.playlist_id !== undefined) {
      const parent = await query<Row>('SELECT owner FROM playlists WHERE id = $1', [patch.playlist_id]);
      if (parent.rows[0]?.owner !== userId) {
        cannotWrite(reply, 'You do not own this playlist.'); return reply;
      }
    }
    if (Object.keys(patch).length === 0) {
      awError(reply, 400, 'document_invalid_structure', 'No data to update.');
      return reply;
    }

    const { rows } = await query<Row>(
      `SELECT id, owner, doc, created_at, updated_at FROM ${collection} WHERE id = $1`,
      [id],
    );
    const existing = rows[0];
    if (!existing) {
      notFound(reply, `Document '${id}' not found in '${collection}'.`);
      return reply;
    }
    if (defs.access !== 'social' && existing.owner !== userId) {
      awError(reply, 403, 'general_forbidden', 'You do not own this document.');
      return reply;
    }
    if (defs.access === 'social' && !socialCanWrite(defs, existing, userId)) {
      awError(reply, 403, 'general_forbidden', 'You do not own this document.');
      return reply;
    }

    if (collection === 'listening_stats') {
      const client = await pool.connect();
      try {
        await client.query('BEGIN');
        const locked = await client.query<Row>('SELECT * FROM listening_stats WHERE id=$1 AND owner=$2 FOR UPDATE', [id, userId]);
        if (!locked.rows[0]) { await client.query('ROLLBACK'); notFound(reply, 'Stats document not found.'); return reply; }
        const merged = mergeListeningStats(locked.rows[0].doc, patch);
        const updated = await client.query<Row>('UPDATE listening_stats SET doc=$2, updated_at=now() WHERE id=$1 RETURNING *', [id, merged]);
        await client.query('COMMIT');
        return reply.send(toAppwriteDoc(updated.rows[0], collection, db, null));
      } catch (error) { await client.query('ROLLBACK'); throw error; }
      finally { client.release(); }
    }

    const { rows: updated } = await query<Row>(
      `UPDATE ${collection} SET doc = doc || $2, updated_at = now() WHERE id = $1
       RETURNING id, owner, doc, created_at, updated_at`,
      [id, patch],
    );
    return reply.send(toAppwriteDoc(updated[0], collection, db, null));
  });

  // ---------------------------------------------------------- delete
  app.delete('/:db/collections/:collection/documents/:id', async (req: Req, reply) => {
    const { db, collection, id } = req.params as Record<string, string>;
    if (!assertDb(db)) { notFound(reply, `Database '${db}' not found.`); return reply; }
    const defs = Object.hasOwn(CATALOG, collection) ? CATALOG[collection] : undefined;
    if (!defs) { notFound(reply, `Collection '${collection}' not found.`); return reply; }
    if (defs.access === 'public') {
      cannotWrite(reply, `'${collection}' is read-only for clients.`); return reply;
    }
    const userId = await bearerUserId(req);
    if (!userId) { awError(reply, 401, 'user_unauthorized', 'Unauthorized.'); return reply; }

    const { rows } = await query<Row>(
      `SELECT id, owner, doc, created_at, updated_at FROM ${collection} WHERE id = $1`,
      [id],
    );
    const existing = rows[0];
    if (!existing) {
      notFound(reply, `Document '${id}' not found in '${collection}'.`);
      return reply;
    }
    if (defs.access !== 'social' && existing.owner !== userId) {
      awError(reply, 403, 'general_forbidden', 'You do not own this document.');
      return reply;
    }
    if (defs.access === 'social' && !socialCanWrite(defs, existing, userId)) {
      awError(reply, 403, 'general_forbidden', 'You do not own this document.');
      return reply;
    }

    await query(`DELETE FROM ${collection} WHERE id = $1`, [id]);
    return reply.code(204).send();
  });
}
