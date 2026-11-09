-- Ghaiss Postgres schema (replaces Appwrite Cloud DB `ghais`).
-- Idempotent: safe to re-run (`npm run migrate` runs this whole file).
--
-- Design:
-- - `users` + `refresh_tokens` back the token auth (email/password + Google).
-- - Every Appwrite collection gets a document table with the SAME name:
--     id          = Appwrite $id (TEXT PK)
--     owner       = authenticated owner user id (NULL for seeded/admin docs)
--     doc         = full attribute payload verbatim (JSONB) — attribute names
--                   are kept exactly as the app sends them (snake_case for
--                   likes/history/etc, camelCase for devotion/leagues/play_
--                   events/charts), so the compat shim needs no per-collection
--                   name translation.
--     created_at  = $createdAt ; updated_at = $updatedAt
-- - `playback_state/listening_stats/devotion` use docId = userId (owner).
-- - Hot read paths get expression indexes (history recency ordering).

-- ---------------------------------------------------------------- auth ----
CREATE TABLE IF NOT EXISTS users (
  id           TEXT PRIMARY KEY,
  email        TEXT UNIQUE NOT NULL,
  password_hash TEXT,
  name         TEXT NOT NULL DEFAULT '',
  provider     TEXT NOT NULL DEFAULT 'email',
  google_sub   TEXT UNIQUE,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
  token_hash TEXT PRIMARY KEY,
  user_id    TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  expires_at TIMESTAMPTZ NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS refresh_tokens_user_idx ON refresh_tokens(user_id);

-- ------------------------------------------------------- document tables ----
DO $$
DECLARE
  c TEXT;
BEGIN
  FOREACH c IN ARRAY ARRAY[
    'likes','playback_state','follows','listening_stats','schedules',
    'history','reciter_stats','routine_backups','user_prefs','khatma_plans',
    'broadcasts','play_events','playlists','playlist_items','charts','reciters',
    'devotion','leagues','friendships','global_charts','achievements',
    'hifz_mastery','search_history','telemetry','moods','surahs'
  ] LOOP
    EXECUTE format(
      'CREATE TABLE IF NOT EXISTS %I (
         id         TEXT PRIMARY KEY,
         owner      TEXT,
         doc        JSONB NOT NULL,
         created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
         updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
       )',
      c
    );
    EXECUTE format(
      'CREATE INDEX IF NOT EXISTS %I ON %I(owner)',
      c || '_owner_idx', c
    );
    EXECUTE format(
      'CREATE INDEX IF NOT EXISTS %I ON %I USING gin(doc)',
      c || '_doc_gin', c
    );
  END LOOP;
END $$;

-- History pull: newest-first per user (played_at_ms is numeric ms).
CREATE INDEX IF NOT EXISTS history_user_recency_idx
  ON history (owner, ((doc->>'played_at_ms')::numeric) DESC);

-- reciter_stats paginated list by slug; single-doc reads also hit id.
CREATE INDEX IF NOT EXISTS reciters_slug_idx ON reciters (owner, ((doc->>'slug')));

-- Editorial catalog: is_public equality is served by the doc GIN index;
-- playlist items are fetched by playlist_id + ordered by numeric position.
CREATE INDEX IF NOT EXISTS playlist_items_playlist_idx
  ON playlist_items (((doc->>'playlist_id')), ((doc->>'position')::numeric));
