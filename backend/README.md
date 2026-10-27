# ghais-api — Ghaith Qur'an backend (replaces Appwrite Cloud)

Fastify 5 + TypeScript + Postgres. Serves the **Appwrite document REST compat
shim** (the mobile app keeps its existing call sites) plus a token auth layer.

## Layout

```
backend/
  src/index.ts            server entry (healthz + route registration)
  src/config.ts           env settings (DATABASE_URL, JWT_SECRET, GOOGLE_*…)
  src/db.ts               pg pool
  src/db/schema.sql       full DDL (users + 21 document tables) — idempotent
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
| `user` | likes, playback_state, follows, listening_stats, schedules, history, routine_backups, user_prefs, khatma_plans, play_events | Bearer; owner-forced |
| `catalog` | playlists, playlist_items | anon read; owner-scoped writes |
| `social` | devotion, leagues, friendships, achievements | auth read-any; write-own/member |

Document payloads live verbatim in JSONB (`doc`), so attribute naming drift
(snake_case vs camelCase) is preserved exactly as the app writes it.

## Deploy on Render

1. Render dashboard -> "New" -> "Blueprint" -> point at `iredox10/ghais`
   (branch with render.yaml). It creates `ghais-api` (web) + `ghais-db` (postgres).
2. Set env vars on the web service: `JWT_SECRET` (generated), `ADMIN_TOKEN`
   (anything strong), `GOOGLE_WEB_CLIENT_ID`
   (`674819710970-0daleci989c7jfgoppplppm29i8md1of.apps.googleusercontent.com`).
3. First build runs `npm run migrate` automatically.
4. Optional data import: run `scripts/export-appwrite.mjs` locally with an
   Appwrite server key, then `DATABASE_URL=<render db> node scripts/seed.mjs`.
5. **Postgres plan**: the Render `free` database expires after ~30 days; switch
   the DB to a paid plan (or export + re-seed) before then. Web `free` plan
   spins down when idle (cold starts ~30s) — upgrade if latency matters.
6. Charts refresh: create a Render CronJob (or any scheduler) hitting
   `POST /v1/jobs/charts-rollup` with `Authorization: Bearer <ADMIN_TOKEN>` daily.

## Decommissioning Appwrite

1. Point the app's endpoint at this API (the app-side change replaces
   `AppwriteConfig.ENDPOINT`; repositories stay untouched thanks to the shim).
2. After a release window with no green traffic: close the Appwrite Cloud
   project (`Settings` -> delete) and drop `io.appwrite` from the Gradle
   version catalog (keep it ONLY while `SyncEngine` still uses the SDK's
   Databases REST client to speak to the shim).
