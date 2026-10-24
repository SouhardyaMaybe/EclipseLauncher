# 02 — Minecraft: Java Edition platform contract

**Verified:** 2026-10-24 against the official Mojang manifest and the 26.3 version JSON.
**Scope:** what Mojang publishes, how a launcher is expected to consume it, and what changed in 26.x.
**Labels:** **Verified** (primary source), **Observed** (source code), **Inference** (reasoned, needs a test), **Open** (unknown).

---

## 1. The version snapshot that matters

| Fact | Value | Evidence |
|---|---|---|
| Latest release | **26.3** | **Verified**, `version_manifest_v2.json` |
| Release time | **2026-09-15T11:23:02Z** | **Verified**, 26.3 JSON |
| Latest snapshot | **26.4-snapshot-5** | **Verified**, manifest |
| Snapshot time | **2026-10-20T12:15:06Z** | **Verified**, manifest |
| Required Java | **25** (`java-runtime-epsilon`, 25.0.1) | **Verified**, 26.3 JSON + runtime manifest |
| Main class | `net.minecraft.client.main.Main` | **Verified**, 26.3 JSON |
| Minimum launcher version | `21` | **Verified**, 26.3 JSON |
| Client jar | 41,483,720 bytes, SHA-1 `e877b6a0…e7b87` | **Verified**, 26.3 JSON |
| Asset index | id `34`, 597,035 bytes, total asset size 484,894,038 bytes | **Verified**, 26.3 JSON |
| Libraries | 114 entries; 66 selected on Linux x64 | **Verified**, computed from 26.3 JSON |
| Vanilla footprint | ~581.5 MiB (libraries + client + assets), before the JRE, worlds, mods, caches | **Verified**, computed |
| Window/input | **SDL3**, replacing GLFW | **Verified**, 26.3 changelog |
| Graphics | OpenGL + experimental Vulkan 1.2 backend | **Verified**, 26.2/26.3 changelogs |

The 2026 cadence is **year.drop.hotfix**: 26.1, 26.2, 26.3, with snapshots named `26.4-snapshot-N`. This is not cosmetic: 26.1, 26.2 and 26.3 each changed a load-bearing part of the Android problem (Java, graphics API, windowing).

**Product consequence:** a launcher that claims “supports Minecraft” without naming a version is making an untestable claim. EclipseLauncher must expose a **compatibility tier** per version, not a binary supported/unsupported flag.

---

## 2. Metadata sources and trust model

### 2.1 Official endpoints

| Purpose | Endpoint | Notes |
|---|---|---|
| Version index | `https://piston-meta.mojang.com/mc/game/version_manifest_v2.json` | Lists releases, snapshots and historical versions |
| Version document | URL from the manifest's `versions[].url` | Immutable package URL with SHA-1 |
| Client jar | `downloads.client.url` | Content-addressed object |
| Libraries | `libraries[].downloads.artifact.url` | Maven-style coordinates plus explicit URL |
| Assets | `assetIndex.url`, then `objects[].hash` | Content-addressed object store |
| Java runtime | `https://launchermeta.mojang.com/v1/products/java-runtime/2ec0cc96c44e5a76b9c8b7c39df7210883d12871/all.json` | Platform-specific runtime manifests |
| Logging config | `logging.client.file.url` | Log4j XML, SHA-1 |

**Verified:** these are the endpoints used by the official launcher metadata and by the 26.3 JSON. The launcher must not scrape the website, bundle game files, or use an unofficial mirror as the primary source.

### 2.2 Manifest shape

```json
{
  "latest": { "release": "26.3", "snapshot": "26.4-snapshot-5" },
  "versions": [
    {
      "id": "26.3",
      "type": "release",
      "url": "https://piston-meta.mojang.com/v1/packages/<sha1>/26.3.json",
      "time": "2026-09-15T11:23:02+00:00",
      "releaseTime": "2026-09-15T11:23:02+00:00",
      "sha1": "<json sha1>",
      "complianceLevel": 1
    }
  ]
}
```

Rules for EclipseLauncher:

1. Treat a version document URL as **immutable**. Cache by SHA-1, not by version ID alone.
2. Re-fetch the manifest on a schedule (for example, every 6–24 hours when foregrounded) and on explicit “Check for updates”; do not poll continuously.
3. Verify the version JSON's SHA-1 before parsing. A truncated or tampered JSON must never become an install plan.
4. Keep a last-known-good manifest so the launcher remains usable offline.
5. Do not infer “latest release” from semantic version sorting; use `latest.release` and validate that the ID exists in `versions[]`.

**Open:** Mojang does not document a public ETag/If-None-Match contract for piston-meta. Use a conditional request only as an optimization, never as the sole freshness mechanism.

### 2.3 What Mojang does *not* publish

- No Android ARM64 LWJGL/SDL/OpenAL/FreeType natives.
- No Android JRE.
- No launcher API for installing mods or modpacks.
- No guarantee that the version JSON schema is stable across major launcher versions.
- No license to redistribute the client jar or assets.

**Verified:** EULA requires game downloads and updates to come from a source Mojang authorizes. **Inference:** the safe implementation downloads from `piston-meta.mojang.com`/`piston-data.mojang.com` directly, verifies hashes, and never mirrors game binaries in our APK or our CDN without written permission.

---

## 3. Version JSON: the launch contract

The 26.3 document contains these top-level fields:

| Field | 26.3 value / meaning | Launcher action |
|---|---|---|
| `id` | `26.3` | Internal version key; never use as a file path without sanitizing |
| `type` | `release` | UI badge, update channel |
| `mainClass` | `net.minecraft.client.main.Main` | JVM entry point unless a loader overrides it |
| `inheritsFrom` | absent in 26.3 | If present, merge parent first, child last |
| `javaVersion` | `{component: java-runtime-epsilon, majorVersion: 25}` | Select/install a Java 25 runtime |
| `assets` | `34` | Asset index ID passed to the game |
| `assetIndex` | URL, SHA-1, size, `totalSize` | Download, verify, expand objects |
| `downloads.client` | URL, SHA-1, size | Download and verify `client.jar` |
| `libraries` | 114 entries | Resolve rules, classpath, natives |
| `arguments` | `default-user-jvm`, `jvm`, `game` | Build the command line with rules and placeholders |
| `logging` | Log4j config | Download and pass `-Dlog4j.configurationFile` |
| `minimumLauncherVersion` | `21` | Reject only if our launcher is older than this; we are not |
| `complianceLevel` | `1` | Retain for future use; do not interpret as a security guarantee |
| `releaseTime` / `time` | ISO-8601 | Display and sorting |

### 3.1 Inheritance (`inheritsFrom`)

Loader profiles and some old versions use inheritance. The merge is not a shallow JSON merge.

**Required semantics (Verified against the launcher metadata format; loader profiles observed):**

1. Load the parent chain recursively, detect cycles, cap depth (for example, 8).
2. Merge scalar fields with child-overrides-parent (`mainClass`, `assets`, `assetIndex`, `javaVersion`).
3. Concatenate `libraries`, de-duplicating by Maven coordinate. Child entries win when the same coordinate appears with different rules.
4. Merge `arguments.game` and `arguments.jvm` as ordered lists. Parent arguments come first; child arguments are appended.
5. Merge `downloads` by key; child wins.
6. Preserve the **origin** of every library and argument for diagnostics (“from 26.3”, “from fabric-loader 0.20.1”).
7. Validate the final merged document before planning.

**Observed:** Fabric and Quilt launcher profiles use `inheritsFrom` and add a loader main class plus libraries. NeoForge/Forge installers generate a profile that inherits from the vanilla version. A launcher that ignores inheritance will fail on every modded instance.

**Inference:** merging must be deterministic and unit-tested with fixtures from at least Fabric, Quilt, NeoForge and Forge. Do not implement it ad hoc in the UI layer.

### 3.2 Rules

A rule object has `action` (`allow` or `deny`), optional `os` (`name`, `arch`, `version`), and optional `features` (`is_demo_user`, `has_custom_resolution`, quick-play flags).

Correct evaluation:

- If no rules: allowed.
- If rules exist: default deny, evaluate in order, and let the **last matching rule** set the result.
- `os.name` values are `windows`, `osx`, `linux`; Android is not one of them.
- `os.arch` values are `x86`, `x86_64`, `arm64`; Android arm64 must be mapped deliberately, not assumed.
- `os.version` matching is platform-specific and must not be applied to Android's kernel string.
- Feature rules are evaluated against the launch request, not the device.

**Critical Android mapping:** if we present ourselves as `linux`/`x86_64`, Mojang's rules select Linux x86_64 natives that cannot load on Android. If we present `linux`/`arm64`, some entries still select glibc-built Linux ARM64 libraries. EclipseLauncher should resolve the vanilla library set for **classpath-only** artifacts, then **replace the native set** with Android-built natives, and never pass the desktop natives directory to LWJGL.

### 3.3 Library selection in 26.3

Computed with the Linux x86_64 rules:

- 114 declared entries.
- 66 selected artifacts.
- 83,353,549 bytes (79.49 MiB) of libraries.
- The selected set includes LWJGL 3.4.3 modules: core, OpenGL, SDL, Shaderc, SPIR-V Cross, STB, VMA, Vulkan, OpenAL, FreeType, jemalloc.
- It also includes Netty 4.2.16, JNA 5.17.0, Authlib 10.0.77, Log4j 2.26.0, Gson 2.14.0, Guava 33.6.0, ICU4J 78.3, OSHI 6.9.0, JOML 1.10.9, jtracy 1.14.38, and the Mojang libraries.
- The only `linux-aarch_64` native in the selected set is Netty's epoll transport. It is a Linux/glibc artifact, not an Android-tested library. **Verified:** the JSON contains no `natives-linux-arm64` classifier for LWJGL.

**Important nuance:** “no Android natives” does not mean “no ARM64 code anywhere”. Netty publishes a Linux AArch64 epoll classifier; JNA may contain Android natives inside its jar; LWJGL's Linux ARM64 builds exist upstream but are not in Mojang's 26.3 library list. None of these removes the need for our own Android native build and validation.

### 3.4 Maven coordinates and paths

A library name is `group:artifact:version[:classifier]`. The launcher should:

- Build the classpath from the selected **artifact** entries only.
- Keep `natives` classifiers in a separate native directory, never on the classpath.
- Use the explicit `downloads.artifact.path` from the JSON; do not reconstruct the Maven path unless the path is absent.
- Normalize and sandbox every path (`..`, absolute paths, symlinks, NUL bytes, Windows separators) before writing.
- De-duplicate identical coordinates and identical hashes.
- Record SHA-1 and size; verify after download and before launch.

### 3.5 Native extraction

Desktop launchers extract `natives-*` jars into a directory and pass `java.library.path`. On Android:

- The official native jars are for Windows/macOS/Linux desktop and are not usable.
- Our native bundle must be extracted or installed into a location the dynamic linker and `System.load` can use.
- ABI must match the process: `arm64-v8a` for the v1 target.
- ELF load segments must be 16 KB-aligned on devices with 16 KB pages, even if we do not ship through Play.
- Every `.so` needs a source, license, version, SHA-256, build recipe and ABI manifest.

**Open spike:** whether Android 10+ permits `dlopen` of renderer/JVM libraries from the app's writable data directory on every target API. Existing launchers do this, but behavior varies with target SDK, SELinux and vendor policy. The Phase 0 spike must prove the chosen location and loading method on Android 12, 14 and 16.

---

## 4. Assets

### 4.1 Index and object store

The asset index is a JSON document:

```json
{
  "objects": {
    "icons/icon_16x16.png": {
      "hash": "bdf48ef6b5d0d23bbb02e17d04865216179f510a",
      "size": 3665
    }
  }
}
```

The object file lives at:

```
<assets-root>/objects/<first-two-hex>/<hash>
```

The game is launched with `--assetsDir <assets-root>` and `--assetIndex 34`. Do not rename objects or flatten the store; Minecraft expects the hash layout.

### 4.2 Integrity and disk use

- 26.3 asset total is **484,894,038 bytes** (462.43 MiB), index 597,035 bytes.
- Assets are shared across versions by hash; a global object store avoids duplicate downloads.
- Verify size and SHA-1. Do not trust the file name.
- Use atomic writes: download to `<hash>.part`, verify, then rename.
- Never delete an object that another installed version references. Use reference counting or a mark-and-sweep GC.
- A full asset store plus multiple versions can exceed 1–2 GB. Show a storage breakdown and let users clean caches explicitly.

### 4.3 Legacy assets

Old versions may use `virtual` or `map_to_resources` asset indexes. Those require materializing a directory tree, not just the hash store. This is a compatibility-tier concern, not a v1 requirement if v1 is 26.3+.

---

## 5. Arguments and placeholders

### 5.1 26.3 `default-user-jvm`

**Verified:**

```json
[
  "-Xms2G",
  "-Xmx4G",
  "-XX:+UseCompactObjectHeaders",
  "-XX:+AlwaysPreTouch",
  "-XX:+UseStringDeduplication",
  {
    "rules": [{"action":"allow","os":{"name":"osx"}},
              {"action":"allow","os":{"name":"linux"}},
              {"action":"allow","os":{"name":"windows","versionRange":{"min":"10.0.17134"}}}],
    "value": ["-XX:+UseZGC"]
  }
]
```

This is a desktop default, not a mobile default.

| Flag | Desktop intent | Android consequence | Eclipse policy |
|---|---|---|---|
| `-Xms2G` | Reserve 2 GB up front | Over-commits on 4–6 GB phones; can trigger LMK | Replace with a device-aware initial heap (e.g. 256–512 MB) |
| `-Xmx4G` | Allow up to 4 GB | Most phones cannot give a single app 4 GB; 32-bit impossible | Cap by device memory class and free RAM; default 2 GB, max 3 GB on 8 GB+ devices |
| `-XX:+UseCompactObjectHeaders` | Reduce object headers (Java 25 feature) | Supported only if our OpenJDK 25 build enables it; may interact with Android's heap | Candidate to keep after spike; do not assume |
| `-XX:+AlwaysPreTouch` | Touch all heap pages at startup | Wastes memory and battery; slows launch; can cause OOM | **Remove** |
| `-XX:+UseStringDeduplication` | Save memory | Only meaningful with G1; adds CPU | Keep only if G1 is selected and measured |
| `-XX:+UseZGC` | Low pause on desktop | ZGC needs multi-mapped memory and OS support; may not be built or viable on Android | **Remove by default**; spike only |
| `-XX:StackShadowPages=32` | JVM stack guard | May be excessive on mobile thread stacks | Keep initially; reduce only with evidence |
| `--enable-native-access=ALL-UNNAMED` | Allow restricted native access | Required by Java 25 for JNI/FFM; keep | Keep |
| `--add-exports java.base/jdk.internal.misc=ALL-UNNAMED` | Game/loader internals | Keep for compatibility | Keep |
| `-Djava.library.path=…` | Native lookup | Must point at our Android native dir | Replace |
| `-Dorg.lwjgl.system.SharedLibraryExtractPath=…` | LWJGL extraction | Must be app-private and writable | Replace |
| `-Dio.netty.native.workdir=…` | Netty temp | App-private | Replace |
| `-Dminecraft.launcher.brand/version` | Telemetry/identification | Set to EclipseLauncher and build version | Keep, honest values |

**Inference:** the JVM argument builder should be a policy engine with three layers:

1. **Mojang defaults** (parsed and filtered).
2. **Device policy** (heap, GC, stack, temp dirs, renderer/native paths).
3. **User overrides** (advanced only, validated, with a reset path).

Never concatenate raw user text into a shell command. Use an argument vector and a native `exec`/`JLI_Launch` call.

### 5.2 Game arguments in 26.3

The game arguments include:

```
--username <name>
--version <version>
--gameDir <dir>
--assetsDir <dir>
--assetIndex <id>
--uuid <uuid>
--accessToken <token>
--clientId <entra-client-id>
--xuid <xuid>
--versionType <type>
[--demo]
[--width <w> --height <h>]
[--quickPlayPath <path>]
[--quickPlaySingleplayer <world>]
[--quickPlayMultiplayer <host:port>]
[--quickPlayRealms <realm>]
```

**Security:** the access token is a bearer credential. It must not appear in:
- crash reports;
- logcat lines;
- analytics;
- screenshots;
- process listings visible to other apps (on Android, `/proc/<pid>/cmdline` is restricted, but do not rely on it);
- exported intents or world-readable files.

Use a token file with `MODE_PRIVATE` and pass the token through a protected channel if possible. If the game requires it on the command line, accept that exposure is process-local and redact aggressively everywhere else.

### 5.3 Building the final command

A correct 26.3 launch vector looks like:

```
<jre>/bin/java
  <filtered JVM args>
  -Djava.library.path=<android-natives>
  -cp <client.jar>:<libraries>:<loader-libraries>
  net.minecraft.client.main.Main
  --username <name>
  --version 26.3
  --gameDir <instance>
  --assetsDir <assets>
  --assetIndex 34
  --uuid <uuid>
  --accessToken <redacted>
  --clientId <client-id>
  --xuid <xuid>
  --versionType release
```

On Android we do **not** execute `<jre>/bin/java` as a child process. We load `libjli.so`, resolve `JLI_Launch`, and pass the same argument vector inside the `:game` process. This preserves the desktop launch contract while keeping Android lifecycle and surface ownership in native code.

**Observed:** Pojav/Amethyst use exactly this `dlopen("libjli.so") → dlsym("JLI_Launch") → JLI_Launch(argc, argv, …)` pattern. The Java side supplies `-Djava.home`, `-Djava.io.tmpdir`, `-Duser.home`, `-Dos.name=Linux`, `-Dos.version=Android-…`, `-Djava.library.path`, and the LWJGL/Vulkan library names.

---

## 6. The 26.x platform breaks

### 6.1 26.1 — Java 25 and no obfuscation

**Verified:**

- First release to require **Java 25**.
- First release fully **unobfuscated**, with no obfuscated variant.
- New year.drop.hotfix version format.

**Implications:**

- A Java 8/17/21 runtime cannot launch 26.1+.
- Mod loaders and mods must be built against the new unobfuscated names; old mappings/installers may not apply.
- Our runtime matrix must include a **Java 25** mobile build; Java 21 is only for older versions.
- Unobfuscation does not make the game open source or redistributable.

### 6.2 26.2 — experimental Vulkan

**Verified:**

- Graphics API setting: `Default`, `Prefer Vulkan (Experimental)`, `Prefer OpenGL`.
- `Default` currently behaves like `Prefer OpenGL` but probes Vulkan.
- `Prefer Vulkan` attempts Vulkan and falls back to OpenGL.
- `Prefer OpenGL` avoids Vulkan interaction entirely.
- If the game crashes at startup under Vulkan, it automatically falls back.
- Requirement: **Vulkan 1.2** with **dynamic rendering** and **push descriptors**.
- New `graphics_capabilities` event reports backend, device info, error codes, and missing capabilities.

**Implications:**

- Minecraft's own Vulkan backend is the best long-term Android path because it avoids a desktop-GL translation layer.
- It is experimental; driver bugs and feature gaps are expected.
- The launcher must detect the exact required features, not just `VK_VERSION_1_2`.
- The fallback must be explicit and observable: “Vulkan unavailable: missing VK_KHR_dynamic_rendering” is actionable; “renderer failed” is not.

**Observed:** Zalith and FCL both expose a per-instance graphics API override and write `preferredGraphicsBackend` into `options.txt`. This is the game's supported control surface. Eclipse should use it rather than patching rendering code.

### 6.3 26.3 — SDL3

**Verified:**

- Minecraft uses **SDL3** instead of GLFW for window management, input and platform integration.
- Minimum window size 320×240.
- Keyboard input uses SDL scancodes and keycodes.
- Shaderc is used for OpenGL shader compilation as well as Vulkan.
- LWJGL 3.4.3 supplies the SDL, Shaderc, SPIR-V Cross, VMA and Vulkan modules.

**Implications:**

- The old Android recipe “GLFW stub + EGL + GL4ES” is no longer sufficient for 26.3.
- SDL3's Android backend owns the surface, input, IME, gamepad and audio integration. We must embed SDL's Android Java bridge and native library, or reproduce its JNI contract.
- SDL3 is zlib-licensed; the Android Java bridge is part of SDL. We can vendor it with notices.
- SDL3's Android path normally expects `SDLActivity`/`SDL_main`. Minecraft uses LWJGL's SDL bindings, not SDL's C `main`, so the integration is a **custom host** that initializes SDL and supplies the Android surface. This is exactly what Zalith's `SdlBridge` does.
- The surface can be destroyed and recreated (rotation, fold, multi-window, display switch). The game must survive or fail cleanly.

**Open spike:** SDL3 + LWJGL + Android surface lifecycle on:
- Android 12 phone (rotation, IME);
- Android 14 tablet (split-screen, stylus);
- Android 16 foldable (posture change, 16 KB pages);
- Chromebook (mouse/keyboard, window resize).

---

## 7. Java runtime metadata

The runtime manifest is separate from the game manifest. For `java-runtime-epsilon` on Linux x64:

| Field | Value |
|---|---|
| Version | **25.0.1** |
| Released | **2025-12-10** |
| Manifest | `ccf325ef…` |
| Runtime component | `java-runtime-epsilon` |

**Verified:** the runtime manifest has no Android entry. The available platform keys are `gamecore`, `linux`, `linux-i386`, `mac-os`, `mac-os-arm64`, `windows-arm64`, `windows-x64`, `windows-x86`.

**Implication:** EclipseLauncher cannot download an official Android JRE. It must build, sign, ship and update its own OpenJDK 25 runtime. That runtime is a separate product with its own test matrix and license obligations.

### 7.1 Runtime selection policy

| Minecraft version | Required Java | Runtime strategy |
|---|---|---|
| 26.1+ | 25 | Bundled Eclipse Mobile JRE 25 |
| 1.20.5–1.21.x | 21 | Optional JRE 21 pack (later tier) |
| 1.17–1.20.4 | 17 | Optional JRE 17 pack (later tier) |
| ≤1.16.5 | 8 | Optional JRE 8 pack (legacy, best effort) |

For v1, do not ship four runtimes. Ship **one** (Java 25) and be explicit that only 26.x is supported. Adding runtimes multiplies download size, native build effort, security surface and QA.

---

## 8. Compatibility tiers

A launcher needs a version policy that users can understand.

| Tier | Versions | Promise | Engineering |
|---|---|---|---|
| **A — Primary** | 26.3, 26.4 snapshots | Install, launch, mod, recover; tested on device matrix | SDL3 + Java 25 + Vulkan/GL path |
| **B — Current legacy** | 26.1, 26.2 | Install and launch vanilla; mods best effort | Java 25 + GLFW path + Vulkan detection |
| **C — Back catalog** | 1.20–26.0 | Install and launch if the runtime and renderer are present; no guarantee | JRE 8/17/21, legacy assets, GLFW, older loaders |
| **D — Unsupported** | pre-1.20, modded snapshots | Detect and explain; do not pretend | Avoid for v1 |

**Recommendation:** v1 ships **Tier A only**. Add Tier B after the SDL3 path is stable, and Tier C only if there is demand and a support model. This is a scope decision for sign-off, not a technical limitation hidden from users.

**Why not “support everything” on day one?** Each tier adds:
- another JRE;
- another window/input path;
- another set of loader installers;
- another asset format;
- another graphics translation matrix;
- another class of support tickets.

The market leaders support broad ranges because they inherited a decade of patches. A new launcher should earn that breadth incrementally.

---

## 9. Install footprint and performance expectations

### 9.1 Disk

| Component | Approx. size | Notes |
|---|---|---|
| 26.3 libraries | 79.5 MiB | 66 artifacts |
| 26.3 client jar | 39.6 MiB | |
| 26.3 assets | 462.4 MiB | shared object store |
| Java 25 runtime | ~150–250 MiB | **Inference**, depends on modules and compression |
| Native bundle (JVM, SDL, LWJGL, OpenAL, renderers) | ~80–200 MiB | **Inference**, depends on renderers |
| Instance metadata, configs, logs | < 20 MiB | grows with logs |
| Worlds, mods, resource packs | user-dependent | can be GBs |
| **Minimum comfortable free space** | **4 GB** | install + updates + cache + a small world |
| **Recommended free space** | **8 GB** | modpacks and shader caches |

Show a preflight estimate before install. Do not start a 600 MB download on a device with 700 MB free.

### 9.2 Time

**Inference:** on a typical 4G/5G connection, the 582 MiB vanilla payload takes 1–5 minutes; on Wi-Fi, 30–90 seconds. Asset count is high, so per-request overhead and parallelism matter more than raw bandwidth. The UI should show:
- bytes and files completed;
- current phase (metadata, libraries, assets, runtime, verify);
- an honest ETA based on a rolling window, not a fake linear estimate;
- pause/resume and “continue in background” where Android permits.

### 9.3 Launch time

Targets:
- warm launch (assets/JRE cached): **≤ 20 s** to game window on a supported mid-range device;
- cold install-to-play: **≤ 5 min** on Wi-Fi for vanilla;
- first frame after pressing Play: **≤ 8 s** on a flagship, **≤ 15 s** on a mid-range device.

These are product targets, not verified device results. They become exit criteria for Phase 2.

---

## 10. Launch planner contract

The planner is the heart of correctness. It takes an immutable request and produces an immutable plan.

```kotlin
data class LaunchRequest(
    val instanceId: String,
    val account: AccountSnapshot?,   // null = demo
    val quickPlay: QuickPlayTarget?,
    val resolution: IntSize?,
    val overrides: LaunchOverrides
)

data class LaunchPlan(
    val planId: String,
    val instanceId: String,
    val versionId: String,
    val java: JavaRuntime,
    val classpath: List<Artifact>,
    val natives: NativeSet,
    val assets: AssetSet,
    val jvmArgs: List<String>,
    val gameArgs: List<String>,
    val mainClass: String,
    val workingDir: String,
    val environment: Map<String, String>,
    val renderer: RendererChoice,
    val createdAt: Instant,
    val schemaVersion: Int
)
```

Planner rules:

1. **Pure:** no network, no mutable global state, no UI.
2. **Deterministic:** same inputs + same metadata = same plan ID.
3. **Explainable:** every argument carries an origin and a reason code.
4. **Validated:** classpath entries exist, hashes match, ABI matches, free space is sufficient, Java major version satisfies the version requirement.
5. **Serializable:** the plan is written to a private file and passed to `:game`; no live objects cross the process boundary.
6. **Immutable:** user edits create a new plan; the running game never observes partial changes.

**Failure before launch is cheap. Failure after the JVM starts is expensive.** The planner should reject an invalid plan with a specific error, not let the game crash with `NoClassDefFoundError`.

---

## 11. Error taxonomy for this layer

| Code | Meaning | User-facing action |
|---|---|---|
| `META_UNREACHABLE` | Manifest/version JSON cannot be fetched | Retry, check connection, use cached manifest |
| `META_HASH_MISMATCH` | Version JSON failed SHA-1 | Re-download; do not parse |
| `META_SCHEMA_UNSUPPORTED` | New/unknown schema | Update launcher; show version |
| `LIB_HASH_MISMATCH` | Library corrupt | Re-download that artifact |
| `LIB_ABI_MISMATCH` | Wrong ELF architecture | Reinstall native bundle; report device ABI |
| `LIB_16KB_UNALIGNED` | Native library not 16 KB compatible | Reject and offer updated bundle |
| `ASSET_MISSING` | Asset object absent | Repair assets |
| `JAVA_VERSION_UNSATISFIED` | Required Java missing/old | Install/repair Java 25 |
| `JAVA_LAUNCH_FAILED` | JLI/JVM would not start | Show JVM log tail, offer safe mode |
| `MAIN_CLASS_NOT_FOUND` | Classpath/loader broken | Repair instance; check loader profile |
| `NATIVE_LOAD_FAILED` | `dlopen` failure | Show missing symbol/library; try fallback renderer |
| `GRAPHICS_UNSUPPORTED` | Vulkan/GL requirement unmet | Explain missing capability; switch renderer |
| `OUT_OF_MEMORY` | Heap/native OOM | Lower heap/render distance; close apps |
| `GAME_CRASHED` | Non-zero exit or signal | Show crash report, last log lines, repair options |

Every error has: code, short title, one-sentence cause, evidence (redacted), one recommended action, and a secondary “details” door. No raw stack trace on the first screen.

---

## 12. Open questions and spikes

1. **26.4 changes:** does the next drop alter SDL/Vulkan requirements, assets, or Java? Track snapshots weekly; do not promise 26.4 support before it is tested.
2. **Vulkan feature set:** exact required extensions/features on 26.3 and how to probe them without initializing a window. Build a native probe that returns a capability report.
3. **SDL3 host contract:** can we use SDL's Android Java bridge with a custom Activity and a Compose-hosted SurfaceView without `SDL_main`? Zalith demonstrates a path; reproduce it clean-room and test lifecycle.
4. **JVM 25 build:** does OpenJDK 25 cross-compile cleanly with NDK r28/r29, and which modules can be excluded? See doc 05.
5. **16 KB loading:** prove `dlopen` of JRE, renderer and SDL libraries from app-private storage on 16 KB devices.
6. **Heap policy:** measure actual RSS and LMK behavior for 1.5/2/2.5/3 GB heaps on 6/8/12 GB devices. Do not ship a static `-Xmx4G`.
7. **Quick Play:** verify `--quickPlayMultiplayer` and `--quickPlaySingleplayer` behavior with 26.3 on Android; it is a differentiator for “one tap to play.”
8. **Demo mode:** verify `--demo` launches without an account and what restrictions apply. Demo mode is our test path before AppID approval.

---

## 13. Sources for this document

- [S1] Mojang version manifest — https://piston-meta.mojang.com/mc/game/version_manifest_v2.json
- [S2] 26.3 version JSON — https://piston-meta.mojang.com/v1/packages/61fff1316bd50312624a858bbb5eb18fa45208ed/26.3.json
- [S3] Java Edition 26.3 — https://minecraft.wiki/w/Java_Edition_26.3
- [S4] Java Edition 26.2 — https://minecraft.wiki/w/Java_Edition_26.2
- [S5] Java Edition 26.1 — https://minecraft.wiki/w/Java_Edition_26.1
- [S6] Java runtime manifest — https://launchermeta.mojang.com/v1/products/java-runtime/2ec0cc96c44e5a76b9c8b7c39df7210883d12871/all.json
- [S7] Minecraft EULA — https://www.minecraft.net/en-us/eula
- [S8] LWJGL 3.4.3 — https://github.com/LWJGL/lwjgl3/releases/tag/3.4.3
- [S9] SDL3 — https://github.com/libsdl-org/SDL
- [S10] Android 16 KB pages — https://developer.android.com/guide/practices/page-sizes
- [S11] Amethyst `JREUtils.java`, `jre_launcher.c` (source snapshot)
- [S12] Zalith `SdlBridge.kt`, `SDLActivity.java` (source snapshot)

All source snapshots were read on 2026-10-24. Version numbers and URLs are point-in-time; re-verify before implementation.
