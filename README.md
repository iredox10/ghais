# Quranify

A modern, distraction-free Quran audio platform built with Kotlin Multiplatform and Compose Multiplatform.

## Tech Stack

- **Kotlin Multiplatform** — shared business logic across Android, iOS, and Web
- **Compose Multiplatform** — shared UI framework
- **Appwrite** — backend (auth, database, storage, functions)
- **Cloudflare R2 + CDN** — audio file hosting

## Features

- Routine-based listening modes (Study, Work, Sleep)
- Ayah-by-ayah playback with gapless transitions
- Customizable ambient sound mixer (rain, ocean, birds, etc.)
- Custom playlists and favorites
- Multiple world-class reciters
- Sleep timer with boundary-aware stopping
- Offline downloads
- Cross-device playback sync
- Khatma (completion) plans
- 100% free — no ads, no paywall

## Building

```bash
# Android
./gradlew :androidApp:assembleDebug

# iOS (requires Xcode)
open iosApp/iosApp.xcodeproj
```

## License

All rights reserved.
