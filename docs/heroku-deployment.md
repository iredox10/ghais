# Heroku and PostgreSQL

The `ghaith` Heroku app runs the backend at `https://ghaith-c77c2426ed4d.herokuapp.com`. Heroku Postgres Essential-0 is attached. The root `package.json` and `Procfile` use Heroku's native Node buildpack; the release command applies the schema. Database connections verify the RDS certificate using `backend/certs/rds-global-bundle.pem`.

The Android production endpoint targets the Heroku API. A debug APK was built and installed on the emulator. The GitHub chart workflow targets Heroku and its admin secret is configured, but the workflow and other local changes still need to be published to GitHub. Automatic GitHub-to-Heroku deployment is not configured.

The Appwrite data export has not run. Add a server key with `databases.read` permission to `APPWRITE_API_KEY` in the private, Git-ignored `backend/.env.heroku`. Existing Appwrite accounts and private documents need an identity-preserving migration; importing documents alone does not migrate passwords or reconnect accounts.

On 2026-11-03, a read-only check found the app healthy, one Basic web dyno and one Essential-0 PostgreSQL database. The database contained 242 reciter records, one chart record, and no users, playlists, or play events. This is a snapshot, not proof that all catalog data has been migrated. The deployed release was v10 from 2026-10-29; later local commits still need deployment. The required database, signing, admin, and Google configuration keys were present; their secret values were not displayed.
