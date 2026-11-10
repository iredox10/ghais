# Notification player audit

- [x] Route notification/controller seek, speed, repeat and shuffle actions through shared state; publish repeat/shuffle listener events.
- [x] Review service lifecycle; repair native media after recreation and save position on pause/destruction.
- [x] Review bridge recovery and focus; clear stale metadata, duration, position and errors, preserve subsecond resume position and settings.
- [x] Verify with 3 Android-aware controller tests, 2 metadata tests, 6 interruption tests and the debug APK build.
- [ ] Verify notification shade, Bluetooth controls and service recreation on a connected Android device. No device was connected during final verification.

Updated APK: `androidApp/build/outputs/apk/debug/androidApp-debug.apk`. Automated verification does not establish that every device-specific notification issue is resolved.
