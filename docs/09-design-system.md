# 09 — Design system

**Status:** design-system draft for sign-off, 2026-10-24.
**Scope:** the visual and interaction language of Eclipse Launcher: brand, color, type, spacing, shape, motion, iconography, components, game-specific patterns, accessibility tokens, and the Compose implementation contract.
**Dependencies:** Material 3 (1.4.x) and Material 3 Expressive guidance; Compose; Navigation 3; Android 12+ dynamic color as an option.
**Labels:** **Decision**, **Verified**, **Inference**, **Open**.

---

## 1. Design direction

### 1.1 The idea: a field instrument, not an arcade cabinet

Minecraft launchers tend to look like one of two things: a legacy settings app, or a neon game portal. Eclipse should feel like a **well-made field instrument**: quiet, precise, warm, and legible in bad light. It should feel at home next to a map, a notebook and a good pair of headphones — not next to an RGB keyboard.

Three adjectives drive every choice:

- **Calm** — low chroma surfaces, one accent, no gradients competing for attention.
- **Tactile** — generous targets, physical press states, paper-like cards, a sense of weight.
- **Honest** — status is explicit, numbers are real, errors are written in sentences.

### 1.2 What it is not

- Not “gamer neon”: no purple/cyan gradient, no glow, no angular HUD frames.
- Not “corporate dashboard”: no KPI tiles, no chart junk, no blue-on-white sterility.
- Not “Minecraft skin”: no grass textures, no pixel font, no creeper faces, no copied assets.
- Not “Eclipse Foundation”: no purple branding, no Eclipse IDE iconography, no implied affiliation.

### 1.3 Signature elements

1. **The arc** — a thin crescent/arc used in the wordmark, the app icon and the loading state. It suggests an eclipse without drawing a literal moon.
2. **The paper card** — a warm, low-elevation surface with a 1 dp outline and a 28 dp corner on hero cards.
3. **The status rule** — a 3 dp vertical accent on list rows that carries state (ready, needs attention, broken), always paired with an icon and text.
4. **Monospace for truth** — versions, hashes, signals and paths use a monospace face; prose never does.

---

## 2. Brand

### 2.1 Name and wordmark

- Product name: **Eclipse Launcher**. In running text, “Eclipse” is acceptable after first mention.
- Wordmark: “Eclipse” set in the UI typeface, medium weight, with the arc replacing the dot over the “i” (or sitting above the wordmark). The arc is a single 2.5 dp stroke, open at the lower right.
- The wordmark is a vector, not an image; it must render at 16 dp and at 512 dp.
- Clear space: at least the height of the “E” on all sides.
- Do not add “Minecraft” to the wordmark or the app icon.

### 2.2 App icon

- Adaptive icon: a warm paper foreground arc on a deep ink background.
- No grass block, no pickaxe, no creeper silhouette, no Mojang red.
- Monochrome layer for themed icons: the arc only.
- The icon must survive at 48 dp and in grayscale.

### 2.3 Brand voice

| Trait | Sounds like | Does not sound like |
|---|---|---|
| Direct | “Minecraft could not start. Your device has GLES 3.1; this version needs 3.2.” | “Oops! Something went wrong.” |
| Calm | “The download stopped. It will resume.” | “Download failed!!!” |
| Specific | “Fabric 0.20.1, 187 files, 412 MB.” | “Some files are missing.” |
| Respectful | “This will remove the instance. Your worlds can be exported first.” | “Are you sure?!” |

**Banned words in UI:** oops, whoops, uh-oh, awesome, epic, unleash, supercharge, magic, seamless, robust, leverage.

### 2.4 Trademark hygiene

- The Minecraft disclaimer ships on the About screen, the first-run footer and the release page.
- “Minecraft” is used descriptively, never as a product name or logo.
- “Eclipse” carries a non-affiliation line with the Eclipse Foundation in About and the README.
- No third-party launcher names or logos appear in the UI, only in research documents.

---

## 3. Color

### 3.1 Palette strategy

Eclipse ships a **brand palette** by default: warm neutral surfaces, an amber primary, a teal secondary, and a red error. Android 12+ users may opt into **system colors** in Appearance; the brand palette remains the reference for screenshots, docs and the icon.

**Decision:** dynamic color is **off by default**. A launcher with a distinct identity should not look like every other Material app on the device; users who want system colors can enable them.

### 3.2 Brand seed roles

| Role | Light | Dark | Use |
|---|---|---|---|
| `primary` | `#8A4B08` | `#F2A93B` | Primary action, active state |
| `onPrimary` | `#FFFFFF` | `#2A1700` | Text/icon on primary |
| `primaryContainer` | `#FFDDB6` | `#6A3A00` | Tonal buttons, highlights |
| `onPrimaryContainer` | `#2C1600` | `#FFDDB6` | Text on container |
| `secondary` | `#3F665F` | `#A6CEC5` | Secondary action, links |
| `onSecondary` | `#FFFFFF` | `#0B3733` | Text on secondary |
| `secondaryContainer` | `#C1EAE1` | `#264E48` | Chips, tags |
| `tertiary` | `#5B5D2E` | `#C5C78B` | Loader/version badges |
| `error` | `#BA1A1A` | `#FFB4AB` | Errors, destructive |
| `errorContainer` | `#FFDAD6` | `#93000A` | Error surfaces |
| `surface` | `#FBF8F3` | `#141210` | Page background |
| `surfaceContainerLowest` | `#FFFFFF` | `#0E0D0B` | Cards on dark |
| `surfaceContainerLow` | `#F5F1E9` | `#1C1A17` | Cards |
| `surfaceContainer` | `#EFEAE1` | `#211E1B` | Sheets, bars |
| `surfaceContainerHigh` | `#E9E3D9` | `#2B2723` | Menus, elevated |
| `outline` | `#857468` | `#9F8D80` | Borders |
| `outlineVariant` | `#D8C8BB` | `#4F443B` | Dividers |
| `onSurface` | `#1E1B18` | `#EDE7DF` | Primary text |
| `onSurfaceVariant` | `#51443A` | `#D5C4B7` | Secondary text |
| `inverseSurface` | `#33302C` | `#EDE7DF` | Snackbars |
| `inverseOnSurface` | `#F6F0E8` | `#33302C` | Snackbar text |

These are starting values; the final palette must pass contrast checks after dynamic Material color generation. The table is a **seed**, not a claim that Material's tonal palette will reproduce these exact hex values.

### 3.3 Semantic colors

| Token | Meaning | Light | Dark |
|---|---|---|---|
| `statusReady` | Ready, installed, verified | `#2E6B4F` | `#8FD5B0` |
| `statusAttention` | Needs repair/sign-in, update available | `#8A5A00` | `#F2C66D` |
| `statusBroken` | Crash, missing runtime, incompatible | `#A33A2B` | `#FFB4A8` |
| `statusBusy` | Downloading, launching | `#35618E` | `#A8C8F0` |
| `scrim` | Modal/overlay | `#000000` 40% | `#000000` 60% |
| `gameOverlay` | In-game controls | `#000000` 28% + white icons | same |

Status is **never** color-only. Each status has an icon and a label. Color-blind simulation is part of the release checklist.

### 3.4 Contrast rules

- Body text: ≥ 4.5:1 against its surface.
- Large text (≥ 18.66 sp bold or ≥ 24 sp regular): ≥ 3:1.
- Icons and UI boundaries: ≥ 3:1.
- Focus ring: ≥ 3:1 against both the component and the background.
- Disabled text: ≥ 3:1 is not required by WCAG, but Eclipse keeps ≥ 3:1 for legibility.
- Error text on `errorContainer` must be checked; do not assume red on pink passes.

### 3.5 Color usage rules

- One primary action per screen. If two buttons are filled, one is wrong.
- Tonal buttons for secondary actions; text buttons for tertiary.
- Destructive actions use `error` as text/icon, not as a filled red button, unless the action is irreversible and confirmed.
- Never use color to distinguish “Fabric” from “Vanilla”; use a text badge.
- Game overlay uses white at 90% opacity with a black scrim at 28%; never rely on the game's background being dark.

---

## 4. Typography

### 4.1 Families

| Role | Family | Why |
|---|---|---|
| UI | `FontFamily.Default` (Roboto on Android) | Zero download, excellent hinting, variable weight support |
| Display/wordmark | Same family, 500–600 weight, tightened tracking | Coherent without a second font |
| Code/truth | `FontFamily.Monospace` (Roboto Mono / Droid Sans Mono) | Versions, hashes, signals, paths |
| Fallback | System fallback chain | CJK, Arabic, Hebrew, Devanagari |

**Decision:** v1 bundles no font. A custom display face is a v1.1 brand upgrade only if it is OFL-licensed and the license ships. This avoids a font-licensing and APK-size problem on day one.

### 4.2 Type scale

| Token | Size / line height (sp) | Weight | Tracking | Use |
|---|---|---|---|---|
| `displayLarge` | 57 / 64 | 400 | -0.25 | Marketing/onboarding hero only |
| `displayMedium` | 45 / 52 | 400 | 0 | Empty-state headline |
| `displaySmall` | 36 / 44 | 400 | 0 | Large screen hero |
| `headlineLarge` | 32 / 40 | 400 | 0 | Screen title on expanded |
| `headlineMedium` | 28 / 36 | 400 | 0 | Screen title |
| `headlineSmall` | 24 / 32 | 400 | 0 | Section title |
| `titleLarge` | 22 / 28 | 500 | 0 | Card title, dialog title |
| `titleMedium` | 16 / 24 | 500 | 0.15 | List row title, tab |
| `titleSmall` | 14 / 20 | 500 | 0.1 | Chip, badge |
| `bodyLarge` | 16 / 24 | 400 | 0.5 | Body |
| `bodyMedium` | 14 / 20 | 400 | 0.25 | Secondary body |
| `bodySmall` | 12 / 16 | 400 | 0.4 | Caption, metadata |
| `labelLarge` | 14 / 20 | 500 | 0.1 | Button |
| `labelMedium` | 12 / 16 | 500 | 0.5 | Nav label, status |
| `labelSmall` | 11 / 16 | 500 | 0.5 | Overline, dense metadata |
| `codeMedium` | 13 / 18 | 400 | 0 | Logs, hashes, paths |
| `codeSmall` | 11 / 16 | 400 | 0 | Dense log lines |

Rules:

- Never go below 11 sp in the UI; 12 sp is the floor for body metadata.
- Line height is a multiple of 4; do not tighten below 1.3 for body text.
- Numerals in tables and sizes use tabular figures (`FontFeatureSetting("tnum")`).
- Long instance names truncate with an ellipsis at 2 lines, never 1; the full name is in the detail header.
- No all-caps strings except 11 sp overlines with 0.8 sp tracking.

### 4.3 Type in the game overlay

- Minimum 12 sp; 14 sp preferred.
- White 90% on a 28% black scrim; add a 1 dp shadow only if contrast fails on bright scenes.
- The overlay must not use the game's font; it is an Android view.

---

## 5. Spacing, grid and layout

### 5.1 Spacing scale

`0, 2, 4, 8, 12, 16, 24, 32, 48, 64, 96`

- Base unit: **4 dp**.
- Component padding: 12/16 dp.
- Screen margin: 16 dp compact, 24 dp medium, 32 dp expanded.
- Section gap: 24 dp; card gap: 12 dp; list row gap: 0 with a divider inset 16 dp.
- Do not use arbitrary values like 10, 14, 18, 22.

### 5.2 Grid

| Window | Columns | Gutter | Margin | Max content |
|---|---|---|---|---|
| Compact < 600 dp | 4 | 16 dp | 16 dp | — |
| Medium 600–839 dp | 8 | 16 dp | 24 dp | 720 dp |
| Expanded 840–1199 dp | 12 | 24 dp | 32 dp | 960 dp |
| Large ≥ 1200 dp | 12 | 24 dp | 32 dp | 1200 dp |

- Text measure: 60–75 characters; no full-width paragraphs on a tablet.
- Cards may span 1, 2 or 3 columns; a hero spans the full content width.
- The instance list on expanded uses a 2-pane layout: list 360 dp, detail flexible.

### 5.3 Insets

- Edge-to-edge; `WindowInsets.systemBars`, `displayCutout`, `ime`.
- Touch controls use `safeDrawing` plus a 12 dp margin; never place a control under a cutout.
- The bottom navigation bar has 80 dp height plus the navigation inset.
- The game surface draws edge-to-edge; the overlay respects insets.

---

## 6. Shape

### 6.1 Scale

| Token | Radius | Use |
|---|---|---|
| `extraSmall` | 4 dp | Badges, tiny chips |
| `small` | 8 dp | Inputs, small cards |
| `medium` | 12 dp | Cards, menus |
| `large` | 16 dp | Sheets, large cards |
| `extraLarge` | 28 dp | Hero card, dialog, bottom sheet top |
| `full` | 50% | Buttons, chips, avatars |

**Decision:** buttons are full-round; cards are 12–16 dp; the hero card is 28 dp. This combination is recognisably Material 3 Expressive without turning every rectangle into a pill.

### 6.2 Shape rules

- Concentric corners: an inner element's radius = outer radius − padding, floored at 4 dp.
- Do not mix a 28 dp card with an 8 dp button inside it; use 16/12/full.
- Outlined cards use a 1 dp `outlineVariant` border; filled cards use `surfaceContainerLow`.
- The status rule is a 3 dp rounded bar on the leading edge, inset 12 dp vertically.
- The progress bar is full-round; the determinate track is 6 dp, the indeterminate is 4 dp.

---

## 7. Elevation and surfaces

Eclipse uses **tonal surfaces and outlines**, not shadows, except for transient overlays.

| Level | Token | Use |
|---|---|---|
| 0 | `surface` | Page |
| 1 | `surfaceContainerLow` + 1 dp outline | Cards, list sections |
| 2 | `surfaceContainer` | Bottom bar, rail, top app bar on scroll |
| 3 | `surfaceContainerHigh` + shadow 2 dp | Menus, dialogs |
| 4 | `surfaceContainerHighest` + shadow 4 dp | Modal sheets |
| Scrim | 40% black | Behind modal |

- No elevation on list rows; use a divider or spacing.
- No glassmorphism/blur in v1; it costs GPU time and makes contrast unpredictable.
- The in-game overlay is the one place with a scrim, because the background is unknown.

---

## 8. Motion

### 8.1 Principles

- Motion explains a change in state; it does not decorate.
- Fast for small things, slower for spatial changes.
- Never block input during an animation.
- Reduced motion replaces movement with a crossfade ≤ 100 ms.

### 8.2 Tokens

| Token | Duration | Easing | Use |
|---|---|---|---|
| `motionInstant` | 100 ms | linear | Press, ripple |
| `motionFast` | 150 ms | `FastOutSlowIn` | Chips, checkboxes |
| `motionMedium` | 250 ms | `FastOutSlowIn` | Cards, expand/collapse |
| `motionSlow` | 400 ms | `EmphasizedDecelerate` | Sheet, screen transition |
| `motionExpressive` | spring (damping 0.8, stiffness 380) | spring | Hero, FAB, success |
| `motionExit` | 200 ms | `FastOutLinearIn` | Dismiss, delete |

### 8.3 Patterns

- **Screen transition:** shared-axis X for forward/back on compact; fade-through on expanded.
- **Hero card → detail:** container transform is optional; v1 uses a shared-axis slide plus a fade to keep implementation simple.
- **Play button:** press scale 0.98, 100 ms; release spring. No infinite pulse.
- **Launch progress:** determinate bar; the stage label crossfades at 150 ms. No orbiting dots.
- **Success:** a single 250 ms check draw; no confetti.
- **Crash sheet:** slides up 400 ms with `EmphasizedDecelerate`; the scrim fades 200 ms.
- **Reduced motion:** all of the above become 100 ms crossfades; the progress bar still updates.

---

## 9. Iconography

- **Material Symbols Rounded**, 24 dp default, 20 dp inline, 1.5–2 dp stroke.
- Filled variant for active navigation; outlined for inactive.
- Custom icons: instance, loader, Java runtime, renderer, touch control. Draw on a 24 dp grid with 2 dp strokes; no brand logos.
- Loader badges are **text**, not logos: `Fabric`, `Quilt`, `NeoForge`, `Vanilla`. This avoids trademark and asset licensing problems.
- Version badges are text: `26.3`, `26.4-snap-5`.
- Status icons: check, alert triangle, error octagon, progress arc.
- Do not use emoji as icons. Do not use the Minecraft grass block or creeper face.

---

## 10. Components

### 10.1 Buttons

| Variant | Height | Padding | Radius | Use |
|---|---|---|---|---|
| Filled | 48 dp (Play: 56) | 24 dp horizontal | full | Primary |
| Tonal | 48 dp | 24 dp | full | Secondary |
| Outlined | 48 dp | 24 dp | full | Tertiary/neutral |
| Text | 40 dp | 12 dp | full | Inline, low emphasis |
| Icon | 48 × 48 dp | — | full | Toolbar |
| FAB | 56 × 56 dp | — | 16 dp | Create/import |

- Label: `labelLarge`, sentence case.
- Icon + label gap: 8 dp.
- Disabled: 38% content, 12% container; never remove the label.
- Loading: replace the icon with a 16 dp spinner, keep the label, disable repeat taps.
- Destructive: outlined or text with `error` content; a filled red button only in a confirmation dialog.

### 10.2 Cards

- **Hero card:** `surfaceContainerLow`, 28 dp radius, 1 dp outline, 24 dp padding, 16:9 illustration optional.
- **Instance card:** 16 dp radius, 16 dp padding, status rule, title + metadata + trailing overflow.
- **Content card:** 12 dp radius, 12 dp padding, 16:9 thumbnail, title 2 lines max.
- **Stat card:** only in diagnostics; 12 dp radius, label + value + unit.
- Tap target: the whole card; no nested clickable that steals the tap.

### 10.3 List rows

- Height: 72 dp one-line, 88 dp two-line, 104 dp with actions.
- Leading: 40 dp icon/avatar, 16 dp gap.
- Title `titleMedium`, subtitle `bodyMedium` on `onSurfaceVariant`.
- Trailing: status pill, chevron, or overflow; never both chevron and overflow.
- Divider: 1 dp `outlineVariant`, inset 72 dp (aligned to text), optional.
- Swipe actions: only for destructive/archive, with a confirm; not the only way to act.

### 10.4 Status pill

- Height 24 dp, 8 dp horizontal padding, full radius.
- Icon 14 dp + label `labelSmall` + 6 dp gap.
- Background: 12% status color; content: status color (dark theme: 20% background).
- Do not use a dot alone.

### 10.5 Chips and segmented buttons

- Filter chip: 32 dp height, 8 dp horizontal, full radius, 1 dp outline, selected uses `secondaryContainer`.
- Segmented button: 40 dp height, 3–5 segments; selected uses `secondaryContainer`; no more than 5.
- Loader/version chips are non-interactive badges, not filters, unless they filter the list.

### 10.6 Navigation

- **Bottom bar:** 80 dp + inset, 5 items max, icon 24 dp + label 12 dp, active indicator 64 × 32 dp full-round.
- **Rail:** 80 dp wide, 5 items, active indicator 56 × 32 dp, label below.
- **Drawer:** 360 dp wide, header with product mark, sections, footer with version.
- **Top app bar:** 64 dp, title `titleLarge`, actions 48 dp.
- **Tabs:** 48 dp, `titleMedium`, active indicator 3 dp.

### 10.7 Search

- Full-width field, 56 dp, 16 dp radius, leading search icon, trailing clear.
- Debounce 250 ms; show recent searches on focus; no search history sync.
- Empty results: “No results for ‘…’” + a suggestion to check the spelling or browse all.

### 10.8 Dialogs and sheets

- Dialog: 28 dp radius, max width 560 dp, 24 dp padding, title + body + actions right-aligned.
- Bottom sheet: 28 dp top radius, drag handle 32 × 4 dp, max height 90%, scrollable content.
- Destructive confirmation: title names the object, body states what is kept, action is explicit (“Delete instance”), not “OK”.
- Never stack two dialogs.

### 10.9 Progress

- Determinate: 6 dp track, full-round, `primary`; label “412 MB of 650 MB · 3 min left”.
- Indeterminate: 4 dp, only when progress is genuinely unknown; pair with a stage label.
- Per-file progress is shown as a count (“1,204 of 5,147 files”), not a spinner.
- Do not show a percentage that jumps backwards; smooth it.

### 10.10 Snackbars and banners

- Snackbar: `inverseSurface`, 4 dp radius, 16 dp padding, one action max, 4 s.
- Banner: inline at the top of a screen, `statusAttention` container, icon + text + action; dismissible only if not critical.
- Errors that block a task use a sheet, not a snackbar.

### 10.11 Game-specific components

| Component | Spec |
|---|---|
| **Play button** | 56 dp filled, full-round, icon + “Play”, 240 dp min width on tablets, full-width on phones |
| **Device-fit card** | Icon + one-line verdict + “Details”; `statusReady/Attention/Broken` |
| **Launch stage row** | 20 dp icon, label, 16 dp trailing check/spinner; completed rows dim to 70% |
| **Crash panel** | `surfaceContainerHigh`, 16 dp radius, monospace detail block, primary safe-mode button |
| **Touch control** | 56 dp default, 40 dp minimum, 30–100% opacity, 2 dp border at 40% white, haptic on press |
| **Overlay handle** | 48 × 48 dp touch target, 24 × 4 dp visible pill, 60% white, edge-inset 8 dp |
| **Hotbar strip** | 9 slots, 40 dp each, 4 dp gap, only in the game overlay, not the launcher UI |

---

## 11. Data display

- **Sizes:** binary units (`MiB`, `GiB`) with one decimal; show bytes only in diagnostics.
- **Dates:** relative under 7 days (“2h ago”), absolute after (“12 Oct 2026”); locale-aware.
- **Versions:** `26.3`, loader `0.20.1`, runtime `25.0.4`; never prefix with “v” unless the upstream does.
- **Hashes:** first 8 characters in UI with a copy action for the full value.
- **Performance:** FPS as an integer, frame time in ms with one decimal, memory in MiB.
- **Tables:** right-align numbers, use tabular figures, zebra striping only in logs.

---

## 12. Imagery and illustration

- Original geometric line art with a warm paper texture at ≤ 8% opacity.
- One illustration per empty state, max 200 dp tall; no stock photos, no screenshots of Minecraft.
- Screenshots in the README/release notes may show the launcher UI; they must not show a Minecraft world unless the user captured it and consented.
- Iconography and illustrations are Apache-2.0 with the project; record the author in `PROVENANCE.md`.

---

## 13. Accessibility tokens

| Token | Value |
|---|---|
| `minTouchTarget` | 48 dp |
| `preferredTouchTarget` | 56 dp |
| `focusRingWidth` | 2 dp |
| `focusRingOffset` | 2 dp |
| `bodyContrast` | 4.5:1 |
| `largeTextContrast` | 3:1 |
| `maxFontScale` | 200% |
| `reducedMotionDuration` | 100 ms |
| `statusIconSize` | 16 dp |
| `logFontSize` | 12 sp minimum, scalable |

- All interactive elements expose `Role.Button`, a label, and state (`disabled`, `selected`).
- Progress exposes `progressBarRangeInfo`; do not announce every percent.
- The status rule is decorative; the status text is the accessible label.
- Touch controls expose custom actions for TalkBack (“Move forward”, “Jump”, “Attack”).
- The game surface is not accessible; the overlay is.

---

## 14. Compose implementation

### 14.1 Module and packages

```
:designsystem
  dev.shadow.eclipselauncher.designsystem
    EclipseTheme.kt
    Color.kt        // brand roles, semantic roles
    Type.kt         // scale, code styles
    Shape.kt
    Motion.kt
    Dimens.kt       // spacing, sizes
    component/      // only components with project semantics
    preview/        // @Preview catalog
```

Screens depend on `:designsystem`, never on raw hex values. A lint rule or a `grep` gate fails the build on `Color(0x` outside the design system.

### 14.2 Theme API

```kotlin
@Composable
fun EclipseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit
)
```

- `dynamicColor` is a user setting, default `false`.
- `reducedMotion` is derived from system settings and the in-app override.
- The theme provides `MaterialTheme`, `LocalEclipseSpacing`, `LocalEclipseMotion`, `LocalEclipseStatusColors`.
- Do not fork Material components unless the semantics differ; wrap them in project components (`EclipseButton`, `EclipseStatusPill`, `EclipseInstanceCard`).

### 14.3 Component contract

Every component:

- accepts `modifier: Modifier = Modifier` as the first optional parameter;
- exposes `contentDescription` or a semantic label where the content is iconic;
- has a `@Preview` for light, dark, large font, RTL and reduced motion;
- has a screenshot test in CI for the default and error states;
- documents its minimum touch target and its disabled state.

### 14.4 Tokens as code

```kotlin
object EclipseSpacing {
    val xs = 4.dp; val s = 8.dp; val m = 12.dp; val l = 16.dp
    val xl = 24.dp; val xxl = 32.dp; val xxxl = 48.dp
}

object EclipseMotion {
    const val instant = 100
    const val fast = 150
    const val medium = 250
    const val slow = 400
}
```

- Use `Dp` and `sp`, never raw pixels.
- Use `MaterialTheme.colorScheme` for standard roles and `LocalEclipseStatusColors` for status.
- Use `Modifier.semantics { }` for custom roles; do not rely on clickable alone.

### 14.5 Preview and screenshot testing

- A `preview/` catalog renders every component and key screen in 5 configurations: light, dark, 200% font, RTL, compact/expanded.
- CI runs screenshot tests on a pinned emulator or Roborazzi/Paparazzi (choose one; do not run both).
- A visual diff fails the build only for the design-system module; app screens are reviewed, not pixel-gated, until the layout stabilises.
- The screenshot baseline is versioned; updating it requires a design review note.

---

## 15. Do and don't

| Do | Don't |
|---|---|
| One filled button per screen | Three competing filled buttons |
| “Minecraft could not start. GLES 3.1; needs 3.2.” | “Error: OPENGL_MISSING” |
| 56 dp Play button in the lower third | 32 dp Play link in the app bar |
| Text badge “Fabric 0.20.1” | Fabric logo with no version |
| Status pill with icon + label | A red dot with no text |
| 12–16 dp cards, 28 dp hero | Every surface at 28 dp |
| Warm neutral surfaces | Pure black + neon accent |
| Monospace for hashes/logs | Monospace for body text |
| 100–400 ms motion | 1 s decorative transitions |
| Dynamic color as an opt-in | Dynamic color that breaks status semantics |

---

## 16. Design-system governance

- **Owner:** design lead + Android lead.
- **Source of truth:** the `:designsystem` module and the Figma library; a token change lands in both in the same week.
- **Versioning:** semantic; a breaking token rename bumps the major.
- **Review:** any new component needs a use case, an accessibility note, and a screenshot in light/dark/large-font.
- **Deprecation:** mark `@Deprecated` with a replacement; remove after two releases.
- **Contribution:** a new component starts in the feature module; it moves into `:designsystem` only when a second feature needs it.

---

## 17. Decisions for sign-off

1. **Brand palette by default; dynamic color opt-in.**
2. **System font + monospace for truth; no bundled display face in v1.**
3. **Warm neutral surfaces, amber primary, teal secondary; no gamer neon.**
4. **Full-round buttons, 12–16 dp cards, 28 dp hero/dialog.**
5. **Tonal elevation, no blur/glassmorphism in v1.**
6. **Material Symbols Rounded; text badges for loaders/versions; no Minecraft or third-party logos.**
7. **Motion tokens with a reduced-motion path; no decorative loops.**
8. **WCAG 2.2 AA, 48 dp targets, 200% font scale as release gates.**
9. **`:designsystem` owns tokens; screens may not hard-code colors.**
10. **Screenshot tests for the design system, not for every app screen.**

## 18. Open questions

- Does the final Material tonal palette preserve the warm/paper character, or does it drift cool?
- Is Roboto's variable weight available on the minimum API, or does the theme need static weights?
- Does the arc wordmark read at 16 dp, or does it need a simplified glyph?
- Should the game overlay use the same type scale, or a denser one for 720p tablets?
- Which screenshot-test framework is stable with AGP 9.4 and Compose 1.9?
- Do dynamic-color users lose the status palette's contrast guarantees, and does the theme need a fallback?
