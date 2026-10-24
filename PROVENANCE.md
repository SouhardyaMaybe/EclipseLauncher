# Provenance and licensing ledger

**Status:** living document. Every binary that can reach a user must have a row here.
**Rule:** if a component has no explicit license, it does not ship.

---

## 1. What “clean” means here

Eclipse Launcher is an **original implementation with auditable provenance**. We do not claim a formal legal clean-room process, because that requires an isolated specification team and an isolated implementation team. We do claim:

- no source code copied from PojavLauncher, Amethyst, FCL, Zalith, or any other launcher;
- no unofficial fork used as a build input;
- every third-party component consumed from its **official upstream**;
- every license, copyright notice and source link preserved;
- no binary without a reproducible build manifest.

“Looks clean” is not a license strategy. The only durable strategy is **being clean**, and that is what this ledger enforces.

---

## 2. Component ledger

| Component | Upstream | License | Our changes | Status |
|---|---|---|---|---|
| **EclipseLauncher app** | original | Apache-2.0 | all | planned |
| **EclipseRuntime** | original | Apache-2.0 | all | planned |
| **Sandpaper** | original | Apache-2.0 | all | planned |
| **Sash** | original | Apache-2.0 | all | planned |
| **Foreman** | original | Apache-2.0 | all | planned |
| **Crowbar** | original | Apache-2.0 | all | planned |
| **EclipseJRE** | [OpenJDK jdk25u](https://github.com/openjdk/jdk25u), tag `jdk-25.0.4+4` | GPL-2.0 + Classpath Exception | Android platform patches, jlink image, 16 KB link flags | **audit required** |
| **EclipseNatives / LWJGL** | [LWJGL 3.4.3](https://github.com/LWJGL/lwjgl3) | BSD-3-Clause | Android platform target, `pw_` JNI bridge, EGL/GLES loading | **rebuild required** |
| **EclipseNatives / OpenAL Soft** | [OpenAL Soft](https://github.com/kcat/openal-soft), pin `7f9c6de` | LGPL-2.0-or-later | Official OpenSL ES backend, Android build | **rebuild required** |
| **EclipseSDL3** | [SDL 3.4.16](https://github.com/libsdl-org/SDL) | Zlib | Embeddable `dev.shadow` Java bridge, `pw_host` handover, big-core affinity | **cleanup required** |
| **EclipseFFmpeg** | [FFmpeg 7.1.5](https://ffmpeg.org/) | LGPL-2.1+ or GPL-2.0+ depending on flags | Android build, plugin APK | optional; GPL build must be isolated |
| **FreeType** | [freetype](https://gitlab.freedesktop.org/freetype/freetype), 2.14.3 | FTL or GPL-2.0 | Android build | shippable under FTL |
| **Shaderc** | [google/shaderc](https://github.com/google/shaderc) | Apache-2.0 | Android build | shippable |
| **SPIRV-Cross** | [KhronosGroup/SPIRV-Cross](https://github.com/KhronosGroup/SPIRV-Cross) | Apache-2.0 | Android build | shippable |
| **Vulkan Memory Allocator** | [GPUOpen-LibrariesAndSDKs/VulkanMemoryAllocator](https://github.com/GPUOpen-LibrariesAndSDKs/VulkanMemoryAllocator) | MIT | Android build | shippable |

---

## 3. Components that must not ship

These are present in older Eclipse infrastructure snapshots. They are **research artifacts only** and must be removed before a public release.

| Component | Why it cannot ship | Disposition |
|---|---|---|
| **FCL-Team/lwjgl3** | Unofficial Android fork; violates the official-upstream rule; provenance and patch ownership unclear | Replace with official LWJGL 3.4.3 + our own Android platform patch |
| **FCL-Team/OpenAL** | Unofficial fork; same problem | Replace with official OpenAL Soft + the upstream OpenSL ES backend |
| **MioLibPatcher** | No license file, no license statement; author retains all rights | Drop; implement the needed transform in **Crowbar** |
| **MioFabricAgent** | No public source, no license; opaque binary | Drop; implement Fabric profile handling in **Foreman** |
| **Cacio / caciocavallo** | GPL-2.0; large AWT/Swing backend; conflicts with the original-code policy | Replace with **Sash** |
| **installer-agent** | LGPL-3.0; Pojav-lineage installer agent | Replace with **Foreman** |
| **java-sandbox (pro-grade)** | Apache-2.0, but `SecurityManager` is permanently disabled in Java 24+ (JEP 486) | Drop; isolate mods by process and filesystem, not `SecurityManager` |
| **OptiFineRenamer** | MIT code, but OptiFine is proprietary and unsupported | Drop |
| **exp4j** | Apache-2.0; small third-party dependency | Optional; rewrite the expression evaluator if still needed |
| **resconf-hack** | OpenJDK-derived (GPL-2.0+CE); only relevant to JRE 8 | Fold into EclipseJRE, not the app |

---

## 4. Copyleft policy

Copyleft is not automatically a problem. **Hidden** copyleft is.

- **OpenJDK** is GPL-2.0 + Classpath Exception. The Classpath Exception permits our Apache-2.0 launcher to link to the JVM. We publish the exact OpenJDK source, patches and build scripts for every JRE release.
- **OpenAL Soft** is LGPL-2.0-or-later. We ship it as a separate shared library, publish its source and build scripts, and keep it dynamically linked so it can be replaced.
- **FFmpeg** is LGPL-2.1+ by default; the current `--enable-gpl` build (x264/x265) is GPL-2.0+. It is an **optional plugin APK**, not part of the core launcher, and its source is published with the plugin.
- **SDL3, LWJGL, FreeType (FTL), Shaderc, SPIRV-Cross, VMA** are permissive and ship with their notices.

No GPL/LGPL source is copied into the Apache-2.0 app source tree. No notice is removed. No license is renamed to “MIT” to make a directory look uniform.

---

## 5. Build manifest contract

Every release asset is accompanied by:

```json
{
  "component": "eclipse-launcher",
  "version": "0.1.0",
  "commit": "<40-hex>",
  "built_at": "<ISO-8601>",
  "runner": "ubuntu-24.04",
  "toolchain": { "jdk": "...", "ndk": "...", "gradle": "...", "agp": "..." },
  "inputs": [
    { "name": "EclipseJRE", "url": "...", "sha256": "...", "license": "GPL-2.0+CE" }
  ],
  "artifacts": [
    { "name": "EclipseLauncher-0.1.0-arm64-v8a.apk", "sha256": "..." }
  ]
}
```

A release without a manifest is not a release.

---

## 6. Current audit findings (2026-10-24)

1. **EclipseJRE** builds from official `jdk25u`, but its Android patch set is a squashed diff containing OpenJDK, AOSP (Apache-2.0), BSD and other code. The origin of every hunk is not yet documented. **Do not publish a JRE release until the patch set is re-derived or fully attributed.**
2. **EclipseNative** release `native-v1.1.0` is built from FCL forks. It is useful for bring-up but **not shippable** under this policy.
3. **EclipseSDL3** `sdl3-v1.0.0` is built from upstream SDL 3.4.16 with original Eclipse changes and is the first component that passes the policy.
4. **EclipseComponents** contains Cacio (GPL-2.0), MioLibPatcher (no license), MioFabricAgent (no source) and installer-agent (LGPL-3.0). It must be rewritten, not patched around.
5. **16 KB alignment** of the published JRE and native AARs has not yet been verified. The launcher’s release gate will reject any `.so` whose `PT_LOAD` alignment is below `0x4000`, and any APK whose uncompressed `.so` entries are not 16 KB zip-aligned.

---

## 7. What we will not do

- We will not remove copyright headers or license files to make the tree look uniform.
- We will not rename a GPL component to hide it.
- We will not ship an unlicensed binary because “everyone else does.”
- We will not claim a formal clean-room process that did not happen.

If a component cannot meet this policy, it is replaced with an original implementation or dropped.
