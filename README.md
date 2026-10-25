# Eclipse Launcher

**An original Android launcher for Minecraft: Java Edition.**

- **Application ID:** `dev.shadow.eclipselauncher`
- **Author:** Shadow
- **Target:** Android 8.0+ (API 26+), arm64-v8a first; phone, tablet, foldable, Chromebook
- **Game target:** Minecraft: Java Edition 26.3+ (Java 25, SDL3, LWJGL 3.4.x)
- **Build model:** GitHub Actions only. No local builds, no checked-in binaries.
- **Distribution:** GitHub Releases (APK + SHA-256 + build manifest). Play Store is not a launch channel.

---

## What this is

Eclipse Launcher installs Minecraft: Java Edition, authenticates a Microsoft account, manages isolated instances and mods, and launches the game inside a verified OpenJDK 25 runtime with a touch/gamepad-first interface.

It is **not** a Bedrock launcher, a piracy tool, an offline-auth bypass, a server host, or a cheat client.

The product promise:

> **One tap to play. If it breaks, we tell you why and fix it.**

---

## Component map

| Component | Repository / location | Responsibility | License |
|---|---|---|---|
| **EclipseLauncher** | this repo | Android app, UI, auth, installer, launch planner, instance manager | Apache-2.0 |
| **EclipseJRE** | `SouhardyaMaybe/EclipseJRE` | OpenJDK 25 cross-built for Android; `libjvm.so`, jlink modules, headless `java.desktop` | GPL-2.0 + Classpath Exception |
| **EclipseNatives** | `SouhardyaMaybe/EclipseNative` | Official-upstream LWJGL 3, OpenAL Soft, FreeType, Shaderc, SPIRV-Cross, VMA and STB built for Android | BSD-3-Clause, LGPL-2.0+, FTL, Apache-2.0 |
| **EclipseSDL3** | `SouhardyaMaybe/EclipseSDL3` | SDL 3.4.x for Android with the embeddable `dev.shadow` Java bridge | Zlib |
| **EclipseRuntime** | `runtime/` (this repo) | Component resolver, downloader, verifier, installer, rollback | Apache-2.0 |
| **Sandpaper** | `sandpaper/` (this repo) | Desktop GL 3.3 core on GLES 3.2 compatibility runtime, plus the Vulkan capability probe and backend policy | Apache-2.0 |
| **Sash** | `sash/` (this repo) | *Deferred to v1.1.* Patches Android-backed `Toolkit`/`ImageIO` into `java.desktop`; v1 uses real headless AWT | Apache-2.0 |
| **Foreman** | `foreman/` (this repo) | Forge/NeoForge installer engine and processor runner | Apache-2.0 |
| **Crowbar** | `crowbar/` (this repo) | Deterministic library patchers, renamers and jar transforms | Apache-2.0 |
| **EclipseFFmpeg** | `SouhardyaMaybe/EclipseFFmpeg` | Optional FFmpeg plugin for ReplayMod and video export | LGPL/GPL (optional plugin) |

All JNI entry points use the **`pw_`** prefix and are registered through a single `JNI_OnLoad`. No exported symbol names encode Java package paths.

---

## Current implementation

The repository contains the signed-off single-screen launcher shell:

| Module | Contents |
|---|---|
| `:app` | `MainActivity`, the `EclipseApp` back stack, `AppContainer`, local instance/account state, log rotation, file operations, task registry, and the Quick Actions surface |
| `:designsystem` | Ore-inspired palette, Monocraft typography, square shapes, original 12 × 12 pixel glyphs, bevel buttons, menu rows, tabs, panels and dialogs |
| `:core:common` | `Failure` taxonomy, `AppResult`, `EpochClock` |
| `:core:model` | `InstanceSummary`, `InstanceId`, `LoaderKind` |
| `:domain:mojang` | Official version manifest/version JSON models, ordered rule evaluation, Maven coordinates, argument templates and the Android library resolver. Pure JVM and unit-tested in CI. |
| `:domain:install` | Asset-index model, deterministic content-addressed download plan, and a verified sequential artifact store with `.part` writes, SHA-1/size checks, atomic rename, cancellation and byte progress. Pure JVM and unit-tested in CI. |
| `:host` | `GameActivity` in `:game` and `InstallerActivity` in `:installer` |

What works today:

- **Main page:** account bar, exactly one **Quick Actions** section (Controls, Manage Logs, File Manager, Manage Content, Install a .jar), and the bottom instance selector + edit pencil + Play bar.
- **Create Instance:** the reference launcher's centered, full-width-button layout. Vanilla and Fabric create a local instance; Quilt, Forge, NeoForge and OptiFine are visible placeholders; Modpack opens Discover → Modpacks.
- **Manage Content:** Installed/Discover tabs, six content types, game-version/loader/source filters, and an auto-install-dependencies setting. Modrinth and CurseForge catalogue clients are not connected yet, so Discover says so.
- **Manage Logs:** `.minecraft/logs/<instance>/` with `latestlog.txt` rotation, multiselect, copy/cut/paste, rename, delete, share and an in-app text viewer.
- **File Manager:** read/write browser rooted at `.minecraft`, with folder creation, rename, delete, copy/cut/paste and share.
- **Install a .jar:** system picker → direct handoff to `InstallerActivity`; no extra target/run screen.
- **Settings:** local account, Ore theme value, CurseForge API key (stored on device only; source stays **Coming soon** until a key is saved), license and disclaimers.
- **Accounts:** local account is kept without a warning note. Microsoft device-code sign-in is visibly unconfigured until the project's own Entra client ID is supplied through `ECLIPSE_MS_CLIENT_ID`; no third-party launcher client ID is bundled.
- **Metadata core:** the pure-JVM `:domain:mojang` module parses official Mojang manifests and version JSON, evaluates ordered OS/arch/feature rules, resolves Maven coordinates and argument templates, and builds the Java classpath while dropping every desktop native for EclipseNatives.
- **Install core:** `:domain:install` turns resolved metadata and an asset index into a deterministic download plan, then stores each object through `<destination>.part` with streaming SHA-1 and size verification, an fsync and an atomic rename. It reports real completed bytes, counts already-verified objects, and stops mid-file on pause. It is not yet wired to a download or install screen.

It is still a shell, not a complete launcher: it does not install or launch Minecraft, and it does not authenticate a Microsoft account yet. The screens say that plainly. The next milestone is the Phase 0 spike set in doc 12: JRE boot, SDL window/EGL, Vulkan probe, Sandpaper triangle and installer transaction.

---

## Engineering rules

1. **CI is the only build environment.** Every binary is produced by a pinned GitHub Actions workflow. A release is a tag plus a successful workflow run.
2. **No checked-in binaries.** APKs, AARs, JREs, natives and generated components are release assets, never repository content.
3. **Official upstreams only.** OpenJDK, LWJGL, SDL, OpenAL Soft, FreeType, Shaderc, SPIRV-Cross, VMA and FFmpeg are consumed from their official projects at pinned revisions. No code from PojavLauncher, Amethyst, FCL, Zalith, or other launchers, and no GPU driver forks (Turnip, Zink) or namespace bypasses.
4. **Original implementation.** The launcher, Sandpaper, Sash, Foreman, Crowbar and EclipseRuntime are written for this project. Third-party behavior is researched and documented; source code is not copied.
5. **Provenance is auditable.** Every release carries `BUILD-MANIFEST.json`, `SHA256SUMS.txt`, and a component ledger with upstream URL, commit, toolchain and license.
6. **Copyleft is isolated, not hidden.** OpenJDK and OpenAL are separate components with their notices and source links. Our Apache-2.0 code does not absorb copyleft code.
7. **No unlicensed artifacts.** A component with no explicit license is removed, not shipped.

This is a **provenance-audited original implementation**, not a claim of a formal legal clean-room process. The distinction matters and is stated openly in [`PROVENANCE.md`](PROVENANCE.md).

---

## Build and release

```text
push / PR ──► CI: provenance, docs, Gradle build, native checks
tag v*    ──► Release: assemble APK, sign, checksum, publish GitHub Release
```

- CI runs on `ubuntu-24.04` with pinned JDK 25, Gradle 9.6.0, AGP 9.4.0 and Kotlin 2.4.21.
- Native components are downloaded from pinned GitHub Releases by SHA-256; they are never rebuilt in the app repo.
- Release signing uses repository secrets; debug builds use a CI-generated debug key.
- Every APK is checked for 16 KB ELF alignment before publication; the manifest must declare `extractNativeLibs="true"` so HotSpot can `dlopen` real files.

See [`docs/README.md`](docs/README.md) for the research dossier and [`PROVENANCE.md`](PROVENANCE.md) for the component ledger.

---

## Status

- Research dossier: chapters 00–14 complete; implementation gated on sign-off.
- Launcher code: the signed-off single-screen Quick Actions UI is implemented on top of the original `:app`, `:designsystem`, `:core:*` and `:host` modules. CI compiles the JVM core, runs its tests, builds debug and unsigned release APKs, and verifies both with a binary-manifest gate and a 16 KB ELF gate.
- Infrastructure repos: EclipseSDL3 cleanup is pushed; EclipseNatives rework is pushed and its CI now builds, packages and verifies both ABIs; EclipseJRE and EclipseComponents still need their official-upstream rework.
- No production APK has been released yet, and no release signing secrets are configured.

---

## Legal

Eclipse Launcher is not affiliated with Mojang, Microsoft, the Eclipse Foundation, or any launcher project mentioned in the research.

Minecraft is a trademark of Mojang Synergies AB. This project does not distribute Minecraft game files. Users must own a legitimate Minecraft: Java Edition account.

NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.
