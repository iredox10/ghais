// Appwrite document body/row shapes + error helper.
import type { QueryResultRow } from 'pg';

export interface Row extends QueryResultRow {
  id: string;
  owner: string | null;
  doc: SharedDoc;
  created_at: Date;
  updated_at: Date;
}

/** Raw payload as sent by the app (any JSON object). */
export type SharedDoc = Record<string, unknown>;

const iso = (d: Date) => d.toISOString();

/** pg row -> Appwrite-shaped Document JSON. */
export function toAppwriteDoc(
  row: Row,
  collection: string,
  databaseId: string,
  selected: Set<string> | null,
): Record<string, unknown> {
  const out: Record<string, unknown> = {
    $id: row.id,
    $collectionId: collection,
    $databaseId: databaseId,
    $createdAt: iso(row.created_at),
    $updatedAt: iso(row.updated_at),
    $permissions: [],
  };
  for (const [key, value] of Object.entries(row.doc ?? {})) {
    if (key.startsWith('$')) continue;
    if (selected && !selected.has(key)) continue;
    out[key] = value;
  }
  return out;
}

export type AwReply = {
  code(n: number): { send(b: unknown): unknown };
};

/** Appwrite-style error body: {code, type, message} (SDK keys on `code`). */
export function awError(
  reply: AwReply,
  code: number,
  type: string,
  message: string,
): void {
  reply.code(code).send({ code, type, message });
}

/** Strips system keys from an incoming payload; JSONB-safe. */
export function sanitizeData(input: unknown): SharedDoc {
  const out: SharedDoc = {};
  if (input && typeof input === 'object' && !Array.isArray(input)) {
    for (const [key, value] of Object.entries(input as SharedDoc)) {
      if (key.startsWith('$')) continue;
      out[key] = value;
    }
  }
  return out;
}
