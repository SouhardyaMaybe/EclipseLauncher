# 04 — Android platform contract

**Verified:** 2026-10-24 against Android 16/17 behavior-change documentation, Android NDK page-size guidance, Play policy, and F-Droid inclusion policy.
**Scope:** the Android rules that constrain Eclipse Launcher: API levels, 16 KB pages, storage, processes, background work, display/input/audio, thermals, packaging and distribution.
**Labels:** **Verified** (primary source), **Observed** (device or artifact), **Inference** (reasoned, needs a test), **Open** (unknown).

---

## 1. The contract in one page

| Constraint | Decision for Eclipse Launcher | Why |
|---|---|---|
| Minimum SDK | **26** (Android 8.0) | Pointer capture, adaptive icons, modern audio; below this the JVM/native surface is not worth supporting |
| Target SDK | **36** (Android 16) for v1; move to 37 when the runtime is ready | Play requires 36 from 2026-08-31; API 37 adds native-DCL, local-network and reflection restrictions |
| Compile SDK | **36** | Matches AGP 9.x and the installed platform |
| ABI | **arm64-v8a** first; x86_64 for emulator; armeabi-v7a deferred | Java 25 + 16 KB + native stack is heavy; 32-bit is a separate engineering and memory problem |
| Page size | **16 KB compliant** for every shipped `.so` and every uncompressed `.so` in the APK | Play deadline 2027-02-01; 16 KB devices already exist |
| Storage | Instances in `getExternalFilesDir("instances")`; runtime in internal app storage; import/export via SAF | No broad storage permission; user-visible instance folder without `MANAGE_EXTERNAL_STORAGE` |
| Process model | UI process + isolated `:game` process; JVM created in-process with JNI | JVM crashes, driver crashes and LMK kills must not take down the launcher |
| Downloads | User-initiated data transfer jobs (API 34+) with a `dataSync` foreground-service fallback | `dataSync` is capped at 6 h per 24 h; `shortService` is ~3 min |
| Display | Edge-to-edge, adaptive layouts, `Surface.setFrameRate()` | Android 16 ignores orientation/resize restrictions on large screens; games default to 60 Hz |
| Input | Pointer capture, gamepad axes/keys, IME, touch overlay | Desktop Minecraft expects a mouse and keyboard; touch must be a first-class translation layer |
| Audio | Official OpenAL Soft with the OpenSL ES backend, `USAGE_GAME`, audio focus | No Oboe fork; Android 17 hardens background audio, and the game stops audio when backgrounded |
| Distribution | GitHub Releases first; F-Droid only as a separate reviewed flavor; no Play at launch | Play's executable-code policy conflicts with downloading `client.jar` and natives |

---

## 2. API levels and deadlines

### 2.1 The dates that matter

| Date | Requirement | Impact |
|---|---|---|
| 2026-08-31 | New Play apps and updates must target **API 36** | v1 targets 36; an extension to 2026-11-01 exists but is not a strategy |
| 2027-02-01 | Play updates must support **16 KB** page sizes on 64-bit devices | Every `.so`, including `libjvm.so`, must be 16 KB aligned |
| Android 16 (API 36) | Edge-to-edge enforced; predictive back default; large-screen orientation/resize restrictions ignored | The UI must be inset-aware and adaptive; no fixed orientation on tablets |
| Android 17 (API 37) | Static final fields unmodifiable; native DCL read-only; local-network permission; background-audio hardening; MessageQueue rewrite | Mod loaders and reflection-heavy code need testing; LAN multiplayer needs `ACCESS_LOCAL_NETWORK` |

**Verified:** Play target API requirements, 2026-08-31, extension to 2026-11-01 ([Play target API](https://support.google.com/googleplay/android-developer/answer/11926878)); 16 KB Play requirement, 2027-02-01 ([16 KB](https://developer.android.com/guide/practices/page-sizes)); Android 16 behavior changes ([Android 16](https://developer.android.com/about/versions/16/behavior-changes-16)); Android 17 behavior changes ([Android 17](https://developer.android.com/about/versions/17/behavior-changes-17)).

### 2.2 Android 16: the UI is no longer allowed to fight the window

For apps targeting API 36:

- **Edge-to-edge is mandatory.** `windowOptOutEdgeToEdgeEnforcement` is deprecated and disabled. The launcher must consume insets, not draw under system bars by accident.
- **Predictive back is default.** `onBackPressed()` is not called and `KEYCODE_BACK` is not dispatched. Use the predictive-back APIs or opt out temporarily with `android:enableOnBackInvokedCallback="false"`.
- **Large screens ignore orientation, resizability and aspect-ratio restrictions** at `sw >= 600dp`. Games declared with `android:appCategory="game"` are exempt, but the launcher itself should not rely on that exemption. The instance list, settings and download screens must work in portrait, landscape, split-screen and desktop windowing.

**Consequence:** the Compose UI uses `WindowSizeClass` and `NavigationSuiteScaffold`, not a fixed phone layout. The game activity is a separate, game-category activity with a locked surface only where the renderer requires it.

### 2.3 Android 17: the runtime gets stricter

Android 17 (API 37) adds four constraints that hit a Minecraft launcher directly:

1. **Static final fields are unmodifiable.** Reflection or JNI writes to `static final` fields throw `IllegalAccessException` or crash. Mod loaders, Mixin, and some mods do this. Test Fabric/NeoForge on API 37 before raising `targetSdk`.
2. **Native DCL requires read-only files.** `System.load()` of a writable native file throws `UnsatisfiedLinkError`. Any JRE library extracted to app data must be `chmod 0444` before loading; the APK's native library directory is already read-only.
3. **`ACCESS_LOCAL_NETWORK` is a runtime permission.** LAN multiplayer and local discovery need it. It is in the `NEARBY_DEVICES` group, so users who granted another nearby permission may not see a second prompt.
4. **Background audio is hardened.** Background playback/focus/volume changes require a foreground service with while-in-use capability (or exact alarms for `USAGE_ALARM`). Eclipse Launcher avoids the requirement entirely: when the game is not visible it pauses and stops audio, so `:game` never plays in the background and does not need a media-playback service type.

**Inference:** targeting 37 immediately is unnecessary risk. Target 36 for v1, run an API 37 test matrix, and raise the target only after Fabric/NeoForge and the JRE pass.

---

## 3. 16 KB pages

### 3.1 What actually has to change

16 KB devices use 16 KB memory pages. A 4 KB-built `.so` can fail to load or crash because:

- ELF `PT_LOAD` segments must be aligned to `0x4000`, not `0x1000`.
- `PT_GNU_RELRO` must end on a 16 KB boundary; otherwise the linker's read-only protection overlaps writable data and segfaults.
- Uncompressed `.so` files inside the APK must start at a 16 KB-aligned zip offset (`zipalign -P 16`).
- Code that hard-codes `PAGE_SIZE` or `4096` must use `sysconf(_SC_PAGESIZE)` or the NDK's page-size helpers.

**Verified:** NDK r28+ compiles 16 KB-aligned by default; NDK r27 and lower need `-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384`; AGP 8.5.1+ is required for 16 KB zip alignment; `PAGE_ALIGNMENT_16K` in the AAB manifest confirms the bundle requests it ([16 KB](https://developer.android.com/guide/practices/page-sizes)).

### 3.2 Our artifact audit

| Artifact | Current state | Action |
|---|---|---|
| Amethyst OpenJDK 25 Android JRE | **Observed:** `libjvm.so`, `libjava.so`, `libjli.so` are `p_align 0x1000` (4 KB) | Do not use; EclipseJRE must be rebuilt with 16 KB flags |
| EclipseJRE 25 arm64 release | **Open:** built with NDK r28c, but the link flags and RELRO alignment are not yet verified | Add a hard gate; rebuild if any `PT_LOAD` is below `0x4000` |
| EclipseNative `native-v1.1.0` AAR | **Open:** built with NDK 25.2 in the draft pipeline; likely 4 KB | Rebuild with NDK r28+ and our own official-upstream patches |
| EclipseSDL3 `sdl3-v1.0.0` | **Open:** built with NDK 28.2; likely 16 KB, but not yet checked | Add the same gate to its release workflow |
| App APK | Not built | Release workflow runs `check-elf-alignment.py` on the APK before upload |

The gate is intentionally fail-closed: a release with a single 4 KB `.so` is not publishable. Android 16's 16 KB compatibility mode is a user-facing dialog, not a substitute for a correct build.

### 3.3 Build rules

- **NDK r28c or newer** for EclipseJRE, EclipseNatives and EclipseSDL3.
- **Link flags:** `-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384` for any build system that does not already default to 16 KB.
- **Packaging:** `android.packaging.jniLibs.useLegacyPackaging = false`; `zipalign -P 16`; verify with `zipalign -c -P 16 -v 4`.
- **Verification:** `readelf -lW` on every `.so`; our `scripts/check-elf-alignment.py` checks `PT_LOAD`, `PT_GNU_RELRO` and APK/AAR zip offsets without the Android SDK.

**Open:** the JRE's `libjvm.so` is linked by OpenJDK's build, not by a simple CMake target. The 16 KB flags must be injected into the JDK's `LDFLAGS` and the resulting `libjvm.so` checked. If OpenJDK's linker script overrides `-z max-page-size`, the build must patch the HotSpot makefile, not just add a flag.

---

## 4. Storage

### 4.1 The model

Android's scoped storage gives an app:

- **Internal app storage** (`getFilesDir()`, `getCacheDir()`): private, not user-visible, included in backup unless excluded. Best for tokens, logs, runtime binaries, and the JRE.
- **App-specific external storage** (`getExternalFilesDir()`): private to the app, no permission, user-visible over USB/file managers on most devices, deleted on uninstall. Best for multi-gigabyte instances, worlds, resource packs and screenshots.
- **Shared storage** (`MediaStore`, SAF): user-visible, permission-gated, not a filesystem path. Best for import/export.

**Verified:** app-specific external files need no storage permission; scoped storage blocks direct access to other apps' directories; SAF cannot grant the internal-storage root, the SD-card root, or the `Download` directory on Android 11+; `MANAGE_EXTERNAL_STORAGE` is restricted on Play ([app-specific files](https://developer.android.com/training/data-storage/app-specific), [SAF](https://developer.android.com/guide/topics/providers/document-provider)).

### 4.2 Instance layout

```
<external-files>/instances/<uuid>/
  instance.json          # name, icon, game version, loader, renderer, JVM args
  game/                  # --gameDir
    mods/ config/ saves/ resourcepacks/ shaderpacks/ logs/
  runtime/               # optional per-instance JRE/native pin
  cache/                 # downloads, temp, safe to delete
```

The launcher never writes to `/sdcard/games/...` by absolute path. Export uses SAF: the user picks a tree, and the launcher streams a zip. Import uses SAF and extracts into a staging directory, validates paths, then atomically renames.

### 4.3 What not to do

- **Do not request `MANAGE_EXTERNAL_STORAGE` for v1.** It is a Play-restricted permission, a privacy red flag, and unnecessary for app-specific instances.
- **Do not assume SAF can select the SD-card root.** It cannot; use app-specific external storage on the chosen volume, or a user-selected subtree.
- **Do not store tokens in external storage.** Use Android Keystore-wrapped encryption in internal storage.
- **Do not back up instances or tokens.** Set `android:allowBackup="false"` and explicit `dataExtractionRules`; a 20 GB instance must not enter cloud backup.

---

## 5. Processes, the JVM and the low-memory killer

### 5.1 Two processes

```
dev.shadow.eclipselauncher          UI, auth, downloads, instance DB
dev.shadow.eclipselauncher:game     JVM, SDL3, LWJGL, renderer, game
```

The `:game` process is declared with `android:process=":game"`. It is not `isolatedProcess` because it needs the app's UID to read the instance and the JRE. `GameActivity` — not a background service — lives in `:game` and owns the surface, the SDL bridge and the JVM, so the process is visible while the user is playing and LMK treats it accordingly. There is no separate game foreground service.

**Why a separate process:**

- A JVM `SIGSEGV` in a graphics driver kills only the game.
- The UI can show a crash report and offer “safe mode” without restarting the launcher.
- The UI's memory stays small while the game uses 1–3 GB.
- The JVM can be created once per `:game` process and discarded on exit. `JNI_CreateJavaVM` cannot be called twice in one process, so a fresh process per launch is mandatory.

### 5.2 JVM launch model

**Decision:** launch the JVM **in-process** in `:game` via JNI, not by `exec`ing a `java` binary.

- Android 10+ blocks `execve` of files in app-writable storage; the APK's native library directory is the only reliable executable location.
- `libjvm.so` is loaded with `dlopen`/`JNI_CreateJavaVM` from the app's native library directory or a read-only extracted runtime.
- On Android 17+, any extracted native file loaded with `System.load()` must be `0444`.
- The launch arguments are passed as a Java `String[]` to `JNI_CreateJavaVM`; the main class is `net.minecraft.client.main.Main` (or the loader's Knot client).

**Open:** the exact linker namespace behavior for `libjvm.so` and its dependencies (`libjava.so`, `libjli.so`, `libnet.so`, `libnio.so`) on Android 14–17. Phase 0 spike: load the EclipseJRE 25 arm64 runtime in a minimal `:game` process and print `java.vm.version`.

### 5.3 Lifecycle and LMK

- `GameActivity` in `:game` is the visible surface owner; there is no silent background game service.
- If the system kills the process, do not restart it automatically. Persist a `launch-in-progress` marker and offer recovery when the user returns.
- `onTrimMemory(TRIM_MEMORY_RUNNING_LOW/CRITICAL)`: drop caches, close asset streams, and tell the game thread to reduce render distance if it is still responsive.
- The UI process observes `:game` death via `Binder`/`DeathRecipient` or a `Handler` on a `Messenger`; it reads the last crash report from disk and shows a structured diagnosis.
- Do not use `android:persistent="true"`; it is reserved for system apps.

**Verified:** Android's process lifecycle and LMK kill background processes before visible ones; a foreground service raises importance but does not guarantee survival ([process lifecycle](https://developer.android.com/guide/components/activities/process-lifecycle)).

---

## 6. Downloads and background work

### 6.1 The right API for a 600 MB install

A vanilla install is ~581.5 MiB before the JRE; a modpack can be several GB. The download must survive screen-off, network changes and process death.

| API | Limit | Use |
|---|---|---|
| **User-initiated data transfer job** (API 34+) | User-started, notification, extended time subject to system health; not subject to App Standby quotas | **Primary** for game/library/asset downloads |
| `dataSync` foreground service | 6 h per 24 h for target 35+; timer resets when the app is foreground | Fallback for API 26–33 and short repair downloads |
| `shortService` foreground service | ~3 minutes | Never for game downloads; only for a quick hash/repair step |
| `WorkManager` | Deferred, quota-limited | Background repair, log upload, cache cleanup |

**Verified:** UIDT jobs were introduced in Android 14; they must be scheduled while the app is visible, require a notification, and are intended for long user-initiated transfers ([UIDT](https://developer.android.com/develop/background-work/background-tasks/uidt)). `dataSync`/`mediaProcessing` are capped at 6 h in a 24 h period for target 35+ ([FGS timeouts](https://developer.android.com/develop/background-work/services/fgs/timeout)). `shortService` is about 3 minutes ([FGS types](https://developer.android.com/develop/background-work/services/fgs/service-types)).

### 6.2 Download engine requirements

- **Resumable HTTP range requests** with per-file `.part` files and a persisted ETag/Last-Modified.
- **Concurrent limit** of 4–6 connections; Mojang/Modrinth CDNs punish aggressive parallelism.
- **SHA-1/SHA-256 verification** before an asset is moved from `cache/` to its final path.
- **Transactional install:** stage into `<instance>/.staging/<txid>/`, fsync, then atomic rename; write a journal so a crash can roll back.
- **Network awareness:** pause on metered networks unless the user opts in; respect `ConnectivityManager` network capabilities.
- **Notification:** progress, current file, bytes/sec, pause/resume/cancel. The user can stop the job from the notification (UIDT requirement).

**Inference:** use OkHttp + a foreground service/UIDT wrapper rather than `DownloadManager`. `DownloadManager` is opaque, hard to checksum per file, and its queue semantics do not match a transactional installer.

---

## 7. Display, input and audio

### 7.1 Display

- **Edge-to-edge:** `enableEdgeToEdge()` and `WindowInsets` in Compose; never assume system bars are opaque.
- **Adaptive layout:** `WindowSizeClass` breakpoints; list-detail on tablets/foldables; the game surface fills the window in landscape.
- **Refresh rate:** Android 15+ defaults games to 60 Hz. Call `Surface.setFrameRate(fps, compatibility)` when the render loop starts; query `Display.getSupportedModes()` and cap the in-game FPS setting to the panel's maximum. The system may still clamp for power/thermal reasons.
- **Cutouts:** `layoutInDisplayCutoutMode="shortEdges"` for the game; the overlay must keep controls out of the cutout.
- **Multi-window:** the launcher supports split-screen; the game activity declares `resizeableActivity="true"` and handles `onConfigurationChanged` by recreating the surface, not the JVM.

**Verified:** Android 15 sets a default 60 Hz for games; use `Surface.setFrameRate()` or Swappy to request higher rates; the system can still clamp ([refresh rate](https://developer.android.com/games/optimize/display-refresh-rate-change)).

### 7.2 Input

Minecraft expects a mouse, keyboard and gamepad. Android delivers:

- **Touch:** `MotionEvent` with pointer IDs; the overlay maps regions to keycodes/axes.
- **Mouse:** `onGenericMotion` for movement/scroll; `requestPointerCapture()` for relative deltas. Pointer capture is API 26+.
- **Touchpad:** Android 17 delivers relative events during pointer capture by default; `POINTER_CAPTURE_MODE_ABSOLUTE` remains for absolute data.
- **Gamepad:** `InputDevice.SOURCE_GAMEPAD`, `AXIS_X/Y/Z/RZ`, `KEYCODE_BUTTON_A/B/X/Y`, `L1/R1/L2/R2`, `THUMBL/THUMBR`; SDL3's Android joystick backend can consume these directly.
- **Keyboard:** physical keyboard events; `KEYCODE_BACK` is not dispatched to `onBackPressed` on API 36+, so the game must handle it as a key or the system gesture.
- **IME:** Minecraft's chat uses a text field; the launcher opens a Compose `BasicTextField` overlay and forwards the committed text to the game via SDL3's text-input events.

**Verified:** pointer capture delivers all mouse events to a focused view and uses relative coordinates; request it from a click or window-focus event ([pointer capture](https://developer.android.com/develop/ui/views/touch-and-input/gestures/movement#pointer-capture)). Android 17 touchpad behavior is documented in the API 37 behavior changes.

### 7.3 Audio

- **OpenAL Soft** is the game's audio API. v1 uses the official upstream build with the **OpenSL ES** backend; there is no Oboe or FCL fork. AAudio arrives later through OpenAL Soft's official SDL3 backend, which reuses EclipseSDL3's AAudio stream.
- **Usage:** `AudioAttributes.USAGE_GAME`, `CONTENT_TYPE_SONIFICATION` or `MUSIC`.
- **Focus:** request `AUDIOFOCUS_GAIN` when the game starts; pause or duck on `AUDIOFOCUS_LOSS_TRANSIENT`; abandon on stop.
- **Background:** when the activity stops, the game pauses and OpenAL stops the device. Because the app never plays audio while invisible, Android 17's background-audio hardening does not apply.
- **Latency:** OpenSL typically gives 40–80 ms. Minecraft does not need a low-latency path; do not advertise one.

**Verified:** Android 17 background-audio hardening requires a foreground service with while-in-use capability or exact-alarm/`USAGE_ALARM` for background interaction ([Android 17 audio](https://developer.android.com/about/versions/17/behavior-changes-17#background-audio)). **Verified:** OpenAL Soft has no official Oboe backend; its documented Android backend is OpenSL, with SDL3 added in 1.25 ([OpenAL Soft](https://openal-soft.org)).

---

## 8. Thermals, performance and memory

### 8.1 ADPF and Game Mode

- **ADPF** (Android Dynamic Performance Framework) exposes CPU/GPU headroom hints. Use it to adjust render distance, resolution scale and frame rate before the device throttles.
- **Game Mode API** (`GameManager.getGameMode()`) tells the launcher whether the user selected Performance, Balanced or Battery Saver. Honor it: Performance may allow a higher refresh rate; Battery Saver caps FPS and render distance.
- **Game State API** lets the system know when the game is loading, in a menu, or playing; use it to avoid boosting clocks during a long download or a paused menu.
- **Thermal API:** poll `PowerManager.getThermalHeadroom(seconds)` **no more than once every 10 seconds**; calling it too often returns `NaN`. Register a `ThermalStatusListener` for status changes and only then increase polling.

**Verified:** thermal headroom returns 0.0 (no throttling) to 1.0 (severe); do not call it more than once every 10 seconds or from multiple threads; it can return `NaN` ([thermal](https://developer.android.com/games/optimize/adpf/thermal)).

### 8.2 Memory

- **JVM heap is native memory, not Dalvik heap.** `android:largeHeap="true"` does not give the JVM more room; it only raises the Dalvik/ART heap limit. Size the JVM with `-Xmx` based on device RAM and the instance's mod count.
- **Device budget:** a 4 GB phone should run a 1.5–2 GB heap vanilla instance; a 6–8 GB phone can go to 3 GB; 12 GB+ can go to 4 GB. The launcher must measure `ActivityManager.MemoryInfo.totalMem` and `availMem`, not guess from a model name.
- **ZGC vs G1:** the 26.3 default `-XX:+UseZGC` is designed for large heaps and can be slow to start on a phone. Phase 0 measured `-Xms512M -Xmx2G -XX:+UseZGC` at ~1.1–1.2 s startup, while `-Xms2G -Xmx4G` + `AlwaysPreTouch` took 4.6–16 s. The launcher should default to a small initial heap and ZGC, with G1 as a fallback for low-RAM devices.
- **`AlwaysPreTouch`:** remove it on Android. It commits the entire heap up front and triggers LMK/OOM.
- **`UseStringDeduplication`:** only valid with G1; remove it when using ZGC.
- **`UseCompactObjectHeaders`:** Java 25 feature; verify the Android build supports it before keeping it.

**Observed:** on the live Android 16 arm64 test device (5.3 GiB RAM), the Amethyst OpenJDK 25 JRE reported a default max heap of 1.32 GiB; `-Xms512M -Xmx2G -XX:+UseZGC` started in ~1.1–1.2 s; `-Xms2G -Xmx4G -XX:+UseZGC -XX:+AlwaysPreTouch` took 4.6–16 s.

### 8.3 CPU affinity

EclipseSDL3 can pin the render thread to the SoC's big cores (`SDL_HINT_ECLIPSE_BIGCORE_AFFINITY`). Treat this as an optimization, not a correctness requirement:

- Detect heterogeneous clusters from `cpu_capacity`/`sched_domains`, not from a hard-coded core count.
- Do not pin the JVM's GC or the audio thread to big cores; it increases thermal pressure and audio glitches.
- Disable affinity when the device is thermally throttled or when the user selects Battery Saver.

---

## 9. Packaging

### 9.1 APK configuration

| Setting | Value | Reason |
|---|---|---|
| `minSdk` | 26 | API floor |
| `targetSdk` | 36 | Play deadline; API 37 after testing |
| `compileSdk` | 36 | AGP 9.x |
| `android:appCategory` | `game` on the game activity/application | Large-screen orientation exemption; Game Mode |
| `android:extractNativeLibs` | `true` | HotSpot's `dlopen` needs a real read-only file; the APK's native dir is extracted by the installer |
| `android:largeHeap` | `false` | Does not help the JVM; hides memory problems |
| `android:allowBackup` | `false` | Tokens and multi-GB instances must not be backed up |
| `android:usesCleartextTraffic` | `false` | All official endpoints are HTTPS |
| `android:debuggable` | `false` in release | Security and performance |
| `android:networkSecurityConfig` | pinned trust anchors, no user CAs in release | Prevent MITM of auth/downloads |
| `android:enableOnBackInvokedCallback` | `true` | Predictive back |
| `android:resizeableActivity` | `true` | Adaptive/multi-window |

**Verified:** with `extractNativeLibs=true` (and `packaging.jniLibs.useLegacyPackaging = true`), the installer extracts the `.so` files into `ApplicationInfo.nativeLibraryDir`, which is read-only. The 16 KB **ELF** rule still applies to every extracted library; the 16 KB **zip-alignment** rule applies only to uncompressed in-APK libraries, which this configuration does not rely on ([application element](https://developer.android.com/guide/topics/manifest/application-element), [16 KB](https://developer.android.com/guide/practices/page-sizes)).

### 9.2 What goes in the APK vs. what is downloaded

| Item | In APK | Downloaded at first run |
|---|---|---|
| Launcher code, Compose UI | ✅ | — |
| `libSDL3.so`, `liblwjgl*.so`, `libopenal.so`, `libfreetype.so` | ✅ (arm64-v8a) | — |
| EclipseJRE 25 (`libjvm.so`, modules, `java.desktop`) | ❌ | ✅ component archive, extracted read-only |
| Minecraft `client.jar`, libraries, assets | ❌ | ✅ from Mojang |
| Fabric loader libraries | ❌ | ✅ from Maven |
| Mods/modpacks | ❌ | ✅ from Modrinth |
| FFmpeg plugin | ❌ | ✅ optional APK |

**Inference:** with the JRE downloaded separately, an arm64 APK carrying the launcher and the native stack stays under ~60 MB. A JRE-bundled variant would be ~120–180 MB and is not the default. The first-run download keeps the APK small and lets EclipseJRE be updated independently.

### 9.3 Signing and updates

- **Release key:** offline or repository secret; never in the repo. `apksigner` with v2/v3/v4 signatures.
- **Update model:** the launcher checks the GitHub Releases API for a newer tag and opens the release page. It does **not** silently download and install an APK; that is a Play-policy anti-pattern and a security risk.
- **Reproducibility:** pin AGP, Gradle, JDK, NDK and all dependency versions; set `SOURCE_DATE_EPOCH`; use `--no-daemon`; publish `BUILD-MANIFEST.json`.
- **Integrity:** publish `SHA256SUMS.txt` and sign the manifest with the release key or Sigstore.

---

## 10. Distribution reality

### 10.1 Google Play

Play's Device and Network Abuse policy says:

> “an app may not download executable code (such as dex, JAR, .so files) from a source other than Google Play. This restriction does not apply to code that runs in a virtual machine or an interpreter where either provides indirect access to Android APIs.”

A Minecraft launcher downloads `client.jar` (JAR), loader libraries (JAR) and potentially native drivers (`.so`). The JVM exception is arguable for the JARs, but not for downloaded `.so` files, and Google's reviewers are not obligated to accept the interpretation. Play is therefore **not a launch channel**. A Play build would need every native library bundled, no self-update, and a legal review; that is a different product.

**Verified:** exact policy text ([Play Device and Network Abuse](https://support.google.com/googleplay/android-developer/answer/16559646)).

### 10.2 F-Droid

F-Droid requires:

- FLOSS app code and a 100% FLOSS build toolchain (no Oracle JDK, no proprietary SDKs);
- reproducible builds from public source;
- no proprietary dependencies, tracking or ads;
- explicit opt-in consent for downloading additional executable binaries, with a warning that the user is bypassing F-Droid's checks.

A Minecraft launcher depends on proprietary Minecraft assets and Mojang's network service. It can be labeled `NonFreeNet`/`NonFreeDep`, but it is not a clean main-repo candidate. F-Droid is a **post-v1, separate flavor** with its own review, not a launch promise.

**Verified:** F-Droid inclusion policy ([Inclusion Policy](https://f-droid.org/en/docs/Inclusion_Policy/)), anti-features ([Anti-Features](https://f-droid.org/en/docs/Anti-Features/)).

### 10.3 GitHub Releases

Primary channel:

- Tag `v*` triggers the release workflow.
- Assets: APK(s), `SHA256SUMS.txt`, `BUILD-MANIFEST.json`, release notes.
- No auto-update; the app links to the release page.
- The release workflow refuses to publish an unsigned APK or a 4 KB-aligned `.so`.

---

## 11. Device matrix

| Tier | Device class | Test focus |
|---|---|---|
| T0 | Android 16/17 arm64, 8 GB+ | JVM 25, SDL3, Vulkan, 120 Hz, gamepad |
| T1 | Android 14/15 arm64, 6–8 GB | GLES translation, thermal throttling, 16 KB emulator |
| T2 | Android 12/13 arm64, 4–6 GB | Low-memory launch, G1 fallback, SAF import |
| T3 | Android 8–11 arm64, 3–4 GB | Minimum SDK, no UIDT, `dataSync` fallback |
| Emulator | x86_64 API 36 with 16 KB image | CI smoke test, no GPU claims |

**Open:** a 16 KB emulator image is required to test the JRE and natives end-to-end. The current test device is 4 KB; static `readelf` checks are necessary but not sufficient.

---

## 12. Decisions and open questions

### Decisions proposed for sign-off

1. **Target SDK 36 for v1; API 37 after the mod-loader/JRE matrix passes.**
2. **arm64-v8a only for the first release; x86_64 for the emulator; armeabi-v7a deferred.**
3. **Instances in app-specific external storage; no `MANAGE_EXTERNAL_STORAGE`.**
4. **UI process + `:game` process; in-process JVM via JNI; one launch per process.**
5. **UIDT first, `dataSync` fallback; no `shortService` for downloads.**
6. **GitHub Releases first; F-Droid as a separate reviewed flavor; no Play at launch.**
7. **16 KB gate is fail-closed for every release.**
8. **`extractNativeLibs=true`; the JRE is a downloaded, read-only component.**
9. **`GameActivity` lives in `:game`; there is no separate game foreground service, and audio stops when the game is backgrounded.**

### Open questions

- Does Android 17's read-only native-DCL rule apply to `dlopen` of `libjvm.so` and its dependencies, or only `System.load()`? Test on API 37.
- Does the Android linker permit `libjvm.so` in the APK's native library dir to load `libjava.so` from a read-only extracted JRE directory? Test with the EclipseJRE 25 arm64 bundle.
- Does Fabric/NeoForge survive Android 17's `static final` restriction? Test with a representative mod set.
- What is the real cold-start time of EclipseJRE 25 on a 4 GB device with a 1.5 GB heap?
- Does `Surface.setFrameRate()` work when the surface is created by SDL3 rather than a native `SurfaceView`? Test on 90/120 Hz devices.
