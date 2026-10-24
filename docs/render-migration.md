# Render Migration — Ghaith backend off Appwrite Cloud

Branch: `feature/render-migration` (base `main` @ `b3bfac8`)
Strategy: **one custom Fastify/Node API + Render Postgres** replaces Appwrite Cloud
(project `tikitakatalk`, region `fra`, DB `ghais`). Self-hosting Appwrite on Render
was evaluated and rejected (multi-container, unsupported).

Key constraint driving the design: the app speaks raw Appwrite REST over Ktor
(`X-Appwrite-Project` header, `/v1/databases/{db}/collections/{c}/documents`).
We keep those call sites UNCHANGED by serving a **document-compat shim** on the
new API. Auth is rewritten to tokens (Appwrite cookie sessions don't map).

## Phase 0 — Inventory (done during planning)
- [x] Transport: raw Appwrite REST over Ktor (commonMain repositories)
- [x] Auth: `io.appwrite:sdk-for-kotlin` 22.0.0 in androidMain; email+password + Google ID-token exchange (`/account/sessions/id-token`, persistent cookie jar); iOS actual mirrors
- [x] Collections (DB `ghais`): charts, broadcasts, editorial (+playlist_items), reciters (+visibility), reciter_stats, followed_reciters, devotion, leagues, friendships, global_charts, achievements, favorites, play_events (+ per-store: hifz_mastery, schedules, custom_routines, onboarding, user_playlists, users)
- [x] No Appwrite Functions, no Storage buckets → document DB + auth is the full surface
- [x] Deep link: `appwrite-callback-tikitakatalk` scheme in manifest (OAuth bounce) — replaced by `ghais://auth` only if a browser dance remains needed; native ID-token flow needs no bounce

## Phase 1 — Backend scaffold (`/backend`)
- [ ] Fastify + TypeScript + `pg`, ESM, tsx dev, tsc build; `/healthz`
- [ ] `render.yaml` blueprint (web service + postgres, build runs `migrate`)
- [ ] Deps pre-installed for all later units: pg, jose, bcryptjs, zod, cors
- [ ] Stub route modules pre-registered in `src/index.ts` (agents fill files, never touch index.ts)

## Phase 2 — Server units (parallel subagents)
- [ ] **U2 Schema**: `src/db/schema.sql` + `migrate.ts` — typed columns per collection + owner/`created_at`/`updated_at`, users table
- [ ] **U3 Auth**: email+password register/login, bcrypt, access JWT (jose) + revocable refresh (hashed in DB), `/v1/auth/*`
- [ ] **U4 Google**: `POST /v1/auth/google` verifying on-device Google ID token via JWKS (audience = web client ID set), user upsert/link — no client secret needed
- [ ] **U5 Compat shim**: Appwrite document REST shape → SQL; `X-Appwrite-Project` gate; public vs user-scoped collections; Bearer JWT scoping
- [ ] **U6 Public data**: tables/seed for charts, broadcasts, editorial, reciters (+visibility, reciter_stats), charts-rollup job route
- [ ] **U7 Query DSL**: Appwrite `queries[]` JSON → SQL translation for every query pattern the stores use (test script)
- [ ] **U8 Social**: devotion/leagues/friendships/global_charts/achievements shape fit + server helpers (`SocialContracts` reuse)

## Phase 3 — App side
- [ ] **U9**: `BackendConfig` (endpoint), AuthRepository rewrite to token flow (Android + iOS actuals, no SDK, no cookie jar), settings-based token storage, manifest scheme swap, drop `io.appwrite` dependency
- [ ] Build app (`assembleDebug`/`assembleRelease`) green on the branch
- [ ] **U10**: Appwrite export/seed script for editorial/catalog content + decommission notes

## Phase 4 — Cutover & decommission
- [ ] Manual: user connects repo to Render (blueprint), flips plans (free Postgres expires ~30 days), sets secrets
- [ ] Push branch → PR → merge → deploy
- [ ] One-shot data carry-over only if user wants anything from Appwrite (default: start clean)
- [ ] Remove `AppwriteConfig` remnants; close Appwrite project

## Rules
- Coordinator builds + commits (micro commit per unit); subagents never run git
- Never commit `androidApp/build.gradle.kts` (user's local file)
- File ownership is disjoint per unit; `src/index.ts`/`package.json` frozen after U1
