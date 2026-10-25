# 14 — Glossary

**Status:** reference, 2026-10-24.
**Scope:** the terms used across the dossier, with the meaning this project assigns to them. Where a word has a different common meaning, §10 disambiguates it.
**Convention:** a term in **bold** is defined elsewhere in this chapter; a chapter reference points to the fullest treatment.

---

## 1. Project and component names

| Term | Meaning |
|---|---|
| **Eclipse Launcher** | The Android application in this repository: UI, authentication, installation, instance management and launch orchestration. Package `dev.shadow.eclipselauncher`. Apache-2.0. |
| **EclipseJRE** | The project's OpenJDK 25 build for Android: `libjvm.so`, the jlink module image, and the headless `java.desktop` classes. Separate repository and release cadence. GPL-2.0 + Classpath Exception. |
| **EclipseNatives** | The project's Android builds of official upstream native libraries — LWJGL, OpenAL Soft, FreeType, Shaderc, SPIRV-Cross, VMA, STB. It does not fork them. |
| **EclipseRuntime** | The component subsystem: signed index, resolver, downloader, verifier, installer and rollback for EclipseJRE, EclipseNatives and EclipseSDL3. In the module graph it is `:domain:runtime` plus `:core:storage`. |
| **EclipseSDL3** | SDL 3.4.x for Android with the original `dev.shadow.eclipselauncher.sdl` Java bridge and the `pw_host` handover. Zlib. |
| **Sandpaper** | The original compatibility runtime that implements desktop OpenGL 3.3 core on top of GLES 3.2. It is **not** a Vulkan renderer, a GPU driver, or a GLSL compiler. |
| **Sash** | The deferred `java.awt` shim that patches Android-backed `Toolkit` and `ImageIO` into `java.desktop`. It is not a full AWT implementation and does not ship in v1. |
| **Foreman** | The Forge/NeoForge installer engine: reads `install_profile.json`, runs processors, verifies outputs. Used by the `:installer` process. |
| **Crowbar** | The binary patcher and renamer used by Foreman for loader installs. |
| **`pw_`** | The prefix for JNI symbols exported by `libeclipse_host.so`, for example `pw_host_start`. Distinguishes project symbols from SDL's and LWJGL's. |
| **The dossier** | Chapters 00–14 in `docs/`, the evidence base for the architecture. |

---

## 2. Minecraft terms

| Term | Meaning |
|---|---|
| **Minecraft: Java Edition** | The desktop Java game this launcher runs on Android. Not Bedrock Edition. |
| **Vanilla** | A Minecraft instance with no mod loader. |
| **Instance** | An isolated game installation: a version, an optional loader, a game directory, a manifest and a launch plan. The user-facing unit of play. |
| **Profile** | Ambiguous. In Mojang metadata, a version/loader JSON; in launcher UI, avoid it and say **instance**. See §10. |
| **`client.jar`** | The official game jar downloaded from Mojang. Never redistributed by this project. |
| **Version JSON** | Mojang's per-version document: libraries, arguments, asset index, downloads, Java version. |
| **Version manifest** | `version_manifest_v2.json`, the list of versions and their JSON URLs/hashes. |
| **Asset index** | A JSON map from logical asset names to hashes and sizes. 26.3 uses index `34`. |
| **Asset object** | A file in the content-addressed asset store, named by its SHA-1 and fetched from `resources.download.minecraft.net`. |
| **Library** | A Maven artifact declared by the version JSON, usually a Java jar. See §10 for the native-library distinction. |
| **Native / native artifact** | A platform-specific `.so` packaged in a Maven classifier (for example `natives-linux`). Mojang's are x86_64 glibc; EclipseNatives supplies Android builds. |
| **Rules** | Mojang's allow/disallow conditions on libraries and arguments (`os.name`, `os.arch`, `features`). Evaluation order matters; the last match wins. |
| **`mainClass`** | The Java class the JVM invokes. Vanilla: `net.minecraft.client.main.Main`; Fabric: `net.fabricmc.loader.impl.launch.knot.KnotClient`. |
| **Launch arguments** | The game arguments (`--username`, `--uuid`, `--accessToken`, `--gameDir`, …) and JVM arguments (`-Xmx`, `-Djava.library.path`, …). |
| **Log4j config** | The `logging.client` file Mojang supplies; downloaded, verified and passed as `-Dlog4j.configurationFile`. |
| **Intermediary** | Fabric's mapping layer. For 26.1+ the game is unobfuscated, so 26.3 reports `0.0.0` and no remap is needed. |
| **Mod** | A jar that modifies the game, usually through a loader. Untrusted code in v1. |
| **Resource pack** | Assets that replace textures, models, sounds or language files. |
| **Shader pack** | A pack for a shader mod such as Iris. Renderer-dependent; not a v1 promise. |
| **World / save** | A user's game world under `saves/`. Never deleted by repair or update without explicit confirmation. |
| **`.mrpack`** | Modrinth's modpack format: a ZIP with `modrinth.index.json` and optional `overrides/`. |
| **Overrides** | Files in a modpack that are copied into the instance after the declared downloads. |
| **Piston** | Mojang's metadata and object host (`piston-meta.mojang.com`, `piston-data.mojang.com`). |
| **Maven coordinates** | `group:artifact:version[:classifier][@extension]`, for example `org.lwjgl:lwjgl:3.4.3:natives-linux`. |
| **Loader** | Fabric, Quilt, NeoForge or Forge. Adds a launch main class, libraries, arguments and often binary patches. |
| **Installer processor** | A Java program declared by a Forge/NeoForge installer that transforms jars or generates outputs. Runs in the `:installer` process. |
| **OptiFine** | A proprietary performance mod with an invasive installer. Unsupported; detected and explained, never silently stripped. |

---

## 3. JVM and runtime

| Term | Meaning |
|---|---|
| **JVM** | Java Virtual Machine. In this project, the HotSpot 25 VM created inside `:game` or `:installer`. |
| **JRE** | Java Runtime Environment: the modules, native libraries and launcher files needed to run Java. EclipseJRE is our Android JRE. |
| **JDK** | Java Development Kit: a JRE plus compilers and tools. Used in CI, not shipped. |
| **HotSpot** | OpenJDK's JVM implementation. Exported by `libjvm.so` as `JNI_CreateJavaVM@@SUNWprivate_1.1`. |
| **ART** | Android Runtime. Executes DEX bytecode and hosts the Android UI and the SDL Java bridge. It cannot load the desktop `client.jar` as the game's VM. |
| **Two-VM model** | One Linux process containing ART and HotSpot, with no Java object crossing between them. See [doc 05 §2](05-runtime-and-graphics.md). |
| **`JNI_CreateJavaVM`** | The JNI invocation API that creates a JVM in the current process. Called once per `:game`/`:installer` process. |
| **`JLI_Launch`** | The entry point used by the desktop `bin/java` launcher. It calls `exit()`, so this project does **not** use it. |
| **`DestroyJavaVM`** | The JNI call that waits for non-daemon threads and destroys the VM. Not called; the host flushes logs and calls `_exit(0)`. |
| **`JNI_EEXIST`** | The error returned if a second JVM is created in the same process. Treated as a bug and a process restart. |
| **`_exit`** | The POSIX immediate process exit used after the game returns, to avoid hanging on non-daemon threads. |
| **JNI** | Java Native Interface. The boundary between Java and native code. |
| **`JNIEnv`** | The per-thread JNI function table. ART's and HotSpot's are not interchangeable. |
| **JavaVM** | The process-wide VM handle. SDL caches ART's; HotSpot creates its own. |
| **jlink** | The JDK tool that builds a custom module image. EclipseJRE uses it to cut the 159 MB reference image to the modules Minecraft needs. |
| **Module image** | The `lib/modules` jimage produced by jlink. |
| **Classpath** | The ordered list of jars and directories the JVM searches for classes. |
| **Classpath Exception** | The OpenJDK licensing exception that allows linking GPL-2.0 Java code into a larger work without forcing the larger work under GPL. |
| **Heap** | The Java object memory, bounded by `-Xmx`. Distinct from native memory. |
| **ZGC / G1** | HotSpot garbage collectors. ZGC on 6 GB+ devices; G1 on 3–4 GB devices. |
| **`-XX:+AlwaysPreTouch`** | A JVM flag that commits and touches the whole heap at startup. Removed: measured 4.6–16 s startup on the test device. |
| **`hs_err_pid*.log`** | HotSpot's fatal error report. Written to the instance's `crash/` directory. |
| **Headless AWT** | `java.awt.headless=true`; no X11. v1 uses the real headless `java.desktop` from EclipseJRE, not Sash. |
| **JVM flag** | A `-X`/`-XX`/`-D` option passed at VM creation. Unrecognized flags fail closed (`ignoreUnrecognized=JNI_FALSE`). |

---

## 4. Graphics and native stack

| Term | Meaning |
|---|---|
| **OpenGL (GL)** | The desktop graphics API Minecraft's GL backend targets. Android does not provide desktop GL. |
| **OpenGL ES (GLES)** | The mobile subset of OpenGL. Android provides GLES 3.2 on capable devices. |
| **GL 3.3 core** | The desktop OpenGL profile Minecraft's `GL33C` backend expects. Sandpaper emulates it on GLES 3.2. |
| **GLES 3.2** | The minimum Android graphics level for the Sandpaper path. |
| **Sandpaper** | The GL 3.3-core-on-GLES-3.2 compatibility runtime. It intercepts `SDL_GL_*`, exposes the 126 entry points in the contract, and rewrites GLSL 330 to 320 es. |
| **Vulkan** | The low-level graphics API. Minecraft 26.2+ has a Vulkan backend; the launcher probes for API 1.2 plus the required extensions/features. |
| **Vulkan 1.2** | The API level 26.3 requires. A device without it, or without the five required extensions, uses Sandpaper. |
| **Extension** | An optional Vulkan capability, for example `VK_KHR_dynamic_rendering`. |
| **Feature** | A Vulkan 1.2 capability bit, for example `timelineSemaphore` or `shaderDrawParameters`. |
| **Swapchain** | The Vulkan presentation queue's set of images. Recreated on surface resize/loss. |
| **EGL** | The Android native window/graphics interface. Creates GLES contexts and ties them to an `ANativeWindow`. |
| **EGLSurface** | An EGL buffer surface bound to a native window. Recreated on rotation; the JVM is not recreated. |
| **`ANativeWindow`** | The native handle to an Android surface, obtained from a `Surface` via `ANativeWindow_fromSurface`. The only surface object that crosses the VM boundary. |
| **Surface** | Android's producer-side buffer queue. Owned by `SurfaceView`/`GameActivity`, consumed by EGL/Vulkan. |
| **`SurfaceView`** | The Android view that owns a dedicated surface. `EclipseSurfaceView` adds lifecycle, pointer capture and IME hooks. |
| **SDL** | Simple DirectMedia Layer, version 3.4.x. Provides window, input, audio and Vulkan/GL platform glue. |
| **EclipseSDL3** | The project's SDL build and Java bridge. |
| **SDL_GL_* / SDL_Vulkan_* ** | The SDL APIs Minecraft uses to create and manage graphics contexts. Sandpaper intercepts the GL family. |
| **GLSL** | The OpenGL shading language. Minecraft 26.3 emits `#version 330` for its 72 core shaders. |
| **GLSL 330 / 320 es** | Desktop GLSL 3.30 and GLSL ES 3.20. Sandpaper rewrites the former to the latter. |
| **SPIR-V** | The intermediate shader representation. Minecraft compiles GLSL to SPIR-V with Shaderc. |
| **Shaderc** | Google's GLSL→SPIR-V compiler. Official upstream, pinned. |
| **SPIRV-Cross** | Khronos's SPIR-V→GLSL translator. Re-emits GLSL 330 for the GL backend. |
| **VMA** | Vulkan Memory Allocator. Used by LWJGL/Minecraft for GPU allocations. |
| **LWJGL** | Lightweight Java Game Library, version 3.4.3. Provides Java bindings to GL, Vulkan, SDL, OpenAL, STB, Shaderc and SPIRV-Cross. |
| **Native library** | A `.so` file loaded by `dlopen`. Distinct from a Maven **library**. |
| **`dlopen`** | The POSIX call that loads a shared object. Android 10+ restricts `execve` from app data; `dlopen` of a real extracted file is the supported path. |
| **`extractNativeLibs`** | The Android manifest flag that makes the installer extract `.so` files to `nativeLibraryDir`. Set to `true` so HotSpot can `dlopen` them. |
| **`p_align`** | The ELF program-header alignment. Must be at least 16 KB (`0x4000`) for 16 KB-compatible devices. |
| **ABI** | Application binary interface. Shipped: `arm64-v8a`; emulator: `x86_64`. |
| **NDK** | Android Native Development Kit. Cross-compiles the native stack. |
| **16 KB page size** | An Android device page size that requires every shipped `.so` to be linked with 16 KB `p_align`. Play requires compatibility for updates from 2027-02-01. |

---

## 5. Android platform

| Term | Meaning |
|---|---|
| **Process** | A Linux process with its own address space. The app uses `:app`, `:game` and `:installer`. |
| **`:game`** | The process that hosts `GameActivity`, SDL, HotSpot and Minecraft. |
| **`:installer`** | The process that hosts `InstallerActivity`, HotSpot and loader processors. |
| **`:app`** | The main process: Compose UI, account vault, downloads, database. |
| **Activity** | An Android UI component. `GameActivity` is the visible game surface owner; `InstallerActivity` is the visible install progress owner. |
| **Service** | A background component. The app uses a download service; there is no game or installer foreground service in v1. |
| **FGS** | Foreground service. `dataSync` is capped at 6 h per 24 h; `shortService` at about 3 min. |
| **UIDT** | User-initiated data transfer job (API 34+). The preferred download path. |
| **LMK** | Low-memory killer. It can kill `:game` without a Java exception; the UI reads `ApplicationExitInfo`. |
| **ANR** | Application Not Responding: the main thread blocked for too long. |
| **`ApplicationExitInfo`** | API 30+ API that reports why a process died: crash, native crash, ANR, low memory, signal. |
| **Zygote** | The Android process template. Every app process, including `:game`, starts with ART already present. |
| **Binder** | Android's IPC mechanism. Used for intents and system services; not used for the game's token handover. |
| **Intent** | A message that starts a component. The launch request is an explicit Intent with no secrets. |
| **Parcelable** | Android's serialization interface for Binder/Intent payloads. Used for `LaunchRequest` but never for tokens. |
| **SAF** | Storage Access Framework. Used only at the import/export boundary; no broad storage permission. |
| **`getExternalFilesDir`** | App-specific external storage. Private to the app, visible over USB, no permission. |
| **Keystore** | Ambiguous: **Android Keystore** holds encryption keys; a **signing keystore** holds the release key. See §10. |
| **DataStore** | Android's async key-value/preferences store. Used for settings. |
| **Room** | SQLite ORM. Used as a rebuildable index, not the source of truth. |
| **Baseline profile** | Precompiled code shipped with the APK to improve startup and scroll performance. |
| **R8** | The shrinker/optimizer for release builds. |
| **Adaptive layout** | A layout that responds to window size class, not device type. Compact/medium/expanded. |
| **Edge-to-edge** | Drawing behind the system bars and consuming insets. Mandatory on Android 16. |
| **Predictive back** | The Android 14+ back gesture preview. |

---

## 6. Authentication and security

| Term | Meaning |
|---|---|
| **Microsoft account (MSA)** | The consumer identity used to sign in. Work/school accounts are not the target. |
| **Entra** | Microsoft's identity platform (formerly Azure AD). The launcher registers one public client application. |
| **Public client** | An OAuth client that cannot keep a secret; the client ID ships in the APK. |
| **Device authorization grant** | RFC 8628 flow: the app shows a code, the user signs in on any browser, the app polls for a token. |
| **Device code** | The short code the user types at `microsoft.com/devicelogin`. Single-use, ~15 minutes. |
| **`consumers` tenant** | The Microsoft tenant that issues tokens for personal accounts. Required for `XboxLive.signin`. |
| **Scope** | The permissions requested: `XboxLive.signin offline_access`. |
| **Access token** | A short-lived bearer token. Microsoft access ~1 h; Minecraft access ~24 h. |
| **Refresh token** | A long-lived token used to obtain new access tokens. Stored encrypted; rotated on use. |
| **XBL** | Xbox Live. The user-authentication step returns an XBL token and a user hash (`uhs`). |
| **XSTS** | Xbox Secure Token Service. Exchanges an XBL token for a relying-party token for Minecraft. |
| **Relying party** | The service an XSTS token is for. Must be `rp://api.minecraftservices.com/` for Java Edition. |
| **`XErr`** | Xbox error code in a 401 response, for example `2148916238` (child account not in a family). |
| **UHS** | User hash from Xbox Live. Stable per account; PII-adjacent. |
| **XUID** | Xbox user ID. Extracted from the XSTS token claims; passed to the game as `--xuid` if required. |
| **Minecraft Services** | `api.minecraftservices.com`: login, entitlements, profile. |
| **Entitlement** | Proof of ownership. `game_minecraft` / `product_minecraft` for Java Edition. |
| **Profile UUID** | The Minecraft profile's 32-hex identifier, distinct from the Microsoft object ID. |
| **AppID allowlist** | Mojang's manual approval of third-party Entra client IDs. Unapproved apps get HTTP 403 `Invalid app registration`. |
| **Launch ticket** | A one-shot, AES-GCM-encrypted file containing the Minecraft access token, UUID, name and XUID, with a 60-second TTL. |
| **Account vault** | The `:app` subsystem that stores the encrypted refresh token and metadata. |
| **Single-flight refresh** | One refresh per account at a time, so two coroutines cannot invalidate a rotating refresh token. |
| **Redaction** | Removing tokens, JWTs and device codes from logs and crash reports before they are written or exported. |
| **Threat model** | The documented attackers and mitigations. Notably: mods are not sandboxed in v1. |
| **Provenance** | The record of where code and artifacts came from, and under what license. See [`PROVENANCE.md`](../PROVENANCE.md). |
| **Clean-room** | A development process that forbids copying source. This project claims a **provenance-audited original implementation**, not a formal legal clean-room. |

---

## 7. Installation, storage and integrity

| Term | Meaning |
|---|---|
| **Component** | A signed, versioned runtime artifact managed by EclipseRuntime: EclipseJRE, EclipseNatives, EclipseSDL3. Distinct from an Android component. |
| **Component index** | A signed JSON list of components, versions, ABIs and manifest hashes. |
| **Component manifest** | `BUILD-MANIFEST.json`: artifacts, hashes, sizes, ABI, `minApi`, page-size, licenses, upstream source. |
| **Content-addressed store** | A directory layout where a file's path is derived from its digest (`<2>/<sha1>`), shared across instances. |
| **Staging** | A temporary tree where an install or update is materialised before commit. |
| **Commit** | The atomic `rename` of a verified staging tree into the live instance directory, after `fsync` and a journal write. Not a git commit. |
| **Journal** | A small JSON file recording a transaction's state across a crash. |
| **Transaction** | A persisted install/update/repair operation with idempotent stages. |
| **Plan / `LaunchPlan`** | The deterministic JSON describing classpath, arguments, paths, renderer and environment for one launch. |
| **Plan hash** | SHA-256 over the canonical plan JSON, excluding secrets and the hash field. |
| **FAST / FULL / LAUNCH verify** | Size-only check; full digest pass; launch-critical presence check. FULL is not on the launch path. |
| **Repair** | Re-downloading missing/corrupt managed files without touching user files or upgrading versions. |
| **Quarantine** | Moving a corrupt file to `.bad` and re-downloading once, instead of using it. |
| **GC** | Here, **garbage collection of files**: removing unreferenced store objects after a 7-day grace. Not the JVM garbage collector. |
| **Manifest (install)** | `manifest.json`: every managed file with its digest, size and origin. User files are not listed. |
| **`instance.json`** | The instance's portable identity: version, loader, pack, plan hash, creation time. |
| **Refcount** | The number of live manifests/transactions referencing a store object, used by GC. |
| **Preflight** | The free-space, ABI, component, renderer and account checks before a download. |
| **Resume** | Continuing a partial `.part` download with a `Range` request; not restarting from zero. |
| **`.part`** | The temporary file for an incomplete download. |
| **Overrides backup** | A timestamped copy of user files that a pack override would replace. |

---

## 8. Product and design

| Term | Meaning |
|---|---|
| **Home** | The destination with the hero instance and the primary **Play** action. |
| **Instances** | The destination listing all instances; detail is a first-class screen. |
| **Discover** | Modrinth content search and project detail. Not a store; no commerce. |
| **Downloads** | Active, queued and historical downloads with pause/resume/cancel. |
| **Settings** | Account, appearance, controls, game defaults, storage, diagnostics, about. |
| **Hero card** | The large card on Home for the active instance. |
| **Crash sheet** | The bottom sheet that explains a failed launch by stage, signal, library and next action. |
| **Safe mode** | A reversible launch profile that disables mods, forces a renderer, lowers settings, and records what it changed. |
| **Launch overlay** | The staged progress UI during launch; dismissible to a pill. |
| **In-game overlay** | The Android view above the game surface: pause, controls, keyboard, screenshot, performance, exit. |
| **Touch overlay** | The virtual controls (look area, movement, actions, hotbar). |
| **Pointer capture** | Android's relative-mouse mode, used for camera look and mouse mode. |
| **IME** | Input method editor (software keyboard). Chat text is forwarded to the game. |
| **Dynamic color** | Android 12+ system-derived color. Opt-in, off by default. |
| **M3 Expressive** | Material 3's expressive component/shape/motion update. |
| **Token (design)** | A named design value: color role, type style, spacing, shape, motion duration. Not an auth token. |
| **dp / sp** | Density-independent pixel / scale-independent pixel. Layout uses dp; text uses sp and must survive 200% font scale. |
| **Focus ring** | The visible 2 dp indicator for keyboard/D-pad focus. |
| **Reduced motion** | A setting that replaces movement with a ≤100 ms crossfade. |
| **TalkBack** | Android's screen reader. |
| **WCAG 2.2 AA** | The accessibility conformance target: contrast, target size, focus, dragging, consistent help, redundant entry. |
| **Five states** | Loading, empty, content, partial, error — every screen must design all five. |
| **JTBD** | Jobs To Be Done: the outcome a user is trying to achieve, independent of the feature. |
| **CTA** | Call to action; in this product, usually one primary button. |

---

## 9. Build, release and operations

| Term | Meaning |
|---|---|
| **AGP** | Android Gradle Plugin. Pinned at 9.4.0. |
| **Gradle** | The build system. The wrapper is checksum-verified. |
| **Version catalog** | `gradle/libs.versions.toml`, the single source of dependency versions. |
| **Convention plugin** | Shared build logic in `build-logic/`, so modules do not repeat compiler/test config. |
| **CI** | Continuous integration: GitHub Actions in this project. No local build is authoritative. |
| **SBOM** | Software Bill of Materials: the dependency inventory attached to a release. |
| **SHA-256** | The digest used for our artifacts, components and release APKs. |
| **SHA-1** | The digest Mojang publishes for game artifacts and asset indexes. Used only where the upstream contract requires it. |
| **SHA-512** | The digest Modrinth publishes for pack files. |
| **Signing key** | The private key that signs release APKs. Loss means a new package name. |
| **Certificate SHA-256** | The public fingerprint of the signing certificate, published for verification. |
| **Provenance attestation** | A CI statement of what was built, from which commit, with which inputs. |
| **Draft release** | A GitHub Release prepared by CI but not published until the checklist passes. |
| **versionCode / versionName** | Android's integer update counter and human-readable semver. |
| **Golden fixture** | A committed input/expected-output pair (for example a launch plan) used to detect unintended changes. |
| **Fail closed** | On a missing hash, bad signature, 4 KB `.so` or unsigned APK, stop rather than ship. |

---

## 10. Ambiguous terms

These words appear with more than one meaning. The dossier uses the distinction below.

| Word | Meanings | Convention |
|---|---|---|
| **Instance** | (a) a launcher installation with a version, loader and game dir; (b) an object in Room | Say **instance** for both; the DB row is an index of the directory |
| **Profile** | (a) Mojang version JSON / loader profile; (b) a Minecraft player profile (UUID/name); (c) a saved settings profile | Avoid in UI. Say **version**, **loader profile**, **player profile**, or **settings preset** |
| **Runtime** | (a) EclipseJRE, the Java runtime; (b) the Android runtime (ART); (c) a component subsystem | Say **EclipseJRE** or **Java runtime**; **ART** for Android; **EclipseRuntime** for the subsystem |
| **Library** | (a) a Maven Java artifact; (b) a native `.so`; (c) a user's content library | Say **Java library**, **native library**, or **content** |
| **Manifest** | (a) `AndroidManifest.xml`; (b) Mojang's version manifest; (c) the install `manifest.json`; (d) a component `BUILD-MANIFEST.json` | Always qualify: **Android manifest**, **version manifest**, **install manifest**, **component manifest** |
| **Component** | (a) an Eclipse runtime component (JRE/natives/SDL); (b) an Android app component (activity/service) | Qualify: **runtime component** vs **Android component** |
| **GC** | (a) JVM garbage collection; (b) file garbage collection of the content store | Say **JVM GC** or **store GC** |
| **Renderer** | (a) Minecraft's backend (Vulkan/GL); (b) Sandpaper, the GL compatibility layer; (c) the GPU driver | Say **game backend**, **Sandpaper**, or **driver** |
| **Token** | (a) an auth token; (b) a design token | Qualify: **access token**, **refresh token**, **design token** |
| **Keystore** | (a) Android Keystore (encryption keys); (b) the Java keystore holding the release signing key | Say **Android Keystore** or **release keystore** |
| **Plan** | (a) `LaunchPlan`; (b) a project plan/schedule | Say **launch plan** in code/architecture; **delivery plan** for the schedule |
| **Commit** | (a) git commit; (b) the atomic install commit | Say **git commit** or **install commit** |
| **Driver** | (a) the GPU driver; (b) a SDL video/audio driver | Say **GPU driver** or **SDL driver** |
| **Bridge** | (a) SDL's Java bridge; (b) the JNI host bridge | Say **SDL Java bridge** or **host bridge** |
| **Pack** | (a) modpack; (b) resource pack; (c) shader pack | Say **modpack**, **resource pack**, **shader pack** |
| **Vanilla** | (a) no loader; (b) unmodified game files | In UI, **Vanilla** means no loader; in install text, say **unmodified** |

---

## 11. Abbreviations

| Short | Expansion |
|---|---|
| ABI | Application Binary Interface |
| ADPF | Android Dynamic Performance Framework |
| AGP | Android Gradle Plugin |
| ANR | Application Not Responding |
| API | Application Programming Interface (also Android API level) |
| ART | Android Runtime |
| AWT | Abstract Window Toolkit |
| CDN | Content Delivery Network |
| CI | Continuous Integration |
| DEX | Dalvik Executable |
| EGL | Native platform graphics interface (formerly “Embedded Graphics Library”) |
| FGS | Foreground Service |
| FTL | FreeType License |
| GC | Garbage Collection (JVM or store; qualify) |
| GL | OpenGL |
| GLES | OpenGL ES |
| GLSL | OpenGL Shading Language |
| IME | Input Method Editor |
| IPC | Inter-Process Communication |
| JDK | Java Development Kit |
| JNI | Java Native Interface |
| JRE | Java Runtime Environment |
| JVM | Java Virtual Machine |
| LMK | Low-Memory Killer |
| M3 | Material 3 |
| MSA | Microsoft Account |
| NDK | Native Development Kit |
| OAuth | Open Authorization |
| PII | Personally Identifiable Information |
| SAF | Storage Access Framework |
| SBOM | Software Bill of Materials |
| SDL | Simple DirectMedia Layer |
| SPIR-V | Standard Portable Intermediate Representation (Vulkan) |
| UIDT | User-Initiated Data Transfer |
| UUID | Universally Unique Identifier |
| VMA | Vulkan Memory Allocator |
| WCAG | Web Content Accessibility Guidelines |
| XBL | Xbox Live |
| XSTS | Xbox Secure Token Service |
| XUID | Xbox User ID |
| ZGC | Z Garbage Collector |

---

## 12. Names that are not final

| Name | Risk | Status |
|---|---|---|
| **Eclipse Launcher** | “Eclipse” is a registered trademark of the Eclipse Foundation; a formal search is a Phase 0 task | Working name; rebrand possible before 1.0 |
| **Sandpaper** | No known conflict; descriptive of smoothing rough edges | Working name |
| **Sash** | No known conflict; a window-frame metaphor | Deferred feature |
| **Foreman** | Common word; no known software conflict in this domain | Working name |
| **Crowbar** | Common word; used by other tools | Working name |
| **EclipseJRE / EclipseNatives / EclipseSDL3 / EclipseRuntime** | Inherit the “Eclipse” risk | Working names; rename with the product if needed |

A rename is a documentation and package-name change, not an architectural one. The package `dev.shadow.eclipselauncher` would change with the product name, which is why the final name should be settled before the first public release.

---

## 13. Open glossary questions

- Do we need a translated glossary for community localization, or only the English source terms?
- Should “instance” be the user-facing word in every locale, or is “installation” clearer in some languages?
- Is “runtime” too technical for the Settings screen, where “Java” may be more recognisable?
- Do we expose “Sandpaper” by name in the UI, or only “Compatibility mode” with the name in details?
- Should the crash sheet say “signal” or “crash type” for non-technical users?
- Which terms belong in the in-app help versus this dossier?
