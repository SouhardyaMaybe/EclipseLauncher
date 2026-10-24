# 08 — Product design

**Status:** product draft for sign-off, 2026-10-24.
**Scope:** who Eclipse Launcher is for, what it promises, how the screens fit together, what every state looks like, how touch and controller input work, and how the product recovers when the game does not start. Visual tokens and component specifications are in [09 — Design system](09-design-system.md); the research behind them is in [10 — Design research and skills](10-design-research-and-skills.md).
**Evidence:** the competitor teardown in [01](01-landscape.md), the Android constraints in [04](04-android-platform.md), the launch/runtime model in [05](05-runtime-and-graphics.md), and the installer contract in [07](07-install-pipeline.md).
**Labels:** **Verified**, **Observed**, **Inference**, **Decision**.

---

## 1. The product in one page

> **One tap to play. If it breaks, we tell you why and fix it.**

Eclipse Launcher is an Android launcher for **Minecraft: Java Edition**. It is not a game store, not a social network, and not a settings museum. It exists to do three things well:

1. **Get a player into a world quickly** on a phone, tablet, foldable or Chromebook.
2. **Make modded play understandable** without turning the home screen into a control panel.
3. **Turn failure into a next step** instead of a black screen, a silent crash or a log file the player cannot read.

### 1.1 What v1 is

| Area | v1 |
|---|---|
| Game versions | Current release (26.3) and snapshots; older releases on request |
| Loaders | Vanilla, **Fabric** |
| Content | **Modrinth** search, install, update; `.mrpack` packs |
| Accounts | Microsoft device-code sign-in; multiple accounts |
| Renderer | Native Vulkan 1.2 when the probe passes; otherwise Sandpaper GL 3.3 → GLES 3.2 |
| Input | Touch overlay, physical mouse/keyboard, gamepad |
| Distribution | GitHub Releases; no Play at launch |
| Recovery | Crash detection, safe mode, log/report export, component repair |

### 1.2 What v1 is not

- Not Play Store-first. Play's executable-code policy conflicts with downloading `client.jar` and runtimes.
- Not a piracy tool. No offline auth, no alt manager, no bundled game files.
- Not a Bedrock launcher.
- Not a general-purpose Android file manager or emulator front-end.
- Not a social platform. No friends list, chat network or marketplace of our own.
- Not a benchmarking tool. Performance numbers exist to help the player, not to win screenshots.

### 1.3 The scope boundary

| Deferred to v1.1 | Why |
|---|---|
| Quilt, NeoForge | Installer processors and a second loader matrix need their own test cycle |
| Forge creation | Import-only until Foreman is proven |
| CurseForge | API key, terms and rate limits; Modrinth covers the v1 content story |
| OptiFine | Proprietary, invasive, unsupported |
| Sash / custom AWT | v1 uses real headless `java.desktop`; Sash is a later compatibility layer |
| Shader packs | Iris/Sodium are OpenGL renderer mods; support is content-dependent, not a v1 promise |
| Resolution scaling, HDR | Surface and swapchain complexity; not needed for first-play |
| Cloud sync | Account and privacy surface; local export is enough |

---

## 2. Who it is for

### 2.1 Primary: the returning Java player

- Has a Microsoft account and owns Java Edition.
- Plays on a phone or tablet because the PC is not always available.
- Knows what “Fabric” and “shaders” mean, but does not want to configure a JVM.
- Tried a legacy launcher, hit a crash or a 403, and does not know which layer failed.
- Wants: sign in, pick a version, play; keep the world; not lose settings.

**Design consequence:** the default path must never ask about heap, GC, renderer or JVM flags. Those exist, but behind “Advanced”.

### 2.2 Secondary: the modpack player

- Installs packs from Modrinth, often 100–300 mods.
- Cares about pack version, loader version, mod updates and world compatibility.
- Willing to read a log if the launcher points at the right line.
- Wants: install a pack, update it without losing the world, disable a mod when it breaks.

**Design consequence:** instance detail is a first-class screen, not a long-press menu. Pack metadata, loader and mod list are visible without entering settings.

### 2.3 Tertiary: the couch/controller player

- Plays on a tablet or Android TV-style device with a gamepad.
- May never touch the screen after setup.
- Wants: focus that never gets lost, large targets, no hover-only affordances, a way to open the pause/overlay without a mouse.

**Design consequence:** every screen is fully operable with a D-pad and two buttons; focus is visible and restored; the game overlay is reachable from the controller.

### 2.4 Quaternary: the parent/guardian

- Sets up the device for a child.
- Needs to understand what will be downloaded, how much space it takes, and whether a Microsoft Family is required.
- Wants: no surprise purchases, no hidden downloads, clear consent.

**Design consequence:** the install summary names sizes and sources; child-account errors explain the Family step; no dark-pattern “continue” that hides a 650 MB download.

### 2.5 Anti-persona

The user who wants to run five instances simultaneously, benchmark every GPU, or manage 40 accounts. Eclipse can be extended later; v1 optimises for one active game and a small number of instances.

---

## 3. Jobs to be done

| Job | Current pain | Eclipse response |
|---|---|---|
| “Let me play the version I already have.” | Launcher updates, account expiry, renderer confusion | Home shows the last instance with one primary **Play**; the plan is precomputed |
| “Install this modpack.” | Format ambiguity, loader mismatch, failed downloads | `.mrpack` import with a resolved dependency summary and a single install transaction |
| “Why did it crash?” | Log file, no context, “exit code 1” | Structured report: stage, signal, library, last lines, one-tap safe mode |
| “Make the controls usable.” | Desktop keybind assumptions | Touch overlay with presets and per-control sizing; gamepad mapping |
| “Don't lose my world.” | Launcher wipes or re-creates the instance | Transactional installs, user files never in the managed manifest, export |
| “Tell me the truth about my device.” | “Vulkan on/off” toggle that lies | Probe result with the exact missing extension/feature and a recommended path |
| “Sign in without typing a password on a TV.” | Redirect flows and WebViews | Device code with a short, copyable code and a browser on any device |

---

## 4. Product principles

1. **Play is the verb.** Every screen has one obvious next action. If a screen has three equal-weight buttons, it is wrong.
2. **Evidence over toggles.** Show the probe result, the measured heap, the actual missing extension. Do not offer a switch that cannot work.
3. **Progressive disclosure.** Defaults are good; advanced controls are one level down and searchable.
4. **Never lie about state.** “Installed” means verified. “Running” means the surface is alive. “Crashed” means we have a reason.
5. **Recovery is a feature, not an error page.** The crash screen is designed with the same care as the home screen.
6. **Respect user content.** Worlds, options, screenshots and manually added mods are never deleted by repair, update or uninstall without explicit confirmation.
7. **Offline shell.** The launcher opens, lists instances and reads logs without a network. Only installation and sign-in require connectivity.
8. **One-handed by default.** Primary actions live in the lower third on phones; destructive actions do not.
9. **Accessible is the baseline.** Large text, TalkBack, switch access, reduced motion and high contrast are not a later theme.
10. **No dark patterns.** No fake urgency, no pre-checked bundles, no hidden downloads, no “rate us” interruption.

---

## 5. Success metrics

Metrics are **local-first and opt-in**. The launcher computes them on-device; nothing is uploaded unless the user exports a report or opts into diagnostics.

| Metric | Definition | v1 target |
|---|---|---|
| Time to first world | First launch → world fully loaded, median on T1 | < 4 min after install |
| First-launch success | Launches reaching the main menu / total first launches | ≥ 85% on supported devices |
| Launch success (steady state) | Launches reaching a world / total launches | ≥ 95% |
| Recovery success | Crashed launches recovered by safe mode without reinstall | ≥ 70% |
| Install completion | Installs completed / started, excluding user cancel | ≥ 90% |
| Time to install vanilla | 100 Mbit/s, T1 device | < 6 min |
| Touch-control adoption | Sessions with overlay enabled / game sessions | Informational |
| Account auth success | Device-code flows completed / started | ≥ 80% (excluding user cancel) |
| Crash-report export | Crashed sessions with an exported report | ≥ 40% |

**Guardrails:** uninstall rate after a crash, support issues per 1,000 installs, ANR rate, UI-process OOM rate. A feature that improves time-to-play but raises crash-related uninstalls is not a win.

---

## 6. Information architecture

### 6.1 Top level

```
Home            Instances          Discover           Downloads         Settings
├─ Continue      ├─ All             ├─ Modpacks        ├─ Active          ├─ Account
├─ Last played   ├─ Vanilla         ├─ Mods            ├─ Queued          ├─ Appearance
├─ Quick actions ├─ Modded          ├─ Resource packs  └─ History         ├─ Controls
└─ Status        └─ Import          └─ Shaders?*                         ├─ Storage
                                                                          ├─ Diagnostics
                                                                          └─ About
```

`*` Shaders are content-dependent; the Discover tab shows them only when a compatible renderer mod is present or the user asks.

### 6.2 Adaptive navigation

| Window class | Navigation | Content |
|---|---|---|
| Compact (< 600 dp) | Bottom bar, 5 destinations | Single pane; detail pushes |
| Medium (600–839 dp) | Navigation rail | List-detail |
| Expanded (≥ 840 dp) | Permanent drawer | List-detail-detail |
| Desktop/freeform | Drawer + toolbar | Resizable panes |

Built with Compose Material 3 adaptive APIs and Navigation 3. The game activity is separate and full-screen; it does not participate in the adaptive layout.

### 6.3 Why five destinations

Home, Instances, Discover, Downloads, Settings map to the five jobs. Account is not a destination; it is a chip in the top bar and a section in Settings. Logs are not a destination; they live in instance detail and diagnostics.

**Decision:** no “store” tab. Discover is content discovery, not commerce. There are no prices, carts or checkout.

---

## 7. Screen-by-screen

### 7.1 Home

```
┌──────────────────────────────────────────────┐
│  Eclipse            [account chip]  [search] │
│                                              │
│  ┌────────────────────────────────────────┐  │
│  │  Continue                              │  │
│  │  Aether SMP · Fabric 0.20.1 · 26.3     │  │
│  │  Last played 2h ago · 1.2 GB           │  │
│  │                                        │  │
│  │            [   PLAY   ]                │  │
│  └────────────────────────────────────────┘  │
│                                              │
│  Quick actions                               │
│  [ New instance ] [ Import pack ] [ Repair ] │
│                                              │
│  Recent                                      │
│  • Creative Test · 26.3 · yesterday          │
│  • Vanilla 26.3 · 3 days ago                 │
│                                              │
│  [Home] [Instances] [Discover] [Downloads] [Settings] │
└──────────────────────────────────────────────┘
```

Rules:

- The hero card is the **active instance**, not a generic welcome. If none exists, it becomes a “Create your first instance” card.
- **Play** is the largest target on the screen, at least 56 dp tall, lower half on phones.
- The status line answers: what version, what loader, when last played, how big.
- If the account is expired or the instance is broken, the primary button changes to **Fix** / **Sign in** with a one-line reason.
- “Quick actions” are text buttons, not a grid of six icons.
- Recent instances are a compact list; tapping opens detail, long-press offers Play/Export/Delete.

**Empty state:** a single illustration, one sentence, and **Create instance**. No carousel, no feature tour.

### 7.2 Instances

- Segmented filter: **All / Vanilla / Modded / Packs**.
- Each row: icon or generated color tile, name, version + loader, size, last played, status dot.
- Sort: last played (default), name, size, date created.
- Multi-select via long-press for export/delete; no drag-to-reorder in v1.
- FAB: **New instance**; secondary action in overflow: **Import**.

**Status vocabulary:** Ready, Needs repair, Needs sign-in, Updating, Broken (with reason). Never “Unknown” without a path to diagnostics.

### 7.3 Instance detail

Header: icon, name (editable inline), version, loader, size, last played. Primary **Play**; overflow: Export, Duplicate, Delete.

Tabs:

| Tab | Content |
|---|---|
| **Overview** | Launch summary, device fit, recent activity, worlds, quick settings (render distance, heap preset) |
| **Content** | Mods, resource packs, shader packs; enable/disable, update, add from Modrinth; dependency warnings |
| **Worlds** | Saves with size, last played, game mode if readable; export/import; open folder |
| **Logs** | Latest log tail, crash reports, “Export diagnostics” |
| **Settings** | Per-instance overrides: renderer, heap, resolution, JVM args (advanced), Java runtime |

**Decision:** “Content” is a tab, not a separate destination. A player thinks in instances, not in global mod lists.

### 7.4 Create instance

A three-step sheet, not a wizard with seven pages:

1. **Version** — release/snapshot toggle, search, recommended badge. Show release date and Java requirement.
2. **Loader** — Vanilla, Fabric (stable), with the loader version behind “Choose version”. Quilt/NeoForge show “Coming in 1.1” and are disabled, not hidden.
3. **Name & review** — generated name (`Vanilla 26.3`), editable; size estimate; device-fit line (“Vulkan 1.2 detected — recommended path” or “GLES 3.2 — Sandpaper compatibility”); **Create**.

Advanced (collapsed): custom resolution, heap preset, JVM args, data directory. Changing these later is allowed.

### 7.5 Discover

- Search field with debounce; recent searches; no auto-suggest that leaks keystrokes.
- Chips: Modpacks, Mods, Resource packs, Shaders (conditional).
- Cards show title, author, downloads, last updated, supported MC versions, loader badges.
- Project detail: description, gallery, versions, dependencies, license, external links.
- Install sheet: target instance (or new), version, required dependencies, size, “This pack includes 187 files” summary.

**Trust:** show the source (Modrinth), the license, and whether the file is hosted on Modrinth. Never show a download button for a file we cannot hash.

### 7.6 Downloads

- Active: phase, percent, bytes, speed, ETA, pause/cancel.
- Queued: order, size, reason for waiting.
- History: completed/failed/cancelled, with “Retry” and “View log”.
- A failed download expands inline to the exact file, HTTP status and next action; no modal.

### 7.7 Settings

Top-level sections:

1. **Account** — active account, add/switch, sign out, re-auth.
2. **Appearance** — theme (system/light/dark), dynamic color, UI scale, reduced motion.
3. **Controls** — touch overlay presets, button size/opacity, gamepad deadzones, keyboard layout.
4. **Game defaults** — heap preset, renderer policy, render distance, FPS cap, audio.
5. **Storage** — install location, used/free, GC, move instance.
6. **Diagnostics** — log level, export report, renderer probe, component status.
7. **About** — version, licenses, source, privacy, disclaimer.

**Rule:** settings are searchable, and every setting explains its default in one sentence. “Advanced” is a section, not a junk drawer.

### 7.8 Launch overlay

The launch sequence is a full-screen surface with a small status card:

```
Preparing Aether SMP
✓ Verifying files
✓ Starting runtime
• Loading Minecraft…
[ Cancel ]
```

- Progress is staged, not a fake infinite spinner.
- The card can be dismissed to a pill; the game surface takes over when ready.
- On failure, the card expands into the crash sheet (below).
- The back gesture cancels a launch that has not reached the game; once the game is running, Back is Escape.

### 7.9 Crash sheet

```
Minecraft stopped unexpectedly
Stage: graphics initialization
Signal: SIGSEGV in libsandpaper.so +0x1a24c

What this usually means
The GL compatibility layer crashed while creating the context.

[ Try safe mode ]  [ Export report ]  [ Close ]
```

- **Stage** is one of: install, verify, JVM start, game init, graphics init, world load, runtime.
- **Signal/library/offset** is shown in monospace, copyable.
- **What this usually means** is a short, honest explanation; if we do not know, say so.
- Safe mode is the primary action when the same instance crashed twice within 10 minutes.
- “Export report” opens the file list before sharing.

### 7.10 Safe mode

A normal launch with a recorded flag, presented as a checklist:

- Disable mods (move `mods/` to `mods.disabled-<timestamp>/`)
- Force OpenGL (skip Vulkan)
- Lower heap to 1 GB and render distance to 4
- Reset renderer cache
- Start anyway

Each item explains what it changes and how to undo it. The launcher never silently applies safe mode.

### 7.11 In-game overlay

A small, translucent handle at the screen edge opens:

- **Pause / Resume** (sends Escape or the game's pause)
- **Controls** (toggle overlay, edit layout)
- **Keyboard** (show IME)
- **Screenshot** (capture the surface, save to Pictures/Eclipse)
- **Performance** (FPS, frame time, memory, renderer)
- **Exit game** (confirm; graceful `_exit` after save)

The overlay is an Android view above the surface, not an in-game mod. It must not steal input from the game when closed. It respects cutouts and the IME inset.

---

## 8. First-run and onboarding

### 8.1 The four minutes

| Minute | What happens | What the user sees |
|---|---|---|
| 0:00 | Welcome | One sentence, disclaimer, **Get started** |
| 0:20 | Sign in | Device code, Copy/Open browser, progress |
| 1:30 | Device check | “Your device: Vulkan 1.2 / 8 GB / arm64 — recommended path: Vulkan” |
| 2:00 | Runtime download | Size, speed, pause; “You can keep using the app” |
| 3:30 | Create instance | Version preselected to latest release; **Create** |
| 4:00 | Play | Hero card with **Play** |

If the user is already signed in (reinstall with a restored account? tokens are not backed up), skip to device check. If offline, allow browsing and explain that sign-in/download need a connection.

### 8.2 What we ask, and what we do not

We ask:

- Microsoft sign-in (required to play).
- Nothing else. No email, no username, no birthday, no marketing consent.

We infer:

- Device RAM, ABI, API, GPU strings, GLES/Vulkan capability.
- Free storage.
- Whether a controller is connected.

We do not ask the user to choose a renderer, heap, GC, JVM version or resolution on first run.

### 8.3 Permission timing

| Permission | When | Why |
|---|---|---|
| Notifications | After the first download is queued | Progress and completion |
| `ACCESS_LOCAL_NETWORK` (API 37+) | When the user opens “LAN world” or “Add server” | Multiplayer discovery |
| Storage/SAF | When importing/exporting | No broad storage permission |
| Camera/mic | Never | Not a feature |

No permission prompt on first launch before the user understands why.

---

## 9. Touch controls

### 9.1 Default layout

```
┌──────────────────────────────────────────────┐
│ [pause]                            [F3?]     │
│                                              │
│                                      [jump]  │
│      ┌───────────────┐                       │
│      │               │               [sneak] │
│      │   look area   │                       │
│      │               │      [inventory]      │
│      └───────────────┘                       │
│   [◀] [▲] [▼] [▶]        [attack] [use]      │
│   [hotbar 1..9]                              │
└──────────────────────────────────────────────┘
```

- Look area: relative mouse deltas; one finger; ignores pointers that started on a button.
- Movement: a virtual stick or D-pad (user choice); default D-pad for precision.
- Attack/use: tap vs hold semantics match desktop left/right click.
- Sneak, jump, inventory, drop, swap hands: reachable without moving the thumb far.
- Hotbar: a compact strip; scroll wheel emulation on the look area edge is optional.
- Pause: always visible, small, in the top-left safe area.

### 9.2 Customisation

- Presets: **Survival**, **Creative**, **PvP**, **Builder**, **Minimal**.
- Per-control: size (36–72 dp), opacity (30–100%), position (drag with snap), haptics on/off.
- Global: left-handed mirror, UI scale, “hide in menus”.
- Reset to preset; export/import layout as JSON.
- Layout editor is a separate mode with a grid, safe-area guides and a “test” sandbox.

### 9.3 Mouse mode

- Toggle in the overlay or a three-finger tap (configurable).
- Requests pointer capture; hides the cursor; sends raw relative deltas.
- Right-click and middle-click map to use/pick-block.
- A small “mouse mode” pill shows how to exit.
- Physical mouse: the same path; the overlay hides when a hardware pointer is detected, with a manual override.

### 9.4 Gamepad

- SDL's mapping database plus a launcher-level remap screen.
- Focus navigation on all launcher screens; gamepad B is Back, A is Activate, Menu opens the overlay.
- Deadzones: per-stick, 0–30%, default 10%; show the raw value.
- Rumble: on by default, disabled in Battery Saver.
- Hot-plug: pause the game and show “Controller disconnected” only if it was the active input.

### 9.5 Keyboard

- Physical keyboard scancodes pass through.
- IME opens for chat; committed text goes to the game, not to a hidden field.
- `KEYCODE_BACK` maps to Escape while the game is focused; predictive back is disabled inside `GameActivity`.
- Modifier combos (Ctrl, Alt, Shift) are forwarded; sticky modifiers are a v1.1 accessibility feature.

---

## 10. States, empty screens and copy

### 10.1 Every screen has five states

1. **Loading** — skeleton, not a spinner on a blank page.
2. **Empty** — one sentence, one action.
3. **Content** — the normal case.
4. **Partial** — some data loaded, some failed; show what works.
5. **Error** — what happened, why, what next, and a way to copy details.

### 10.2 Copy formula

> **What happened.** Why it happened. What to do next.

Examples:

- “**Minecraft could not start.** Your device has GLES 3.1; this version needs GLES 3.2. Try a lower Minecraft version or a different device.”
- “**The download stopped.** The connection closed after 312 MB. It will resume from where it stopped.”
- “**Mojang has not approved this build.** Sign-in is unavailable in this development build. Use a release build or contact the project.”
- “**This account does not own Java Edition.** It may own Bedrock. Switch account or buy Java Edition.”

Avoid: “Error”, “Something went wrong”, “Oops”, “Invalid state”, “Exception”, “null”.

### 10.3 Terminology

| Use | Not |
|---|---|
| Instance | Profile, installation, setup |
| Version | Game version, MC version (in body text, “Minecraft 26.3”) |
| Loader | Mod loader (in body text, “Fabric”) |
| Content | Mods/packs (when referring to the category) |
| Sign in | Login, authenticate |
| Runtime | JRE, Java (in advanced screens, “Java runtime”) |
| Renderer | Graphics backend, GL translation layer |
| Crash report | Log, stack trace |

### 10.4 Notifications

| Channel | Content | Priority |
|---|---|---|
| Downloads | Progress, paused, complete, failed | Low, ongoing |
| Game | “Minecraft is ready” only if the user left the app during launch | Default |
| Account | Re-authentication required | Default |
| Storage | Low space before an install | Default |

No promotional notifications. No “come back” nudges.

---

## 11. Accessibility

### 11.1 Standards

- WCAG 2.2 AA for contrast, target size, focus, labels and motion.
- Android accessibility: TalkBack, Switch Access, font scale up to 200%, display size, high-contrast text, reduced motion.
- Material 3 accessibility guidance for state layers and focus.

### 11.2 Requirements

| Area | Requirement |
|---|---|
| Touch targets | ≥ 48 × 48 dp; the Play button ≥ 56 dp |
| Contrast | 4.5:1 body text, 3:1 large text and UI icons |
| Color | Status never conveyed by color alone; icon + text |
| Text | Scales to 200% without clipping; no fixed-height text containers |
| Focus | Visible 2 dp ring, logical order, focus restored after dialogs |
| Screen reader | Every icon has a label; decorative images are skipped; progress announces percent at intervals, not every frame |
| Motion | Reduced-motion setting removes parallax, springs and large transitions |
| Audio | No essential information is audio-only; haptics are supplementary |
| Timing | No time-limited UI except the device code, which shows a countdown and a regenerate button |
| Logs | Monospace text is selectable and copyable; a “copy” action is always available |

### 11.3 The game surface

The launcher cannot make Minecraft itself accessible. It can:

- provide a readable pause/overlay;
- expose gamepad-only navigation;
- avoid overlays that cover subtitles or chat;
- offer a “high contrast touch controls” theme;
- document that Minecraft's own accessibility settings are inside the game.

**Open:** whether Android's accessibility services can meaningfully inspect the SDL surface. Assume no; do not promise it.

---

## 12. Localization and internationalisation

- All user-facing strings in `strings.xml`; no concatenated sentences.
- Plurals for files, minutes, mods, worlds.
- RTL layouts mirror navigation and controls, but **not** the game surface or the touch look area (the game's coordinate system is not mirrored).
- Dates and numbers via `DateTimeFormatter`/`NumberFormat` with the user's locale.
- Font fallback for CJK, Arabic, Hebrew, Devanagari; the UI font must include or fall back cleanly.
- Error details (HTTP status, signal, library) are not translated; the explanation around them is.
- Device-code instructions use Microsoft's localized `message` when available, with our own translation as fallback.
- Translation is community-driven after v1; English ships first, with a glossary and screenshots for translators.

**Open:** Minecraft itself has its own language setting; the launcher should default the game language to the system language on first instance creation, but never override an existing `options.txt`.

---

## 13. Performance as UX

| Interaction | Budget | Consequence if missed |
|---|---|---|
| Cold app start to Home content | < 1.5 s | Skeleton; no blank screen |
| Tab switch | < 100 ms | Content is cached; no re-fetch |
| Instance detail open | < 200 ms | Read from Room, not disk scan |
| Play tap → launch overlay | < 150 ms | Plan is precomputed |
| Plan verify (FAST) | < 2 s | Staged progress with file count |
| Scroll | 120 fps on 120 Hz panels | No bitmap decode on the main thread |
| Overlay open | < 100 ms | Pre-inflated view |

Rules:

- No disk I/O on the main thread.
- No full asset scan on Home; sizes come from the manifest.
- Images decoded at the target size; no 4K pack icons in a 48 dp row.
- The UI process stays under 150 MB RSS; the game process is separate.

---

## 14. Prototyping and validation

### 14.1 Prototype sequence

| Prototype | Question | Method |
|---|---|---|
| Paper: Home + Create | Is the primary action obvious? | 5 users, 5-second test |
| Figma: crash sheet | Can a non-expert choose the right recovery? | 8 users, scenario tasks |
| Compose: touch overlay | Can a player survive the first night? | 6 users, 20-minute session |
| Compose: device-code flow | Can a user sign in on a TV with a phone? | 6 users, no assistance |
| Device: install + launch | Where does it actually fail? | 10 devices, instrumented build |

### 14.2 Usability tasks

1. “Install Minecraft 26.3 and start a world.”
2. “Install this modpack and join a world.”
3. “Your game crashed after adding a mod. Get it running again.”
4. “Switch to your second account.”
5. “Play with a controller only.”
6. “Find out why Vulkan is not available on this device.”
7. “Export your world to another device.”

Success criteria: task completion, time, wrong turns, verbalised confusion, and whether the user reads the error text.

### 14.3 What would falsify the design

- Users cannot find **Play** because the hero card looks like a banner.
- Users interpret “safe mode” as “the game is broken forever”.
- Users cannot complete device code without a second person.
- The touch overlay covers the hotbar or the chat.
- The crash sheet's technical detail scares users away from the export button.
- The instance list becomes a junk drawer after ten instances.

Each of these triggers a design iteration, not a copy tweak.

---

## 15. v1 acceptance criteria

- [ ] A new user can sign in, install vanilla 26.3, and reach a world without reading documentation.
- [ ] A Fabric instance installs and launches with the same flow.
- [ ] A Modrinth pack installs, shows its loader and file count, and launches or fails with a specific reason.
- [ ] Every failure state names a stage and offers one primary next action.
- [ ] Safe mode recovers a mod-caused crash without deleting the world.
- [ ] Touch controls are usable for movement, look, attack, place, inventory and chat.
- [ ] A gamepad can complete onboarding and launch a world without touch.
- [ ] TalkBack can read every screen and activate every primary action.
- [ ] 200% font scale does not clip a label or hide a button.
- [ ] The app opens offline and shows instances and logs.
- [ ] No screen shows a raw exception, `null`, or an unlabelled spinner.
- [ ] Every destructive action confirms and explains what is kept.

---

## 16. Decisions for sign-off

1. **Five top-level destinations:** Home, Instances, Discover, Downloads, Settings.
2. **One hero instance and one primary Play action**; no dashboard of metrics on Home.
3. **Instance detail is a first-class screen with Overview, Content, Worlds, Logs, Settings.**
4. **Create is a three-step sheet, not a wizard; advanced options are collapsed.**
5. **Touch overlay with five presets and per-control customisation.**
6. **Crash sheet with stage, signal, plain-language cause and safe mode as the primary recovery.**
7. **Local-first, opt-in metrics; no analytics SDK.**
8. **WCAG 2.2 AA and 200% font scale are release gates, not stretch goals.**
9. **English first, community localization after v1.**
10. **No social, store, or cloud features in v1.**

## 17. Open questions

- Does the target audience expect a “Java arguments” box on the instance screen, or is hiding it behind Advanced acceptable?
- Is a virtual stick or a D-pad the better default for Minecraft on touch?
- How much of the in-game overlay can be drawn without hurting frame time on a T1 device?
- Does Android TV-style focus navigation need a different Home layout, or does the adaptive rail suffice?
- Should Discover show shader packs before a renderer mod is installed?
- What is the minimum viable accessibility test device set (TalkBack, switch access, large font, RTL)?
- Can the device-code flow be completed entirely with a controller on a TV?
