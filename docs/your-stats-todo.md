# Your Stats fixes

- [ ] Count heard surahs and reciters only during real playback.
- [ ] Correct streak and day rollover without changing historical bucket dates.
- [ ] Make daily charts use measured listening time and include other devices.
- [ ] Refresh visible chart dates across midnight.
- [ ] Replace misleading history-based all-time qari play counts.
- [ ] Merge device listening totals and play counts without counting synced copies twice.
- [ ] Run targeted tests and build the Android app.

Daily bucket keys currently use UTC. Preserve existing data and label this convention explicitly rather than reinterpreting old buckets as local dates.
