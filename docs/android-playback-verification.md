# Android playback interruptions

The Media3 session exposes the system media notification, lock-screen, headset,
Bluetooth, seek, and queue controls. Android controls the exact notification layout.
Media3 1.9.4 supplies its normal foreground grace period for paused playback;
the service no longer forces an immediate foreground exit on pause.

Audio focus has one owner in PlayerBridge. A transient interruption pauses the
recitation and ambient layer together and resumes after focus returns. Repeated
ring/answer focus losses retain the original playback intent. Permanent call
losses also monitor the audio call mode and request focus again after the call
ends, because Android need not issue a gain after permanent loss. Permanent
losses to another media app stay paused. User pause/stop cancels pending resume.
Playback never starts while a focus request is denied or delayed.

The service saves the current track and position locally every five seconds and
on track/pause changes. A restarted service restores that track paused; system
media/headset play resumes it. This restores the current track, not the previous
queue. A deliberate stop clears the resume point. Network errors while paused
keep the same track and never auto-advance; play retries it at its current position.

## Verification

Run `./gradlew :androidApp:assembleDebug :shared:testDebugUnitTest`.
The shared tests include repeated interruptions, late call detection, permanent
media takeover, explicit pause cancellation, and already-paused playback.

On a physical Android device, verify each of these with the screen locked and
again with the app in the background:

- Receive, reject, answer, and end a call: keep the media card and position;
  resume only if playback was active before the interruption.
- Place and end an outgoing call, including a call lasting over ten minutes.
- Play a voice message in another app; verify temporary focus recovery.
- Start another music app; verify Ghaith stays paused until explicitly resumed.
- Pause or stop during a call; verify it stays paused after the call ends.
- Disconnect wired/Bluetooth headphones; verify audio does not jump to speaker.
- Lose network connectivity while paused; restore it and resume the same track.
- Restart the app process without force-stopping it, then press system/headset
  play; verify the saved track and position return without prior autoplay.

OEM battery management and Android process eviction can still remove a service
or notification. Force-stop is an explicit system stop; playback must not restart
itself afterwards. Real incoming/outgoing calls and long interruptions need device
verification; a successful build and state tests do not verify telephony behavior.
