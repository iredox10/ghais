# Ghaith Hifz: Audio-First Portion Memorization Spec & Research

> **Goal:** Turn Ghaith's Hifz section into a global-standard, audio-first memorization system built around **Listen → Pause → Recite**, **Progressive Chaining**, **Active Recall**, and **Spaced Repetition (SRS)**—usable both on-screen and hands-free/eyes-free.

---

## 1. Current Codebase Audit (What Exists vs. What's Missing)

| Capability | Current Status in Repo | Gap to Close |
|---|---|---|
| **Ayah & Range Looping** | `AudioEngine.kt` (`HifzRange`, `decideHifzTransition`, `repeatPerAyah` 1–50, `targetLoops`) | Only supports uniform flat loops (`V1 x N, V2 x N`). No **Progressive Chaining** (`V1 → V2 → V1–2 → V3 → V1–3`). |
| **Pause Gaps** | `HifzRange.pauseBetweenMs` (0–30s fixed stepper) | Fixed seconds only—no **Ayah-Proportional Pause** (e.g., `1.25×` ayah duration) and no **Listen → Delay → Recite** audio cue. |
| **Speed & Qari Controls** | `AudioEngine` playback speed + `EveryAyahReciters` (teachers prioritized) | Needs quick Hifz presets (e.g., `0.85×` articulation, `1.0×` natural, `1.25×` Muraja'ah review). |
| **SRS Mastery Tracking** | `HifzMasteryStore` (`masteryMap`, `dueDayMap`, `intervalMap`, `weakAyahs`, `questsFor`) | Tracks ayah status & due days, but doesn't yet generate **Sabaq / Sabqi / Manzil** audio review queues or **Bridge Loops**. |
| **Voice Recording** | `VoiceRecorderBridge` exists | Manual record only—not wired into the automatic **Listen → Pause (Auto-Record) → A/B Compare** loop. |
| **Long-Ayah Splitting** | Not implemented | Long ayahs (e.g., 2:255, 2:282) play as one single file; cannot loop phrase-by-phrase at *waqf* marks. |
| **Eyes-Free / Blinded Mode** | Not implemented | Verse text is always visible; no blinded recall mode, first-word audio prompt, or headphone gesture mapping. |

---

## 2. Research Foundation (Why Audio-First Hifz Works)

1. **Delayed Repetition (2–4s Encoding Window)**
   - Research from the **Basque Center on Cognition, Brain, and Language (BCBL)** shows that *immediate* verbal repetition of unfamiliar audio can interfere with encoding due to overlapping phonological and motor demands. Inserting a short silent encoding gap (2–4 seconds) before speaking strengthens memory integration.
   - **Design implication:** Offer a brief *"absorb"* pause after the Qari finishes before the *"your turn to recite"* cue, rather than rushing into overlapping repetition.

2. **The Production Effect & Phonological Loop**
   - Cognitive memory studies consistently show that speaking aloud (*production effect*) engages sensorimotor and auditory feedback loops, producing significantly stronger verbal recall than silent reading or passive listening.
   - **Design implication:** Every Hifz session should actively prompt spoken recitation during the pause gap—and optionally record it for self-correction.

3. **Rhythmic & Melodic Chunking (*Bi al-Taghanni*)**
   - Prosody, cadence, and melodic contour act as retrieval scaffolds in auditory memory. Keeping a consistent Qari during initial encoding (*Sabaq*) locks in the rhythmic pattern; switching Qaris later during *Sabqi/Manzil* tests true textual recall rather than melody-only dependence.

4. **Retrieval Practice & Desirable Difficulty**
   - Reading the text while listening creates an *illusion of competence*. Hiding the text (**Blinded / Eyes-Free Mode**) or giving only a **First-Word Audio Prompt** forces active retrieval from long-term memory, which is the strongest driver of retention.

5. **Spaced Repetition & Classical Hifz (*Sabaq / Sabqi / Manzil*)**
   - Traditional *Madrasah* Hifz pedagogy already maps directly to modern SRS algorithms (SM-2 / FSRS):
     - **Sabaq** (New portion): High-repetition accumulation today.
     - **Sabqi** (Recent 7 days): Daily consolidation & transition drills.
     - **Manzil** (Older memorized portions): Expanding intervals (`1d → 3d → 7d → 14d → 30d → 60d`).

---

## 3. Feature Specification (Prioritized Roadmap)

### Phase 1 — Core Audio-Looping Upgrades (Immediate Impact)

1. **Progressive Chaining ("Accumulation / Snowball Mode")**
   - Given a target range `[startAyah..endAyah]` and `repeatPerAyah` (default `3`):
     - Step 1: Play `Ayah 1` × `3` (with pause gap after each).
     - Step 2: Play `Ayah 2` × `3`.
     - Step 3 (**Chain**): Play `Ayah 1 → Ayah 2` × `2` (links the transition!).
     - Step 4: Play `Ayah 3` × `3`.
     - Step 5 (**Chain**): Play `Ayah 1 → Ayah 2 → Ayah 3` × `2`.
   - **Implementation note:** Extend `HifzTransition` in `AudioEngine.kt` with a `HifzLoopMode.ACCUMULATION` state machine (`currentAnchorAyah`, `chainCursor`, `phase = LEARN_SINGLE | CHAIN_ACCUMULATE`).

2. **Adaptive (Ayah-Proportional) Pause Gaps**
   - Add a **Pause Mode** selector alongside fixed seconds:
     - **Fixed:** `0s, 3s, 5s, 8s, 10s, 15s, 20s, 30s`
     - **Match Ayah (`1.0×` / `1.25×` / `1.5×`):** Automatically waits `durationMs × multiplier` after the ayah finishes so a 3-second ayah gets a ~4s pause and a 20-second ayah gets a ~25s pause without manual tuning.
   - **Audio Cues (Optional):** Subtle 150ms soft chime at the start and end of the pause gap for eyes-free use.

3. **Eyes-Free / Blinded Recall Mode**
   - Toggle **"Hide Text (Recall Mode)"** on `NowPlayingScreen` in Ayah Mode:
     - Blurs/hides the Arabic verse and translation during playback/pause.
     - Tap anywhere (or wait until verification replay) to **Peek** at the verse if stuck.
     - Works with screen off / pocket mode via Media3 lockscreen & notification controls.

4. **Transition-Only "Bridge Loops"**
   - The #1 failure point in Hifz is forgetting **which ayah comes next** at verse boundaries.
   - **Bridge Mode:** For a range `[start..end]`, plays only the **last 4 seconds of Ayah $N$ → 2s pause (user recites the start of $N+1$) → first 4 seconds of Ayah $N+1$**.
   - **Implementation note:** Uses ExoPlayer `ClippingConfiguration` or `seekTo(durationMs - 4000L)` / early-stop timer in `AudioEngine`.

---

### Phase 2 — Active Recall & Mistake Prevention

5. **First-Word Audio Prompt Mode (Testing Recall)**
   - During **Sabqi / Review** mode:
     - Plays only the first `1.5s–2.5s` (first 1–2 words) of Ayah $N$.
     - Pauses for `1.25×` ayah duration while the user recites the rest of the verse from memory.
     - Plays the full Ayah $N$ afterward so the user immediately hears if they missed a word or vowel (*harakat*).

6. **One-Tap "Stumble / Flag Weak" + Mistake Heatmap**
   - Single prominent **"Stumble"** button during Hifz playback (and mapped to headphone double-tap / notification action):
     - Immediately adds `+2` extra repeats to the current ayah in the active loop.
     - Marks the ayah as `REVIEW_NEEDED` in `HifzMasteryStore` and resets its SRS interval.
   - Displays a visual **Surah Heatmap** (green = mastered, amber = learning, red = stumbled) on `MemorizationReciterScreen`.

7. **Auto-Record & A/B Echo Comparison**
   - When **"Self-Check Mic"** is enabled for a session:
     - `Qari plays Ayah` → `Soft Chime` → `VoiceRecorderBridge` records during the pause gap → `App plays User's Recording` → `App replays Qari's Ayah`.
   - Zero manual tapping required; lets learners catch subtle *Madd*, *Ghunnah*, and *Harakat* mistakes by ear.

8. **Long-Ayah Waqf Splitting**
   - For long verses (e.g., > 20 words or > 15 seconds, like 2:255 or 2:282):
     - Split the verse text at natural *waqf* marks (`ۖ`, `ۗ`, `ۘ`, `ۙ`, `ۚ`, `ۛ`, `ۜ`) and map approximate or timestamped audio segments so users can loop **Phrase 1 → Phrase 2 → Full Ayah**.

9. **Mutashabihat (Similar-Verse) Alerts**
   - Maintain a lightweight static index of high-frequency *Mutashabihat* pairs across the Quran (e.g., *Al-Baqarah* vs. *Al-A'raf*, *Ash-Shu'ara* refrains).
   - Show a subtle `"Similar verse in Surah X"` badge (and optional soft alert tone) before the verse plays, with a 1-tap side-by-side diff sheet so learners lock in the exact distinguishing word.

---

### Phase 3 — Spaced Repetition & Hands-Free Polish

10. **3-Tier Hifz Daily Session Launcher (`Sabaq` / `Sabqi` / `Manzil`)**
    - On `MemorizationScreen`, offer 3 clear daily modes alongside browsing reciters:
      - **New Lesson (`Sabaq`)**: Pick a portion (e.g., 3–5 ayahs or a custom portion) → launches **Accumulation Mode**.
      - **Recent Review (`Sabqi`)**: Pulls ayahs practiced in the last 7 days → launches **First-Word Prompt + Bridge Loops**.
      - **Long-Term (`Manzil`)**: Pulls `dueDayMap` ayahs due today (or flagged weak ayahs) → launches **Blinded Recall**.

11. **Headphone & Lockscreen Hands-Free Gestures**
    - While Hifz Mode is active:
      - **Play/Pause button**: Pause/resume loop (never loses current repetition count).
      - **Next track**: Mark current ayah *Solid / Mastered* and advance.
      - **Previous track**: Flag *Stumble* and replay current ayah immediately.

12. **Optional On-Device / Cloud Arabic ASR (Future)**
    - Run an Arabic Quran-tuned ASR pass (e.g., Whisper-small / Tarteel-style model) on the recorded pause-gap audio to highlight skipped words or wrong endings automatically.

---

## 4. Recommended Implementation Order

1. **Step 1 (Audio Engine):** `HifzLoopMode.ACCUMULATION` + **Ayah-Proportional Pause** + **Bridge Mode** in `AudioEngine.kt`.
2. **Step 2 (Player Hifz UI):** **Eyes-Free / Blinded Mode** toggle + **First-Word Prompt** + **Stumble** button in `HifzPlayerComponents.kt`.
3. **Step 3 (Hifz Hub & SRS):** Wire `Sabaq` / `Sabqi` / `Manzil` daily session cards + **Mistake Heatmap** on `MemorizationScreen.kt` & `MemorizationReciterScreen.kt`.
4. **Step 4 (Self-Check & Waqf):** Auto-record A/B echo loop (`VoiceRecorderBridge`) + Long-Ayah *Waqf* splitting + *Mutashabihat* index.
