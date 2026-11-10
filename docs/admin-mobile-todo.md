# Admin and mobile integration checklist

- [x] Restrict private playlist and track reads to their owners; deployed in Heroku release v19.
- [x] Allow admins to publish playlists with playable tracks and discovery metadata.
- [x] Make broadcasts match mobile behavior: public in-app inbox only; no push or targeting.
- [x] Implement deletion synchronization across duplicate schedules and test the mobile logic. Schedule conflict resolution uses device timestamps and can be affected by clock skew.
- [x] Implement mood display-copy editing and mobile synchronization with bundled playback fallback.
- [ ] Decide whether surah catalog changes should be managed centrally; the app currently uses bundled surah data.
- [x] Replace misleading analytics placeholders with available data or clear unavailable states.
- [ ] Recover legacy Appwrite data: the server key is missing and prior reads were blocked. Account identity and password migration require a separate safe plan.
- [x] Run targeted automated checks and deploy backend and admin to Heroku v19. Mobile changes require an updated app build on devices; full device-to-admin workflows remain to be verified.
