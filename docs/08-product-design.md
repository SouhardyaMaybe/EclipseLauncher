# 08 — Product design

**Status:** signed off 2026-10-25. This revision replaces the five-destination shell with the single-screen Quick Actions surface that ships in v1. A same-day layout pass aligns every pushed screen with the reference launcher's structure and flow while keeping the original Ore visuals.
**Scope:** who Eclipse Launcher is for, what the signed-off v1 surface promises, how its screens fit together, what every state looks like, how touch and controller input work, and how the product recovers when the game does not start. Visual tokens and component specifications are in [09 — Design system](09-design-system.md); the research behind them is in [10 — Design research and skills](10-design-research-and-skills.md).
**Evidence:** the competitor teardown in [01](01-landscape.md), the Android constraints in [04](04-android-platform.md), the launch/runtime model in [05](05-runtime-and-graphics.md), and the installer contract in [07](07-install-pipeline.md).
**Labels:** **Verified**, **Observed**, **Inference**, **Decision**.

---

## 1. The product in one page

> **One tap to play. If it breaks, we tell you why and fix it.**

Eclipse Launcher is an Android launcher for **Minecraft: Java Edition**. It is not a game store, not a social network, and not a settings museum. It exists to do three things well:

1. **Get a player into a world quickly** on a phone, tablet, foldable or Chromebook.
2. **Make modded play understandable** without turning the home screen into a control panel.
3. **Turn failure into a next step** instead of a black screen, a silent crash or a log file the player cannot read.

### 1.1 What the signed-off v1 surface contains

| Area | Signed-off v1 |
|---|---|
| Main page | One screen. Account bar at the top, exactly one section (**Quick Actions**), instance selector + edit + **Play** at the bottom. |
| Quick Actions | **Controls**, **Manage Logs**, **File Manager**, **Manage Content**, **Install a .jar**. No other rows, tiles or cards. |
| Navigation | No bottom navigation, rail or drawer. Pushed screens use the system back gesture. Dialogs cover tasks, instance editing and confirmations. |
| Accounts | **Local account** is kept. Microsoft device-code sign-in is present as a visibly unconfigured path until the project owns an Entra client ID. No Ely.by or other third-party account provider. |
| Create Instance | The reference layout: centered section titles, a divider, and full-width buttons. Vanilla creates the instance and starts the official-file install; Fabric creates a local instance and says the loader is not connected; Quilt, Forge, NeoForge, OptiFine and Modpack are visible placeholders. |
| Manage Content | **Installed** / **Discover** top tabs, then content types: Mods, Resource Packs, Data Packs, Shaders, Worlds, Modpacks. Discover filters: game version, loader, Modrinth/CurseForge, plus auto-install required dependencies. |
| CurseForge | No bundled key. The source is **Coming soon** until the user adds their own key in **Settings → Miscellaneous**; clearing the key returns it to Coming soon. |
| Logs | `.minecraft/logs/<instance>/`; first start creates `latestlog.txt`; the next process start renames the previous file to `YYYY-MM-DD_HH-mm-ss.txt` and creates a new `latestlog.txt`. Multiselect, copy, paste, cut, delete, rename, share and an in-app text viewer. |
| File Manager | Opens the app-private `.minecraft` tree, read/write, with the reference launcher's in-app picker behaviour. It is a browser, not a shortcuts hub. |
| Install a .jar | Tapping the row opens the system document picker. A selected `.jar` is handed straight to the installer process; there is no extra target or run screen. |
| Play | Kept in the reference launcher's bottom-bar position. Starts the Vanilla install when the instance has no committed manifest; once installed it explains that the game runtime is not connected rather than faking progress. |
| Visual system | Ore-inspired dark palette, Monocraft for chrome, system sans for prose, original 12×12 pixel glyphs, square bevels and 2 dp borders. |
| Accessibility | 200% font scale, 48 dp targets, WCAG 2.2 AA contrast, visible focus, labels on every icon, no fake progress. |

### 1.2 What v1 is not

- Not Play Store-first. Play's executable-code policy conflicts with downloading `client.jar` and runtimes.
- Not a piracy tool. No offline auth, no alt manager, no bundled game files, no "local account" launch bypass.
- Not a Bedrock launcher.
- Not a general-purpose Android file manager or emulator front-end.
- Not a social platform. No friends list, chat network or marketplace of our own.
- Not a benchmarking tool. Performance numbers exist to help the player, not to win screenshots.

### 1.3 Connected in this build, and deliberately not connected

The signed-off surface is implemented. Backend services arrive behind it without adding a second navigation model.

| Capability | State in this build | Honest surface |
|---|---|---|
| Local instance record and directories | Connected | Creating Vanilla/Fabric writes the record and the `.minecraft` tree. |
| Vanilla game-file install | Connected | The official manifest, version JSON and asset index feed a deterministic plan; the client, libraries, assets and logging config download into the shared store and commit atomically. |
| Device and space preflight | Connected | The launcher requires `arm64-v8a` or `x86_64` and reserves missing bytes + 15% scratch + 512 MiB before the first download. Runtime and account checks wait for those components. |
| Log rotation and file operations | Connected | Real files, real rename/copy/cut/delete/share. |
| `.jar` handoff | Connected to the installer process | The installer process shows the selected file; the pipeline is not connected yet. |
| Microsoft device-code auth | Not configured | The Accounts screen says so. `ECLIPSE_MS_CLIENT_ID` is empty by default. |
| Modrinth catalogue | Not connected | Discover shows real filters and an honest "Catalogue not connected" empty state. |
| CurseForge catalogue | Not connected | Disabled with "Coming soon" until a user key exists; a saved key enables the source, not a fictional catalogue. |
| Game runtime, JRE and natives | Not connected | The game files install; Play then explains that the runtime is not connected. No fake progress. |
| Quilt, Forge, NeoForge, OptiFine installers | Placeholder | Buttons exist and say "coming soon"; they do not create a broken instance. |

### 1.4 Deferred beyond v1

| Deferred | Why |
|---|---|
| Full Quilt, Forge, NeoForge and OptiFine installers | Installer processors and a second loader matrix need their own test cycle. |
| CurseForge catalogue client | API terms, rate limits and a user-supplied key; the settings path exists first. |
| Modrinth search, install, update and `.mrpack` import | The UI is ready; the network client is the next milestone. |
| Sash / custom AWT | v1 uses real headless `java.desktop`; Sash is a later compatibility layer. |
| Cloud sync, telemetry, social features | Privacy surface; local export is enough. |

---

## 2. Who it is for

### 2.1 Primary: the returning Java player

- Has a Microsoft account and owns Java Edition.
- Plays on a phone or tablet because the PC is not always available.
- Knows what "Fabric" and "shaders" mean, but does not want to configure a JVM.
- Tried a legacy launcher, hit a crash or a 403, and does not know which layer failed.
- Wants: sign in, pick a version, play; keep the world; not lose settings.

**Design consequence:** the default path must never ask about heap, GC, renderer or JVM flags. Those exist later, behind an advanced screen.

### 2.2 Secondary: the modpack player

- Installs packs from Modrinth or CurseForge, often 100–300 mods.
- Cares about pack version, loader version, mod updates and world compatibility.
- Willing to read a log if the launcher points at the right line.
- Wants: install a pack, update it without losing the world, disable a mod when it breaks.

**Design consequence:** Manage Content is one surface with **Installed** and **Discover** views. The player does not need a second navigation tree to find a mod or a pack.

### 2.3 Tertiary: the couch/controller player

- Plays on a tablet or Android TV-style device with a gamepad.
- May never touch the screen after setup.
- Wants: focus that never gets lost, large targets, no hover-only affordances, a way to open the pause/overlay without a mouse.

**Design consequence:** every screen is fully operable with a D-pad and two buttons; focus is visible and restored; the game overlay is reachable from the controller.

### 2.4 Quaternary: the parent/guardian

- Sets up the device for a child.
- Needs to understand what will be downloaded, how much space it takes, and whether a Microsoft Family is required.
- Wants: no surprise purchases, no hidden downloads, clear consent.

**Design consequence:** the install summary names sizes and sources; child-account errors explain the Family step; no dark-pattern "continue" that hides a 650 MB download.

### 2.5 Anti-persona

The user who wants to run five instances simultaneously, benchmark every GPU, or manage 40 accounts. Eclipse can be extended later; v1 optimises for one active game and a small number of instances.

---

## 3. Jobs to be done

| Job | Current pain | Eclipse response |
|---|---|---|
| "Let me play the version I already have." | Launcher updates, account expiry, renderer confusion | The bottom bar always shows the selected instance and one primary **Play** action. |
| "Install this modpack." | Format ambiguity, loader mismatch, failed downloads | Discover → Modpacks, with game-version and loader filters and dependency handling. |
| "Why did it crash?" | Log file, no context, "exit code 1" | Manage Logs opens the instance log directory and an in-app viewer; crash reporting builds on it. |
| "Make the controls usable." | Desktop keybind assumptions | Controls is a first-class Quick Action; presets and per-control sizing arrive with the runtime. |
| "Don't lose my world." | Launcher wipes or re-creates the instance | Transactional installs preserve `game/` across repair and version changes; the File Manager and Logs never delete user content without confirmation. |
| "Tell me the truth about my device." | "Vulkan on/off" toggle that lies | Probe result with the exact missing extension/feature and a recommended path. |
| "Sign in without typing a password on a TV." | Redirect flows and WebViews | Device code with a short, copyable code and a browser on any device. |

---

## 4. Product principles

1. **Play is the verb.** Every screen has one obvious next action. If a screen has three equal-weight buttons, it is wrong.
2. **Evidence over toggles.** Show the probe result, the measured heap, the actual missing extension. Do not offer a switch that cannot work.
3. **Progressive disclosure.** Defaults are good; advanced controls are one level down and searchable.
4. **Never lie about state.** "Installed" means verified. "Running" means the surface is alive. "Crashed" means we have a reason.
5. **Recovery is a feature, not an error page.** The crash screen is designed with the same care as the home screen.
6. **Respect user content.** Worlds, options, screenshots and manually added mods are never deleted by repair, update or uninstall without explicit confirmation.
7. **Offline shell.** The launcher opens, lists instances and reads logs without a network. Only installation and sign-in require connectivity.
8. **One-handed by default.** Primary actions live in the lower third on phones; destructive actions do not.
9. **Accessible is the baseline.** Large text, TalkBack, switch access, reduced motion and high contrast are not a later theme.
10. **No dark patterns.** No fake urgency, no pre-checked bundles, no hidden downloads, no "rate us" interruption.

---

## 5. Success metrics

| Signal | Target |
|---|---|
| Time to install vanilla | 100 Mbit/s, T1 device: < 6 min |
| Touch-control adoption | Sessions with overlay enabled / game sessions: informational |
| Account auth success | Device-code flows completed / started: ≥ 80% (excluding user cancel) |
| Crash-report export | Crashed sessions with an exported report: ≥ 40% |

**Guardrails:** uninstall rate after a crash, support issues per 1,000 installs, ANR rate, UI-process OOM rate. A feature that improves time-to-play but raises crash-related uninstalls is not a win.

---

## 6. Information architecture

### 6.1 The single surface

```
┌──────────────────────────────────────────────────────┐
│  [account]  Player                         [tasks] ⚙ │  account bar
├──────────────────────────────────────────────────────┤
│                                                      │
│                    QUICK ACTIONS                     │  exactly one
│  ──────────────────────────────────────────────────  │  section
│  [icon]  Controls                                  > │
│  ──────────────────────────────────────────────────  │
│  [icon]  Manage Logs                               > │
│  ──────────────────────────────────────────────────  │
│  [icon]  File Manager                              > │
│  ──────────────────────────────────────────────────  │
│  [icon]  Manage Content                            > │
│  ──────────────────────────────────────────────────  │
│  [icon]  Install a .jar                            > │
│  ──────────────────────────────────────────────────  │
│                                                      │
├──────────────────────────────────────────────────────┤
│  New Instance · 26.3 · Fabric          [edit] [PLAY] │  instance bar
└──────────────────────────────────────────────────────┘
```

There is no second main page and no top-level tab bar. The account bar, Quick Actions section and instance bar are chrome around one surface, not additional sections.

### 6.2 Pushed screens

| Screen | Opened from | Back behaviour |
|---|---|---|
| **Create Instance** | Instance bar → New instance; edit pencil when no instance exists; Modpack row | Returns to the main page. |
| **Controls** | Quick Actions → Controls | Returns to the main page. |
| **Manage Logs** | Quick Actions → Manage Logs | Exits selection mode first, then returns. |
| **File Manager** | Quick Actions → File Manager; "Open folder" from content | Goes up one folder, then returns. |
| **Manage Content** | Quick Actions → Manage Content; Create Instance → Modpack | Returns to the main page. |
| **Settings** | Gear in the account bar | Returns to the main page. |
| **Settings detail** | Settings → Appearance, Miscellaneous or About | Returns to the Settings category list. |
| **Accounts** | Account area or Settings → Account | Returns to Settings or the main page; the local-login pane backs out to the auth-method card first. |
| **Text viewer** | A log or text file | Returns to the browser that opened it. |

Dialogs do not create history entries: **Tasks**, **Edit instance**, **Rename**, **Delete**, **Message**, **Content filters** and **Name prompt** are overlays on the current screen.

**Layout reference:** every pushed screen follows the reference launcher's structure and flow — the back-row preference list, the centred auth-method card, the full-screen control layout with a right-hand drawer, the path-bar file selector with a two-button footer, the search row plus filter dialog plus content cards, and the logger-style viewer bar. Only layout and flow are matched. Colors, type, icons, focus treatment and all assets remain the original Ore system; no reference code or assets are copied.

### 6.3 Why one surface

The reference launcher puts the five jobs a player actually performs — controls, logs, files, content and local `.jar` installs — on one screen, with the selected instance and Play always visible. Splitting those jobs across a five-tab shell adds navigation without adding capability. The single surface keeps the primary action in the lower third, keeps account and tasks in the top bar, and lets Manage Content own the Installed/Discover split where it belongs.

**Decision:** no bottom navigation, no navigation rail, no drawer in v1. A tablet or Chromebook gets the same surface with more breathing room, not a different information architecture.

---

## 7. Screen-by-screen

### 7.1 Main page

- **Account bar (top):** local account name and "Local account" subtitle on the left; **Tasks** icon (small, 48 dp target) immediately left of **Settings** on the right. The Tasks icon carries a numeric badge when tasks are active.
- **Quick Actions:** centered heading, 2 dp divider, then exactly five full-width rows. Each row has an original pixel icon, a label and a chevron. Row height is at least 66 dp; the whole row is the target.
- **Instance bar (bottom):** instance name and `version · loader` on the left, a chevron indicating a selector, an edit pencil, and a green **Play** button. The selector opens a menu with **Edit instance** (when one exists) and **New instance**. The pencil edits the current instance, or creates one when none exists.
- **No inline progress panel.** Installs, downloads and exports live in the Tasks dialog.

**Empty state:** the instance bar reads **No instance** and **Create an instance**; Play is disabled. There is no fake instance, no default `1.7.10`, and no placeholder row on the main page.

### 7.2 Tasks dialog

- Opened by the Tasks icon.
- Empty state: **No tasks** and one sentence explaining what will appear there.
- Each running task shows its real phase, completed files and bytes, and **Pause** / **Cancel**. A paused task shows **Resume** / **Cancel**; a failed task shows the stable code and **Retry** when retrying can help.
- A progress bar appears only after the plan exists. There is no percentage while metadata is being resolved.
- The badge counts tasks in the registry; it does not invent background work.

### 7.3 Create Instance

The layout follows the reference launcher literally:

```
                 Create Instance
  ──────────────────────────────────────────────
              Vanilla-like versions
  [                Vanilla                   ]
  [                OptiFine                  ]

                  Modded versions
  [                 Fabric                   ]
  [                 Quilt                    ]
  [                 Forge                    ]
  [                NeoForge                  ]
  [                 Modpack                  ]
```

- Centered section titles, thick dividers, full-width square buttons, generous vertical spacing.
- No cards, chips, badges or explanatory rows inside the picker.
- **Vanilla** creates a local instance with the default game version, returns to the main page and starts the official-file install. **Fabric** creates a local instance and says the loader is not connected; it does not start a download.
- **Quilt**, **Forge**, **NeoForge** and **OptiFine** show a "coming soon" message. They do not create an instance.
- **Modpack** opens Manage Content → Discover → Modpacks.
- The instance editor (pencil) changes the name and Minecraft version; the loader is shown read-only.

### 7.4 Manage Content

**Top-level tabs:** **Installed** | **Discover**.
**Content-type tabs:** Mods, Resource Packs, Data Packs, Shaders, Worlds, Modpacks. The type row scrolls horizontally rather than shrinking labels.

Both views start with a search row. On Installed it filters the files that are actually present; on Discover it keeps the catalogue query and shows the filter button. The filter button opens a dialog with **Game version**, **Loader**, **Source** (Modrinth / CurseForge) and **Auto-install required dependencies**, plus **Apply** and **Cancel**. The active filter summary is shown under the search row. There is no visual sort control.

**Installed**

- Lists the real contents of `.minecraft/instances/<instance>/<type>/` (Worlds uses `saves/`) as reference-style content cards: a 65 dp thumbnail cell, title, type/size line and a folder badge.
- Tapping a card opens a details dialog with **Open**, **Open folder**, **Share** and **Delete**. Destructive actions confirm first.
- Empty state: **Nothing installed**, with a sentence that points to Discover or File Manager. A non-empty folder with no search matches shows **No matches** instead.

**Discover**

- Filters live in the dialog; the screen itself shows only the search row and the active filter summary. This keeps the reference launcher's search-first layout.
- **Auto-install required dependencies** is a real setting, persisted on the device and also reachable from Settings → Miscellaneous.
- Mod version lists, when the catalogue client arrives, follow Modrinth's default oldest → newest order. No sort UI is shown.
- Compatibility choice, when versions exist: newest compatible release, then beta, then alpha. If none is compatible, install is disabled with the reason.
- CurseForge is disabled and labelled **Coming soon** until a key is saved. A saved key enables the source; clearing it returns to Coming soon.
- Worlds are not a Modrinth category, so Discover → Worlds explains that and offers the installed browser.
- With no catalogue client, the screen shows **Catalogue not connected** and does not invent projects.
- Discover → Modpacks shows the reference launcher's full-width **Import local modpack** footer. It is disabled in this build and says that local modpack import arrives with the modpack installer; it never pretends to import a pack.

### 7.5 Manage Logs

- Directory: `.minecraft/logs/<instance>/`.
- On the first launcher start for an instance, create `latestlog.txt`.
- On the next process start, rename the previous `latestlog.txt` to `YYYY-MM-DD_HH-mm-ss.txt` and create a new `latestlog.txt`. Activity recreation is not a new start.
- The screen follows the reference file-selector layout: a path bar at the top (`.minecraft/logs/<instance>`) with a 48 dp back button, the list, and a two-button footer.
- List shows file name and size, newest first.
- Footer: **Select** enters multiselect; **Share latest** shares the newest log directly. In selection mode the footer is replaced by the selection action bar.
- Selection actions: **Copy**, **Cut**, **Paste**, **Rename** (one file), **Delete** (confirmed), **Share** (files only).
- Tapping a file opens the in-app text viewer. Binary files show **No preview** with Share; files larger than the preview limit show the first part with a clear notice.
- Empty state: **No logs yet**, with the rotation rule in one sentence.
- No instance: **No instance**, with a sentence explaining that logs need an instance.

### 7.6 File Manager

- Root is the app-private `.minecraft` directory. It is a browser, not a shortcuts hub.
- Read/write: open folders, open text files in the viewer, create folders, rename, delete, copy, cut, paste, share.
- Path bar shows `.minecraft` plus the relative path and carries the 48 dp back button.
- The list starts with a **..** parent row when the current folder is below the root.
- The two-button footer is **New folder** and **Select**. In selection mode the footer is replaced by the selection action bar.
- Back goes up one folder, then leaves the screen. Selection mode is exited before navigation.
- New folder and rename use a name prompt that rejects empty names, slashes and existing names.
- Destructive actions confirm and state that they cannot be undone.
- Empty folder: **This folder is empty**, with a sentence about files added to the instance.

### 7.7 Install a .jar

- Tapping the Quick Action opens the system document picker directly.
- The picker accepts any document so providers that do not label `.jar` files still work; the display name is checked for a `.jar` suffix.
- A valid file is handed to `InstallerActivity` with a read grant and the `modUri` extra. The installer process opens immediately and shows the selected file. There is no second target/run screen.
- A non-`.jar` selection shows a short message and returns to the main page.

### 7.8 Settings

The Settings root is a reference-style category list, not a dashboard: a back row, then section labels and full-width preference rows.

1. **Account** — the current account name and **Local account** summary; opens the Accounts screen.
2. **Appearance** — opens a detail screen with the fixed Ore palette and the Monocraft/Noto Sans split. This build ships one dark palette; text size follows Android's font scale.
3. **Miscellaneous** — opens a detail screen with the **CurseForge API key** field, Save, Clear, a status line (**CurseForge: Coming soon** or **CurseForge: enabled on this device**) and the persisted **Auto-install required dependencies** switch.
4. **About** — opens a detail screen with version, Apache-2.0 license, source, Monocraft OFL notice, Minecraft disclaimer and Eclipse Foundation non-affiliation.

Every detail screen starts with the same back row. The key is stored on the device only. It is never bundled, logged or uploaded. A blank key keeps CurseForge disabled.

### 7.9 Accounts

The Accounts screen follows the reference launcher's add-account flow:

1. **List** — a back row, the current local account as a preference row, and **Add account**.
2. **Auth method** — a centred card with full-width **Sign in with Microsoft** and **Local account** buttons. Microsoft is visibly unavailable in this build and explains why; there is no Ely.by or other provider.
3. **Local login** — a centred card with one username field and a full-width save button. The field accepts 1–16 letters, numbers or underscores and explains the rule only when the typed name is invalid. There is no extra warning note on the local account.

The local account is the active account in this build and can be renamed at any time. A successful Microsoft flow will add an account; the local account remains available as a separate, clearly labelled option.

### 7.10 Controls

- The screen is the reference launcher's full-screen control layout: a faint original control preview, a drawer handle at the top centre, and a right-hand drawer.
- The drawer lists the active layout and the control groups (Movement, Action buttons, Interface, Mouse and keyboard). Every value reads **Preview only**.
- A 48 dp back button returns to the main page.
- The note says that touch, keyboard, mouse and gamepad mapping arrive with the game runtime. There are no fake sliders, toggles or save actions that do not affect a running game.

### 7.11 Text viewer

- Follows the reference logger layout: a 70 dp bar with the file name, **Share** and a 48 dp close button, over a black monospace text area.
- Text is selectable and scrolls vertically. A truncated preview shows a notice above the text.
- Loading, binary and unreadable files keep the bar and show a centred state underneath; they never show raw exceptions.

### 7.12 Play

- The Play button is always in the bottom bar, in the reference launcher's position.
- With no instance, it is disabled and the bar offers creation.
- With a Vanilla instance that has no committed manifest, it starts the official-file install and opens the Tasks dialog. Pause, resume, retry and cancel are real worker controls.
- With an installed instance, it opens a message: **Game runtime not connected**, with one sentence explaining that this build downloads the game files but does not launch Minecraft.
- With a Fabric instance, it says the loader is local-only until loader installation exists.
- No fake progress, no invented download, no "almost there" state.

---

## 8. First run and onboarding

### 8.1 The current first run

1. The app opens on the main page with a local account named **Player** and no instance.
2. The player taps the instance bar or the edit pencil, chooses a loader, and gets a local instance. Vanilla starts the official-file install immediately; Fabric stays local-only.
3. The instance bar now shows the name, version and loader. The Tasks dialog shows real install progress; when the install commits, Play explains that the runtime is not connected.
4. Logs and the File Manager are immediately usable; the log directory is created with the instance.

There is no multi-page welcome tour, no forced Microsoft sign-in, and no permission request on first launch.

### 8.2 What we ask, and what we do not

- We ask for a loader and a name only when the player creates an instance.
- We do not ask for a Microsoft password in our UI.
- We do not request storage permissions; all launcher files live in app-private storage.
- We do not pre-select a game version we cannot install.

### 8.3 Permission timing

- **Notifications:** only when a real long-running task exists.
- **Network:** declared for future catalogue/auth work; the shell opens offline.
- **Storage:** none; the File Manager is scoped to `.minecraft`.

---

## 9. Touch controls

The control overlay is a runtime feature. The product requirements are recorded here so the UI does not grow a second, conflicting control surface:

### 9.1 Default layout

- Left thumb: a 4-way movement pad with a visible dead zone.
- Right thumb: look area with tap-to-attack and hold-to-place.
- Hotbar strip above the right thumb; inventory and chat buttons at the edges.
- Every control has a 48 dp minimum target and a visible pressed state.

### 9.2 Customisation

- Presets: **Default**, **Left-handed**, **Compact**, **Large**, **Custom**.
- Per-control size, opacity and position; reset to preset.
- Changes preview on the launcher surface before the game starts.

### 9.3 Mouse mode

- A virtual cursor with left/right click, scroll and drag.
- Useful for inventory-heavy mods and for testing; not the default.

### 9.4 Gamepad

- SDL mapping database plus a launcher-level remap screen.
- Focus navigation on all launcher screens; gamepad B is Back, A is Activate, Menu opens the overlay.

### 9.5 Keyboard

- Physical keyboard and mouse are forwarded to the game surface.
- Modifier combos (Ctrl, Alt, Shift) are forwarded; sticky modifiers are a v1.1 accessibility feature.

---

## 10. States, empty screens and copy

### 10.1 Every screen has five states

1. **Loading** — a real read or a short, honest "Loading…"; never a fake percentage.
2. **Empty** — one sentence and, when useful, one action.
3. **Content** — the normal case.
4. **Partial** — some data loaded, some failed; show what works.
5. **Error** — what happened, why, what next, and a way to copy details.

### 10.2 Copy formula

> **What happened.** Why it happened. What to do next.

Examples:

- "**Minecraft could not start.** Your device has GLES 3.1; this version needs GLES 3.2. Try a lower Minecraft version or a different device."
- "**The download stopped.** The connection closed after 312 MB. It will resume from where it stopped."
- "**Mojang has not approved this build.** Sign-in is unavailable in this development build. Use a release build or contact the project."
- "**This account does not own Java Edition.** It may own Bedrock. Switch account or buy Java Edition."
- "**Catalogue not connected.** The filters are real, but the Modrinth client is not part of this build yet."

Avoid: "Error", "Something went wrong", "Oops", "Invalid state", "Exception", "null".

### 10.3 Terminology

| Use | Not |
|---|---|
| Instance | Profile, installation, setup |
| Version | Game version, MC version (in body text, "Minecraft 26.3") |
| Loader | Mod loader (in body text, "Fabric") |
| Content | Mods/packs (when referring to the category) |
| Sign in | Login, authenticate |
| Runtime | JRE, Java (in advanced screens, "Java runtime") |
| Renderer | Graphics backend, GL translation layer |
| Crash report | Log, stack trace |

### 10.4 Notifications

| Channel | Content | Priority |
|---|---|---|
| Tasks | Progress, paused, complete, failed | Low, ongoing |
| Game | "Minecraft is ready" only if the user left the app during launch | Default |
| Account | Re-authentication required | Default |
| Storage | Low space before an install | Default |

No promotional notifications. No "come back" nudges.

---

## 11. Accessibility

### 11.1 Standards

- WCAG 2.2 AA for contrast and target size.
- Android accessibility: TalkBack, Switch Access, font scale up to 200%, display size, RTL.
- The game surface is a special case: the launcher chrome must remain accessible, but the game's own UI is not reimplemented.

### 11.2 Requirements

- Every icon button has a content description; decorative pixel glyphs are skipped.
- Every action is reachable by keyboard/D-pad; focus is visible and restored after a dialog.
- Touch targets are at least 48 × 48 dp; menu rows are at least 66 dp.
- Text does not clip at 200% font scale. Long labels wrap to two lines; rows grow rather than ellipsize a primary action.
- Status is never conveyed by color alone; a label or icon accompanies it.
- No fake progress. A spinner only appears when a real operation is running.
- Dialogs trap focus, restore it on close, and have a labelled dismiss action.
- Motion is limited to press states and short fades; reduced-motion settings remove the fade.

### 11.3 The game surface

- The overlay is reachable without a mouse.
- Controls scale with the device and with the user's size setting.
- The launcher never draws over a system permission dialog or the keyboard.

---

## 12. Localization and internationalisation

- English ships first. All user-facing strings live in `strings.xml`; no hard-coded UI copy in Kotlin.
- RTL mirrors navigation and row chevrons, but not the game surface or the touch look area.
- Monocraft covers Latin; the system sans fallback covers CJK, Arabic, Hebrew and Devanagari. Long prose uses the system sans face for this reason.
- Dates use the device locale; log archive names use a fixed, sortable `yyyy-MM-dd_HH-mm-ss` pattern.
- File sizes use Android's formatter; paths are shown as relative to `.minecraft` where possible.

---

## 13. Performance as UX

- The main page does no network work and no full asset scan.
- File listings happen on demand; the UI does not block on a directory with thousands of entries without a loading state.
- Log previews are capped and read off the main thread.
- The app opens offline and shows the instance, logs and files.
- Cold start target: < 1.5 s to the main page on a T1 device; no blank screen.
- The launcher process must not hold the game surface or the JVM.

---

## 14. Prototyping and validation

### 14.1 Prototype sequence

1. Static main page with the five Quick Actions rows and the bottom bar.
2. Real local instance creation and log rotation.
3. File Manager operations and the text viewer.
4. Manage Content Installed/Discover split with the honest not-connected states.
5. Microsoft device-code flow with the project's own client ID.
6. Modrinth catalogue and dependency resolution.
7. Runtime launch, controls and crash recovery.

### 14.2 Usability tasks

| Task | Question |
|---|---|
| Find the log for the current instance | Is Manage Logs discoverable from the main page? |
| Create a Fabric instance | Is the loader picker's layout clear without explanatory copy? |
| Find and delete a mod | Does Installed → Mods behave like a file browser? |
| Install a local `.jar` | Is the direct picker-and-run flow obvious? |
| Add a CurseForge key | Is Settings → Miscellaneous findable? |
| Read the screen at 200% font | Does any primary action clip or disappear? |

### 14.3 What would falsify the design

- Players cannot find logs or files from the single surface.
- The five Quick Actions rows read as a settings menu rather than the five jobs.
- The bottom instance selector is mistaken for a second navigation bar.
- Manage Content's two tab rows are confusing without a visible content-type label.
- The direct `.jar` flow feels unsafe because there is no confirmation; if so, add a one-line confirmation, not a second screen.

---

## 15. v1 acceptance criteria

- [ ] The main page has exactly one section, Quick Actions, with exactly the five signed-off rows.
- [ ] The account bar shows the local account, Tasks (with badge) and Settings in that order.
- [ ] The bottom bar keeps the instance selector, edit pencil and Play in the reference position.
- [ ] No Wiki, Discord, social link, default `1.7.10`, or dummy row appears on the main page.
- [ ] Create Instance uses centered titles, dividers and full-width buttons; placeholder loaders say "coming soon".
- [ ] Manage Content has Installed/Discover then the six content types; Discover has version, loader, source and dependency toggle, and no sort control.
- [ ] CurseForge is Coming soon until a user key is saved; clearing the key returns it to Coming soon.
- [ ] Manage Logs follows the `latestlog.txt` rotation rule and supports multiselect, copy, paste, cut, delete, rename, share and the in-app viewer.
- [ ] File Manager opens `.minecraft`, supports read/write operations and is not a shortcuts hub.
- [ ] Install a .jar opens the picker and hands a valid file straight to the installer process.
- [ ] Play starts a real Vanilla install when none is committed, then is honest about the missing runtime; no fake progress.
- [ ] 200% font scale does not clip a label or hide a button.
- [ ] TalkBack can read every screen and activate every primary action.
- [ ] No screen shows a raw exception, `null`, or an unlabelled spinner.
- [ ] Every destructive action confirms and explains what is kept.

---

## 16. Decisions for sign-off

1. **One main screen with a single Quick Actions section**, not five top-level destinations.
2. **Account bar + Quick Actions + instance/Play bar** is the complete main-page structure.
3. **Tasks is an icon-only button left of Settings**, with a badge and a dialog; no inline progress.
4. **Manage Content merges Browse and Manage** into Installed/Discover plus content types.
5. **Discover has no visual sort control**; version lists follow Modrinth's oldest → newest order when connected.
6. **Compatibility preference is release → beta → alpha**, newest first; none disables install.
7. **Create Instance copies the reference launcher's centered, full-width-button layout.**
8. **Install a .jar is a direct picker-and-run flow**, with no extra screen.
9. **The local account stays, without a warning note; Ely.by is removed.**
10. **No third-party launcher client ID is shipped**; Microsoft auth waits for the project's own Entra registration.
11. **CurseForge uses a user-supplied key**, stored locally, and stays Coming soon otherwise.
12. **Ore-inspired dark palette, Monocraft chrome, system sans prose, original pixel icons.**
13. **WCAG 2.2 AA, 48 dp targets and 200% font scale are release gates, not stretch goals.**
14. **No social, store, cloud, Wiki or Discord surfaces in v1.**

## 17. Open questions

- Does the target audience expect a "Java arguments" box on the instance screen, or is hiding it behind Advanced acceptable?
- Is a virtual stick or a D-pad the better default for Minecraft on touch?
- How much of the in-game overlay can be drawn without hurting frame time on a T1 device?
- Does Android TV-style focus navigation need a different main-page layout, or does the single column suffice?
- Should Discover show shader packs before a renderer mod is installed?
- What is the minimum viable accessibility test device set (TalkBack, switch access, large font, RTL)?
- Can the device-code flow be completed entirely with a controller on a TV?
