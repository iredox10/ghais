# Admin and mobile integration checklist

- [x] Restrict private playlist and track reads to their owners (pending deployment).
- [x] Allow admins to publish playlists with playable tracks and discovery metadata.
- [x] Make broadcasts match mobile behavior: public in-app inbox only; no push or targeting.
- [ ] Verify deletion synchronization across duplicate schedules and mobile devices. Schedule conflict resolution uses device timestamps and can be affected by clock skew.
- [ ] Complete and verify mood display-copy editing and mobile synchronization; surahs remain a bundled reference catalog.
- [x] Replace misleading analytics placeholders with available data or clear unavailable states.
- [x] Assess legacy Appwrite access: the server key is missing and prior reads were blocked. Account identity and password migration require a separate safe plan.
- [ ] Run final automated checks and document deployment status. Local commits are not yet deployed; the mobile changes require an updated app build.
