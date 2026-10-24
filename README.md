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

Eclipse Launcher installs Minecraft: Java Edition, authenticates a Microsoft account, manages isolated instances and mods, and launches the game inside a bundled OpenJDK 25 runtime with a touch/gamepad-first interface.

It is **not** a Bedrock launcher, a piracy tool, an offline-auth bypass, a server host, or a cheat client.

The product promise:

> **One tap to play. If it breaks, we tell you why and fix it.**

---

## Component map

| Component | Repository / location | Responsibility | License |
|---|---|---|---|
| **EclipseLauncher** | this repo | Android app, UI, auth, installer, launch planner, instance manager | Apache-2.0 |
| **EclipseJRE** | `SouhardyaMaybe/EclipseJRE` | OpenJDK 8/17/21/25 cross-built for Android; `libjvm.so`, modules, JLI | GPL-2.0 + Classpath Exception |
| **EclipseNatives** | `SouhardyaMaybe/EclipseNative` | LWJGL 3 Android natives, OpenAL Soft, FreeType, Shaderc | BSD-3-Clause, LGPL-2.0+, Apache-2.0, FTL |
| **EclipseSDL3** | `SouhardyaMaybe/EclipseSDL3` | SDL 3.4.x for Android with the Eclipse driver-handover seam | Zlib |
| **EclipseRuntime** | `runtime/` (this repo) | Component resolver, downloader, verifier, installer, rollback | Apache-2.0 |
| **Sandpaper** | `sandpaper/` (this repo) | Renderer policy and translation layer: Vulkan, GLES, GL compatibility | Apache-2.0 |
| **Sash** | `sash/` (this repo) | Minimal `java.awt` shim for Minecraft and mods, backed by Android | Apache-2.0 |
| **Foreman** | `foreman/` (this repo) | Forge/NeoForge installer engine and processor runner | Apache-2.0 |
| **Crowbar** | `crowbar/` (this repo) | Deterministic library patchers, renamers and jar transforms | Apache-2.0 |
| **EclipseFFmpeg** | `SouhardyaMaybe/EclipseFFmpeg` | Optional FFmpeg plugin for ReplayMod and video export | LGPL/GPL (optional plugin) |

All JNI entry points use the **`pw_`** prefix and are registered through a single `JNI_OnLoad`. No exported symbol names encode Java package paths.

---

## Engineering rules

1. **CI is the only build environment.** Every binary is produced by a pinned GitHub Actions workflow. A release is a tag plus a successful workflow run.
2. **No checked-in binaries.** APKs, AARs, JREs, natives and generated components are release assets, never repository content.
3. **Official upstreams only.** OpenJDK, LWJGL, SDL, OpenAL Soft, Oboe, FreeType, Shaderc, SPIRV-Cross, VMA and FFmpeg are consumed from their official projects. No code from PojavLauncher, Amethyst, FCL, Zalith, or other launchers.
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

- CI runs on `ubuntu-24.04` with pinned JDK, NDK, CMake and Gradle versions.
- Native components are downloaded from pinned GitHub Releases by SHA-256; they are never rebuilt in the app repo.
- Release signing uses repository secrets; debug builds use a CI-generated debug key.
- Every APK is checked for 16 KB ELF alignment and zip alignment before publication.

See [`docs/README.md`](docs/README.md) for the research dossier and [`PROVENANCE.md`](PROVENANCE.md) for the component ledger.

---

## Status

- Research dossier: chapters 00–03 complete; 04–14 in progress.
- Infrastructure repos: existing; being cleaned to the official-upstream policy.
- Launcher code: scaffold begins after the architecture sign-off.
- No production APK has been released yet.

---

## Legal

Eclipse Launcher is not affiliated with Mojang, Microsoft, the Eclipse Foundation, or any launcher project mentioned in the research.

Minecraft is a trademark of Mojang Synergies AB. This project does not distribute Minecraft game files. Users must own a legitimate Minecraft: Java Edition account.

NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.
