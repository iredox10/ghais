# Your Stats fixes

- [x] Count heard surahs and reciters only during real playback.
- [x] Correct streak and day rollover without changing historical bucket dates.
- [x] Make daily charts use measured listening time and include other devices.
- [x] Refresh visible chart dates across midnight.
- [x] Replace misleading history-based all-time qari play counts.
- [x] Merge device listening totals and play counts without counting synced copies twice.
- [x] Run targeted tests and build the Android app.

Daily bucket keys currently use UTC. Preserve existing data and label this convention explicitly rather than reinterpreting old buckets as local dates.

Validation: 15 backend tests and build passed; 3 targeted Android stats tests and debug APK build passed. Real PostgreSQL concurrent-update checks confirmed additive counters and retry idempotence. Historical undercounts cannot be reconstructed. The phone is disconnected, so installation and device verification remain pending.
