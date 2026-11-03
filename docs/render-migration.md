# Appwrite to Heroku migration

Branch: `feature/render-migration`, based on `main` at `b3bfac8`.

The app now targets a custom Fastify API and PostgreSQL on Heroku. The document URL and JSON shapes remain compatible with existing data; the app uses its own shared Ktor transport instead of the Appwrite SDK.

## Implemented

- [x] Fastify API, health check, PostgreSQL schema, Heroku release configuration
- [x] Email/password registration and login, Google ID-token verification
- [x] Access JWTs, persistent refresh tokens, atomic refresh-token consumption, logout revocation
- [x] Shared Android/iOS auth and document HTTP transport, serialized refresh and single retry on 401
- [x] Preserve offline identity on transient refresh failures; clear identity on rejected refresh
- [x] Shared sync engine: favorites, follows, playback, stats, history, schedules, Khatma, Hifz mastery, search history, routines, preferences, play events, and telemetry
- [x] Public catalog, reciter images/statistics, broadcasts, editorial playlists, and charts
- [x] Remove Appwrite SDK, cookie jar, browser OAuth callback, and Cloud endpoint from application code
- [x] All 24 document collections covered by SQL schema and export/import scripts
- [x] User-scoped ownership, social membership checks, playlist item ownership, anonymous catalog document reads
- [x] Chart aggregation parity: UTC-day dedupe, correct yesterday window, 7d/30d rankings, velocity-based trending
- [x] Reciter follower/like count aggregation
- [x] Daily GitHub Actions job, guarded by `HEROKU_ADMIN_TOKEN`
- [x] Regression tests for backend contracts and shared HTTP/token behavior

Historical `appwrite://likes/...` values remain recognized as local placeholders so existing favorites can be restored. They do not make network calls.

## Operational steps still required

- [ ] Restore Heroku and GitHub authentication and verify the deployed release, environment, and database configuration.
- [ ] Publish the committed work and configure automatic Heroku deployment if desired; the scheduled chart workflow must be on the default branch and use `HEROKU_ADMIN_TOKEN`.
- [ ] Decide which existing documents to transfer; run export/import with operator credentials if needed.
- [ ] Test registration, login, Google login, logout, offline restart, sync, catalog, and jobs against the deployed service.
- [ ] Retire Appwrite only after validating the cutover.

The Heroku deployment notes report an existing app and database; this does not establish that the current local commits are deployed. Existing Appwrite cookie sessions cannot authenticate to the new API. The importer does not import accounts or passwords. Private documents retain their original owner IDs, so importing them requires an account migration preserving those IDs; re-registering with a new ID does not attach old cloud data. Catalog-only imports do not have this requirement.

## iOS

Authentication (email/password), token refresh, and sync now share the backend implementation with Android. There is no iOS host/Xcode project in this repository, so iOS build/runtime verification requires a macOS host project. Native Google UI must provide a Google ID token through `IosGoogleSignIn.idTokenProvider`; without it, users receive an explicit email-sign-in fallback message. Other pre-existing iOS features, such as scheduling and voice recording, are separate platform work.

Social collections are available through the backend; local/feature-flagged social functionality is not expanded by this migration.

## Verification

- Android debug APK build passed.
- Shared Kotlin metadata compilation passed; portable replacements preserve the existing cache, sort, filter, and ranking behavior.
- 812 shared/Android unit tests passed, including five backend-client regression tests.
- Backend TypeScript build and seven backend regression tests passed.
- Export/import scripts passed JavaScript syntax checks.

These checks do not exercise a deployed PostgreSQL service or an iOS host app.
