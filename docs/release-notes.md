# Eclipse Launcher release notes

## What this build is

This release is the signed-off v1 launcher shell: the single-screen Quick
Actions surface, local instance records, real log and file operations, and
honest not-connected states for services that are not implemented yet.

**It does not install or launch Minecraft in this build.** Play explains that
the game runtime is not connected. No JRE, game files, or native runtime are
bundled or downloaded.

## What works

- **Main page:** account bar, exactly one **Quick Actions** section (Controls,
  Manage Logs, File Manager, Manage Content, Install a .jar), and the bottom
  instance selector + edit pencil + Play bar.
- **Create Instance:** the centered, full-width-button layout. Vanilla and
  Fabric create a local instance record; Quilt, Forge, NeoForge and OptiFine
  are visible placeholders; Modpack opens Discover → Modpacks.
- **Manage Content:** Installed/Discover tabs, six content types, game-version,
  loader and source filters, and the auto-install-dependencies setting.
  Modrinth is not connected. CurseForge is **Coming soon** until a key is saved
  in **Settings → Miscellaneous**.
- **Manage Logs:** `.minecraft/logs/<instance>/` with `latestlog.txt` rotation,
  multiselect, copy/cut/paste, rename, delete, share and an in-app text viewer.
- **File Manager:** a read/write browser rooted at `.minecraft`, with folder
  creation, rename, delete, copy/cut/paste and share.
- **Install a .jar:** the system document picker, followed by a direct handoff
  to the installer process. There is no extra target or run screen.
- **Settings:** local account, Ore theme value, CurseForge API key stored on the
  device only, license and disclaimers.
- **Accounts:** the local account is kept without a warning note. Microsoft
  device-code sign-in is visibly unconfigured until the project's own Entra
  client ID is supplied through `ECLIPSE_MS_CLIENT_ID`.

## Requirements

- Android 8.0 (API 26) or newer.
- arm64-v8a or x86_64.
- No storage permission; launcher files live in app-private storage.
- A legitimate Minecraft: Java Edition account is required for future online
  play. This build does not authenticate or launch the game.

## Install

1. Download the APK from the release assets.
2. Verify it against `SHA256SUMS.txt` and `BUILD-MANIFEST.json`.
3. Allow installation from your browser or file manager when prompted.

## Known limitations

- No game runtime, JRE, native libraries, Microsoft authentication, Modrinth
  catalogue or CurseForge catalogue in this build.
- Quilt, Forge, NeoForge and OptiFine installers are placeholders.
- Modpacks are not installable yet; the Modpack row opens the Discover tab.
- A local account is a display profile. It does not prove game ownership and
  cannot be used for online play.
- The installer process currently shows the selected `.jar`; the install
  pipeline is not connected yet.

---

This release does not include Minecraft game files. Users must own a
legitimate Minecraft: Java Edition account.

NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.
