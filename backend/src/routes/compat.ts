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
import { query } from '../db.js';
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
    const defs = CATALOG[collection];
    if (!defs) { notFound(reply, `Collection '${collection}' not found.`); return reply; }

    let userId: string | null = null;
    // 'public' and 'catalog' reads are anonymous-friendly (editorial pulls
    // run signed-out); 'user' and 'social' require a bearer token.
    if (defs.access === 'user' || defs.access === 'social') {
      userId = await bearerUserId(req);
      if (!userId) { awError(reply, 401, 'user_unauthorized', 'Unauthorized.'); return reply; }
    }

    const qs = req.query as Record<string, unknown>;
    // Normal (fastify-parsed) `queries` may be a single param or array; also
    // accept indexed `queries[0]=...` keys straight from the raw querystring.
    const queryValues: unknown[] = [];
    if (Array.isArray(qs.queries)) queryValues.push(...qs.queries);
    else if (typeof qs.queries === 'string') queryValues.push(qs.queries);
    else if (qs.queries !== undefined) queryValues.push(qs.queries);
    for (const [key, value] of Object.entries(qs)) {
      if (/^queries\[\d*\]$/.test(key) && typeof value === 'string') queryValues.push(value);
    }

    const whereOwner = defs.access === 'user' ? 'owner = $1' : null;
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
    const defs = CATALOG[collection];
    if (!defs) { notFound(reply, `Collection '${collection}' not found.`); return reply; }
    if (defs.access === 'public') {
      cannotWrite(reply, `'${collection}' is read-only for clients.`); return reply;
    }
    const userId = await bearerUserId(req);
    if (!userId) { awError(reply, 401, 'user_unauthorized', 'Unauthorized.'); return reply; }

    const body = (req.body ?? {}) as { documentId?: unknown; data?: unknown };
    const rawId = typeof body.documentId === 'string' ? body.documentId.trim() : '';
    const id = !rawId || rawId === 'unique()' ? genId() : rawId;
    if (id.length > 36) { awError(reply, 400, 'document_invalid_structure', 'Document id too long.'); return reply; }
    const data = sanitizeData(body.data);

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
    const defs = CATALOG[collection];
    if (!defs) { notFound(reply, `Collection '${collection}' not found.`); return reply; }

    let userId: string | null = null;
    if (defs.access !== 'public') {
      userId = await bearerUserId(req);
      if (!userId) { awError(reply, 401, 'user_unauthorized', 'Unauthorized.'); return reply; }
    }

    const { rows } = await query<Row>(
      `SELECT id, owner, doc, created_at, updated_at FROM ${collection} WHERE id = $1`,
      [id],
    );
    const row = rows[0];
    if (!row || (defs.access === 'user' && row.owner !== userId)) {
      notFound(reply, `Document '${id}' not found in '${collection}'.`);
      return reply;
    }
    void defs; // (catalog/social reads are open by design)
    return reply.send(toAppwriteDoc(row, collection, db, null));
  });

  // ---------------------------------------------------------- update
  app.patch('/:db/collections/:collection/documents/:id', async (req: Req, reply) => {
    const { db, collection, id } = req.params as Record<string, string>;
    if (!assertDb(db)) { notFound(reply, `Database '${db}' not found.`); return reply; }
    const defs = CATALOG[collection];
    if (!defs) { notFound(reply, `Collection '${collection}' not found.`); return reply; }
    if (defs.access === 'public') {
      cannotWrite(reply, `'${collection}' is read-only for clients.`); return reply;
    }
    const userId = await bearerUserId(req);
    if (!userId) { awError(reply, 401, 'user_unauthorized', 'Unauthorized.'); return reply; }

    const body = (req.body ?? {}) as { data?: unknown };
    const patch = sanitizeData(body.data);
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
    const defs = CATALOG[collection];
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
