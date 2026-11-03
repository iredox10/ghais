// Appwrite Query DSL -> SQL translation (over the JSONB `doc` column).
//
// Queries arrive as Appwrite method-JSON, either as one JSON array string,
// repeated `queries=<json>` params, or indexed `queries[0]=<json>` keys.
// Supported methods (everything the app actually uses, plus safe extras):
// equal, notEqual, lessThan, lessThanEqual, greaterThan, greaterThanEqual,
// between, orderAsc, orderDesc, limit, offset, select.
import { CATALOG, isAttr, type CollectionDef } from './catalog.js';

type RawQuery = { method: string; attribute?: unknown; values?: unknown; value?: unknown };

export type ParsedListSql = {
  where: string;
  params: unknown[];
  orderBy: string;
  limit: number;
  offset: number;
  select: Set<string> | null;
};

const textOf = (v: unknown): string => String(v);
/** Normalizes every supported `queries` request shape into RawQuery[]. */
export function parseQueries(raw: unknown): RawQuery[] {
  const chunks: string[] = [];
  const push = (v: unknown) => {
    if (typeof v === 'string' && v.trim()) chunks.push(v);
  };
  if (Array.isArray(raw)) raw.forEach(push);
  else push(raw);
  const out: RawQuery[] = [];
  for (const chunk of chunks) {
    const trimmed = chunk.trim();
    if (trimmed.startsWith('[')) {
      // A JSON array of JSON-string-encoded queries (Appwrite native shape).
      try {
        const arr = JSON.parse(trimmed) as unknown[];
        for (const element of arr) {
          if (typeof element === 'string') out.push(JSON.parse(element) as RawQuery);
          else if (element && typeof element === 'object') out.push(element as RawQuery);
        }
        continue;
      } catch {
        throw new Error('Malformed queries parameter.');
      }
    }
    // A single JSON-encoded query object.
    try {
      const obj = JSON.parse(trimmed) as unknown;
      if (obj && typeof obj === 'object') out.push(obj as RawQuery);
    } catch {
      throw new Error('Malformed queries parameter.');
    }
  }
  return out;
}

/**
 * SQL expression for a comparison against `attribute` (identifier-checked).
 * `$id` -> id column; `$createdAt`/`$updatedAt` -> timestamp columns;
 * everything else -> the JSONB path. Throws on unsafe attribute names.
 */
export function attrSql(attr: string): string {
  if (attr === '$id') return 'id';
  if (attr === '$createdAt') return 'created_at';
  if (attr === '$updatedAt') return 'updated_at';
  if (!isAttr(attr)) throw new Error('4001');
  return `(doc->>'${attr}')`;
}

export function buildList(
  defs: CollectionDef,
  rawQueries: unknown,
  whereOwner: string | null,
  ownerParam: unknown,
): ParsedListSql {
  const queries = parseQueries(rawQueries);
  const params: unknown[] = [];
  const clauses: string[] = [];
  let orderBy = '';
  let limit = 25;
  let offset = 0;
  let select: Set<string> | null = null;

  const bind = (value: unknown): string => {
    params.push(value);
    return `$${params.length}`;
  };

  if (whereOwner) {
    clauses.push(whereOwner);
    if (ownerParam !== undefined) params.push(ownerParam);
  }

  for (const q of queries) {
    const method = String(q.method ?? '');
    // Only methods that look at an attribute validate one; limit/offset/
    // select legitimately carry no attribute at all.
    const attr = typeof q.attribute === 'string' ? q.attribute : '';
    const needsAttr = ['equal','notEqual','lessThan','lessThanEqual','greaterThan','greaterThanEqual','between','orderAsc','orderDesc'].includes(method);
    const lhs = needsAttr ? attrSql(attr) : '';

    const values = Array.isArray(q.values) ? q.values : [];
    const single = values.length > 0 ? values[0] : q.value;
    const numeric = defs.numeric.includes(attr);

    switch (method) {
      case 'equal': {
        if (attr === '') throw new Error('equal: missing attribute');
        if (values.length === 0) continue;
        const eq = values.map((v) => `${lhs} = ${bind(textOf(v))}`).join(' OR ');
        clauses.push(values.length > 1 ? `(${eq})` : eq);
        break;
      }
      case 'notEqual': {
        if (attr === '') throw new Error('notEqual: missing attribute');
        clauses.push(`${lhs} <> ${bind(textOf(single))}`);
        break;
      }
      case 'lessThan':
      case 'lessThanEqual':
      case 'greaterThan':
      case 'greaterThanEqual': {
        if (attr === '') throw new Error(`${method}: missing attribute`);
        const op =
          method === 'lessThan' ? '<' : method === 'lessThanEqual' ? '<=' : method === 'greaterThan' ? '>' : '>=';
        clauses.push(
          numeric
            ? `(${lhs}::numeric ${op} ${bind(Number(single))})`
            : `(${lhs} ${op} ${bind(textOf(single))})`,
        );
        break;
      }
      case 'between': {
        if (attr === '' || values.length !== 2) continue;
        const lo = values[0];
        const hi = values[1];
        clauses.push(
          numeric
            ? `(${lhs}::numeric BETWEEN ${bind(Number(lo))} AND ${bind(Number(hi))})`
            : `(${lhs} BETWEEN ${bind(textOf(lo))} AND ${bind(textOf(hi))})`,
        );
        break;
      }
      case 'orderAsc':
      case 'orderDesc': {
        if (attr === '') continue;
        const dir = method === 'orderAsc' ? 'ASC' : 'DESC';
        orderBy = `${defs.numeric.includes(attr) ? `${lhs}::numeric` : lhs} ${dir} NULLS LAST`;
        break;
      }
      case 'limit':
        limit = Number(single);
        if (!Number.isSafeInteger(limit) || limit < 1) throw new Error('Invalid limit');
        break;
      case 'offset':
        offset = Number(single);
        if (!Number.isSafeInteger(offset) || offset < 0) throw new Error('Invalid offset');
        break;
      case 'select': {
        const fields = values.filter((v) => typeof v === 'string') as string[];
        select = new Set(fields);
        break;
      }
      default:
        // Unknown method: ignore (matches this app's tolerant usage).
        break;
    }
  }

  const where = clauses.length > 0 ? `WHERE ${clauses.join(' AND ')}` : '';
  return { where, params, orderBy: orderBy ? ` ORDER BY ${orderBy}` : '', limit, offset, select };
}
