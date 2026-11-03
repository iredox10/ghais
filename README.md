# Ghaith (Quranify)

A Quran listening, reading, and memorization app built with Kotlin Multiplatform and Compose Multiplatform.

- Quran recitations, ayah repeats, queues, offline audio, and sleep timers
- Arabic text, translations, tafseer, and Mushaf reading
- Hifz practice, Khatma plans, routines, schedules, and ambient audio
- Reciter discovery, favorites, playlists, listening history, and progress

## Architecture

- `shared/`: shared UI, playback logic, backend HTTP client, authentication, and offline-first sync
- `androidApp/`: Android host, Media3 playback, widgets, and alarm scheduling
- `backend/`: Fastify/TypeScript API, PostgreSQL document tables, authentication, and chart/stat jobs
- `Procfile`: Heroku release migration and web process
- `.github/workflows/charts-rollup.yml`: daily chart/stat refresh, after its repository secret is configured

The app no longer depends on the Appwrite SDK or Appwrite Cloud sessions. The Heroku API retains the existing document wire format to preserve stored data and repository behavior. `appwrite/` contains legacy schema/function references; `backend/scripts/export-appwrite.mjs` is the one-time migration reader.

Android is the runnable app in this repository. Shared authentication and sync also target iOS; the iOS host/Xcode project is not included. Native iOS Google sign-in must supply `IosGoogleSignIn.idTokenProvider`. The web target is not configured.

## Build and test

```bash
./gradlew :androidApp:assembleDebug :shared:testDebugUnitTest
cd backend
npm ci
npm run build
npm test
```

See [backend setup](backend/README.md), [Heroku deployment](docs/heroku-deployment.md), and [migration status](docs/render-migration.md) for deployment and data-transfer instructions.

## License

All rights reserved.
