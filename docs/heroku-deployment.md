# Heroku and PostgreSQL

The `ghaith` Heroku app runs the backend at `https://ghaith-c77c2426ed4d.herokuapp.com`. Heroku Postgres Essential-0 is attached. The root `package.json` and `Procfile` use Heroku's native Node buildpack; the release command applies the schema. Database connections verify the RDS certificate using `backend/certs/rds-global-bundle.pem`.

The Android production endpoint targets the Heroku API. A debug APK was built and installed on the emulator. The GitHub chart workflow and its admin secret are configured, and a manual chart refresh succeeded. Automatic GitHub-to-Heroku deployment is not configured.

On 2026-11-10, release v19 deployed the admin/mobile integration changes from commit `e02cb9c` using a clean Git archive and the native Node buildpack. The release, API health endpoint, public mood endpoint, authenticated admin and its PostgreSQL/R2 configuration checks succeeded. The mobile changes must be included in a new app build and installed on devices. The scheduled GitHub chart workflow was last observed completing successfully on 2026-11-09.

Appwrite currently rejects document reads with `team_read_only`, so the cutover uses the existing Heroku catalog and bundled app data. Existing Appwrite accounts and private cloud documents are unavailable; users must create new accounts. Device-local data remains on the device, subject to the app's existing local storage behavior. Do not overwrite the existing Heroku catalog with demo fixtures.

If Appwrite access is restored later, an export can be performed with the existing migration script and a server key in the private, Git-ignored `backend/.env.heroku`. Accounts and private documents would still require an identity-preserving migration; importing documents alone does not migrate passwords or reconnect accounts.

On 2026-11-03, a read-only check found the app healthy, one Basic web dyno and one Essential-0 PostgreSQL database. Before cutover, the database contained 242 reciter records, one chart record, and no users, playlists, or play events. This is a snapshot, not proof that all catalog data has been migrated. Release v11 deployed the current backend and ran the database migration successfully. The required database, signing, admin, and Google configuration keys were present; their secret values were not displayed. A protected pre-cutover snapshot is stored in the ignored `backend/export` directory.
