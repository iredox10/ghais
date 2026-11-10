# ghais-api — Ghaith Qur'an backend (replaces Appwrite Cloud)

Fastify 5 + TypeScript + Postgres. Serves the **Appwrite document REST compat
shim** (the mobile app keeps its existing call sites) plus a token auth layer.

## Layout

```
backend/
  src/index.ts            server entry (healthz + route registration)
  src/config.ts           env settings (DATABASE_URL, JWT_SECRET, GOOGLE_*…)
  src/db.ts               pg pool
  src/db/schema.sql       full DDL (users + 24 document tables) — idempotent
  src/tokens.ts           access JWT (jose) + rotating refresh tokens
  src/routes/auth.ts      /v1/auth/{register,login,refresh}
  src/routes/google.ts    /v1/auth/google (on-device Google ID token verify)
  src/routes/compat.ts    Appwrite document REST shim (no app-side changes!)
  src/routes/jobs.ts      /v1/jobs/charts-rollup (ADMIN_TOKEN)
  src/compat/catalog.ts   collection access classes + numeric registry
  src/compat/query.ts     Appwrite Query DSL -> SQL
  scripts/export-appwrite.mjs  one-shot Appwrite Cloud export (server key)
  scripts/seed.mjs             import the export into Postgres
  export/                      dumps land here (gitignored)
```

## Local dev

```bash
npm install
npm run build           # tsc
npm run dev             # tsx watch, http://localhost:8080/healthz
DATABASE_URL=postgres://… npm run migrate   # applies schema.sql
DATABASE_URL=postgres://… node scripts/seed.mjs  # optional import
```

## Auth model

- `POST /v1/auth/register|login` -> `{userId, email, name, accessToken, refreshToken, expiresIn}`
- Access tokens live 15 min (HS256); refresh tokens are 30-day opaque secrets,
  hashed in `refresh_tokens`, and **rotate on every refresh**.
- The bearer header (`Authorization: Bearer <accessToken>`) is what user-scoped
  shim reads/writes check; the `X-Appwrite-Project` header is accepted and ignored.
- Google sign-in: the app requests an ID token on device and posts it to
  `POST /v1/auth/google` — verification is against Google's JWKS with audience
  = GOOGLE_WEB_CLIENT_ID (+ optional GOOGLE_EXTRA_CLIENT_IDS). No client secret.

## Data plane (compat shim)

Same paths/shapes as Appwrite document REST, in database `ghais`:

| Access class | Collections | Client rules |
|---|---|---|
| `public` | charts, broadcasts, reciters, reciter_stats, global_charts | anon read; writes admin-only |
| `user` | likes, playback_state, follows, listening_stats, schedules, history, routine_backups, user_prefs, khatma_plans, play_events, hifz_mastery, search_history, telemetry | Bearer; owner-forced |
| `catalog` | playlists, playlist_items | anon read; owner-scoped writes |
| `social` | devotion, leagues, friendships, achievements | auth read-any; write-own/member |

Document payloads live verbatim in JSONB (`doc`), so attribute naming drift
(snake_case vs camelCase) is preserved exactly as the app writes it.

## Deploy on Heroku

The root `package.json` installs and builds this backend with the Node buildpack. The root `Procfile` runs the database migration in the release phase and starts the web process. See [Heroku deployment](../docs/heroku-deployment.md) for configuration and cutover steps. Configure `DATABASE_SSL_CA=/app/backend/certs/rds-global-bundle.pem` for Heroku Postgres certificate verification. The importer accepts the same environment variable; when running locally, use the absolute path to the local certificate file instead.

## Application client and scheduled jobs

The app uses shared Ktor HTTP code for auth and document sync on Android and iOS. It no longer includes the Appwrite SDK. `BackendConfig.ENDPOINT` points at this API; document paths/shapes remain compatible. iOS Google UI is supplied by the native host through `IosGoogleSignIn.idTokenProvider`.

Copy Heroku’s `ADMIN_TOKEN` into GitHub Actions secret `HEROKU_ADMIN_TOKEN` to enable `.github/workflows/charts-rollup.yml` (daily at 03:00 UTC, plus manual dispatch). Set repository variable `HEROKU_API_URL` if the API uses a different host. The job refreshes both charts and reciter follower/like counts. It replaces the legacy `appwrite/functions/aggregate-charts` function, including per-owner/surah/day deduplication and velocity trending. Play-event `ms` is playback position, not a listening duration or event timestamp. Chart windows use a plausible `playedAtMs` recorded by updated clients; older clients and records without a valid timestamp fall back to server `created_at`, so offline plays from older app versions are dated by upload time.

## Tests

```bash
npm run build
npm test
```

The backend regression suite substitutes database I/O to verify document ownership, membership, guest catalog access, query validation, migration collection coverage, token consumption, and aggregation. It does not replace a deployment smoke test against real PostgreSQL.

## Data cutover

The export/import scripts cover all 24 document collections. Both accept an optional collection allowlist. For a catalog-only transfer:

```bash
node scripts/export-appwrite.mjs charts broadcasts playlists playlist_items reciters reciter_stats global_charts
node scripts/seed.mjs charts broadcasts playlists playlist_items reciters reciter_stats global_charts
```

Provide Appwrite operator credentials only to the exporter and `DATABASE_URL` only to the importer. Existing Appwrite sessions and account passwords are not imported. Private documents preserve their owner IDs: importing them requires matching accounts with the same IDs; a newly registered account receives a new ID and cannot read the previous owner's documents. Do not treat a private-document export as a complete account migration.

After validating the deployed app and any required account/data transfer, retire the Appwrite project. Legacy schema/function files remain as migration references and are not called by the app.
