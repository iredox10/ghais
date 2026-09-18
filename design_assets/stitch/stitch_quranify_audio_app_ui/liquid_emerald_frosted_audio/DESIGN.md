---
name: Liquid Emerald Frosted Audio
colors:
  surface: '#0f1412'
  surface-dim: '#0f1412'
  surface-bright: '#353a38'
  surface-container-lowest: '#0a0f0d'
  surface-container-low: '#181d1a'
  surface-container: '#1c211e'
  surface-container-high: '#262b29'
  surface-container-highest: '#313633'
  on-surface: '#dfe4e0'
  on-surface-variant: '#bbcabf'
  inverse-surface: '#dfe4e0'
  inverse-on-surface: '#2c322f'
  outline: '#86948a'
  outline-variant: '#3c4a42'
  surface-tint: '#4edea3'
  primary: '#4edea3'
  on-primary: '#003824'
  primary-container: '#10b981'
  on-primary-container: '#00422b'
  inverse-primary: '#006c49'
  secondary: '#45dfa4'
  on-secondary: '#003825'
  secondary-container: '#00bd85'
  on-secondary-container: '#00452e'
  tertiary: '#95d3ba'
  on-tertiary: '#003829'
  tertiary-container: '#71af97'
  on-tertiary-container: '#004231'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#6ffbbe'
  primary-fixed-dim: '#4edea3'
  on-primary-fixed: '#002113'
  on-primary-fixed-variant: '#005236'
  secondary-fixed: '#68fcbf'
  secondary-fixed-dim: '#45dfa4'
  on-secondary-fixed: '#002114'
  on-secondary-fixed-variant: '#005137'
  tertiary-fixed: '#b0f0d6'
  tertiary-fixed-dim: '#95d3ba'
  on-tertiary-fixed: '#002117'
  on-tertiary-fixed-variant: '#0b513d'
  background: '#0f1412'
  on-background: '#dfe4e0'
  surface-variant: '#313633'
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
    lineHeight: 38px
    letterSpacing: -0.025em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.015em
  headline-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
    letterSpacing: -0.01em
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: -0.005em
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
    lineHeight: 18px
    letterSpacing: 0.01em
  label-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 10px
    fontWeight: '600'
    lineHeight: 12px
    letterSpacing: 0.04em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-tablet: 1.25rem
  gutter-desktop: 1.5rem
  margin: 1rem
  margin-tablet: 2rem
  margin-desktop: 3rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2.5rem
---

## Brand & Style

This design system expresses contemplative serenity, spiritual elevation, and Apple-grade mechanical precision. Crafted for immersive, long-session Quranic and spiritual audio engagement, it blends iOS fluid depth with translucent frosted glassmorphism. The aesthetic balances deep celestial nocturnal environments with focused, luminous emerald illumination.

The emotional goal is quiet reverence combined with tactile delight. The UI recedes behind layers of atmospheric diffusion, allowing sacred verses, melodic recitations, and ambient soundscapes to take center stage. Through specular highlights, subtle rim lighting, and fluid glass planes, elements feel like physical sheets of precision-cut sapphire and smoked silica floating over deep volumetric darkness.

## Colors

The palette is engineered around dark translucent optical stacks punctuated by vivid emerald bioluminescence.

- **Primary (`#10b981`) & Secondary (`#34d399`)**: Used for active audio waveforms, scrubbing playheads, primary actionable buttons, and luminous focus states. Emits an ambient emerald aura (`rgba(16, 185, 129, 0.15)` to `rgba(52, 211, 153, 0.35)`).
- **Tertiary (`#064e3b`)**: Provides rich nocturnal undertones for volumetric radial gradients and subtle illuminated backdrops behind glass slabs.
- **Neutral Canvas (`#0a0f0d`)**: The ultimate base canvas—a tinted obsidian black with a microscopic hint of pine-moss undertone.
- **Translucent Surface Tiers**:
  - `Base Glass`: `rgba(18, 26, 22, 0.55)` with 20px–30px backdrop blur.
  - `Elevated Glass`: `rgba(28, 40, 34, 0.65)` with 40px backdrop blur.
  - `Active/Highlight Surface`: `rgba(52, 211, 153, 0.12)` overlaid on glass.
- **Specular Rim Gradients**: 1px linear borders transitioning from `rgba(255, 255, 255, 0.20)` at the top edge to `rgba(255, 255, 255, 0.03)` at the bottom edge.

## Typography

Plus Jakarta Sans is utilized across all scales, delivering a balance of geometric clarity and humanist curvature reminiscent of Apple's SF Pro, while granting distinctive character to contemporary spiritual audio.

- **Hierarchical Contrast**: Large verse titles and Surah headings leverage medium-to-bold weights with negative tracking (`-0.02em` to `-0.03em`) for crisp, modern cohesion.
- **Audio Scannability**: Track metadata, reciter credits, and timestamps rely on `label-md` and `body-sm` with slight positive tracking to ensure effortless legibility against translucent, moving audio visualizers.
- **Arabic Text Pairings**: When rendering Arabic ayah text, preserve a 1.6x line-height ratio relative to Western typography scales to accommodate complex diacritics (tashkeel).

## Layout & Spacing

The layout is built on a responsive 4-column (mobile), 8-column (tablet), and 12-column (desktop) fluid grid structure. Vertical cadence follows strict 4px/8px multiples, establishing spacious breathing room for meditative focus.

- **Mobile (< 768px)**: 16px screen margins (`margin: 1rem`) maximize horizontal real estate for glass cards and audio scrubbing rails. The bottom floating glass player floats 16px above the home indicator.
- **Tablet (768px - 1024px)**: 32px screen margins with side-by-side player and verse list layouts.
- **Desktop (> 1024px)**: Max-width content bound of 1280px with 48px outer canvas gutters, orchestrating a dual-pane master-detail layout (pinned glass audio player pane on the right, fluid library browser on the left).
- **Floating Island Safe Insets**: All scrolling views must reserve an extra `space-xl` bottom padding buffer to guarantee content scrolls fully clear of floating frosted pill navigation docks.

## Elevation & Depth

Visual depth is achieved exclusively through optical refraction, multi-layered backdrop filters, and delicate top-down rim lighting, eschewing murky drop shadows in favor of luminous liquid glass physics.

- **Layer 0 (Canvas Base)**: Pitch obsidian canvas (`#0a0f0d`) hosting ambient, slow-moving radial emerald light pools (`rgba(16, 185, 129, 0.08)` to `rgba(6, 78, 59, 0.25)` with 80px blur).
- **Layer 1 (Card & Content Glass)**: `background: rgba(22, 32, 26, 0.60)`, `backdrop-filter: blur(24px) saturate(180%)`. Outlined with a top-lit 1px border gradient (`linear-gradient(180deg, rgba(255, 255, 255, 0.16) 0%, rgba(255, 255, 255, 0.02) 100%)`).
- **Layer 2 (Floating Modals & Playing Pill)**: `background: rgba(30, 44, 36, 0.75)`, `backdrop-filter: blur(40px) saturate(200%)`. Enhanced with a subtle ambient glow: `box-shadow: 0 16px 40px -8px rgba(0, 0, 0, 0.5), 0 0 24px 0 rgba(16, 185, 129, 0.12)`.
- **Layer 3 (Active Controls & Tooltips)**: Solid or hyper-translucent emerald highlights (`rgba(52, 211, 153, 0.25)` to `#10b981`) emitting localized inner and outer neon diffusion (`box-shadow: 0 0 16px rgba(52, 211, 153, 0.45)`).

## Shapes

The interface employs Apple's continuous-corner curvature (squircle emulation) across all containers:

- **Surface Panels & Glass Cards**: Utilize `rounded-2xl` (16px) on mobile scaling to `rounded-3xl` (24px) on tablet/desktop to feel smooth and pebble-like.
- **Floating Control Bars & Pills**: Always use full pill radii (`rounded-full` / 9999px) to convey tactile fluidity for audio progress indicators, category chips, and floating dock bars.
- **Nested Corner Rule**: Child components inside cards have their corner radius offset by the card's padding (`R_child = R_parent - padding`) to ensure concentric border flows.

## Components

### Buttons & Audio Triggers
- **Primary Play/Pause Button**: Circular 64px frosted emerald button. Features a background of `#10b981` transitioning to `#059669`, an inner highlight bevel (`box-shadow: inset 0 1px 1px rgba(255, 255, 255, 0.4)`), and an external emerald bloom (`0 8px 24px rgba(16, 185, 129, 0.4)`).
- **Secondary Glass Buttons**: Translucent pill shape, `rgba(255, 255, 255, 0.08)` fill, `backdrop-filter: blur(16px)`, with 1px border of `rgba(255, 255, 255, 0.12)`. On hover/tap: background shifts to `rgba(255, 255, 255, 0.16)`.

### Glass Cards (Album Art & Surah Modules)
- Layered Apple Podcasts style cards. The card consists of an outer glass chassis (`rgba(22, 32, 26, 0.5)` with `backdrop-filter: blur(24px)`), 1px specular highlight border, and an internal artwork container with a soft 12px corner radius.
- An underlying blurred duplicate of the artwork radiates color-matched glows beneath the card container.

### Chips & Category Selectors
- Translucent frosted pills (`h-9`, horizontal padding `space-md`). Inactive state: `rgba(255, 255, 255, 0.05)` with `rgba(255, 255, 255, 0.1)` border.
- Active state: Emerald wash (`rgba(16, 185, 129, 0.2)`), border `rgba(52, 211, 153, 0.6)`, text `#34d399`.

### Audio Scrubbers & Sliders
- Track is a 4px frosted channel (`rgba(255, 255, 255, 0.1)`).
- Elapsed track fills with a luminous gradient (`#10b981` to `#34d399`).
- Scrubber thumb is a 14px circle of pure white encased in a 2px emerald halo ring, scaling to 18px on touch or active drag.

### Lists & Ayah Rows
- Borderless rows resting directly on translucent glass cards. Separated by hairline 1px rules made of `linear-gradient(90deg, transparent 0%, rgba(255, 255, 255, 0.08) 50%, transparent 100%)`.
- Active playing ayah triggers a soft horizontal glass highlight (`rgba(16, 185, 129, 0.1)`) and a 3px vertical emerald indicator line on the leading edge.

### Floating Frosted Navigation Dock
- Centered bottom floating pill dock elevated 20px above screen floor.
- `backdrop-filter: blur(32px) saturate(210%)`, fill `rgba(18, 26, 22, 0.75)`, 1px top highlight `rgba(255, 255, 255, 0.2)`.
- Active navigation icons illuminate in `#34d399` with a subtle underglow dot.

### Inputs & Search Bars
- Recessed pill containers (`background: rgba(0, 0, 0, 0.35)`), inner specular shadow, with placeholder text in `rgba(255, 255, 255, 0.4)`.
- Focus transition smoothly elevates border into emerald glow (`0 0 0 2px rgba(52, 211, 153, 0.4)`).