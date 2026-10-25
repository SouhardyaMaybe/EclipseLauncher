# 09 — Design system

**Status:** signed off 2026-10-25. This revision replaces the warm-paper Material direction with the Ore-inspired, pixel-chrome system that ships in v1.
**Scope:** the visual and interaction language of Eclipse Launcher: palette, type, shape, spacing, iconography, components, motion, accessibility tokens and the Compose implementation contract.
**Dependencies:** Jetpack Compose, Material 3 foundations (components are project-owned), Monocraft 4.2.1 (SIL OFL 1.1), Android 8.0+.
**Labels:** **Decision**, **Verified**, **Inference**, **Open**.

---

## 1. Design direction

### 1.1 The idea: a Minecraft launcher that feels built, not skinned

The signed-off direction is **Ore-inspired**: dark stone surfaces, a green primary action, square bevels, thin borders and small original pixel glyphs. It borrows the *material logic* of a block game — hard edges, pressed-in controls, visible seams — without copying a single texture, logo or screen.

Three adjectives drive every choice:

- **Direct** — the player sees five jobs and one Play button, not a dashboard.
- **Tactile** — controls look pressed or raised; the press state is the motion.
- **Honest** — no fake progress, no invented catalogue rows, no decorative status.

### 1.2 What it is not

- Not a Minecraft skin: no grass block, creeper face, Mojang logo, copied GUI texture or official launcher asset.
- Not Material You: no dynamic color, no large rounded cards, no floating action button, no bottom navigation.
- Not "gamer neon": no gradients, glow, scanlines or animated backgrounds.
- Not the reference launcher's assets: the layout is studied, the pixels and code are original.

### 1.3 Signature elements

1. **The bevel** — a square control with a light 2 dp top edge and a dark 3 dp bottom edge; pressing swaps the face color. No shadow.
2. **The pixel glyph** — an original 12 × 12 grid icon drawn as rectangles; 16–30 dp in chrome.
3. **The section seam** — a 2 dp divider under a centered heading, and 1 dp dividers between rows.
4. **Monocraft for chrome** — labels, buttons, tabs and headings; system sans for prose, system monospace for logs and paths.

---

## 2. Color

### 2.1 Palette strategy

Eclipse is **dark-only** in v1. The palette is fixed so contrast and the green primary action are predictable on every device. There is no dynamic color and no light theme.

The tokens live in `designsystem/theme/EclipseColors.kt`; screens use `EclipsePalette` or `MaterialTheme.colorScheme`, never raw hex values.

### 2.2 Core tokens

| Token | Value | Use |
|---|---|---|
| `Canvas` | `#1C1D1D` | Page background |
| `Panel` | `#2A2B2C` | Dialogs, panels, text-field containers |
| `Border` | `#39393B` | Dividers, outlines, disabled edges |
| `Control` | `#39393B` | Neutral button face, selected row |
| `ControlHover` | `#47484A` | Neutral top edge, hover |
| `ControlPressed` | `#2A2B2C` | Neutral pressed face |
| `Primary` | `#3B8526` | Play and primary actions |
| `PrimaryHover` | `#4B9736` | Primary hover |
| `PrimaryPressed` | `#2F6B1E` | Primary pressed face |
| `PrimaryTop` | `#5AAE43` | Primary top bevel |
| `PrimaryBottom` | `#1F4A12` | Primary bottom bevel |
| `Text` | `#FFFFFF` | Primary text |
| `Muted` | `#B0B0B0` | Secondary text, metadata |
| `Disabled` | `#737476` | Disabled text and icons |
| `Focus` | `#9CD78C` | Focus ring, focused field border |
| `Danger` | `#FF8A80` | Destructive text/icon |
| `DangerFill` | `#C93636` | Task badge |
| `Warning` | `#FDDC69` | Warnings, truncated preview notice |
| `Success` | `#9CD78C` | Enabled/ready text |
| `Link` | `#6FA8FF` | Future links |
| `AccountBar` | `#242424` | Top bar |
| `BottomBar` | `#232323` | Instance/Play bar |
| `Overlay` | `#464646` | Overlay surfaces |

### 2.3 Material roles

`EclipseColorScheme` maps the palette onto Material 3's dark scheme: `background = Canvas`, `surface = Panel`, `primary = Primary`, `onPrimary = Text`, `outline = Border`, `error = Danger`, `surfaceContainer*` = the control/panel family. Components may use `MaterialTheme.colorScheme` for standard roles, but project chrome uses `EclipsePalette` directly.

### 2.4 Contrast rules

- Body text: ≥ 4.5:1 against its surface.
- Large text: ≥ 3:1.
- Icons and UI boundaries: ≥ 3:1.
- Focus ring: ≥ 3:1 against both the control and the background.
- Disabled text: not required by WCAG, but `Disabled` is kept legible (≥ 3:1 against `Canvas`).
- Status is never color-only. `Success`, `Warning` and `Danger` always appear with a label or icon.

**Verified:** white on `#3B8526` passes 4.5:1 for the Play label. `Muted` on `Canvas` passes 4.5:1. `Danger` on `Panel` passes 4.5:1. Re-check if a token changes.

### 2.5 Color usage rules

- One primary action per screen. In v1 that is usually **Play** or **Save**.
- Destructive actions use `Danger` as text/icon. A filled red button is reserved for a confirmation dialog.
- Never use color to distinguish Vanilla from Fabric; use the loader name.
- Do not use `Primary` for decoration. If it is green, it is actionable.

---

## 3. Typography

### 3.1 Families

| Role | Family | Why |
|---|---|---|
| Chrome | **Monocraft** 4.2.1 | Pixel geometry for headings, buttons, tabs, labels and menu rows. OFL-1.1; license ships in `docs/licenses/Monocraft-OFL.txt`. |
| Prose | `FontFamily.SansSerif` (Roboto/system) | Long body copy, dialogs and explanations. |
| Truth | `FontFamily.Monospace` | Log bodies, paths, versions in technical views. |

Monocraft is bundled in `designsystem/src/main/res/font/`. No other font is bundled. The OFL notice appears in Settings → About and in the repository.

### 3.2 Type scale

| Token | Size / line height | Family | Use |
|---|---|---|---|
| `headlineMedium` | 20 / 26 | Monocraft | Large screen heading |
| `headlineSmall` | 18 / 24 | Monocraft | Secondary heading |
| `titleLarge` | 20 / 26 | Monocraft | Centered section title, dialog title |
| `titleMedium` | 16 / 22 | Monocraft | Top-bar title, instance name |
| `titleSmall` | 14 / 20 | Monocraft | Panel title |
| `bodyLarge` | 16 / 24 | System sans | Body copy |
| `bodyMedium` | 14 / 20 | System sans | Secondary body |
| `bodySmall` | 12 / 18 | System sans | Fine print, captions |
| `labelLarge` | 14 / 20 | Monocraft | Buttons, menu rows, tabs |
| `labelMedium` | 12 / 16 | Monocraft | Selection bar, dense labels |
| `labelSmall` | 11 / 16 | Monocraft | Metadata, overlines, badges |

Rules:

- Never go below 11 sp. Log text may use 12 sp monospace with 18 sp line height.
- Primary actions use `labelLarge` and may wrap to two lines; never ellipsize a button label into meaninglessness.
- Uppercase is allowed for section overlines and status chips; not for body copy.
- At 200% font scale, rows grow; headings wrap; the layout must not clip.

### 3.3 Type in the game overlay

The overlay uses the same scale but a denser layout: 12–14 sp labels, 48 dp minimum controls, high-contrast white on a dark scrim. It does not reuse Monocraft for chat or long text.

---

## 4. Spacing, grid and layout

### 4.1 Spacing scale

`4, 8, 12, 16, 22, 24, 32, 48 dp`.

Common values:

| Use | Value |
|---|---|
| Screen horizontal padding | 16 dp (22 dp for text blocks) |
| Menu row vertical padding | 12 dp |
| Menu row minimum height | 66 dp |
| Button horizontal padding | 16 dp |
| Button vertical padding | 14 dp |
| Section title top padding | 24–32 dp |
| Divider after heading | 12 dp below the title |
| Gap between full-width loader buttons | 12 dp vertical |
| Bottom bar horizontal padding | 8 dp |
| Bottom bar vertical padding | 8 dp |

### 4.2 Grid

The launcher is a single column on phones. On tablets and Chromebooks the column stays centered and capped at a readable width (about 720 dp) rather than turning into a multi-pane dashboard. Dialogs cap at 560 dp.

### 4.3 Insets

- The account bar draws its background through the status-bar inset and pads its content with `statusBarsPadding()`.
- Pushed screens use the same top-bar treatment.
- The bottom instance bar draws through the navigation-bar inset and pads with `navigationBarsPadding()`.
- Scrollable content never hides its last row behind the bottom bar; the bar is part of the column, not an overlay.

---

## 5. Shape and elevation

### 5.1 Shape scale

| Token | Value | Use |
|---|---|---|
| `RectangleShape` | 0 dp | Buttons, panels, dialogs, text fields |
| `CircleShape` | 50% | Task badge only |
| Pills | none | No pill buttons or rounded chips in v1 |

### 5.2 Rules

- Square corners are the default. A rounded corner must have a reason; in v1 only the task badge is round.
- Borders are 1–2 dp. The section divider under a heading is 2 dp; list dividers are 1 dp.
- The bevel is drawn, not elevated: 2 dp top edge, 3 dp bottom edge.
- No shadows, blur, glassmorphism or tonal elevation. Depth comes from the bevel and the surface value.

---

## 6. Iconography

### 6.1 Original pixel glyphs

All chrome icons are original 12 × 12 patterns drawn by `EclipsePixelIcon`. Each cell is a rectangle; there is no anti-aliased vector, no icon font and no third-party icon pack.

| Kind | Use |
|---|---|
| `ACCOUNT` | Account bar, Settings → Account |
| `CONTROLS` | Quick Actions → Controls |
| `LOGS` | Quick Actions → Manage Logs |
| `FOLDER` | Quick Actions → File Manager, folder rows |
| `CONTENT` | Quick Actions → Manage Content, empty states |
| `JAR` | Quick Actions → Install a .jar |
| `TASKS` | Tasks button |
| `SETTINGS` | Settings button |
| `CHEVRON_RIGHT` | Navigable row, folder row |
| `CHEVRON_UP` / `CHEVRON_DOWN` | Instance selector, filter menus |
| `FILE` | File rows |
| `EDIT` | Instance edit pencil |
| `SEARCH` | Reserved for the catalogue search |
| `PLUS` | Reserved for create actions |
| `CHECK` | Reserved for selection |
| `WORLD` | Reserved for world rows |
| `CLOSE` | Dialog dismiss |
| `MORE` | Overflow |

### 6.2 Rules

- Icons are decorative when paired with a label; they are skipped by TalkBack.
- Icon-only buttons have a content description and a 48 dp target.
- Do not use emoji, Material Symbols or the game's textures.
- Loader and version badges are text, never logos.
- The task badge is a number, not a dot.

---

## 7. Components

### 7.1 Account bar

- Full-width `AccountBar` background, status-bar inset.
- Left: pixel account glyph, account name (`labelLarge`), "Local account" (`labelSmall`, muted). The whole left area opens Accounts.
- Right: **Tasks** icon button (22 dp glyph in a 48 dp target) with an optional numeric badge, then **Settings**.
- The tasks icon sits immediately left of settings. There is no search icon in v1.

### 7.2 Quick Actions menu row

- Full-width row, minimum 66 dp, 22 dp start padding, 16 dp end padding.
- Leading 30 dp pixel icon, 22 dp gap, label `labelLarge`, trailing 16 dp chevron.
- 1 dp divider between rows.
- Press state uses the standard indication; the row has no card background.
- Disabled rows dim to `Disabled` and do not navigate.

### 7.3 Bevel button

- Full-width by default; square; minimum 48 dp, 56 dp for Play and loader buttons.
- Face: `Primary` or `Control`; pressed: `PrimaryPressed` or `ControlPressed`; disabled: `Control` with `Disabled` text.
- Top edge 2 dp (`PrimaryTop` / `ControlHover`); bottom edge 3 dp (`PrimaryBottom` / `Canvas`).
- Label `labelLarge`, centered, up to two lines.
- No icon by default; the Play button relies on its label and position.
- Minimum touch target 48 dp even when the visual height is smaller.

### 7.4 Tabs

- Text tab with 16 dp horizontal and 12 dp vertical padding.
- Selected: `Text` color plus a 24 × 2 dp `Primary` underline.
- Unselected: `Muted`; disabled: `Disabled`.
- Used for **Installed / Discover** and the six content types. Content-type tabs scroll horizontally.
- Tabs do not use pills or filled backgrounds.

### 7.5 Section label and centered heading

- **Section label:** uppercase `labelSmall`, muted, 22 dp start padding, 20 dp top and 8 dp bottom. Used in Settings.
- **Centered heading:** `titleLarge`, centered, 24–32 dp top padding, followed by a 2 dp divider. Used for Quick Actions and Create Instance section titles.

### 7.6 Status chip

- 1 dp border in the status color, 6 dp horizontal and 2 dp vertical padding, uppercase `labelSmall`.
- Used for the Local account chip and future status labels.
- Always paired with text; no dot-only status.

### 7.7 Panel

- `Panel` background, 2 dp `Border`, square corners, 16 dp padding.
- Used for the CurseForge key block and future grouped settings.
- No elevation, no rounded corners.

### 7.8 Empty state

- Centered column, 24 dp padding, 12 dp gaps.
- 48 dp muted pixel glyph, `titleMedium` title, `bodyMedium` body, optional text action.
- One sentence for the body; no illustration, no carousel.

### 7.9 Dialogs

- `AlertDialog` with `RectangleShape`, `Panel` container, `Text` title, `Muted` body.
- Buttons are text buttons; destructive confirmations use `Danger` for the confirm label.
- Title names the object; body states what is kept or lost; confirm label is a verb ("Delete", "Rename"), not "OK".
- Never stack two dialogs.

### 7.10 File rows and path bar

- **Path bar:** relative path (`labelSmall`, muted) with optional trailing actions; two lines maximum.
- **File row:** 52 dp minimum, optional 20 dp selection box, 24 dp folder/file glyph, name `labelLarge`, detail `labelSmall`, trailing chevron for folders.
- Selected row uses `Control`; selected icon uses `Primary`.
- 1 dp divider inset to the text start.
- Tap opens a folder or the text viewer; selection mode toggles a checkbox instead.

### 7.11 Selection action bar

- Header row: "N selected" plus **Done**.
- Action row: horizontally scrollable text buttons — **Copy**, **Cut**, **Paste**, **Rename**, **Delete**, **Share**.
- Paste is enabled only when the clipboard has entries; rename only for one item; share only for files.
- Destructive actions open a confirmation dialog.

### 7.12 Top bar (pushed screens)

- 52 dp minimum, `AccountBar` background, status-bar inset.
- Back icon button (48 dp), title `titleMedium`, optional trailing actions.
- Back label is "Back" for TalkBack.

### 7.13 Task badge and Tasks dialog

- Badge: 16 dp minimum circle, `DangerFill`, white `labelSmall` number, top-end of the Tasks button.
- Dialog: title **Tasks**, empty state or a scrollable list of title/detail/actions.
- Each task: **Pause** or **Resume**, plus **Cancel**. No fake percentage.
- The badge counts registry entries; it is not a decoration.

---

## 8. Motion

- Motion is limited to press states, dialog transitions and short fades.
- Pressed buttons change face and edge colors immediately; no scale bounce.
- No decorative loops, parallax, shimmer or animated gradient.
- Reduced motion removes fades; the press color remains as the only feedback.
- Progress, when a real task exists, is determinate and updates from real bytes. Indeterminate spinners are allowed only when the operation genuinely has no measurable progress, and must carry a stage label.

---

## 9. Accessibility tokens

| Token | Value |
|---|---|
| Minimum touch target | 48 × 48 dp |
| Menu row height | ≥ 66 dp |
| Body text contrast | ≥ 4.5:1 |
| Large text contrast | ≥ 3:1 |
| Focus ring | 2 dp `Focus` |
| Font scale support | 200% |
| Text floor | 11 sp |
| Icon-only button label | required |
| Selection state | text + visual, not color alone |

**Release gates:** TalkBack can read every screen; every primary action is reachable by D-pad; 200% font scale does not clip a label or hide a button; no unlabelled spinner; no raw exception or `null` in UI.

---

## 10. Compose implementation

### 10.1 Module and packages

```
:designsystem
  dev.shadow.eclipselauncher.designsystem
    theme/
      EclipseColors.kt        // palette + Material scheme
      EclipseTypography.kt    // Monocraft + system families
      EclipseShapes.kt        // square shapes
      EclipseStatusColors.kt  // semantic status
      EclipseTheme.kt         // MaterialTheme + status local
    component/
      EclipseComponents.kt    // pixel icons, bevel, rows, tabs, panels, top bar
:app
  ui/
    EclipseApp.kt             // single-screen back stack and dialogs
    components/               // account bar, bottom bar, file rows, dialogs
    screens/                  // Home, Create, Logs, Files, Content, Settings, Accounts, Controls, Viewer
```

Screens depend on `:designsystem` for tokens and project components. They do not define raw hex colors.

### 10.2 Theme API

```kotlin
@Composable
fun EclipseTheme(content: @Composable () -> Unit)
```

- Dark-only; no `darkTheme` or `dynamicColor` parameter in v1.
- Provides `MaterialTheme` with `EclipseColorScheme`, `EclipseTypography`, `EclipseShapes` and `LocalEclipseStatusColors`.
- `EclipsePalette` is the token source; `EclipseColorScheme` maps it onto Material roles.

### 10.3 Component contract

Every component:

- accepts `modifier: Modifier = Modifier` as the first optional parameter where it makes sense;
- exposes a content description or a visible label for iconic content;
- uses a 48 dp minimum touch target;
- has a disabled state that keeps its label;
- uses `EclipsePalette` tokens, not raw colors;
- avoids fake progress and decorative animation.

### 10.4 Typography and font license

Monocraft 4.2.1 is licensed under the SIL Open Font License 1.1. The full text is at `docs/licenses/Monocraft-OFL.txt`; the Settings → About screen names the license. The font files are binary assets, not source code, and are recorded in `PROVENANCE.md`.

### 10.5 Preview and screenshot testing

- A component preview catalog is desirable, but v1 does not gate screens on pixel diffs.
- CI builds debug and release APKs and checks the manifest and native alignment.
- Screenshot tests, when added, should cover the main page, Create Instance, Manage Content tabs, a file list, and a dialog at 200% font scale.
- Do not introduce a screenshot framework that cannot run on the pinned CI image.

---

## 11. Do and don't

| Do | Don't |
|---|---|
| One green Play button in the bottom bar | Three competing filled buttons |
| Square bevels and 2 dp borders | Rounded cards with shadows |
| Original 12 × 12 pixel glyphs | Material Symbols, emoji or game textures |
| Monocraft for chrome, system sans for prose | Monocraft for long paragraphs |
| "Catalogue not connected" | Invented project rows |
| Real task count on the badge | A decorative red dot |
| 66 dp menu rows | 32 dp icon-only rows |
| 200% font scale checked | Ellipsized primary labels |
| Text + icon for status | Color alone |

---

## 12. Design-system governance

- **Owner:** design lead + Android lead.
- **Source of truth:** `:designsystem` in this repository.
- **Token changes:** update `EclipseColors.kt` / `EclipseTypography.kt` and the tables in this document in the same change.
- **New components:** start in the feature module; move into `:designsystem` only when a second feature needs them.
- **Deprecation:** mark `@Deprecated` with a replacement; remove after two releases.
- **Asset review:** every new glyph or font needs an authorship note and a license entry in `PROVENANCE.md`.

---

## 13. Decisions for sign-off

1. **Ore-inspired dark palette, fixed; no dynamic color or light theme in v1.**
2. **Monocraft for chrome, system sans for prose, system monospace for logs.**
3. **Square shapes, bevels and 1–2 dp borders; no rounded cards or shadows.**
4. **Original 12 × 12 pixel glyphs; no Material Symbols or game textures.**
5. **One Quick Actions section; no bottom navigation or dashboard.**
6. **Green is reserved for actions; destructive actions use red text/icon.**
7. **48 dp targets, 66 dp menu rows, 200% font scale and WCAG 2.2 AA are release gates.**
8. **No fake progress and no decorative motion.**
9. **`:designsystem` owns tokens and components; screens do not hard-code colors.**
10. **The Monocraft OFL notice ships in the app and the repository.**

## 14. Open questions

- Does Monocraft render legibly at 11 sp on low-density phones, or should `labelSmall` move to 12 sp?
- Does the pixel glyph set need a dedicated world and shader icon before Manage Content ships?
- Should the task badge use `DangerFill` or a neutral high-contrast color when tasks are paused?
- Does the single-column tablet layout need a wider content cap for file lists?
- Which screenshot-test framework is stable with AGP 9.4 and the pinned Compose BOM?
- Do dynamic-color users lose the status palette's contrast guarantees, and does the theme need a fallback?
