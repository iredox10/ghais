---
name: Obsidian & Emerald Spiritual Elegance
colors:
  surface: '#111415'
  surface-dim: '#111415'
  surface-bright: '#373a3b'
  surface-container-lowest: '#0c0f10'
  surface-container-low: '#191c1d'
  surface-container: '#1d2021'
  surface-container-high: '#272a2b'
  surface-container-highest: '#323536'
  on-surface: '#e1e3e4'
  on-surface-variant: '#bbcabf'
  inverse-surface: '#e1e3e4'
  inverse-on-surface: '#2e3132'
  outline: '#86948a'
  outline-variant: '#3c4a42'
  surface-tint: '#4edea3'
  primary: '#4edea3'
  on-primary: '#003824'
  primary-container: '#10b981'
  on-primary-container: '#00422b'
  inverse-primary: '#006c49'
  secondary: '#ffb95f'
  on-secondary: '#472a00'
  secondary-container: '#ee9800'
  on-secondary-container: '#5b3800'
  tertiary: '#68dba9'
  on-tertiary: '#003825'
  tertiary-container: '#3eb686'
  on-tertiary-container: '#00422c'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#6ffbbe'
  primary-fixed-dim: '#4edea3'
  on-primary-fixed: '#002113'
  on-primary-fixed-variant: '#005236'
  secondary-fixed: '#ffddb8'
  secondary-fixed-dim: '#ffb95f'
  on-secondary-fixed: '#2a1700'
  on-secondary-fixed-variant: '#653e00'
  tertiary-fixed: '#85f8c4'
  tertiary-fixed-dim: '#68dba9'
  on-tertiary-fixed: '#002114'
  on-tertiary-fixed-variant: '#005137'
  background: '#111415'
  on-background: '#e1e3e4'
  surface-variant: '#323536'
typography:
  display-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 40px
    fontWeight: '700'
    lineHeight: 48px
    letterSpacing: -0.03em
  display-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 11px
    fontWeight: '600'
    lineHeight: 14px
    letterSpacing: 0.03em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  margin: 1.25rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

This design system establishes a reverent, high-fidelity audio environment tailored for immersive Quranic recitation, contemplation, and memorization. Synthesizing the refined, media-rich spatial fluency of modern streaming architectures with Islamic aesthetics, the experience delivers a state of serene focus (*Khushu'*).

The visual personality hinges on three pillars:
- **Sacred Precision:** Crisp UI architecture, impeccable hierarchy, and noise-free layouts that prioritize listening fluidity and textual legibility.
- **Atmospheric Luminance:** Deep obsidian darkness punctuated by ambient emerald auras and warm champagne gold micro-accents, establishing a tactile, late-night sanctuary mood.
- **Contemporary Spiritual Glassmorphism:** Translucent structural planes, refined micro-borders, and rounded geometric elements that feel weightless, modern, and respectful.

## Colors

The palette operates in pure, deep darkness to minimize eye fatigue during extended recitation and nocturnal study.

### Color Tokens & Roles
- **Background Root (`#0A0D0E`):** Pure obsidian slate. Serves as the global backdrop canvas.
- **Surface Layer 1 / Base Card (`#111618`):** Midnight graphite container used for primary media cards, elevated sheets, and list row backplates.
- **Surface Layer 2 / Elevated Glass (`#182022`):** Elevated tonal container for active states, floating sheets, and interactive modules.
- **Primary / Emerald Accent (`#10B981`):** Vibrantly illuminates playback controls, progress meters, active ayah tracking, and affirmative actions.
- **Primary Deep / Forest Emerald (`#059669`):** Grounded emerald for active states, gradient stops, and secondary interactive backdrops.
- **Secondary / Champagne Gold (`#F59E0B`):** Sacred accent reserved for illuminated Surah numbers, Tajweed focus cues, bookmarks, and achievement badges.
- **Gold Warm Glow (`#D97706`):** Secondary depth color for gilded gradients, verified badges, and memorization mastery indicators.
- **Border / Micro-Stroke (`rgba(255, 255, 255, 0.08)`):** Ultra-subtle ghost stroke preserving layer boundaries over deep obsidian.
- **Text High-Emphasis (`#F8FAFC`):** Primary reading color for translations, Surah titles, and active Ayahs.
- **Text Medium-Emphasis (`#94A3B8`):** Metadata, reciter names, timestamps, and Ayah counts.
- **Text Low-Emphasis / Inactive (`#475569`):** Disabled states, progress bar rails, and unobtrusive structural glyphs.

## Typography

The interface employs **Plus Jakarta Sans** for modern geometric clarity, rhythm, and optical balance across all UI controls, audio data, and Latin translations. 

### Arabic Script Integration
- While Plus Jakarta Sans powers standard UI shells, Arabic text elements (Surah names, Mushaf Ayah text) pair directly with traditional Naskh-style web fonts (*Amiri* or *Scheherazade New*).
- Arabic verse typography must maintain a 1.8x line-height multiplier compared to Latin body sizes to accommodate diacritics (*tashkeel*) cleanly without vertical clipping.
- Dynamic Ayah highlighting during active audio playback applies weight shifts from Regular (400) to SemiBold (600) alongside color transitions from `#94A3B8` to `#10B981` or `#F59E0B`.

## Layout & Spacing

The layout is built around high-density mobile vertical flows with responsive adaptive extensions for tablet and desktop viewports.

### Grid & Canvas Structure
- **Mobile Handheld (360px - 480px):** 4-column fluid layout with a global outer margin of `1.25rem` (20px) and gutters of `1rem` (16px).
- **Tablet / Split Screen (481px - 1024px):** 8-column layout with `1.5rem` margins and `1rem` gutters. Accommodates dual-pane viewing: Recitation list / Mushaf pane alongside full-height player controls.
- **Desktop / Web Player (1025px+):** 12-column grid capped at a maximum width of `1280px`, anchored by a sticky three-tier architecture: Left sidebar navigation, central scrollable discovery/reading canvas, and a persistent bottom player bar.

### Spatial Rhythm
- **Vertical Rhythm:** Content clusters adhere strictly to 4px and 8px base units. 
- **Floating Player Accommodation:** On mobile screens, all primary views inject a mandatory bottom padding offset (`6.5rem` / 104px) to ensure scrollable feeds are never occluded by the floating mini player and bottom navigation dock.

## Elevation & Depth

Visual depth is achieved through translucent glassmorphism, surface tonal shifts, and ambient luminous glows rather than traditional muddy drop shadows.

### Elevation Hierarchy
1. **Canvas (Level 0):** Pure `#0A0D0E` obsidian ground. Zero elevation.
2. **Resting Cards & Tiles (Level 1):** `#111618` solid with a `1px` border of `rgba(255, 255, 255, 0.05)`.
3. **Interactive & Hover Cards (Level 2):** `#182022` with a `1px` border of `rgba(255, 255, 255, 0.1)` accompanied by an ambient emerald wash: `box-shadow: 0 12px 32px -8px rgba(16, 185, 129, 0.12)`.
4. **Floating Mini Player & Overlays (Level 3):** Glassmorphic surface built with `background: rgba(17, 22, 24, 0.75)`, `backdrop-filter: blur(16px)`, `border: 1px solid rgba(255, 255, 255, 0.10)`, and an elevated shadow: `box-shadow: 0 16px 40px -4px rgba(0, 0, 0, 0.6)`.
5. **Full Player Modal / Bottom Sheet (Level 4):** Radial gradient base blending from `#182022` at the top center to `#0A0D0E` at the bottom, framed with a top specular border `rgba(255, 255, 255, 0.15)`.

### Luminous Accent Gradients
- **Emerald Pulse:** Used behind album art and active visualizers: `radial-gradient(circle, rgba(16, 185, 129, 0.25) 0%, rgba(10, 13, 14, 0) 70%)`.
- **Champagne Radiance:** Used for active Tajweed bookmarks and verified emblems: `radial-gradient(circle, rgba(245, 158, 11, 0.2) 0%, rgba(10, 13, 14, 0) 65%)`.

## Shapes

The design system embraces an organic, approachable aesthetic utilizing curvature to evoke harmony, flow, and modern tactile delight.

- **Base Radius (Rounded):** Standard elements, buttons, inputs, and list rows maintain an `8px` (`0.5rem`) corner radius.
- **Containers & Tiles (`rounded-2xl` / 16px):** Album artwork, Surah discovery cards, modal containers, and audio cards use generous 16px curvature.
- **Floating Sheets & Mini Player (`rounded-3xl` / 24px):** Floating navigation bars, active mini player docks, and memorization prompts leverage dynamic 24px rounding to feel friendly and detached from device bezels.
- **Pill System (`rounded-full` / 9999px):** All categorical tags, Revelation indicators (e.g., "Makkah", "Madinah"), Ayah counters, scrubber handles, and filter chips utilize full pill radius.

## Components

### 1. Buttons
- **Primary Audio / CTA:** Emerald pill button (`#10B981`) with bold obsidian text (`#0A0D0E`). Scale hover transition (`scale-105`) and an emerald halo glow on active states (`box-shadow: 0 0 20px rgba(16, 185, 129, 0.4)`).
- **Secondary / Ghost:** Semi-transparent dark slate surface (`rgba(255, 255, 255, 0.05)`) with a crisp `1px` border of `rgba(255, 255, 255, 0.1)`. High-emphasis white text.
- **Icon Action Buttons:** `44x44px` minimum hit target. Rounded-full translucent disc (`rgba(24, 32, 34, 0.6)`) with subtle hover highlight.

### 2. Floating Mini Player
- Suspended `12px` above the bottom tab bar.
- Features frosted glass backdrop (`rgba(17, 22, 24, 0.85)`, `blur-md`), 16px corner radius, and an ultra-thin gold or emerald progress micro-line flush to its top edge.
- Layout: Square Surah/Reciter thumbnail (`40x40px`, `rounded-lg`), title and reciter marquee text, circular bookmark toggle, and play/pause morphing button.

### 3. Audio Wave Visualizer & Progress Slider
- **Waveform:** Dynamic oscillating vertical bars (widths of 2px, gaps of 2px) colored in `#10B981` with opacity scaling based on amplitude.
- **Slider Track:** 4px tall container. Inactive track colored `#182022`, filled track dynamically filled with `#10B981` or champagne gradient `#F59E0B`. Smooth 12px pill scrubber knob with inner shadow.

### 4. Chips & Badges
- **Pill Metadata Badges:** Height 24px, padding 2px 10px, font size 11px semi-bold.
  - *Revelation Place:* Subtle emerald fill (`rgba(16, 185, 129, 0.12)`) with `#10B981` text.
  - *Juz / Ayah Count:* Neutral fill (`rgba(255, 255, 255, 0.06)`) with `#94A3B8` text.
  - *Tajweed Rule / Mode:* Champagne gold outline (`rgba(245, 158, 11, 0.3)`) with `#F59E0B` text.
- **Reciter Avatar Chips:** Circular portraits bordered with a 2px ring. Verified Sheikhs feature a champagne gold verified check badge anchored at the bottom-right coordinate.

### 5. Surah & Playlist Cards
- **Discovery Card:** Rectangular card (`rounded-2xl`), deep midnight backdrop (`#111618`), delicate geometric Islamic motif overlaid at 4% white opacity.
- **Header:** Surah number in an illuminated champagne star polygon (*Rub el Hizb*) glyph on the left; Calligraphic Arabic title on the right.
- **Footer:** Reciter subtitle and duration/Ayah counts in medium-emphasis slate.

### 6. Ayah Recitation Row
- Two-column or stacked layout for continuous reading.
- Active Ayah container receives a soft background wash of `rgba(16, 185, 129, 0.06)` and a left-aligned vertical indicator bar (3px width, `#10B981`).
- Integrated actions row (Play Verse, Tafsir, Repeat Loop, Memorize, Share) appearing on tap or hover via a frosted glass pill ribbon.

### 7. Form Controls & Toggles
- **Switches (Audio Loop, Word-by-Word):** Pill track (`24px` height, `44px` width) in `#182022` switching to `#10B981` when active, with smooth sliding circular thumb (`20px`).
- **Search Bar:** Glassmorphic pill (`rounded-full`), `#111618` filled with `rgba(255, 255, 255, 0.05)` border, leading search icon, trailing microphone and filter glyphs.