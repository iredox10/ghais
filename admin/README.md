# Quranify Admin

Quranify Admin connects to the same Heroku PostgreSQL database as the mobile API. Reciter image uploads use Cloudflare R2; existing bundled mobile portraits remain available from the mobile backend. The admin server serves its compiled frontend and API from one origin.

## Configuration

Copy `.env.example` to a private `.env` (preserve any existing file) and set:

- `DATABASE_URL`: the mobile API's Heroku Postgres connection URL. Use `DATABASE_SSL_CA=/app/server/certs/rds-global-bundle.pem` on Heroku to verify its certificate.
- `ADMIN_USERNAME` and `ADMIN_PASSWORD`: unique credentials for the admin. The password must be at least 16 characters. The server protects its UI and management API with HTTP Basic authentication; use HTTPS in production.
- `PUBLIC_API_URL`: the mobile API origin. `MOBILE_API_ADMIN_TOKEN` enables the manual chart refresh and must match the mobile API's `ADMIN_TOKEN`. The mobile API's existing scheduled job continues to run independently.
- `R2_ACCOUNT_ID`, `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY`, and `R2_BUCKET`: scoped R2 credentials. Upload and delete requests remain server-side.
- `ADMIN_PUBLIC_URL`: this admin app's public HTTPS origin. With no R2 custom domain, the app serves uploaded portraits through `/media/reciters/...` without exposing R2 credentials. Only these image reads are public; all management routes require authentication.
- `R2_PUBLIC_URL`: optional public custom-domain origin for direct image delivery. Leave empty until the domain is configured. Existing proxy image URLs remain valid when a domain is added later.

No credential belongs in Git or in a `VITE_` variable. A missing R2 configuration leaves uploads disabled and clearly reports that state.

## Local development

```bash
npm ci
npm run migrate
npm run dev
```

The migration creates the admin's `surahs` and `moods` tables if missing. The other tables must already exist from the mobile API's migration. Vite runs on port 5173 and proxies `/api` to port 5000. Keep `ADMIN_DEV_ORIGIN=http://localhost:5173` only in local development.

## Heroku

The admin is mounted at `/admin` in the existing Ghaith Heroku app and shares its web dyno. The Ghaith release builds the frontend with the `/admin/` base path and runs both backend migrations. Configure the variables above on that app, set `ADMIN_ENABLED=true`, and set `ADMIN_PUBLIC_URL` to `https://ghaith-c77c2426ed4d.herokuapp.com/admin`. A custom R2 domain is optional for initial operation. The standalone `Procfile` remains available for local or separate deployments.

```bash
npm test
npx tsc --noEmit
npm run build
```

Set `TEST_DATABASE_URL` to an isolated loopback PostgreSQL database to run the database integration test. Tests never require R2 credentials.

The former Appwrite maintenance scripts are kept in `legacy/appwrite/` as historical references; they are not part of the running app or the Heroku build. They are not configured for the new backend and should not be run against production.

## Move the existing portraits to R2

After the bucket and admin service are configured, run the following from a trusted machine with database and R2 environment variables set. The first command only reports the candidate count; the second uploads JPEGs and updates records that lack a usable image URL. It does not overwrite existing working image URLs and can be resumed after a failure.

```bash
node scripts/migrate-portraits.js --source ../quranify/backend/assets/reciters
node scripts/migrate-portraits.js --source ../quranify/backend/assets/reciters --execute
```

Keep the Heroku app available while its `/admin/media/reciters/...` proxy URLs are in use. The source directory contains the filtered catalog portraits; generic placeholders were excluded.
