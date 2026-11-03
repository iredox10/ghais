# Run and verify locally

The local API uses `http://127.0.0.1:18080`; PostgreSQL uses `127.0.0.1:15439`. Use an isolated local test database.

## Backend

Install and start PostgreSQL locally, create an isolated database and user matching `backend/.env.local.example`, then from the project directory:

```bash
cp backend/.env.local.example backend/.env
cd backend
npm ci
npm run migrate
npm run build
npm start
```

Preserve any existing `.env`; the local example is only for an isolated test database.

In another terminal, from `backend/`:

```bash
npm run smoke:local
npm run seed:local
```

`smoke:local` runs 69 real HTTP/PostgreSQL checks across all 24 document collections. It tests registration/login, owner isolation, query ordering/pagination, playlists, social membership, charts, counters, refresh-token races, and logout. It creates temporary users and deletes their documents afterward. It only accepts loopback API/database addresses. `seed:local` creates a small public catalog and a reusable local test account:

- Email: `local@quranify.test`
- Password: `QuranifyLocal2026!`

Local test keys and credentials in the example are for this isolated environment.

## Android emulator

Start an emulator, then run from the project directory:

```bash
adb reverse tcp:18080 tcp:18080
./gradlew :androidApp:installDebug -Pghais.backendUrl=http://127.0.0.1:18080/v1
adb shell am start -n com.ghais.android/.MainActivity
```

The `ghais.backendUrl` option applies only to debug builds and only accepts local addresses ending in `/v1`. HTTP access is enabled in the debug manifest. Release builds use the Heroku URL. Keep the API running while testing. The app still fetches Quran audio, text, and images from their usual external providers.

## Automated checks

```bash
./gradlew :androidApp:assembleDebug :shared:testDebugUnitTest -Pghais.backendUrl=http://127.0.0.1:18080/v1
cd backend
npm run build
npm test
npm run smoke:local
```

Google sign-in requires a Google account on the emulator/device and matching Google OAuth configuration; the local test account uses email/password.
