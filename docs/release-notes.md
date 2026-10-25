# Eclipse Launcher release notes

## What this build is

This release is the signed-off v1 launcher shell plus a real Vanilla install
path: the single-screen Quick Actions surface, local instance records, official
Mojang metadata, a verified download store, an atomic instance commit, real log
and file operations, and honest not-connected states for the services that are
not implemented yet.

**It installs the Vanilla game files but does not launch Minecraft.** After a
successful install, Play explains that the game runtime is not connected. No
JRE, native runtime or game files are bundled with the APK; the game files are
downloaded from Mojang on demand.

## What works

- **Main page:** account bar, exactly one **Quick Actions** section (Controls,
  Manage Logs, File Manager, Manage Content, Install a .jar), and the bottom
  instance selector + edit pencil + Play bar.
- **Create Instance:** the centered, full-width-button layout. Vanilla creates
  the instance and starts the official-file install; Fabric creates a local
  instance and says the loader is not connected; Quilt, Forge, NeoForge and
  OptiFine are visible placeholders; Modpack opens Discover → Modpacks.
- **Vanilla install:** Play or Create Instance fetches Mojang's official
  version manifest, version JSON and asset index, resolves the Linux/arm64 (or
  x86_64) rules, builds a deterministic plan, checks the device ABI and free
  space, downloads the client, libraries, assets and logging config into a
  shared content-addressed store, verifies every size and SHA-1, and commits
  the instance atomically.
- **Tasks:** the Tasks icon opens a dialog with real phase, file and byte
  progress. Pause stops the worker between chunks; Resume reuses the plan and
  skips verified objects; Retry re-runs a failed attempt; Cancel removes the
  task. A progress bar appears only after the plan exists.
- **User content:** a reinstall or version change hard-links the existing
  `game/` tree into the new instance tree (copy fallback), so worlds, options,
  screenshots and manually added mods survive the metadata swap.
- **Manage Content:** Installed/Discover tabs, six content types, a search row, a filter dialog for game version, loader, source and auto-install dependencies, and content cards for installed files. Modrinth and CurseForge catalogue clients are not connected. CurseForge is **Coming soon** until a key is saved in **Settings → Miscellaneous**. Discover → Modpacks shows the disabled **Import local modpack** footer.
- **Manage Logs:** `.minecraft/logs/<instance>/` with `latestlog.txt` rotation, a path bar, a **Select** / **Share latest** footer, multiselect, copy/cut/paste, rename, delete, share and an in-app logger-style text viewer.
- **File Manager:** a read/write browser rooted at `.minecraft`, with a path bar, a **..** parent row, a **New folder** / **Select** footer, folder creation, rename, delete, copy/cut/paste and share.
- **Install a .jar:** the system document picker, followed by a direct handoff
  to the installer process. There is no extra target or run screen. The
  installer process currently shows the selected file; `.jar` execution is not
  connected yet.
- **Settings:** a category list with detail screens for Appearance,
  Miscellaneous and About. The CurseForge API key is stored on the device only;
  license and disclaimers live under About.
- **Accounts:** a list → auth-method card → local-login card flow. The local
  account is kept without a warning note. Microsoft device-code sign-in is
  visibly unconfigured until the project's own Entra client ID is supplied
  through `ECLIPSE_MS_CLIENT_ID`.
- **Controls:** the reference launcher's full-screen control layout with a
  right-hand drawer. It is a preview shell; the drawer says editing arrives
  with the game runtime.

Every pushed screen except Home follows the reference launcher's layout and
flow with original Ore colors, type, icons and assets. No reference code or
assets are copied.

## Requirements

- Android 8.0 (API 26) or newer.
- arm64-v8a or x86_64.
- No storage permission; launcher files live in app-private storage.
- A network connection for the Vanilla install.
- A legitimate Minecraft: Java Edition account is required for future online
  play. This build does not authenticate or launch the game.

## Install

1. Download the APK from the release assets.
2. Verify it against `SHA256SUMS.txt` and `BUILD-MANIFEST.json`.
3. Allow installation from your browser or file manager when prompted.

## Known limitations

- No game runtime, JRE, native libraries or launch path in this build. The
  Vanilla files install, but Play cannot start the game.
- No Microsoft authentication yet.
- Fabric, Quilt, Forge, NeoForge and OptiFine loader installs are not
  implemented. Fabric is a local instance only.
- Modrinth and CurseForge catalogue clients are not connected. Modpacks are not
  installable yet; the Modpack row opens the Discover tab.
- The installer process currently shows the selected `.jar`; `.jar` execution
  is not connected yet.
- Free-space and ABI preflight are wired: the launcher reserves the missing
  bytes, 15% scratch and 512 MiB headroom, and rejects a device without
  `arm64-v8a` or `x86_64` before downloading. Runtime and account preflight
  are not possible in this build. Other failures surface as a task failure
  with a stable code.
- A pending install plan is held in memory only. After the app process is
  killed, the install restarts from metadata; already verified store objects
  are reused.
- Downloads are sequential and do not use Range requests yet. Pausing deletes
  the current `.part`; the object is fetched again on resume.
- A local account is a display profile. It does not prove game ownership and
  cannot be used for online play.

---

This release does not include Minecraft game files. Users must own a
legitimate Minecraft: Java Edition account.

NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.
