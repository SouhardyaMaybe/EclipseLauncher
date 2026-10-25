# 05 — Runtime and graphics

**Status:** architecture draft for sign-off, 2026-10-24.
**Scope:** how Eclipse Launcher turns an installed instance into a running Minecraft 26.3 process: the JRE, the JVM, the native stack, SDL3, the renderer path, audio, input and failure handling.
**Evidence:** Mojang's 26.3 version JSON, bytecode and resources from `client-26.3.jar`, the exact native pins recorded inside LWJGL 3.4.3's native jars, Android linker documentation, and JVM start-up measurements on the API 36 arm64 test device.
**Labels:** **Verified** (primary source inspected), **Observed** (artifact or device measurement), **Inference** (reasoned, needs a test), **Open** (unknown).

The machine-readable contracts extracted from the client are in [`contracts/26.3-gl-functions.txt`](contracts/26.3-gl-functions.txt) and [`contracts/26.3-vulkan.json`](contracts/26.3-vulkan.json).

---

## 1. The contract in one page

| Layer | Decision | Why |
|---|---|---|
| Process | `GameActivity` and the game run in `dev.shadow.eclipselauncher:game`; the launcher UI stays in the main process | A JVM or driver crash kills only the game; the UI can still show the crash report |
| Virtual machines | **ART** hosts the Android UI and SDL's Java bridge; **HotSpot 25** hosts Minecraft | SDL's bridge classes are DEX, so they cannot be loaded by HotSpot; the game needs a real OpenJDK 25 |
| Object boundary | No Java object crosses between ART and HotSpot; only native handles (`ANativeWindow*`, file descriptors, ints) cross | Two JVMs have incompatible `JNIEnv` and object models |
| JRE | EclipseJRE 25, jlink image, extracted read-only to app storage | 60–75 MB instead of 159 MB; one runtime per device, not per instance |
| JVM creation | `JNI_CreateJavaVM` in-process, once per `:game` process | Android 10+ blocks `execve` from app data; a JVM cannot be created twice in one process |
| Natives | Bundled in the APK, `extractNativeLibs=true`, loaded by absolute path from `ApplicationInfo.nativeLibraryDir` | HotSpot's `dlopen` cannot read a `.so` from inside an APK; extraction gives a real, read-only path |
| SDL | EclipseSDL3 3.4.16 with the `dev.shadow.eclipselauncher.sdl` Java bridge | SDL 3.4.14 is what LWJGL 3.4.3 was built against; 3.4.16 is ABI-compatible |
| Vulkan | Minecraft's own Vulkan backend, only when the device meets every requirement | Native, no translation, best performance; 26.3 already has the backend |
| OpenGL | Desktop GL 3.3 core emulated on GLES 3.2 by **Sandpaper** | Android has no desktop GL; the game's GL backend is `GL33C` behind `SDL_GL_*` |
| Shaders | Minecraft compiles GLSL → SPIR-V with Shaderc, then SPIRV-Cross re-emits GLSL 330 for the GL backend; Sandpaper rewrites 330 → 320 es | No GLSL compiler is written from scratch |
| Audio | Official OpenAL Soft, OpenSL backend for v1; AAudio through SDL3 later | No Oboe fork, no cross-VM audio callbacks |
| Input | SDL3 Android events, pointer capture, IME bridge, gamepad bridge | Desktop Minecraft expects mouse, keyboard and controller |
| Failure | Structured diagnosis, never a silent fallback | “If it breaks, we tell you why” |

---

## 2. Two virtual machines in one process

### 2.1 Why there are two

Every Android app process, including `:game`, is created by Zygote and already contains ART. Loading `libjvm.so` and calling `JNI_CreateJavaVM` adds a second VM to that process. This is unusual but not new: it is how a desktop JVM runs inside an Android app.

The two VMs have different jobs:

```
dev.shadow.eclipselauncher:game  (one Linux process, one UID)
├── ART
│   ├── GameActivity, EclipseSurfaceView, Compose overlay
│   ├── EclipseSDL / EclipseInputConnection / EclipseControllerManager
│   └── SDL's cached JavaVM and JNIEnv
└── HotSpot 25
    ├── Minecraft client, Fabric loader, mods
    ├── LWJGL 3.4.3 bindings
    └── Sandpaper via SDL_GL_GetProcAddress
```

**Verified:** `SDL_android.c` caches the VM and calls Java through `EclipseSDL`'s `jclass`; the class is loaded from the APK's DEX, which only ART can resolve. `JNI_CreateJavaVM` is exported by `libjvm.so` as `JNI_CreateJavaVM@@SUNWprivate_1.1`.

### 2.2 The rules that make it safe

1. **The SDL bridge is initialised on ART before the JVM exists.** The app calls `EclipseSDL.attach(activity)`, creates the `EclipseSurfaceView`, and calls `EclipseSDL.nativeSetupJNI()` from the Android main thread. That call captures ART's `JavaVM` and registers the native method table against ART's `EclipseSDL` class. If it were called after `JNI_CreateJavaVM` from a HotSpot thread, SDL would cache the wrong VM.
2. **No Java reference crosses.** The surface is converted to an `ANativeWindow*` by `ANativeWindow_fromSurface` on ART. SDL and the game only see the native pointer. Clipboard, IME and gamepad callbacks are Java calls made by SDL on ART; their arguments are primitives or strings created by ART.
3. **The JVM is created once.** `JNI_CreateJavaVM` returns `JNI_EEXIST` on a second call. A relaunch means a new `:game` process.
4. **`DestroyJavaVM` is not called.** Minecraft leaves non-daemon threads behind; `DestroyJavaVM` can block forever. When `Main.main` returns, the launcher flushes logs and calls `_exit(0)`. `System.exit()` inside the game terminates `:game`, not the launcher.
5. **Signal handlers are chained, not replaced blindly.** HotSpot installs handlers for `SIGSEGV`, `SIGBUS`, `SIGILL` and `SIGFPE` and saves the previous (ART) handlers. A fault outside JVM code should chain to ART. This is standard HotSpot behaviour but must be tested on API 36 and 37 because ART's implicit null checks also use `SIGSEGV`.

**Open:** the exact chaining behaviour on Android 17, where ART and HotSpot both use `SIGSEGV` for different purposes. Phase 0 test: run a small Swing-free AWT call, a Fabric mixin, and a forced null dereference in ART code while HotSpot is alive; confirm both report correctly and neither loops.

### 2.3 What runs where

| Work | VM | Thread |
|---|---|---|
| Compose UI, instance list, downloads | ART | Android main / IO |
| `GameActivity`, `SurfaceView`, lifecycle | ART | Android main |
| SDL Java bridge, clipboard, IME, gamepad | ART | SDL callbacks / Android main |
| `SDL_Init`, `SDL_CreateWindow`, `SDL_GL_*`, `SDL_Vulkan_*` | HotSpot → native | game thread |
| Minecraft, Fabric, mods, LWJGL | HotSpot | game thread + JVM threads |
| OpenAL Soft, OpenSL | native | audio thread |
| Sandpaper GL shim | native | game thread |

---

## 3. EclipseJRE

### 3.1 What is built

EclipseJRE is an OpenJDK 25 image cross-compiled for Android with the NDK. It is a separate component with its own release pipeline and its own GPL-2.0 + Classpath Exception notices.

| Item | Value |
|---|---|
| Upstream | `openjdk/jdk25u`, tag `jdk-25.0.4+4` |
| Target | Android API 26+, arm64-v8a (x86_64 for the emulator) |
| NDK | r28c or newer |
| Patches | Android platform patches, re-derived and attributed before release |
| Image | `jlink` with the module profile below |
| Link flags | `-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384` |
| Runtime path | `-Wl,-rpath,'${ORIGIN}:${ORIGIN}/..'` |

**Observed in the current Amethyst OpenJDK 25 Android image (the closest working reference):**

- total image **159 MB**; `lib/modules` is **126,800,736 bytes**;
- `lib/server/libjvm.so` is **19,387,112 bytes** and exports `JNI_CreateJavaVM@@SUNWprivate_1.1`;
- `libjvm.so` needs `libc++_shared.so`, `libm.so`, `libdl.so`, `libc.so`;
- `libjava.so`, `libnet.so`, `libnio.so`, `libawt.so`, `libfontmanager.so`, `libfreetype.so` all need `libc++_shared.so`;
- **every `PT_LOAD` segment is `p_align 0x1000` (4 KB)** on `libjvm.so`, `libjava.so`, `libjli.so` and `libawt.so`; the image is **not 16 KB compliant**;
- there is no `DT_RUNPATH`; `libjvm.so` and friends must be found by `java.library.path` or a new rpath.

The JRE that ships must be rebuilt, not reused. `libc++_shared.so` must be present in the image (or in the APK) because Android does not provide it.

### 3.2 jlink profile

The full image carries modules Minecraft does not use. The v1 profile is:

```
java.base
java.desktop
java.logging
java.management
java.naming
java.net.http
java.xml
jdk.crypto.ec
jdk.localedata
jdk.net
jdk.security.auth
jdk.unsupported
jdk.zipfs
```

Rationale:

- `java.desktop` is required by Minecraft and by many mods for `BufferedImage`, `ImageIO`, `Color`, `Dimension` and `Rectangle`. The EclipseJRE build is **headless**: `libawt_xawt.so` is a stub and the X11 toolkit is not linked. `java.awt.headless=true` is mandatory.
- `java.management` and `java.naming` are used by Netty, Log4j and OSHI; excluding them produces `NoClassDefFoundError` in code paths that are hard to predict.
- `jdk.crypto.ec` is required for TLS ECDHE; without it Microsoft and Mojang HTTPS fail.
- `jdk.localedata` keeps date/number formatting correct for non-English devices.
- `jdk.unsupported` provides `sun.misc.Unsafe`, which several libraries still use.
- `jdk.zipfs` is needed by loaders and mod scanners that open jars as file systems.

**Inference:** this profile is ~60–75 MB uncompressed after `jlink --strip-debug --no-header-files --no-man-pages --compress=zip-6`. The exact size is measured in CI and recorded in the component manifest. If a mod needs a missing module, the launcher reports the module name and offers the full image as a repair download.

**Open:** whether `java.instrument` is needed by Fabric or NeoForge. Fabric's Mixin does not require JVMTI instrumentation; NeoForge is v1.1. Add it only if a loader test proves it necessary.

### 3.3 On-device layout and loading

```
files/runtime/jre-25.0.4-android-arm64/
  release
  conf/
  lib/
    modules
    security/cacerts
    tzdb.dat
    server/libjvm.so
    libjava.so  libnet.so  libnio.so  libzip.so  libjli.so
    libawt.so  libawt_headless.so  libfontmanager.so  libfreetype.so
    libc++_shared.so
  legal/
```

- The archive is verified by SHA-256, extracted into a staging directory, `chmod 0444` is applied to every `.so`, then the directory is atomically renamed into place.
- `java.home` points at this directory.
- `java.library.path` is `<jre>/lib:<jre>/lib/server:<nativeLibraryDir>`.
- `DT_RUNPATH` is `${ORIGIN}:${ORIGIN}/..` so `lib/server/libjvm.so` finds `../libjava.so` and `lib/libjava.so` finds `libjvm.so` without relying on the process environment.

**Verified:** Android's linker supports `DT_RUNPATH` since API 24 and expands `${ORIGIN}` to the directory containing the ELF file; `${LIB}` and `${PLATFORM}` are not implemented ([Android linker changes](https://android.googlesource.com/platform/bionic/+/master/android-changes-for-ndk-developers.md)).

**Open:** whether Android 17's read-only native-DCL rule applies to `dlopen` of `libjvm.so` as well as `System.load()`. The implementation treats both as covered: every `.so` is `0444` before it is mapped.

### 3.4 Why not bundle the JRE in the APK

- A universal APK with the JRE is ~200 MB; GitHub Releases is fine with that, but every launcher update would re-download the JRE.
- The JRE changes on its own cadence (security updates, 16 KB rebuilds). A component release lets it update independently.
- The APK stays under ~60 MB, which matters for direct APK distribution and for users on metered connections.

The cost is a first-run download. The launcher shows the size, resumes it, verifies it, and can fall back to a smaller “vanilla-only” runtime profile if the user is low on storage.

---

## 4. Creating the JVM

### 4.1 Sequence

```
GameActivity.onCreate (ART, already in :game)
  ├─ System.load(nativeLibraryDir + "/libSDL3.so")
  ├─ System.load(nativeLibraryDir + "/libeclipse_host.so")
  ├─ EclipseSDL.attach(this)
  ├─ setContentView(EclipseSurfaceView + Compose overlay)
  └─ EclipseSurfaceView.surfaceCreated:
             ├─ EclipseSDL.nativeSetupJNI()          // captures ART's JavaVM
             ├─ native pw_host_start(config)          // new native thread
             │    ├─ chdir(instance/game)
             │    ├─ dup2(stdout/stderr -> logs/)
             │    ├─ dlopen(libjvm.so, RTLD_NOW|RTLD_GLOBAL)
             │    ├─ JNI_CreateJavaVM(...)
             │    ├─ FindClass(mainClass)
             │    ├─ CallStaticVoidMethod(main, args)
             │    └─ fflush; _exit(0)
             └─ UI observes process death / result
```

`GameActivity` declares `android:process=":game"` and `android:configChanges` for orientation, screen size, keyboard, smallest screen size, screen layout and UI mode. Rotation recreates the EGL surface, not the JVM.

### 4.2 Arguments

The 26.3 JSON contributes these JVM arguments (macOS/Windows-only entries removed):

```
-XX:StackShadowPages=32
--enable-native-access=ALL-UNNAMED
--add-exports java.base/jdk.internal.misc=ALL-UNNAMED
-Djava.library.path=${natives_directory}/java
-Djna.tmpdir=${natives_directory}/jna
-Dorg.lwjgl.system.SharedLibraryExtractPath=${natives_directory}/lwjgl
-Dio.netty.native.workdir=${natives_directory}/netty
-Dminecraft.launcher.brand=${launcher_name}
-Dminecraft.launcher.version=${launcher_version}
-cp ${classpath}
```

Eclipse replaces the native paths with its own and adds:

| Argument | Value | Reason |
|---|---|---|
| `-Xms` | 384–512 MB | Small initial commit; `AlwaysPreTouch` is removed |
| `-Xmx` | 1.5–4 GB, computed from device RAM | The JVM sees total device RAM, not the app's cgroup limit |
| `-XX:+UseZGC` | on 6 GB+ devices | Measured 1.1–1.2 s start with `-Xms512M -Xmx2G` |
| `-XX:+UseG1GC` | on 3–4 GB devices | Lower virtual reservation and better behaviour at small heaps |
| `-XX:+ExitOnOutOfMemoryError` | on | Produces a clean exit code and a report instead of a wedged process |
| `-XX:ErrorFile` | `<instance>/crash/hs_err_pid%p.log` | Fatal JVM reports are readable by the launcher |
| `-XX:-UsePerfData` | on | `/tmp/hsperfdata_*` is not reliably writable on Android |
| `-Djava.awt.headless=true` | on | No X11; the AWT build is headless |
| `-Djava.io.tmpdir` | `<instance>/cache/tmp` | Writable and cleanable |
| `-Duser.home` | `<instance>/game` | Minecraft writes relative to the game directory |
| `-Duser.dir` | `<instance>/game` | Same, for libraries that read it directly |
| `-Djava.net.preferIPv4Stack=false` | on | Android networks are dual-stack |
| `-Dfile.encoding=UTF-8` | on | Deterministic logs and resource names |
| `-Dorg.lwjgl.librarypath` | `ApplicationInfo.nativeLibraryDir` | LWJGL loads by absolute path |
| `-Dorg.lwjgl.system.SharedLibraryExtractPath` | `<instance>/cache/lwjgl` | Minecraft's bootstrap writes here; keep it inside the instance |
| `-Dorg.lwjgl.system.EmulateSystemLoadLibrary` | `false` | `dlopen` by path is the reliable path on Android |

`-Djava.library.path` must include both the JRE's `lib` and `lib/server` and the APK's native directory. `-Djna.tmpdir` and `-Dio.netty.native.workdir` point into the instance cache; JNA and Netty may not find Android natives, but the directories must exist so their initialisers do not fail on a missing path.

### 4.3 Heap sizing

| Device RAM | `-Xmx` | `-Xms` | GC |
|---|---|---|---|
| 3 GB | 1.25 GB | 384 MB | G1 |
| 4 GB | 1.5 GB | 384 MB | G1 |
| 6 GB | 2 GB | 512 MB | ZGC |
| 8 GB | 3 GB | 512 MB | ZGC |
| 12 GB+ | 4 GB | 512 MB | ZGC |

Rules:

- Cap at **35% of `MemoryInfo.totalMem`**, and never above 4 GB for v1. A 4 GB phone that reports 3.6 GB usable must not be given a 4 GB heap.
- Reserve headroom for the native stack: SDL, LWJGL, Shaderc, SPIRV-Cross, OpenAL and the GPU driver can use 300–600 MB of native memory outside the Java heap.
- `-XX:+AlwaysPreTouch` is **removed**. **Observed:** `-Xms2G -Xmx4G -XX:+UseZGC -XX:+AlwaysPreTouch` took 4.6–16 s to start on the test device; the same JVM without pre-touch and with a 512 MB initial heap took 1.1–1.2 s.
- `-XX:+UseStringDeduplication` is **removed** when ZGC is active; it is a G1 feature.
- `-XX:+UseCompactObjectHeaders` is kept only if the EclipseJRE build accepts it. **Open:** verify on the 25.0.4 build; it is a Java 25 feature, not a JVM-wide guarantee.
- `-XX:ActiveProcessorCount` is an experiment, not a default. Pinning the JVM to the big-core count can reduce GC contention, but it can also starve the render thread on devices with a 1+3+4 topology.

### 4.4 Failure handling

`JNI_CreateJavaVM` returns a `jint`:

| Code | Meaning | Launcher response |
|---|---|---|
| `JNI_EEXIST` | A JVM already exists in this process | Bug: the process was reused; kill `:game` and retry once |
| `JNI_EVERSION` | Unsupported JNI version | Wrong `libjvm.so`; repair the JRE component |
| `JNI_ENOMEM` | Not enough memory for the VM | Lower `-Xmx`, show the memory advice screen |
| `JNI_EINVAL` | Invalid option | Show the exact option; never silently drop it |

The launcher sets `ignoreUnrecognized = JNI_FALSE`, so a typo in a JVM flag fails at creation rather than being ignored. Every failure writes a JSON report with the code, the full option list, the device memory, the JRE manifest and the last 200 lines of native log.

---

## 5. The native stack

### 5.1 Exact libraries and upstream pins

LWJGL 3.4.3's native jars record the upstream commit used to build each library. Those commits are the compatibility contract: the Java bindings and the Android `.so` must be built from the same source revision.

| Library | Upstream | Pin | Linux x64 size | Purpose |
|---|---|---|---|---|
| `libSDL3.so` | `libsdl-org/SDL` | 3.4.16 (LWJGL built 3.4.14, commit `147a8ee`) | 4.1 MB | Window, EGL, Vulkan surface, input, IME |
| `liblwjgl.so` | `LWJGL/lwjgl3` | 3.4.3 | 137 KB | Core JNI, memory, library loading |
| `liblwjgl_opengl.so` | `LWJGL/lwjgl3` | 3.4.3 | 84 KB | GL JNI support |
| `liblwjgl_stb.so` | `nothings/stb` via LWJGL | 3.4.3 | 280 KB | Image/font decode |
| `liblwjgl_vma.so` | `GPUOpen-LibrariesAndSDKs/VulkanMemoryAllocator` via LWJGL | 3.4.3 | 64 KB | Vulkan allocator JNI |
| `libopenal.so` | `kcat/openal-soft` | `7f9c6de` (2026-07-09) | 1.2 MB | 3D audio |
| `libfreetype.so` | `freetype/freetype` | 2.14.3 (`0a0221a`) | 1.4 MB | Font rasterisation |
| `libshaderc.so` | `google/shaderc` | v2026.3 (`2c8cae7`) | 3.7 MB | GLSL → SPIR-V |
| `libspirv-cross.so` | `KhronosGroup/SPIRV-Cross` | `6c09849` (2026-07-06) | 1.3 MB | SPIR-V reflection and GLSL emission |
| `libc++_shared.so` | Android NDK | r28c | ~1 MB | C++ runtime for the JRE and natives |
| `libsandpaper.so` | Eclipse Launcher | this repo | ~300 KB target | GL 3.3 → GLES 3.2 runtime |
| `libeclipsehost.so` | Eclipse Launcher | this repo | ~100 KB target | `pw_` JNI bridge: JVM, host, renderer probe |

**Verified:** the `.git` files inside `lwjgl-*-3.4.3-natives-linux.jar` contain the pins above; `libSDL3.so` reports `SDL-3.4.14`; `libshaderc.so` reports Shaderc v2026.3; `libfreetype.so` is 2.14.3.

The Android build must not take the Linux jars. EclipseNatives builds each library from the pinned upstream commit with NDK r28c, applies only the Android platform changes that are ours, and publishes arm64-v8a (plus x86_64 for the emulator) with a `BUILD-MANIFEST.json`.

### 5.2 Why these and not forks

- **SDL**: official upstream, zlib. EclipseSDL3 adds the embeddable Java bridge and the host handover; those changes are ours and are published.
- **OpenAL Soft**: official upstream, LGPL-2.0-or-later. The old FCL fork is removed. v1 builds the official **OpenSL** backend; SDL3/AAudio is a later option.
- **FreeType, Shaderc, SPIRV-Cross, VMA, STB**: official upstreams at the exact revisions LWJGL 3.4.3 was built against.
- **LWJGL**: official upstream. The Android platform target is a build configuration, not a source fork.
- **No Turnip, no Zink, no driver namespace bypass**: v1 uses the system GLES/Vulkan drivers. Driver replacement is a support and provenance problem that does not belong in the first release.

### 5.3 Load order

Minecraft 26.3 deliberately **shuffles** `[OpenGL, OpenAL, freetype, shaderc, spvc]` at startup (`NativeLibrariesBootstrap`). On desktop this surfaces missing libraries early; on Android it makes failures nondeterministic. The launcher preloads the stack in a fixed order before `Main.main`:

```
libc++_shared.so        (linker dependency, loaded with SDL)
libSDL3.so              (ART, before the JVM)
liblwjgl.so
liblwjgl_opengl.so
libopenal.so
libfreetype.so
libshaderc.so
libspirv-cross.so
liblwjgl_stb.so
liblwjgl_vma.so
libsandpaper.so         (lazy, on first SDL_GL_LoadLibrary)
libjvm.so               (JRE, app storage)
```

**Inference:** preloading makes Minecraft's shuffle a no-op because `Library.loadNative` finds the library already mapped. It also gives the launcher a single place to report which library failed and why.

### 5.4 `extractNativeLibs=true`

v1 sets `android:extractNativeLibs="true"` and `packaging.jniLibs.useLegacyPackaging = true`.

Why:

- HotSpot's `System.loadLibrary`/`dlopen` resolves `java.library.path`, which must point at real files. It cannot open a `.so` stored inside the APK.
- With extraction, ART loads `libSDL3.so` from a read-only system-managed directory; HotSpot `dlopen`s the same path and gets the same mapping.
- The 16 KB **ELF** requirement still applies. The 16 KB **zip alignment** requirement does not, because the `.so` entries are compressed and extracted by the installer.

The cost is install size: the APK plus the extracted copy. For a ~15–20 MB native set this is acceptable. The release notes state the installed size.

**Open:** whether AGP 9.4 still honours `useLegacyPackaging` for all splits; verify in CI by inspecting the built manifest and the APK's `lib/` entries.

---

## 6. SDL3 and the Android bridge

### 6.1 What EclipseSDL3 is

EclipseSDL3 is SDL 3.4.16 with original changes:

- an embeddable Java bridge (`dev.shadow.eclipselauncher.sdl.*`) instead of the sample `SDLActivity`;
- a surface handover that lets a host `Activity` own the `SurfaceView`;
- an optional `pw_host` handover for display parameters and CPU affinity;
- the standard SDL Android video, audio, input and Vulkan backends.

It is not a renderer and it does not replace the system GPU driver.

### 6.2 Java bridge classes

| Class | Responsibility |
|---|---|
| `EclipseSDL` | Activity attach/detach, surface publication, lifecycle, clipboard, permissions, file dialogs, key/touch/mouse forwarding |
| `EclipseSurfaceView` | `SurfaceView` + `SurfaceHolder.Callback`, pointer capture, IME visibility, frame-rate hint |
| `EclipseInputConnection` | Committed text and Unicode scancodes from the IME |
| `EclipseAudioManager` | Audio device enumeration (not the game's audio path) |
| `EclipseControllerManager` | Gamepad enumeration, axis/hat/button events, rumble |
| `EclipseHIDDeviceManager` | USB/BLE HID devices (controller support, optional) |

The bridge is ~4,200 lines of original Java. It is zlib-licensed like SDL and carries the SDL notice plus the Eclipse copyright. The package is renamed from `me.shadow` to `dev.shadow` as part of the component cleanup.

### 6.3 Surface handover

```
EclipseSurfaceView.surfaceCreated(surface)
  → EclipseSDL.setNativeSurface(surface)
  → EclipseSDL.onNativeSurfaceCreated()
  → SDL_android.c: Android_JNI_GetNativeWindow()
       → EclipseSDL.getNativeSurface()
       → ANativeWindow_fromSurface(env, surface)
```

Ordering is strict:

1. publish the `Surface` **before** `onNativeSurfaceCreated()`;
2. clear it **after** `onNativeSurfaceDestroyed()`;
3. never create an EGL surface against a `Surface` that has been destroyed.

`GameActivity` handles `surfaceDestroyed` by pausing the game and waiting for a new surface; it does not destroy the JVM.

### 6.4 GL handover

Minecraft's GL backend calls `SDL_GL_LoadLibrary`, `SDL_GL_SetAttribute`, `SDL_GL_CreateContext`, `SDL_GL_GetProcAddress`, `SDL_GL_MakeCurrent`, `SDL_GL_SwapWindow`, `SDL_GL_DestroyContext` and `SDL_GL_UnloadLibrary`. `GlBackend.getFunctionProvider()` returns a provider backed by `SDL_GL_GetProcAddress`.

EclipseSDL3 implements the Android GL driver in one of two modes:

| Mode | `SDL_GL_LoadLibrary` | `SDL_GL_GetProcAddress` | Context |
|---|---|---|---|
| `system` | `dlopen("libGLESv2.so")` | `eglGetProcAddress` | EGL/GLES 3.2 |
| `sandpaper` | `dlopen("libsandpaper.so")` | `pw_sp_get_proc_address` | EGL/GLES 3.2 created by Sandpaper |

The app selects `sandpaper` when the device lacks a compliant Vulkan 1.2 driver, and `system` only for diagnostics. Normal gameplay never uses `system` directly: the game asks for GL 3.3 core, which Android's GLES driver cannot provide.

### 6.5 Vulkan handover

- `SDL_Vulkan_LoadLibrary(NULL)` → `dlopen("libvulkan.so")`, then `vkGetInstanceProcAddr`.
- `SDL_Vulkan_GetVkGetInstanceProcAddr()` → the loader's `vkGetInstanceProcAddr`.
- `SDL_Vulkan_GetInstanceExtensions()` → `VK_KHR_surface`, `VK_KHR_android_surface`.
- `SDL_Vulkan_CreateSurface(window)` → `vkCreateAndroidSurfaceKHR` with the `ANativeWindow*`.
- `SDL_Vulkan_UnloadLibrary()`.

Minecraft's `VulkanBackend` calls `SDL_Vulkan_LoadLibrary` and checks that the returned `vkGetInstanceProcAddr` matches LWJGL's; a mismatch is reported as `VULKAN_LOADER_MISSING`.

**Verified:** the strings in `VulkanBackend.class` include `SDL_Vulkan_LoadLibrary`, `SDL_Vulkan_GetVkGetInstanceProcAddr`, `vkGetInstanceProcAddr mismatch`, `SDL_Vulkan_UnloadLibrary`.

### 6.6 Lifecycle hints

- `SDL_HINT_ANDROID_BLOCK_ON_PAUSE=0`: the game pauses through its own pause menu, not by blocking SDL's event thread.
- `SDL_HINT_ANDROID_TRAP_BACK_BUTTON=0`: Back is translated to Escape by the host, not consumed by SDL.
- `SDL_HINT_ORIENTATIONS`: set from the activity's requested orientation.
- `SDL_HINT_IME_SHOW_UI=1`: the host controls the IME window.

The bridge must survive `onPause`/`onResume`, `onConfigurationChanged`, `onTrimMemory` and `onLowMemory` without destroying the surface or the JVM.

---

## 7. Backend selection

### 7.1 Minecraft already has a selector

26.3 stores `preferredGraphicsBackend` in `options.txt` with values `default`, `opengl` and `vulkan` (`net.minecraft.client.PreferredGraphicsApi`). `default` tries Vulkan and falls back to OpenGL; the failure reasons are `VULKAN_LOADER_MISSING`, `VULKAN_DEVICE_VERSION_TOO_LOW`, `VULKAN_MISSING_EXTENSION`, `VULKAN_MISSING_FEATURE`, `VULKAN_NO_GRAPHICS_QUEUE`, `OPENGL_MISSING` and others.

The launcher therefore does not need to inject a backend into the game. It writes the option before launch:

| Probe result | `preferredGraphicsBackend` | Notes |
|---|---|---|
| All Vulkan requirements met | `vulkan` | Best path; no translation |
| Vulkan present but incomplete | `opengl` | Sandpaper path |
| GLES < 3.2 or Sandpaper init failed | `opengl` + warning | The game will report `OPENGL_MISSING`; the launcher shows a compatibility screen |
| User override | user value | Developer settings only |

### 7.2 Vulkan requirements

**Verified** from `VulkanInstance`, `VulkanPhysicalDevice`, `VulkanDevice` and `VulkanFeatureSets` in `client-26.3.jar`:

- instance API **1.2**;
- device API **1.2**;
- extensions: `VK_KHR_swapchain`, `VK_KHR_dynamic_rendering`, `VK_KHR_push_descriptor`, `VK_KHR_synchronization2`, `VK_EXT_vertex_attribute_divisor`;
- features: `multiDrawIndirect`, `drawIndirectFirstInstance`, `samplerAnisotropy`, `shaderDrawParameters`, `timelineSemaphore`, `hostQueryReset`, `synchronization2`, `dynamicRendering`, `vertexAttributeInstanceRateDivisor`;
- a graphics queue that can present to the Android surface.

`VK_KHR_dynamic_rendering` and `VK_KHR_synchronization2` are core in Vulkan 1.3 but must be enabled as extensions on a 1.2 device. `VK_KHR_push_descriptor` is optional in Vulkan but mandatory for 26.3. `VK_EXT_vertex_attribute_divisor` is mandatory even though `vertexAttributeDivisor` is core in 1.1, because the engine asks for the EXT feature struct.

The probe is a small native function, `pw_sp_probe_vulkan()`, in `libsandpaper.so`. It:

1. `dlopen("libvulkan.so")` and resolves `vkGetInstanceProcAddr`;
2. checks `vkEnumerateInstanceVersion` (if absent, Vulkan is 1.0 and fails);
3. creates a minimal instance with `VK_KHR_surface` + `VK_KHR_android_surface` and no layers;
4. enumerates physical devices, checks `apiVersion`, extensions and features;
5. destroys the instance and returns a JSON capability report.

It does not create a surface or a device. The full create happens inside Minecraft, so a driver bug at device creation is reported by the game, not by the probe.

**Open:** some mobile drivers report an extension but fail at `vkCreateDevice`. The probe is necessary, not sufficient. The first-run matrix records both.

### 7.3 GL requirements

Minecraft's GL backend is **desktop GL 3.3 core** (`GL33C`) with these ARB extensions referenced by the backend:

`ARB_base_instance`, `ARB_buffer_storage`, `ARB_clip_control`, `ARB_debug_output`, `ARB_direct_state_access`, `ARB_draw_indirect`, `ARB_multi_draw_indirect`, `ARB_shader_draw_parameters`, `ARB_vertex_attrib_binding`, `EXT_debug_label`, `EXT_texture_filter_anisotropic`, `KHR_debug`.

There is no desktop GL on Android. Sandpaper is the only way to run this backend, and it requires **GLES 3.2** as the substrate. GLES 3.1 is not enough (no `glDrawElementsBaseVertex` guarantee, no `glColorMaski`/`glEnablei`, no compute/SSBO parity). Devices with only GLES 3.1 are out of scope for v1 and get a clear compatibility message.

---

## 8. Sandpaper

### 8.1 What it is

Sandpaper is an original **GL 3.3-core-on-GLES-3.2 compatibility runtime** plus a backend policy layer. It is loaded by EclipseSDL3 when the GL path is selected and is the only GL function provider the game sees.

It is **not**:

- a Vulkan renderer (Minecraft's own Vulkan backend is used when available);
- a desktop GL driver (no JIT, no LLVM, no software rasteriser);
- a GLSL compiler (Minecraft already ships Shaderc and SPIRV-Cross);
- a full GL 4.x implementation (26.3's GL backend is 3.3 core).

### 8.2 Architecture

```
Minecraft / LWJGL GL33C
        │  function pointers
        ▼
SDL_GL_GetProcAddress
        │
        ▼
libsandpaper.so
├── symbol table: 126 entry points + fail-loud stubs
├── GLES 3.2 dispatch (eglGetProcAddress)
├── object shadow map (buffers, framebuffers, textures, VAOs, samplers)
├── emulation: DSA, buffer storage, clip control, multi-draw, base instance
├── shader rewrite: GLSL 330 → GLSL ES 320
├── EGL config/context/swapchain
└── capability report (JSON) for the launcher
```

C ABI (all symbols `pw_sp_*`):

```c
bool  pw_sp_initialize(const pw_sp_config *config);
void  pw_sp_shutdown(void);
void *pw_sp_get_proc_address(const char *name);
bool  pw_sp_create_context(pw_sp_window *window, const pw_sp_gl_config *cfg);
void  pw_sp_make_current(void);
void  pw_sp_swap_buffers(void);
void  pw_sp_destroy_context(void);
const char *pw_sp_last_error(void);
const char *pw_sp_capability_json(void);
```

The JNI prefix for the app's own bridge is `pw_`. Sandpaper's C ABI deliberately uses `pw_sp_` so a symbol dump shows which layer owns it.

### 8.3 Entry-point coverage

[`contracts/26.3-gl-functions.txt`](contracts/26.3-gl-functions.txt) lists **126** GL entry points referenced by 26.3. They fall into five groups:

| Group | Examples | Strategy |
|---|---|---|
| Core GLES 3.2 | `glBindBuffer`, `glTexImage2D`, `glDrawElements`, `glEnable`, `glViewport` | Direct dispatch |
| DSA / named objects | `glCreateBuffers`, `glNamedBufferData`, `glNamedFramebufferTexture`, `glBlitNamedFramebuffer`, `glMapNamedBufferRange` | Shadow object map + bind/restore |
| Storage and mapping | `glBufferStorage`, `glMapBufferRange`, `glFlushMappedBufferRange`, `glUnmapBuffer` | Mutable allocation + staging for persistent maps |
| Draw variants | `glMultiDrawArraysIndirect`, `glDrawArraysInstancedBaseInstance`, `glDrawElementsInstancedBaseVertexBaseInstance` | Loop or uniform-assisted emulation |
| Debug / queries / state | `glObjectLabel`, `glPushDebugGroup`, `glQueryCounter`, `glGetTexLevelParameteri`, `glClipControl` | Tracked state, extension mapping or no-op |

Unknown `gl*` names return `pw_sp_unimplemented`, a function that logs the name and, in release builds, aborts with a clear message. This is deliberate: a silent no-op in a renderer produces a black screen and a week of guessing.

Capability identity presented to the game:

```
GL_VERSION                 3.3.0 Sandpaper
GL_SHADING_LANGUAGE_VERSION 3.30
GL_RENDERER                Sandpaper on <GPU>
GL_VENDOR                  Eclipse Launcher
GL_MAJOR_VERSION           3
GL_MINOR_VERSION           3
```

`glGetStringi(GL_EXTENSIONS, i)` reports the ARB extension list the backend checks, plus the GLES extensions Sandpaper actually relies on. It never claims an extension Sandpaper cannot honour.

### 8.4 Emulation details

| GL 3.3 call | GLES 3.2 basis | Sandpaper strategy | Risk |
|---|---|---|---|
| `glCreateBuffers` / `glNamedBufferData` / `glNamedBufferSubData` | `glGenBuffers` / `glBufferData` / `glBufferSubData` | Shadow buffer map; bind, upload, restore previous binding | Medium: binding cache must be exact |
| `glCreateFramebuffers` / `glNamedFramebufferTexture` | `glGenFramebuffers` / `glFramebufferTexture2D` | Shadow FBO map; bind, attach, restore | Medium |
| `glBlitNamedFramebuffer` / `glCopyNamedBufferSubData` | `glBlitFramebuffer` / `glCopyBufferSubData` | Bind both objects, call, restore | Low |
| `glBufferStorage` | `glBufferData` | Allocate mutable storage; record flags; reject persistent-coherent semantics | High: Minecraft may assume immutable size |
| `glMapBufferRange` with `GL_MAP_PERSISTENT_BIT` | none | Return a staging allocation; upload on unmap/flush | High: correctness and stutter |
| `glClipControl` | none | No-op; use GLES's 0..1 depth and lower-left origin; adjust projection if the game asks for upper-left | High: Y-flip and depth-range bugs |
| `glMultiDrawArraysIndirect` / `glMultiDrawElementsIndirect` | `glDrawArraysIndirect` / `glDrawElementsIndirect` | Loop over a CPU shadow of the indirect buffer; one draw per command | Medium: performance on chunk-heavy scenes |
| `glDrawArraysInstancedBaseInstance` | `glDrawArraysInstanced` | Bind base instance as a uniform; rewrite `gl_InstanceID` in the shader if needed | High |
| `glDrawElementsInstancedBaseVertexBaseInstance` | `glDrawElementsInstancedBaseVertex` | Native base vertex; base instance via uniform | High |
| `glGetTexLevelParameteri` | none | Track texture level sizes at `glTexImage2D`/`glTexStorage*` time | Low |
| `glGetInteger64` | partial | Track 64-bit limits; return 32-bit where safe | Low |
| `glQueryCounter` | `EXT_disjoint_timer_query` | Use the extension when present; otherwise return 0 and log | Medium |
| `glPolygonMode` / `glLogicOp` | none | No-op (debug/wireframe only) | Low |
| `glDrawBuffer` / `glReadBuffer` | `glDrawBuffers` / `glReadBuffer` | Map `GL_BACK`/`GL_FRONT` to `GL_BACK`/`GL_NONE`; attachments pass through | Low |
| `glClearDepth` | `glClearDepthf` | Convert double to float | Low |
| `glColorMaski` / `glEnablei` / `glDisablei` | ES 3.2 core | Direct dispatch | Low |
| `glBlendEquationSeparate` / `glBlendFuncSeparate` | ES 2.0 core | Direct dispatch | Low |
| `glTexBuffer` | ES 3.1 core | Direct dispatch | Low |
| `glObjectLabel` / debug groups | `KHR_debug` in ES 3.2 | Direct or no-op if the driver lacks it | Low |

The exact behaviour is defined by tests against 26.3, not by the table alone. Every emulation path has a named test scene: world load, chunk rebuild, entity rendering, GUI, particle, weather, end portal, shadow pass and screenshot.

### 8.5 The shader path

26.3 does not hand raw GLSL to the driver:

1. 72 core shaders are authored as `#version 330` with `GL_ARB_separate_shader_objects` and Vulkan-style `gl_VertexIndex`.
2. `GlslCompiler` compiles them with **Shaderc** to SPIR-V, resolving `#include <minecraft:...>` through the resource manager.
3. `SPIRVModule` reflects descriptors and interface variables with **SPIRV-Cross**.
4. The GL backend's `GlPipelineRecompiler` uses SPIRV-Cross to emit **GLSL 330** from the SPIR-V, then compiles it with `glShaderSource`/`glCompileShader`.
5. The Vulkan backend consumes the SPIR-V directly.

Sandpaper therefore receives **generated, regular GLSL 330**, not hand-written shaders. The rewrite is narrow:

| Transform | Reason |
|---|---|
| `#version 330` → `#version 320 es` | GLES 3.2 accepts 320 es |
| Insert `precision highp float; precision highp int;` and sampler precisions | GLSL ES requires precision qualifiers |
| Strip `layout(location = N)` from `in`/`out` varyings | ES 3.2 does not allow location qualifiers on varyings |
| Remove `#extension GL_ARB_*` lines after allowlisting | Most ARB extensions do not exist in ES |
| Map `gl_BaseInstance`/`gl_BaseVertex` to Sandpaper uniforms if emitted | `ARB_shader_draw_parameters` is not in ES |
| Reject `gl_ClipDistance` unless the driver exposes `EXT_clip_cull_distance` | Not core in ES 3.2 |
| Keep `layout(binding = N)` only if the driver accepts it; otherwise bind by name | ES binding rules differ |

Programs are linked with `glCreateProgram`/`glAttachShader`/`glLinkProgram`; 26.3's function list contains no `glGenProgramPipelines` or `glProgramUniform*`, so separate-shader-object program pipelines do not need emulation. The `GL_ARB_separate_shader_objects` requirement in the source is a GLSL qualifier requirement, not a runtime pipeline requirement.

If compilation fails, Sandpaper writes `sandpaper-shader-<hash>.glsl` and the full info log to `<instance>/logs/sandpaper/`, then fails the launch with a report naming the shader. It does not substitute a magenta shader and pretend the game is running.

### 8.6 Formats and state

- **Color:** `RGBA8`, `SRGB8_ALPHA8`, `R8`, `RG8`, `RGBA16F`, `R11F_G11F_B10F` are supported by ES 3.2; color-renderability is queried with `glGetInternalformativ`.
- **Depth:** `DEPTH24_STENCIL8` and `DEPTH_COMPONENT32F` are the primary formats; `DEPTH_COMPONENT24` is mapped where the driver requires it.
- **BGRA:** use `EXT_texture_format_BGRA8888` when present; otherwise upload as RGBA with a swizzle.
- **sRGB:** use sRGB internal formats for sRGB targets. `GL_FRAMEBUFFER_SRGB` has no GLES equivalent; Sandpaper records the intent and selects the right internal format at attachment time.
- **Compressed textures:** S3TC/DXT is not available in core GLES. Vanilla and normal resource packs decode PNG to RGBA before upload, so this is not a v1 blocker. A pack that ships DXT data is transcoded or decompressed in v1.1.
- **Anisotropy:** `EXT_texture_filter_anisotropic` is present on most GLES 3.2 drivers; query and clamp to `GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT`.

### 8.7 Presentation

- EGL 1.5, `EGL_OPENGL_ES3_BIT`, RGBA8888, depth 24, stencil 8, no multisample (Minecraft does its own AA).
- `eglSwapInterval(1)` by default; the user can choose “uncapped” in developer settings.
- `Surface.setFrameRate(fps, FRAME_RATE_COMPATIBILITY_FIXED_SOURCE)` is called by `EclipseSurfaceView` when the game starts. Android 15+ defaults games to 60 Hz; requesting 90/120 unlocks the panel rate, subject to thermal and power policy.
- Resolution scale is **not** in v1. The surface is the native window size; Minecraft's own render distance and GUI scale are the supported controls. A scaled `SurfaceView` is a later feature because it changes input coordinates and screenshot behaviour.
- HDR is **not** in v1. The swapchain is 8-bit sRGB; HDR needs a 10/16-bit config, tone mapping and a UI that can draw over it.

### 8.8 Phases

| Phase | Deliverable | Gate |
|---|---|---|
| **P0 — v1** | 126 entry points, DSA/storage/clip/multi-draw emulation, 330→320 es rewrite, EGL context, vanilla 26.3 main menu and world | Vanilla world runs 10 minutes on Adreno and Mali without a GL error; screenshot matches the desktop reference within tolerance |
| **P1 — performance** | Persistent-buffer staging, indirect batching, texture upload paths, frame pacing, shader cache | 60 fps target on T0/T1 devices; no regression in P0 scenes |
| **P2 — policy** | Vulkan probe, device quirk database, per-GPU defaults, safe mode | Probe agrees with Minecraft's own backend outcome on 20 devices |
| **P3 — compatibility** | DXT transcode, GL 4.x extensions if a mod requires them, resolution scale | Modded scenario matrix |

**Non-goals for Sandpaper:** a software rasteriser, a Vulkan renderer, geometry/tessellation shaders (26.3 does not use them), ray tracing, and driver replacement.

### 8.9 Risks

1. **`glClipControl` and Y-flip.** GLES's clip space is 0..1 with a lower-left origin; desktop GL is -1..1 and configurable. If 26.3's projection assumes `GL_UPPER_LEFT`, the world renders upside down or with an inverted depth test. This is the first P0 test.
2. **Persistent mapped buffers.** If 26.3 uses persistent coherent mapping for chunk buffers, the staging emulation adds a copy per frame. The P1 work is to detect the pattern and use `glBufferSubData` or orphaning instead.
3. **Base instance.** Instanced rendering with a non-zero base instance is used by modern chunk renderers. The uniform rewrite must be applied to every shader that reads `gl_InstanceID`, including modded shaders.
4. **sRGB.** Rendering to an sRGB target with the wrong internal format produces washed-out or dark output that is easy to miss in a screenshot test. Compare pixel values, not just “it looks fine”.
5. **Driver bugs.** Adreno and Mali differ on `glBlitFramebuffer` depth, `glTexImage2D` alignment, and `GL_DEPTH_COMPONENT32F` renderability. The quirk database is per-GPU, keyed by `GL_RENDERER` and `Build.SOC_MODEL`.
6. **GLES 3.2 coverage.** Android 8.0 does not guarantee GLES 3.2. Devices with 3.1 are unsupported in v1 and must be told so before a 500 MB download, not after.

---

## 9. Display, window and lifecycle

- **Activity:** `GameActivity` in `:game`, `android:appCategory="game"`, `resizeableActivity="true"`, `configChanges` as above.
- **Orientation:** `sensorLandscape` by default; the user can choose portrait for keyboard-heavy setups. Android 16 ignores orientation restrictions on large screens unless the app is a game; the game category is declared, but the layout still handles a resize.
- **Edge-to-edge:** the game surface fills the window; the Compose overlay consumes insets and keeps controls out of cutouts.
- **Cutouts:** `layoutInDisplayCutoutMode="shortEdges"` on the game window.
- **Multi-window:** split-screen is supported; a resize recreates the EGL surface and sends `SDL_EVENT_WINDOW_RESIZED`. The JVM is not recreated.
- **Background:** when the activity stops, the game pauses, audio stops, and the surface may be destroyed. Android 17's background-audio rules are satisfied by not playing in the background.
- **Process death:** the UI process reads `ApplicationExitInfo` (API 30+) for the `:game` process and maps `REASON_CRASH_NATIVE`, `REASON_ANR`, `REASON_LOW_MEMORY` and `REASON_SIGNALED` to a human explanation.

---

## 10. Audio

### 10.1 Path

Minecraft uses **OpenAL** through LWJGL (`lwjgl-openal`). SDL's audio subsystem is not the game's audio path.

v1 builds **official OpenAL Soft** at the LWJGL 3.4.3 pin (`7f9c6de`) with the **OpenSL ES** backend:

- `libopenal.so` links `libOpenSLES.so` from the platform;
- no Java callbacks, so no cross-VM audio bridge;
- `ALSOFT_DRIVERS=opensl` is set before `alcOpenDevice(NULL)`;
- HRTF is enabled if the user asks for it; the HRTF data set is embedded.

**Why not Oboe:** OpenAL Soft has no official Oboe backend (the upstream issue is still open). The old FCL fork added one, and that fork is excluded by the provenance policy. Building a private Oboe backend is possible but makes OpenAL a maintained fork; it is not justified for v1.

**AAudio later:** OpenAL Soft 1.25 has an official **SDL3** output backend. Because EclipseSDL3 already uses AAudio for SDL audio, a later component build can enable `ALSOFT_BACKEND_SDL3` and get AAudio without a fork. That is a v1.1 evaluation, not a v1 dependency.

### 10.2 Latency and focus

- OpenSL's latency is device-dependent, typically 40–80 ms. Minecraft's audio is not rhythm-critical; this is acceptable for v1.
- The activity requests `AUDIOFOCUS_GAIN` with `USAGE_GAME`. On `AUDIOFOCUS_LOSS`, the game pauses and OpenAL stops the device. On `AUDIOFOCUS_LOSS_TRANSIENT`, the game ducks or pauses.
- The `:game` process does not run a background-audio foreground service. If the user backgrounds the game, audio stops.

**Open:** whether OpenSL on Android 16 still has the underrun behaviour fixed in OpenAL Soft 1.25; test on the device matrix.

---

## 11. Input

### 11.1 Touch

- The Compose overlay draws the touch controls (D-pad, jump, sneak, inventory, hotbar, look area).
- Touches inside the look area are translated to relative mouse motion; touches on buttons are translated to key events.
- The overlay has a “mouse mode” toggle that hides the controls, requests pointer capture, and sends raw relative deltas.
- Pointer IDs are tracked so multi-touch does not confuse the look area with a button press.

### 11.2 Mouse

- `EclipseSurfaceView.onGenericMotion` forwards `ACTION_HOVER_MOVE`, `AXIS_X/Y`, `AXIS_VSCROLL` and `AXIS_HSCROLL`.
- `requestPointerCapture()` (API 26+) gives relative deltas and hides the cursor. Android 17 delivers relative touchpad events during capture by default.
- Right-click, middle-click and side buttons map to SDL button indices.
- The physical mouse wheel maps to the hotbar.

### 11.3 Keyboard

- Physical keyboards deliver `onNativeKeyDown`/`onNativeKeyUp` with Android key codes; SDL maps them to SDL scancodes.
- `KEYCODE_BACK` is intercepted by `OnBackInvokedCallback` and translated to `SDLK_ESCAPE`; it does not finish the activity.
- Modifier state, repeat events and dead keys are handled by SDL's Android keyboard code.

### 11.4 Gamepad

- `EclipseControllerManager` enumerates `InputDevice.SOURCE_GAMEPAD`/`JOYSTICK`, builds a stable device ID, and forwards buttons, axes, hats and sensors.
- SDL's gamepad mapping database maps the device to the standard layout.
- Deadzones are applied in the launcher, not in Minecraft, so the user can tune them.
- Rumble uses `InputDevice.getVibrator()`; it is disabled when the device is in battery saver.

### 11.5 IME and clipboard

- `SDL_StartTextInput` → `EclipseSDL.showTextInput` → the activity shows the IME.
- `EclipseInputConnection.nativeCommitText` sends the committed string to SDL; `nativeGenerateScancodeForUnichar` handles characters that need a key event.
- The IME is inset-aware: `WindowInsets.ime` moves the chat field above the keyboard.
- Clipboard read/write goes through `EclipseSDL`'s `ClipboardManager` bridge; SDL's clipboard events are forwarded to the game.

---

## 12. Diagnostics and failure handling

### 12.1 What is written

```
<instance>/logs/
  latest.log              Minecraft/Log4j
  game-stdout.log         redirected fd 1/2
  sandpaper.log           renderer decisions, GL errors, shader failures
  sandpaper/*.glsl        failed shader sources
  hs_err_pid*.log         JVM fatal error
  crash-report.json       launcher's structured report
```

`crash-report.json` contains:

```json
{
  "schema": 1,
  "instance": "…",
  "minecraft": "26.3",
  "loader": "fabric-0.18.4",
  "renderer": {"requested": "auto", "selected": "sandpaper", "gles": "3.2", "vulkan": "1.1"},
  "device": {"model": "…", "soc": "…", "ram_mib": 5924, "abi": "arm64-v8a", "api": 36},
  "jvm": {"version": "25.0.4", "xmx_mib": 2048, "gc": "ZGC", "args": ["…"]},
  "exit": {"reason": "REASON_CRASH_NATIVE", "signal": 11, "library": "libsandpaper.so", "offset": "0x1a24c"},
  "last_log_lines": ["…"]
}
```

### 12.2 Safe mode

When the previous launch crashed before the main menu, the next launch offers:

1. **Disable mods** (Fabric: move `mods/` aside; vanilla: no-op).
2. **Force OpenGL** (skip the Vulkan probe).
3. **Reduce heap** to 1 GB and render distance to 4.
4. **Reset renderer cache** (delete `sandpaper/` shader cache and `options.txt` graphics keys).

Safe mode is a normal launch with a recorded flag, not a separate code path.

### 12.3 No silent fallback

- If Vulkan fails, Minecraft itself falls back to GL and reports the reason. The launcher shows the reason, not just “running”.
- If GL fails, the launcher shows the shader/GL error and offers safe mode. It does not relaunch in a loop.
- If the JRE is missing or corrupt, the launcher repairs it; it does not run with a “close enough” runtime.

---

## 13. Performance budgets

| Metric | Target | Method |
|---|---|---|
| JVM creation | < 1.5 s | `pw_game_start` timestamps; **Observed** 1.1–1.2 s with ZGC 512M–2G |
| Native preload | < 300 ms | `dlopen` timestamps |
| Cold start to main menu | < 25 s on a 2022 midrange | Screen recording + log timestamps |
| World load (vanilla, RD 8) | < 25 s | Log timestamps |
| Frame time, T0 (8 GB, Adreno 730/Mali-G710) | 16.7 ms at 60 fps | GPU profiling, 10-minute run |
| Frame time, T1 (6 GB, Adreno 642L) | 33 ms at 30 fps, 60 fps in menus | Same |
| UI process RSS | < 150 MB | `dumpsys meminfo` |
| Game process RSS | 1.5–2.5 GB | `dumpsys meminfo`, `ApplicationExitInfo` |
| APK (arm64, natives) | < 60 MB | Release artifact |
| JRE download | < 50 MB compressed | Release artifact |
| First-run total | < 700 MB (JRE + vanilla assets) | Installer report |

**Inference:** the 26.3 asset index alone is 484,894,038 bytes / 581.5 MiB before libraries and the client jar. A “vanilla install” is therefore ~650 MB, and a 1.5 GB free-space requirement in the release notes is honest but tight; the launcher warns below 2 GB.

---

## 14. Decisions for sign-off

1. **`GameActivity` lives in `:game`.** The UI process stays separate; the game activity owns the surface, the SDL bridge and the JVM. This replaces the earlier “`:game` foreground service only” wording in doc 04.
2. **Two VMs, one process.** ART hosts the bridge; HotSpot hosts the game; no Java object crosses. The bridge is initialised on ART before `JNI_CreateJavaVM`.
3. **`extractNativeLibs=true`** for the core native stack. The JRE is a downloaded component.
4. **EclipseJRE 25 with the jlink profile in §3.2**, read-only extraction, `${ORIGIN}` rpath, 16 KB build flags and a fail-closed ELF gate.
5. **JVM flags and heap policy in §4.2–4.3.** `AlwaysPreTouch` and `UseStringDeduplication` are removed; ZGC on 6 GB+, G1 below.
6. **Sandpaper is a GL 3.3 → GLES 3.2 compatibility runtime and backend policy layer**, not a Vulkan renderer and not a GLSL compiler.
7. **Vulkan first when the probe passes; Sandpaper GL otherwise.** The game's own `preferredGraphicsBackend` option is authoritative.
8. **Official OpenAL Soft with OpenSL in v1.** AAudio via the official SDL3 backend is a v1.1 evaluation.
9. **Sash is deferred.** v1 uses the real headless `java.desktop`; Sash later patches `android`-backed `Toolkit`/`ImageIO` into `java.desktop` rather than reimplementing AWT.
10. **No Turnip, Zink, or linker namespace bypass.** System GLES/Vulkan only.

## 15. Open questions

- Does HotSpot's signal chaining coexist with ART's implicit null checks on API 36/37?
- Does `dlopen` of a `0444` JRE from app storage satisfy Android 17's native-DCL rule on a real device?
- Does loading `libSDL3.so` in ART and then `dlopen`ing the same file in HotSpot cause a double `JNI_OnLoad` or a namespace mismatch?
- Does 26.3's GL backend run on GLES 3.2 through Sandpaper for a full vanilla session, including the end portal, weather and screenshots?
- Does `glClipControl(GL_UPPER_LEFT, GL_ZERO_TO_ONE)` require a projection flip, or does 26.3 already emit a GLES-compatible projection?
- Does Fabric/Mixin survive Android 17's unmodifiable `static final` fields?
- Does OpenSL meet the audio latency target on Android 16, or does v1 need the SDL3/AAudio backend earlier?
- Does `Surface.setFrameRate()` work when the surface is created by SDL rather than a native `SurfaceView`?
- Does the 16 KB EclipseJRE boot on a 16 KB device? No such device is available locally; the gate is static until one is.
