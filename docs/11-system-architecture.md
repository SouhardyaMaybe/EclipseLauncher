# 11 — System architecture

**Status:** architecture draft for sign-off, 2026-10-24.
**Scope:** the module graph, process and thread model, IPC contracts, domain and persistence model, concurrency rules, state machines, error taxonomy, security boundaries, observability, component interfaces, and build/release architecture that implement chapters [04](04-android-platform.md)–[10](10-design-research-and-skills.md).
**Audience:** the engineers who will build it, and the reviewer who has to decide whether the design is safe to start.
**Labels:** **Verified** (platform or upstream fact inspected), **Decision** (proposed for sign-off), **Inference**, **Open**.

---

## 1. The contract in one page

| Concern | Decision | Why |
|---|---|---|
| Shape | A modular monolith: one application, one Gradle build, explicit module boundaries | A distributed system would add failure modes the product cannot debug on a phone |
| Processes | `:app` (UI, auth, downloads), `:game` (SDL + JVM + Minecraft), `:installer` (JVM + loader processors) | Crash and memory isolation without a second APK |
| Virtual machines | ART in every process; HotSpot 25 only in `:game` and `:installer` | The game needs a real OpenJDK; the Android UI cannot run on HotSpot |
| Object boundary | No Java object crosses a VM boundary; native handles, files and a length-prefixed socket protocol only | Two VMs have incompatible `JNIEnv` and object models |
| Source of truth | The filesystem is authoritative; the database is a rebuildable index | A crash mid-transaction must not leave the UI believing in an instance that does not exist |
| Persistence | Room for queries, JSON manifests for contracts, `AtomicFile` for small state, content-addressed store for large objects | Each format is used where it is strongest |
| Concurrency | Structured coroutines, keyed mutexes, persisted work, bounded queues | Cancellation and process death are normal, not exceptional |
| Errors | One sealed failure type with a stable code, a stage, a retryability flag and a user action | “Something went wrong” is a design defect |
| Security | App sandbox + least privilege; mods are **not** sandboxed in v1 and this is stated | A false security claim is worse than a documented limit |
| Observability | Local structured logs, a launch journal, `ApplicationExitInfo`, opt-in export | The product promise is “we tell you why”; that requires evidence |
| Compatibility | Components are versioned, signed manifests with ABI/API/feature requirements | The JRE and natives ship on a different cadence than the APK |
| Build | GitHub Actions only, pinned toolchains, dependency verification, no checked-in binaries | Reproducibility and supply-chain review |

### 1.1 Architecture invariants

These are review rules, not aspirations. A change that violates one needs an ADR.

1. **No Java object crosses between ART and HotSpot.** Only primitives, strings created by the receiving VM, file descriptors and native pointers.
2. **`:app` never loads game code, a mod, or a loader processor.** `:game` and `:installer` never own the account vault.
3. **The `:game` process never writes to the shared asset, library or component store.** It reads; installation writes.
4. **Every managed file has a digest and a size in a manifest.** A file without a digest is a user file and is never touched by repair.
5. **The filesystem wins.** On startup, the database is reconciled against the filesystem, not the other way round.
6. **No network or disk I/O on the Android main thread.**
7. **No database transaction is held across network I/O.**
8. **A state transition is persisted before the UI renders it.** “Installed” means the commit record exists.
9. **Tokens never appear in a plan file, an Intent extra, a log line or a crash report.** The only exception is the short-lived encrypted launch ticket, deleted after one read.
10. **Only the launcher activity is exported.** No deep links, no `FileProvider` outside a scoped grant, no implicit broadcasts.
11. **Every failure has a stable code, a stage and exactly one primary user action.**
12. **No silent fallback.** A renderer, runtime or download that degrades says so in the UI and in the log.

---

## 2. Design forces

The architecture is shaped by five forces, in order:

| Force | Consequence |
|---|---|
| **Two VMs in one process** | A hard native boundary; the host library is the only bridge; the SDL Java bridge must initialise before HotSpot exists |
| **Untrusted content** | Mods, packs and loader processors are code; the design confines their *blast radius* and is honest that same-UID code is not a sandbox |
| **Multi-gigabyte, resumable work** | Every long operation is a persisted transaction with a journal, not a coroutine that lives in memory |
| **The launcher must survive the game** | The UI is a separate process with a small heap; the game can be killed, crash or be replaced without losing the launcher |
| **The project is built only in CI** | The build must be reproducible from a clean checkout, with no local SDK assumptions and no checked-in artifacts |

A sixth force is organisational: a small team cannot maintain twenty perfectly separated modules on day one. The module graph below is the **target**; §3.4 defines the minimum v1 graph and the rule for when a module must be split.

---

## 3. Module graph

### 3.1 Modules

```
:app                         Android application, navigation host, AppContainer, services
:designsystem                theme, tokens, components, previews, screenshot catalog
:core:model                  pure Kotlin domain types and serializable contracts
:core:common                 Result/Failure, dispatchers, clocks, keyed mutex, logging API
:core:database               Room entities, DAOs, migrations, reconciliation queries
:core:storage                layout, atomic writes, locks, space checks, manifests, GC
:core:network                HTTP client, host allowlist, retry, digesting, rate limits
:core:crypto                 Keystore AES-GCM, ECDSA verification, redaction
:core:ipc                    Parcelable launch request/result, socket protocol, status schema
:domain:auth                 Microsoft chain, token lifecycle, account repository
:domain:mojang               version manifest, version JSON, rules, assets, Maven coordinates
:domain:install              planner, transaction engine, repair, loaders, modpacks, GC
:domain:content              Modrinth API, search, project/version models, install intent
:domain:launch               LaunchPlan builder, argument templates, heap/renderer policy
:domain:runtime              component index, verification, install/update of JRE and natives
:feature:home                hero instance, continue, quick actions
:feature:instances           list, detail, create, worlds, content, logs
:feature:discover            search, project detail, install sheet
:feature:downloads           active/queued/history, pause/resume/cancel
:feature:settings            account, appearance, controls, defaults, storage, diagnostics
:host                        GameActivity, InstallerActivity, JNI `pw_` bridge, overlay
:installer-core              pure JVM: Foreman/Crowbar processors, no Android types
```

Native and JVM side projects:

```
native/host/                 libeclipse_host.so: JVM creation, status, socket, signal hygiene
sandpaper/                   libsandpaper.so: desktop GL 3.3 on GLES 3.2 (v1 fallback)
sash/                        deferred: java.desktop Toolkit/ImageIO patches (v1.1)
foreman/                     Forge/NeoForge installer model, consumed by :installer-core
crowbar/                     binary patchers and renamers, consumed by :installer-core
```

### 3.2 Dependency rules

```
:app ──► :feature:* ──► :designsystem
  │          │
  │          └──────► :core:model, :core:common
  │
  ├──► :domain:* ──► :core:*
  ├──► :host
  └──► :core:ipc

:host ──► :core:ipc, :core:common, :designsystem (overlay only)
:installer-core ──► :core:model (JVM-compatible subset), foreman, crowbar
```

Rules enforced by a Gradle dependency-check task:

- A feature module may not depend on another feature module.
- No `:core:*` module may depend on a feature, a domain module, or `:app`.
- `:domain:*` may depend on `:core:*`, never on `:designsystem`, `:app`, `:host`, or a feature.
- `:host` may not depend on `:domain:auth`, `:domain:install`, or any feature. It receives paths and a ticket; it does not resolve accounts or installs.
- `:installer-core` may not reference `android.*`, `androidx.*`, or Room. It is compiled as JVM 25 bytecode and packaged as a jar.
- `:core:model` is pure Kotlin/JVM. Room annotations live in `:core:database`, not in the domain model.

### 3.3 Package naming

| Layer | Package |
|---|---|
| Android app and features | `dev.shadow.eclipselauncher.*` |
| Design system | `dev.shadow.eclipselauncher.designsystem` |
| SDL bridge | `dev.shadow.eclipselauncher.sdl` |
| Native JNI | `Java_dev_shadow_eclipselauncher_host_NativeHost_*`, prefix `pw_` |
| Installer JVM | `dev.shadow.eclipselauncher.installer.*` |

The SDL package rename from `me.shadow` to `dev.shadow` is part of the EclipseSDL3 cleanup. The bridge classes keep their names (`EclipseSDL`, `EclipseSurfaceView`, …) but the Java package and the JNI symbol names move together.

### 3.4 Minimum v1 graph

If the team is one or two engineers, ship v1 with:

```
:app  :designsystem  :core:{model,common,database,storage,network,crypto,ipc}
:domain:{auth,mojang,install,launch,runtime}  :host  :installer-core
```

Collapse the five feature modules into `:app` packages until a second consumer appears. Collapse `:domain:content` into `:domain:install` until the Modrinth client is reused. **Do not** collapse `:host`, `:installer-core`, `:core:storage`, or `:domain:launch`: those boundaries carry the invariants.

The rule for splitting a module: it has a second consumer, a different test runtime, a different release cadence, or a dependency that must not leak (Android, Room, OkHttp). “It feels big” is not a reason.

### 3.5 Why these boundaries

| Boundary | Prevents |
|---|---|
| `:domain:launch` separate from `:domain:install` | The game process re-resolving metadata at launch; install-time and launch-time rules drifting |
| `:core:network` separate from domain | Every feature inventing its own retry, allowlist and hashing behaviour |
| `:host` separate from `:app` | The UI accidentally linking against HotSpot-only code or the JNI surface |
| `:installer-core` pure JVM | Android types leaking into a jar that must run on EclipseJRE |
| `:core:storage` separate | Ad-hoc file writes that bypass atomic rename and digest manifests |
| `:designsystem` separate | Hard-coded colors, one-off buttons, inaccessible contrast |

---

## 4. Process and thread model

### 4.1 Processes

| Process | Components | Heap class | Lifetime | May be killed by |
|---|---|---|---|---|
| `dev.shadow.eclipselauncher` | `MainActivity`, Compose UI, `DownloadService`, account vault, Room | Normal, ≤ 150 MB target | Whole session | LMK, user swipe, crash |
| `dev.shadow.eclipselauncher:game` | `GameActivity`, `EclipseSurfaceView`, SDL bridge, `libeclipse_host.so`, HotSpot 25 | Large, 1.5–4 GB JVM + 300–600 MB native | One launch | LMK, driver/JVM crash, user exit |
| `dev.shadow.eclipselauncher:installer` | `InstallerActivity`, `libeclipse_host.so`, HotSpot 25, `installer.jar` | Medium, 512 MB–1 GB | One install/repair transaction | LMK, processor crash, user leave |

**Decision:** `:installer` is an **activity process, not a background service**. While the installer runs, the user sees a progress screen; the screen keeps the process visible and avoids a foreground-service type that would need a Play declaration. If the user leaves, the transaction checkpoints and the process may die; returning resumes from the last committed stage. A background-install service is deferred to v1.1 with a policy review.

**Decision:** `GameActivity` is the visible surface owner in `:game`. There is no game foreground service. When the game is backgrounded it pauses and stops audio (doc 04 §5, doc 05 §10).

### 4.2 Threads

| Thread | Process | Runs | Rules |
|---|---|---|---|
| `main` | all | Android lifecycle, view hierarchy, Compose | No I/O, no hashing, no lock waits |
| `DefaultDispatcher` | `:app`, `:installer` | CPU work: rule evaluation, plan building, JSON | Bounded by core count; no blocking calls |
| `eclipse-io` | `:app` | File I/O, Room, manifests | 8–16 threads; injected, never `Dispatchers.IO` directly |
| `eclipse-download` | `:app` | HTTP and digest streaming | 6 global, 4 per host; bounded channel |
| `eclipse-auth` | `:app` | OAuth chain, token refresh | 2 threads; single-flight per account |
| `game-main` | `:game` | `JNI_CreateJavaVM`, `Main.main`, SDL window, GL/Vulkan context | The only thread allowed to call SDL video/GL functions |
| Minecraft threads | `:game` | Render, sound, Netty, worker pools | Created by the game; the host does not join them |
| `installer-main` | `:installer` | `JNI_CreateJavaVM`, processor `main` | One processor at a time; `_exit` when done |
| SDL callback threads | `:game` | Java bridge calls into ART | Must not block; post to main if UI is touched |
| OpenAL/OpenSL thread | `:game` | Audio | Native only; no JNI callbacks into ART |

**Decision:** the thread that calls `JNI_CreateJavaVM` is the thread that calls `Main.main` and owns the SDL window. LWJGL's GL context is thread-affine, and SDL's Android video backend assumes a single window thread. `GameActivity` starts one `game-main` thread; it is never recreated for a rotation.

### 4.3 `:game` startup sequence

```
:app
  ├─ verify session, refresh token if needed
  ├─ build/read LaunchPlan, run LAUNCH verify
  ├─ write launch ticket (encrypted, one-shot)
  ├─ insert launch_sessions row (PREPARING)
  └─ startActivity(GameActivity, LaunchRequest)

:game  GameActivity.onCreate (ART)
  ├─ System.load(libSDL3.so); System.load(libeclipse_host.so)
  ├─ EclipseSDL.attach(this)
  ├─ setContentView(EclipseSurfaceView + overlay)
  ├─ read LaunchPlan, decrypt ticket in memory
  └─ surfaceCreated:
       ├─ EclipseSDL.nativeSetupJNI()        // captures ART JavaVM
       ├─ pw_host_start(configJson)           // spawns game-main
       │    ├─ chdir(instance/game), dup2 logs
       │    ├─ open status file, connect log socket
       │    ├─ dlopen(libjvm.so, RTLD_NOW|RTLD_GLOBAL)
       │    ├─ JNI_CreateJavaVM(options)
       │    ├─ FindClass(mainClass); CallStaticVoidMethod(main, args)
       │    ├─ write ENDED status, fsync
       │    └─ _exit(exitCode)
       └─ delete ticket after the JVM has consumed it
```

The host writes a status transition **before** each step that can hang or crash: `HOST_START`, `JVM_CREATING`, `JVM_READY`, `MAIN_ENTERED`, `MAIN_RETURNED`. The last persisted transition is the stage reported in a crash.

### 4.4 `:game` lifecycle

| Android event | Host action | JVM action |
|---|---|---|
| `onCreate` | load natives, attach bridge | none |
| `surfaceCreated` | `nativeSetupJNI`, start host | create JVM, run main |
| `onPause` | write `PAUSED`, pause SDL | game receives focus loss; audio stops |
| `onResume` | write `RESUMED`, resume SDL | game receives focus gain |
| `onConfigurationChanged` | update surface size/frame rate | renderer recreates swapchain/EGL surface |
| `surfaceDestroyed` | write `SURFACE_LOST`, stop rendering | block until a new surface arrives |
| `onDestroy` / Back | request graceful stop, then `_exit(0)` | save and quit if possible; timeout 5 s |

**Decision:** Back inside the game is **Escape**, not “finish the activity”. The overlay's Exit action is the explicit quit. A long-press Back or the overlay Exit sends the game's pause/quit sequence; after a 5 s grace period the host calls `_exit(0)`.

**Inference:** some mods install shutdown hooks that never return. The 5 s timeout is deliberate; the launcher writes a `FORCED_EXIT` status so the next launch can mention it.

### 4.5 Process death detection

The UI observes a launch through three independent signals:

1. **Activity result** — normal path; `GameActivity.setResult` with a `GameResult` before `finish()`.
2. **Status file** — `files/launch/<launchId>/status.json`, written atomically at each transition; survives a UI death.
3. **`ApplicationExitInfo`** (API 30+) — reason, status, timestamp, and trace for the `:game` process, when the system provides it.

On any `:app` start, `LaunchSessionReconciler` scans sessions in `RUNNING`/`STARTING`:

- status `ENDED` → close the session with its exit code;
- status missing and process absent → read `ApplicationExitInfo`; classify as `CRASH_NATIVE`, `CRASH_JAVA`, `ANR`, `LMK`, `SIGNALED`, `UNKNOWN`;
- status `RUNNING` but process absent and no exit info → `LOST` with an honest “the system ended the process without a report” message;
- status `RUNNING` and process alive → leave it; the UI can re-attach.

**Verified:** `ApplicationExitInfo` exposes `REASON_CRASH`, `REASON_CRASH_NATIVE`, `REASON_ANR`, `REASON_LOW_MEMORY`, `REASON_SIGNALED`, `REASON_EXIT_SELF` and a trace input stream on API 30+.

### 4.6 `:installer` sequence

```
:app
  ├─ create install_transactions row (PREPARING)
  ├─ startActivity(InstallerActivity, InstallRequest)
  └─ observe transaction row + progress events

:installer
  ├─ resolve/verify plan and staging directory
  ├─ start host with installer.jar on the classpath
  ├─ run Foreman/Crowbar processors (one at a time)
  ├─ write processor outputs into staging
  ├─ verify outputs against install_profile.json
  ├─ commit staging → instance (shared :core:storage code)
  └─ _exit(0) or _exit(70 + reason)
```

The host library is the same `libeclipse_host.so`; only the config (`mainClass`, classpath, cwd, heap) differs. This keeps JVM creation, logging, status and signal handling in one reviewed place.

---

## 5. IPC and contracts

### 5.1 What crosses which boundary

| Boundary | Mechanism | Payload | Never |
|---|---|---|---|
| `:app` → `:game` | Explicit `Intent` + `LaunchRequest` | Launch id, plan path, status path, socket name, account id, flags | Access token, refresh token, password, account email |
| `:app` → `:game` | Encrypted one-shot ticket file | Minecraft access token, UUID, name, XUID | Refresh token, Microsoft token |
| `:game` → `:app` | `LocalSocket` newline-delimited JSON | Stage events, exit, metrics, redacted log lines | Tokens, full log files |
| `:game` → `:app` | Status file + log files | Durable state and diagnostics | Secrets |
| `:app` → `:installer` | Explicit `Intent` + `InstallRequest` | Transaction id, staging path, loader, component paths | Account tokens |
| `:installer` → `:app` | Room row + status file + socket | Progress, outputs, failure | Arbitrary file paths outside staging |
| Mod ↔ launcher | None | — | No custom API in v1 |

### 5.2 `LaunchRequest`

```kotlin
@Parcelize
data class LaunchRequest(
    val schema: Int = 1,
    val launchId: String,
    val instanceId: String,
    val planPath: String,
    val statusPath: String,
    val logSocket: String?,
    val ticketPath: String,
    val accountId: String,
    val renderer: RendererChoice,
    val flags: Int,
) : Parcelable
```

Rules:

- `launchId` is a random UUID generated by `:app`; it is the correlation id for every log line, status write and crash report in this launch.
- `planPath` is an immutable file under `files/plans/<instanceId>/<planHash>.json`; it is re-verified before launch.
- `ticketPath` is under `files/launch/<launchId>/token.ticket`, mode `0600`, and deleted by `:game` immediately after the JVM has read the token.
- No token, no password, no email in the Intent. Intents can be logged by the system and by `dumpsys`.
- `renderer` is the **resolved** choice (`VULKAN` or `SANDPAPER`), not “auto”. The plan records why.

### 5.3 Launch ticket

```
version   1 byte
accountId length + UTF-8
expiresAt int64 (epoch millis)
nonce     12 bytes
ciphertext (AES-256-GCM)
```

- Key: Android Keystore alias `eclipse.launch.ticket.v1`, AES-256-GCM, non-exportable, `setRandomizedEncryptionRequired(true)`.
- AAD: `launchId + "|" + accountId + "|" + expiresAt`.
- TTL: 60 seconds. `:game` refuses an expired ticket and reports `IPC_TICKET_EXPIRED`.
- One read: `:game` reads, decrypts, zeroes the byte array, deletes the file. A second read fails.
- Cleanup: `:app` deletes orphan tickets older than 10 minutes on start and on boot.

**Decision:** the ticket is a file, not a Binder call. A bound service in `:app` would restart a killed UI process and lose the in-memory token; the file survives a UI death and keeps `:game` independent. The trade-off is a brief on-disk ciphertext, bounded by TTL and one-shot deletion.

### 5.4 Status file

```json
{
  "schema": 1,
  "launchId": "5f0c…",
  "state": "RUNNING",
  "stage": "GAME_INIT",
  "pid": 24173,
  "startedAt": "2026-10-24T09:12:03.114Z",
  "updatedAt": "2026-10-24T09:12:04.921Z",
  "exitCode": null,
  "signal": null
}
```

- Written with `O_CREAT|O_TRUNC`, `fsync`, `rename` (native `pw_status_write`).
- `state` ∈ `STARTING | RUNNING | PAUSED | EXITING | ENDED | FAILED`.
- `stage` ∈ `HOST_START | JVM_CREATING | JVM_READY | MAIN_ENTERED | GAME_INIT | WORLD_LOAD | RUNNING | SHUTDOWN`.
- A write failure is logged and does not abort the game; the journal in `:app` remains the fallback.

### 5.5 Log socket

- `:app` creates a `LocalServerSocket` in the abstract namespace named `eclipse.game.<launchId>` before starting the activity.
- `:game` connects once; failure is non-fatal and retried once after 500 ms.
- Framing: one UTF-8 JSON object per `\n`, max 4 KiB per frame; oversized lines are truncated with a marker.
- Event types: `stage`, `log`, `metric`, `exit`.
- The socket is a **convenience** for live UI; the durable record is the log file and the status file.
- The reader applies the same redaction filter as the writer; defense in depth.

**Open:** whether abstract-namespace `LocalSocket` is permitted for all target API levels and SELinux policies. Phase 0 test; fall back to a filesystem socket under `files/run/` with mode `0600`.

### 5.6 Contract versioning

- Every contract (`LaunchRequest`, status JSON, ticket, component manifest, plan) has a `schema` integer.
- Readers accept `schema` in a known range and **reject** unknown major versions with a specific error.
- Additive fields are allowed; unknown fields are ignored only for upstream Mojang data, never for our own contracts.
- A contract change requires a fixture update and a compatibility test in CI.

---

## 6. Domain model

### 6.1 Core types

```kotlin
data class Instance(
    val id: InstanceId,
    val name: String,
    val minecraftVersion: String,
    val loader: LoaderRef?,          // null = vanilla
    val pack: PackRef?,              // Modrinth project/version
    val state: InstanceState,
    val sizeBytes: Long,
    val lastPlayedAt: Instant?,
    val createdAt: Instant,
    val planHash: String?,
)

sealed interface LoaderRef {
    val version: String
    data class Fabric(override val version: String) : LoaderRef
    data class Quilt(override val version: String) : LoaderRef
    data class NeoForge(override val version: String) : LoaderRef
    data class Forge(override val version: String) : LoaderRef
}

data class Account(
    val id: AccountId,
    val profileName: String,
    val profileUuid: String,
    val xuid: String?,
    val state: AccountState,         // READY, REAUTH_REQUIRED, SIGNED_OUT
    val lastUsedAt: Instant?,
)

data class LaunchPlan(
    val schema: Int,
    val instanceId: InstanceId,
    val minecraft: String,
    val loader: LoaderRef?,
    val mainClass: String,
    val classpath: List<String>,
    val jvmArgs: List<String>,
    val gameArgs: List<String>,
    val assetsDir: String,
    val gameDir: String,
    val nativeDir: String,
    val javaHome: String,
    val env: Map<String, String>,
    val renderer: RendererChoice,
    val planHash: String,
)
```

`LaunchPlan` is serialized with `kotlinx.serialization`, canonical field order, and no secrets. `planHash` is SHA-256 over the canonical JSON with `planHash` itself removed.

### 6.2 Persistence split

| Data | Where | Why |
|---|---|---|
| Instance list, accounts, sessions, transactions, content index | Room | Queries, sorting, reactive UI |
| `instance.json`, `manifest.json`, `plan.json` | Instance directory | Portable, human-readable, survives a DB rebuild |
| Version JSON, asset index, loader profile | Shared cache, keyed by SHA-1 | Immutable upstream data |
| Assets, libraries, mods | Content-addressed store | Dedup and integrity |
| Tokens | Keystore-encrypted files | Not queryable, small, security-sensitive |
| Launch status, logs | Files | Written by native code that cannot use Room |
| Settings | DataStore (Preferences) | Small, async, no schema ceremony |

**Decision:** the database is never the only record of an instance. `InstanceReconciler` runs on app start and after every transaction, and can rebuild the `instances` table from `instance.json` files if the DB is lost.

### 6.3 Room schema (v1)

```sql
CREATE TABLE instances (
  id TEXT PRIMARY KEY NOT NULL,
  name TEXT NOT NULL,
  minecraft_version TEXT NOT NULL,
  loader_kind TEXT,
  loader_version TEXT,
  pack_project_id TEXT,
  pack_version_id TEXT,
  state TEXT NOT NULL,
  size_bytes INTEGER NOT NULL DEFAULT 0,
  last_played_at INTEGER,
  created_at INTEGER NOT NULL,
  plan_hash TEXT,
  icon_key TEXT,
  sort_index INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE accounts (
  id TEXT PRIMARY KEY NOT NULL,
  profile_name TEXT NOT NULL,
  profile_uuid TEXT NOT NULL,
  xuid TEXT,
  state TEXT NOT NULL,
  token_meta_json TEXT NOT NULL,
  created_at INTEGER NOT NULL,
  last_used_at INTEGER
);

CREATE TABLE install_transactions (
  id TEXT PRIMARY KEY NOT NULL,
  instance_id TEXT NOT NULL,
  kind TEXT NOT NULL,              -- CREATE, UPDATE, REPAIR, IMPORT
  state TEXT NOT NULL,
  stage TEXT NOT NULL,
  bytes_total INTEGER NOT NULL DEFAULT 0,
  bytes_done INTEGER NOT NULL DEFAULT 0,
  staging_path TEXT,
  error_code TEXT,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL,
  FOREIGN KEY(instance_id) REFERENCES instances(id) ON DELETE CASCADE
);

CREATE TABLE component_installations (
  component TEXT NOT NULL,         -- eclipsejre, eclipsenatives, eclipsesdl3
  version TEXT NOT NULL,
  abi TEXT NOT NULL,
  path TEXT NOT NULL,
  manifest_sha256 TEXT NOT NULL,
  state TEXT NOT NULL,
  installed_at INTEGER NOT NULL,
  PRIMARY KEY(component, version, abi)
);

CREATE TABLE content_items (
  id TEXT PRIMARY KEY NOT NULL,
  instance_id TEXT NOT NULL,
  kind TEXT NOT NULL,              -- MOD, RESOURCEPACK, SHADER, DATAPACK
  file_name TEXT NOT NULL,
  sha512 TEXT NOT NULL,
  project_id TEXT,
  version_id TEXT,
  enabled INTEGER NOT NULL DEFAULT 1,
  size_bytes INTEGER NOT NULL,
  source TEXT NOT NULL,
  FOREIGN KEY(instance_id) REFERENCES instances(id) ON DELETE CASCADE
);

CREATE TABLE launch_sessions (
  id TEXT PRIMARY KEY NOT NULL,
  instance_id TEXT NOT NULL,
  state TEXT NOT NULL,
  stage TEXT NOT NULL,
  exit_code INTEGER,
  signal INTEGER,
  failure_code TEXT,
  started_at INTEGER NOT NULL,
  ended_at INTEGER,
  log_dir TEXT NOT NULL,
  FOREIGN KEY(instance_id) REFERENCES instances(id) ON DELETE CASCADE
);

CREATE INDEX idx_instances_last_played ON instances(last_played_at DESC);
CREATE INDEX idx_sessions_instance ON launch_sessions(instance_id, started_at DESC);
CREATE INDEX idx_content_instance ON content_items(instance_id, kind, enabled);
```

Rules:

- Export the schema JSON; every migration has a test that opens the previous version.
- No `fallbackToDestructiveMigration` in release.
- Foreign keys are enforced (`PRAGMA foreign_keys=ON`).
- Timestamps are epoch millis, UTC; formatting happens in the UI layer.
- Enums are stored as stable strings, never ordinal integers.

### 6.4 Filesystem layout

```
files/                                  internal, never backed up
  components/
    index.json                          pinned, signed component index
    eclipsejre-25.0.4-arm64/            read-only, 0444 .so
    eclipsenatives-3.4.3-arm64/
  accounts/<id>/token.bin               AES-GCM refresh token
  plans/<instanceId>/<planHash>.json    immutable launch plans
  launch/<launchId>/
    token.ticket                        one-shot, 0600, 60 s TTL
    status.json
    host.log
  logs/launcher.log
  installs.db

<external-files>/                       app-specific external
  instances/<id>/
    instance.json
    manifest.json
    game/                               cwd: saves, options, mods, config
    logs/latest.log, game-stdout.log, sandpaper.log
    crash/hs_err_pid*.log, tombstone.json
    cache/tmp, cache/lwjgl, cache/jna
    .lock                               advisory install/launch lock
  shared/
    objects/<2>/<sha1>                  Mojang assets
    libraries/<maven path>              Java artifacts
    indexes/34.json
    mods/<sha512>.jar
  staging/<txid>/
```

- `files/` is internal and smaller; the JRE and plans live there because `dlopen` and stable paths matter.
- `instances/` and `shared/` are app-specific external, visible over USB for backup, still private to the app.
- `cache/` is evictable; no managed file lives only there.
- `stage` and `commit` are on the same filesystem so `rename(2)` is atomic.

---

## 7. Data flow

### 7.1 Install

```
UI: Create instance
  → domain:install.resolve(version, loader)
      → domain:mojang.manifest()            (cached, SHA-1 verified)
      → domain:mojang.versionJson()         (SHA-1 verified)
      → loader profile merge                (Fabric v1)
  → domain:install.plan()                   → LaunchPlan + FileList
  → domain:install.preflight()              → space, ABI, component, renderer, account
  → transaction row (RESOLVE → … → ACQUIRE)
  → DownloadEngine                           (persisted work, 6 connections)
  → storage.materialise()                    (staging tree)
  → loader processors                        (only if needed; :installer)
  → storage.verify(FULL)
  → storage.commit()                         (fsync → journal → rename)
  → Room: instances + content_items + component refs
  → GC
```

Every stage is idempotent. The transaction row is updated **before** the side effect where possible, and always after it, so a crash leaves a state the reconciler can interpret.

### 7.2 Launch

```
UI: Play
  → auth.ensureValid(account)               (refresh single-flight)
  → launch.plan(instance)                   (read or rebuild, verify hash)
  → storage.verify(LAUNCH)                  (stat storm, classpath, natives, index)
  → composition.renderer(instance, device)  (Vulkan probe or Sandpaper)
  → launch.buildArgs(plan, account)         (token injected in memory)
  → write ticket + session row
  → start GameActivity
```

The game process never resolves metadata, never queries Modrinth, never refreshes a token. If the plan is stale, `:app` rebuilds it before launch.

### 7.3 Auth refresh

```
launch or account screen
  → TokenStore.read(account)
  → if expiry > now + 5 min: use
  → else: single-flight refresh
       → POST /consumers/oauth2/v2.0/token
       → new refresh token → atomic write
       → XBL → XSTS → login_with_xbox
       → profile + entitlement
       → TokenStore.write
```

If refresh fails with `invalid_grant`, the account moves to `REAUTH_REQUIRED`; instances are untouched.

### 7.4 Crash

```
:game dies
  → status.json last stage
  → ApplicationExitInfo (if available)
  → hs_err_pid*.log / tombstone
  → :app reconciler builds CrashReport
  → crash sheet: stage, signal, library+offset, plain explanation
  → safe mode / export / close
```

The report is assembled from evidence, not guessed. If the evidence is insufficient, the sheet says so.

---

## 8. Concurrency

### 8.1 Principles

1. **Structured concurrency.** Every coroutine has an owner scope; no `GlobalScope`.
2. **Cancellation is cooperative.** Long loops call `ensureActive()`; blocking I/O uses interruptible channels or timeouts.
3. **Persist before you promise.** A queued download exists in the DB before the UI says “queued”.
4. **One writer per instance.** Installs, repairs and deletes for the same instance are serialised.
5. **One refresher per account.** Token refresh is single-flight.
6. **Bounded everything.** Queues, connection pools, log buffers, and retry counts have caps.

### 8.2 Keyed mutex

```kotlin
class KeyedMutex {
    private val locks = mutableMapOf<String, Mutex>()
    private val refs = mutableMapOf<String, Int>()

    suspend fun <T> withLock(key: String, block: suspend () -> T): T {
        val mutex = synchronized(locks) {
            refs[key] = (refs[key] ?: 0) + 1
            locks.getOrPut(key) { Mutex() }
        }
        try {
            return mutex.withLock { block() }
        } finally {
            synchronized(locks) {
                val n = refs.getValue(key) - 1
                if (n == 0) { refs.remove(key); locks.remove(key) } else refs[key] = n
            }
        }
    }
}
```

Used for `instance:<id>`, `account:<id>`, `component:<name>:<abi>`, `store:gc`.

### 8.3 Cross-process exclusion

- `<instance>/.lock` contains `{pid, startedAt, purpose}` and is created with `O_CREAT|O_EXCL`.
- `:installer` holds it for the whole transaction; `:game` checks it before launch and refuses with `INSTALL_IN_PROGRESS`.
- A stale lock is detected by checking `/proc/<pid>` and the process start time; if stale, it is renamed to `.lock.stale` and a new lock is taken.
- This is advisory, not a kernel lock. Same-UID code can ignore it; the goal is to stop our own components from colliding, not to defend against malicious code.

**Open:** whether `FileChannel.tryLock` behaves reliably on the emulated external volume across processes. Phase 0 test; the advisory lock is the fallback.

### 8.4 Download engine

- Work items are rows in `download_tasks` (or a JSON journal) with URL, digest, size, destination, state, attempts.
- `DownloadScheduler` picks user-initiated transfer jobs on API 34+, `dataSync` foreground service on older APIs (doc 04).
- `DownloadEngine` runs at most 6 requests globally and 4 per host; a `Semaphore` enforces it.
- Per object: `.part` file, streaming digest, `fsync`, verify, `rename`.
- Progress events are conflated to 10 Hz; the DB is updated at most every 500 ms per transaction, not per chunk.
- Pause sets a flag and stops scheduling; in-flight requests finish; `.part` files remain.
- Cancel deletes `.part` files only after the user confirms; a cancelled install keeps its transaction row for “Retry”.

### 8.5 Database rules

- Writes happen on `eclipse-io`; reads that feed Compose use `Flow` from Room.
- A transaction never spans a suspending network call.
- Bulk insert uses `withTransaction`; 5,000 asset rows are inserted in batches of 500.
- The UI observes `instances` and `launch_sessions`, not the raw file system.

### 8.6 Backpressure

| Producer | Consumer | Strategy |
|---|---|---|
| HTTP chunks | digest + file | fixed 64 KiB buffers, no accumulation |
| Download completions | Room | batch 50 or 500 ms |
| Log socket | UI | `conflate()`, keep last 2,000 lines in memory |
| Install progress | UI | 10 Hz, plus a final exact value |
| Crash reports | exporter | one at a time, bounded queue |

### 8.7 Idempotency

- `launchId`, `transactionId` and `accountId` are generated before the first side effect.
- Every file write is `write temp → fsync → rename`.
- A repeated `commit` with the same transaction id is a no-op if the target exists and the journal says committed.
- A repeated processor run is allowed only into a fresh staging directory; outputs are verified, not trusted.

---

## 9. State machines

### 9.1 Instance

```
CREATING ──► READY ──► UPDATING ──► READY
    │          │  │
    │          │  └──► NEEDS_REPAIR ──► READY
    │          └──────► NEEDS_AUTH   ──► READY   (on successful re-auth)
    │          └──────► BROKEN       ──► NEEDS_REPAIR
    └──► FAILED (transaction rolled back; no instance row)
any ──► DELETING ──► (gone)
```

- `READY` requires a committed manifest and a passing `FAST` verify.
- `NEEDS_AUTH` is an account-level condition surfaced on the instance; it does not damage files.
- `BROKEN` requires a `failure_code`; the UI never shows it without a reason.
- `DELETING` is a transaction; user files are moved to `trash/<timestamp>/` for 7 days unless the user chooses “delete everything”.

### 9.2 Install transaction

```
RESOLVE → PLAN → PREFLIGHT → ACQUIRE → MATERIALISE → PATCH → VERIFY → COMMIT → GC
   │        │        │          │           │           │        │        │
   └────────┴────────┴──────────┴───────────┴───────────┴────────┴────────┴──► FAILED
                                                                              │
                                                                       rollback staging
```

- `FAILED` is terminal for that attempt; a retry creates a new attempt linked to the same transaction.
- `COMMIT` is the only stage that mutates the live instance directory.
- `GC` failure is logged and retried later; it never fails the install.

### 9.3 Launch session

```
PREPARING → VERIFYING → STARTING → RUNNING → EXITING → ENDED
     │           │           │         │
     └───────────┴───────────┴─────────┴──► FAILED
```

- `FAILED` carries a `failure_code` and a `stage`.
- `RUNNING` is set when the host reports `MAIN_ENTERED`, not when the activity starts.
- `ENDED` is written by the host before `_exit`, or by the reconciler after process death.

### 9.4 Account

```
SIGNED_OUT → DEVICE_CODE → POLLING → XBL → XSTS → MC_LOGIN → READY
                                                               │
READY ──► REFRESHING ──► READY                                 │
  │            └──────────────► REAUTH_REQUIRED ◄──────────────┘
  └──► SIGNED_OUT (user action)
```

- `DEVICE_CODE` survives process death only until `expires_in`; the code is persisted with its expiry, not the device code secret.
- `REAUTH_REQUIRED` preserves the account id, profile name and instances.

---

## 10. Error taxonomy

### 10.1 The failure type

```kotlin
data class Failure(
    val code: FailureCode,
    val stage: Stage,
    val retryable: Boolean,
    val userAction: UserAction,
    val messageKey: String,          // localized explanation
    val detail: String?,             // technical, monospace, copyable
    val cause: Throwable? = null,
    val context: Map<String, String> = emptyMap(), // redacted
)
```

- `code` is stable and documented; it never contains a message.
- `stage` is the same enum used by launch sessions and install transactions.
- `userAction` is exactly one of `RETRY`, `SIGN_IN`, `OPEN_SETTINGS`, `FREE_SPACE`, `REPAIR`, `SAFE_MODE`, `UPDATE_APP`, `CONTACT_SUPPORT`, `NONE`.
- `detail` is shown behind “Details”, never as the primary message.
- `context` is key/value, redacted, and safe to export.

### 10.2 Code families

| Prefix | Area | Examples |
|---|---|---|
| `NET_` | transport, DNS, TLS, HTTP | `NET_OFFLINE`, `NET_TLS`, `NET_429`, `NET_HOST_NOT_ALLOWED` |
| `META_` | Mojang/loader metadata | `META_MANIFEST_STALE`, `META_VERSION_HASH`, `META_RULE_NO_MATCH` |
| `FS_` | local filesystem | `FS_NO_SPACE`, `FS_READ_ONLY`, `FS_RENAME`, `FS_LOCKED` |
| `DL_` | download | `DL_DIGEST`, `DL_TRUNCATED`, `DL_RANGE_IGNORED`, `DL_CANCELLED` |
| `INSTALL_` | transaction | `INSTALL_PREFLIGHT`, `INSTALL_COMMIT`, `INSTALL_ROLLBACK`, `INSTALL_IN_PROGRESS` |
| `LOADER_` | loader profile/processors | `LOADER_UNSUPPORTED`, `LOADER_PROCESSOR_FAILED`, `LOADER_OUTPUT_MISMATCH` |
| `AUTH_` | Microsoft/Xbox/Minecraft | `AUTH_DEVICE_EXPIRED`, `AUTH_XSTS_CHILD`, `AUTH_NO_JAVA`, `AUTH_APPID` |
| `COMP_` | components | `COMP_MISSING`, `COMP_HASH`, `COMP_ABI`, `COMP_16KB`, `COMP_SIGNATURE` |
| `JVM_` | VM creation and main | `JVM_EEXIST`, `JVM_ENOMEM`, `JVM_EINVAL`, `JVM_MAIN_MISSING` |
| `GFX_` | renderer/probe | `GFX_NO_VULKAN`, `GFX_VULKAN_FEATURE`, `GFX_NO_GLES32`, `GFX_CONTEXT_LOST` |
| `GAME_` | game runtime | `GAME_CRASH_JAVA`, `GAME_CRASH_NATIVE`, `GAME_ANR`, `GAME_LMK`, `GAME_FORCED_EXIT` |
| `IPC_` | cross-process | `IPC_TICKET_EXPIRED`, `IPC_TICKET_INVALID`, `IPC_STATUS_LOST` |
| `POLICY_` | unsupported/forbidden | `POLICY_PIRACY`, `POLICY_OPTIFINE`, `POLICY_UNSUPPORTED_ABI` |

### 10.3 Mapping examples

| Signal | Code | Message | Action |
|---|---|---|---|
| `XErr 2148916238` | `AUTH_XSTS_CHILD` | “This is a child account. A parent must add it to a Microsoft Family.” | Open redirect URL |
| 403 `Invalid app registration` | `AUTH_APPID` | “This build is not approved by Mojang for sign-in.” | Contact support / update app |
| Entitlement empty | `AUTH_NO_JAVA` | “This account does not own Minecraft: Java Edition.” | Switch account |
| `JNI_ENOMEM` | `JVM_ENOMEM` | “Minecraft needs more memory than this device can give it.” | Lower heap / close apps |
| `SIGSEGV` in `libsandpaper.so` | `GFX_CONTEXT_LOST` or `GAME_CRASH_NATIVE` | “The graphics compatibility layer crashed during context creation.” | Safe mode (force Vulkan/disable mods) |
| `REASON_LOW_MEMORY` | `GAME_LMK` | “Android ended the game to free memory.” | Lower render distance / heap |
| Digest mismatch ×2 | `DL_DIGEST` | “A downloaded file did not match its hash.” | Retry / report |
| 16 KB check fails | `COMP_16KB` | “This component is not compatible with 16 KB page devices.” | Update component |

### 10.4 Exit codes

The host exits with a small, stable set so `ApplicationExitInfo` and the status file agree:

| Code | Meaning |
|---|---|
| 0 | normal exit after `Main.main` returned |
| 70 | host internal error |
| 71 | JVM creation failed |
| 72 | main class or main method missing |
| 73 | verified file missing at launch |
| 74 | ticket invalid/expired |
| 75 | SDL/window initialization failed |
| 137 | killed by `SIGKILL` (LMK or user) |
| 139 | `SIGSEGV` |

Java exceptions inside `Main.main` normally produce a HotSpot exit code of 1; the launcher reads the crash log and maps it to `GAME_CRASH_JAVA`, not to a raw “exit code 1”.

### 10.5 Presentation rules

- The primary message is one sentence, no stack trace.
- The technical detail is monospace, selectable, and copyable.
- The stage is always visible in a crash sheet.
- A retry button exists only when `retryable` is true.
- A failure that is a build/release bug (`AUTH_APPID`, `COMP_SIGNATURE`) says so, instead of blaming the user.

---

## 11. Security boundaries

### 11.1 Trust zones

| Zone | Trust | Holds | Can reach |
|---|---|---|---|
| `:app` | Trusted | Refresh token, account metadata, DB, network allowlist | Keystore, app files, network |
| `:game` | Semi-trusted | Minecraft, mods, access token (memory), launch ticket (brief) | Same UID files, network (game), GPU, audio |
| `:installer` | Semi-trusted | Loader processors, staging files | Staging, Maven allowlist, same UID files |
| Content | Untrusted | Mods, packs, configs, worlds | Whatever the game process can reach |
| Network | Untrusted | Mojang, Modrinth, Microsoft, component CDN | Only allowlisted hosts |

### 11.2 The honest limit: mods are not sandboxed

Minecraft mods run in the same Linux process and UID as the game. They can read the app's private files, including the encrypted refresh token, and can ask the Keystore to decrypt it because the key is app-scoped. **v1 does not claim otherwise.**

Mitigations that are real:

- The refresh token is encrypted at rest and never present in the plan, the Intent or a log.
- The Minecraft access token is short-lived (24 h) and passed only in memory.
- The launch ticket is one-shot and expires in 60 s.
- Mod installation shows a one-time notice: “Mods can access this app's files. Install mods you trust.”
- The diagnostic exporter never includes tokens.

Mitigations that are **not** claimed:

- “Mods cannot read your token.”
- “Safe mode sandboxes mods.”
- “The installer runs untrusted code safely.”

**Decision:** a true mod sandbox needs a separate UID or a separate app, which Android does not allow for the same package. It is a v2 research item, not a v1 promise. The privacy policy and the mod-install sheet state the limit.

### 11.3 Component trust

- Component manifests are signed with ECDSA P-256 / SHA-256. The app pins the public key(s) in `BuildConfig`.
- The signature covers the canonical JSON of the manifest, including every artifact digest and size.
- The app refuses a component whose signature, ABI, `minApi`, `maxPageSize` or required feature does not match.
- Key rotation: the index may carry up to two keys; the app trusts the pinned set. A revoked key is removed in an app update, not remotely.
- Compromise response: publish a new app with a new pin, revoke the old key, rebuild components.

**Why not Ed25519:** Android's platform provider support for Ed25519 is not guaranteed at `minSdk 26`; ECDSA P-256 is available everywhere and is sufficient for manifest signing. **Inference:** verify with the exact provider on API 26 and 36 in Phase 0.

### 11.4 Network

- HTTPS only, port 443, no userinfo.
- Host allowlist enforced after every redirect; max 5 hops.
- No user-installed CAs in release (`networkSecurityConfig` with `cleartextTrafficPermitted=false`).
- No certificate pinning for Mojang/Microsoft: pins break on legitimate certificate rotation and the update path is worse than the risk. Component downloads are hash- and signature-verified, which is stronger than pinning.
- Auth requests carry no client secret; the client id is public.
- User-Agent identifies the launcher and version, with a contact URL.

### 11.5 Android surface

| Item | Setting |
|---|---|
| Exported components | `MainActivity` only (launcher intent); everything else `android:exported="false"` |
| Deep links | None in v1 |
| File sharing | `FileProvider` with `grantUriPermissions=true`, explicit paths, one-shot read grants |
| Backup | `allowBackup=false`, `dataExtractionRules` exclude tokens, instances, DB |
| Permissions | `INTERNET`, `ACCESS_NETWORK_STATE`, optional `POST_NOTIFICATIONS`; `ACCESS_LOCAL_NETWORK` only when LAN play is used (API 37+) |
| `PendingIntent` | `FLAG_IMMUTABLE` everywhere |
| WebView | Not used |
| Debuggable | `false` in release |
| Screenshots | Allowed on normal screens; not restricted, because the device code must be shareable |
| Clipboard | Only on explicit tap; Android 13+ preview |

### 11.6 Threat table

| Threat | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Malicious mod steals refresh token | Medium | High | Same-UID limit documented; short-lived access token; ticket TTL; future separate-UID work |
| Malicious `.mrpack` path traversal | Medium | High | Path validation, symlink rejection, size/ratio caps, staging confinement |
| Malicious loader processor writes outside staging | Low | High | Only official loader installers; output path verification; no shell; separate process |
| MITM on downloads | Low | High | TLS, digest verification, signed component manifests |
| Token in log/crash report | Medium | High | Redaction filter at source and reader; tests grep logs for token patterns |
| Another app reads `/proc/<pid>/cmdline` | Low | Medium | Same-UID only; access token is 24 h; no refresh token in argv |
| Backup extracts tokens | Low | High | Backup disabled; extraction rules exclude |
| Fake component index | Low | High | ECDSA signature, pinned key, hash checks |
| Zip bomb | Medium | Medium | Entry/size/ratio caps, streaming extraction |
| Disk exhaustion | Medium | Medium | Preflight, quota, GC, “free space” action |

---

## 12. Observability

### 12.1 Principles

- Local-first. Nothing leaves the device unless the user exports it.
- Structured where it helps; plain text where a human reads it.
- Every log line has a timestamp, level, process, session id and code (when applicable).
- Redaction happens before the line is written, not only before upload.

### 12.2 Log files

| File | Writer | Content | Rotation |
|---|---|---|---|
| `logs/launcher.log` | `:app` | UI actions, installs, auth stages, failures | 5 × 4 MB |
| `<instance>/logs/latest.log` | Minecraft | Game log | Game-managed |
| `<instance>/logs/game-stdout.log` | host | stdout/stderr, JVM banner, native errors | 2 × 8 MB |
| `<instance>/logs/sandpaper.log` | Sandpaper | GL calls, shader rewrites, fallbacks | 2 × 4 MB |
| `<instance>/logs/host.log` | host | Stage transitions, dlopen, JVM creation | 1 MB |
| `<instance>/crash/hs_err_pid*.log` | HotSpot | Fatal error report | Keep last 5 |

### 12.3 Event schema

```json
{
  "ts": "2026-10-24T09:12:04.921Z",
  "level": "INFO",
  "proc": "game",
  "session": "5f0c…",
  "code": "LAUNCH_STAGE",
  "msg": "JVM created",
  "attrs": { "stage": "JVM_READY", "ms": 1184 }
}
```

- `attrs` values are strings, numbers or booleans; no nested objects in v1.
- No free-form user content in `attrs` except a file name the user chose (never a world seed, chat, or server address).
- `session` is the launch id or transaction id.

### 12.4 Metrics

Local counters, aggregated on device:

- launch success/failure by stage and device tier;
- install completion, retry count, bytes, duration;
- JVM creation time, heap settings, GC;
- frame time and renderer path (sampled, not continuous);
- crash counts by code.

They are shown in Diagnostics and included in an exported report only with consent. There is no analytics SDK and no background upload.

### 12.5 Crash pipeline

1. Native host installs a `SIGSEGV`/`SIGBUS`/`SIGABRT` handler that writes a **tombstone** using async-signal-safe calls only (`open`, `write`, `close`, `_exit`) and then chains to the previous handler.
2. HotSpot writes `hs_err_pid*.log` for JVM-internal faults.
3. The host writes `status.json` with the last stage.
4. `:app` reads `ApplicationExitInfo` on next start.
5. `CrashReportBuilder` correlates by `launchId` and time, and picks the most specific evidence.
6. The crash sheet shows stage, signal, library + offset, and a plain-language cause if a rule matches; otherwise it says “we could not determine the cause” and offers the report.

### 12.6 Redaction

Patterns removed or replaced with `[redacted]`:

- `accessToken`, `refresh_token`, `identityToken`, `Authorization: Bearer …`
- JWT-shaped strings (`eyJ…`)
- `XBL3.0 x=…`
- device codes in URL query strings
- account email (never requested, but redact defensively)
- server addresses? No — a server address is useful for support; but it is user data, so it is excluded from automatic reports and included only if the user ticks “include server address”.

A unit test feeds a fixture log containing every secret shape and asserts the output contains none of them.

### 12.7 Diagnostic bundle

`Export diagnostics` creates a ZIP with an explicit file list:

- `instance.json`, `manifest.json`, `plan.json` (secrets removed);
- logs and crash reports listed above;
- device info, ABI, API, RAM, GPU strings;
- renderer probe result and component versions;
- the last 50 structured events.

It never includes tokens, worlds, screenshots, or the account email. The user sees the list before sharing.

---

## 13. Component interfaces

### 13.1 Components and release cadence

| Component | Artifact | Cadence | Consumed by |
|---|---|---|---|
| EclipseJRE | `eclipsejre-25.<patch>-<abi>.tar.zst` | Java security updates | `:game`, `:installer` |
| EclipseNatives | `eclipsenatives-<lwjgl>-<abi>.tar.zst` | LWJGL/OpenAL/FreeType updates | `:game` |
| EclipseSDL3 | `libSDL3.so` + bridge classes | SDL updates | `:game` |
| Sandpaper | `libsandpaper.so` | App releases | `:game` |
| Foreman/Crowbar | jars in `installer.jar` | App releases | `:installer` |
| FFmpeg plugin | optional APK | Rare | media playback (not v1 core) |

EclipseJRE, EclipseNatives and EclipseSDL3 are separate repositories with their own CI and GitHub Releases. The app consumes a **signed index** and never a branch.

### 13.2 Component manifest

`BUILD-MANIFEST.json`:

```json
{
  "schema": 1,
  "component": "eclipsejre",
  "version": "25.0.4+4-eclipse.2",
  "abi": "arm64-v8a",
  "minApi": 26,
  "minPageAlign": 16384,
  "minLauncher": "1.0.0",
  "upstream": {
    "name": "OpenJDK",
    "version": "jdk-25.0.4+4",
    "url": "https://github.com/openjdk/jdk25u"
  },
  "features": ["java.desktop.headless", "jni", "jfr"],
  "artifacts": [
    {
      "path": "lib/server/libjvm.so",
      "sha256": "…",
      "size": 19387112,
      "mode": "0555",
      "elf": { "class": 64, "machine": "aarch64", "minPageAlign": 16384 }
    }
  ],
  "entrypoints": {
    "libjvm": "lib/server/libjvm.so",
    "javaHome": ".",
    "jli": "lib/libjli.so"
  },
  "licenses": [
    { "name": "GPL-2.0-with-classpath-exception", "path": "legal/GPL-2.0-CPE.txt" }
  ],
  "source": {
    "repo": "https://github.com/SouhardyaMaybe/EclipseJRE",
    "commit": "…",
    "build": "GitHub Actions run 123456"
  }
}
```

### 13.3 Index and signature

```json
{
  "schema": 1,
  "generatedAt": "2026-10-24T00:00:00Z",
  "keys": ["eclipse-2026"],
  "components": [
    {
      "component": "eclipsejre",
      "version": "25.0.4+4-eclipse.2",
      "abi": "arm64-v8a",
      "url": "https://github.com/SouhardyaMaybe/EclipseJRE/releases/download/…",
      "manifestSha256": "…",
      "signature": "base64…"
    }
  ]
}
```

- The app ships `component-index.json` + `component-index.sig` in assets, and may fetch a newer index from a pinned GitHub Releases URL.
- The index signature is verified with the pinned key; each component manifest is verified against `manifestSha256`; each artifact against its SHA-256.
- No component is installed from a URL that is not in a signed index, even in developer builds.

### 13.4 Install algorithm

```
1. select component/version/abi
2. fetch index (or use bundled), verify index signature
3. fetch manifest, verify sha256 + signature
4. check abi, minApi, minPageAlign, minLauncher, features
5. download archive to staging, verify archive sha256
6. extract with path/symlink/size checks into components/<name>-<version>-<abi>.tmp
7. verify every artifact digest, size, mode
8. check ELF class/machine and 16 KB `p_align` for every .so
9. fsync tree, rename to final, write component_installations row
10. GC old versions (keep current + previous)
```

The 16 KB check is fail-closed: a component with any `PT_LOAD` segment below 16 KB is rejected before it can be launched.

### 13.5 Version negotiation

- `minLauncher` is a semver floor; the app refuses older-than-required components? No: it refuses components that require a *newer* launcher.
- `features` are capability strings, not booleans scattered in code (`java.desktop.headless`, `jni`, `jfr`).
- The launcher never upgrades a component silently during a launch. Updates happen on the Components screen or before an install, with a size and changelog.

---

## 14. Build and release architecture

### 14.1 Toolchain

| Item | Pin |
|---|---|
| JDK for Gradle | 25 (Temurin) |
| Gradle | Wrapper, checksum-verified |
| AGP | 9.4.0 |
| Kotlin | 2.4.21 |
| Compose Material 3 | 1.4.0 |
| Navigation | Project-owned back stack (no Navigation 3 dependency) |
| NDK | r28c or newer |
| CMake | pinned in `build-logic` |
| `compileSdk` / `targetSdk` | 37 / 36 |
| `minSdk` | 26 |

Every version lives in `gradle/libs.versions.toml`; convention plugins in `build-logic/` apply compiler options, Compose, lint and test config. No module repeats a version.

### 14.2 Variants

| Variant | Purpose | Differences |
|---|---|---|
| `debug` | Local/CI smoke | Debuggable, mock auth base URL allowed, no minify |
| `release` | GitHub Releases | R8, resource shrink, baseline profile, signed, no mock hosts |
| `benchmark` | Macrobenchmark | `profileable`, not shipped |

ABI splits: `arm64-v8a` (shipped), `x86_64` (emulator/CI). `armeabi-v7a` is not built in v1.

### 14.3 Dependency hygiene

- Gradle dependency verification metadata (`verification-metadata.xml`) for every artifact.
- Version catalog only; no dynamic versions.
- No SNAPSHOT dependencies in a release build.
- `dependencyCheck` fails on a module that adds a new repository.
- Licenses are checked by `scripts/check-provenance.sh`; a new dependency requires a `PROVENANCE.md` row.

### 14.4 CI jobs

| Job | Trigger | Gate |
|---|---|---|
| `docs` | every push | `check-docs.py`, relative links, chapter index |
| `lint` | every push | Android lint, detekt/ktlint, dependency rules |
| `unit` | every push | JVM unit tests, golden plans, error mapping |
| `assemble` | every push | `assembleDebug` for arm64 + x86_64 |
| `native-audit` | every push touching natives | `readelf` 16 KB check, exported symbols, `pw_` prefix |
| `instrumented` | nightly/manual | API 36 emulator smoke: install, launch a stub main, crash, recover |
| `screenshot` | every PR touching UI | design-system screenshots (Paparazzi or Roborazzi, one only) |
| `release` | tag `v*` | signed APK, SHA-256, SBOM, provenance attestation, GitHub Release |

**Decision:** the repository never contains an APK, a `.so`, a JRE archive or a `client.jar`. CI builds or downloads them, verifies hashes, and attaches them to a Release.

### 14.5 Release artifacts

```
eclipselauncher-<version>-arm64-v8a.apk
eclipselauncher-<version>-x86_64.apk
eclipselauncher-<version>-mapping.txt
eclipselauncher-<version>-sbom.spdx.json
SHA256SUMS
component-index.json
component-index.sig
```

- The APK is signed with a release key held in GitHub Actions secrets; the certificate SHA-256 is published.
- `apksigner verify --print-certs` runs before upload.
- The release page includes the Minecraft disclaimer, the license, the source link, and the known-limitations section.
- Reproducibility: `SOURCE_DATE_EPOCH` from the commit, pinned JDK/NDK, no timestamps in generated resources. **Open:** R8 and AGP reproducibility is not guaranteed; the CI records the build inputs and a byte-for-byte rebuild check is a stretch goal.

### 14.6 Versioning

- App: semver `MAJOR.MINOR.PATCH`, with `-beta.N` pre-releases.
- Components: upstream version + `-eclipse.N`.
- Plan schema, status schema, manifest schema: independent integers.
- A change to `LaunchPlan` or the component manifest requires a schema bump and a migration note.

---

## 15. Degradation matrix

| Condition | What still works | What the user sees |
|---|---|---|
| Offline, valid token | Browse, logs, launch | “Offline — using the last verified files” |
| Offline, expired token | Browse, logs | “Sign in to play”; no fake offline auth |
| Low storage | Browse, delete, export | Exact shortfall and a “Free space” action |
| No Vulkan 1.2 | Sandpaper GL path | “Vulkan is not available on this device; using compatibility mode” |
| No GLES 3.2 | Nothing playable | “This device cannot run 26.3” with the missing capability |
| 4 KB-only JRE/native | Refuse to launch | “Component is not 16 KB compatible; update it” |
| UI process killed | Game continues | On return, re-attach from status file |
| Game process killed | UI continues | Crash sheet with `LMK`/`SIGNALED` |
| Installer process killed | Transaction resumes | “Install paused — tap to resume” |
| Component signature invalid | Refuse install/launch | “Component verification failed” with the component and hash |
| Mod crashes the game | UI continues | Safe mode offered if it crashed twice in 10 minutes |

---

## 16. Architecture decision records

| ADR | Decision | Status |
|---|---|---|
| 001 | Two processes for UI and game | Accepted (doc 04/05) |
| 002 | Two VMs in `:game`; no object crossing | Accepted (doc 05) |
| 003 | `JNI_CreateJavaVM`, not `JLI_Launch` | Accepted (doc 05) |
| 004 | `extractNativeLibs=true` | Accepted (doc 04/05) |
| 005 | Sandpaper as the only GL fallback; system drivers only | Accepted (doc 05) |
| 006 | Device-code auth, `consumers` tenant, own Entra client | Accepted (doc 06) |
| 007 | Content-addressed global store; filesystem is truth | Accepted (doc 07) |
| 008 | Room as an index, not the source of truth | **Proposed** |
| 009 | `:installer` is a visible activity process, not a background FGS | **Proposed** |
| 010 | Launch ticket file instead of a Binder token service | **Proposed** |
| 011 | Manual DI via `AppContainer`, not Hilt | **Proposed** |
| 012 | ECDSA P-256 component signatures | **Proposed** |
| 013 | No analytics SDK; local metrics only | Accepted (doc 08) |
| 014 | Kotlinx.serialization JSON for contracts, strict for ours | **Proposed** |
| 015 | One `libeclipse_host.so` for game and installer | **Proposed** |

---

## 17. Test architecture

### 17.1 Layers

| Layer | Runtime | What it proves |
|---|---|---|
| Unit | JVM | Rules, Maven coordinates, plan building, error mapping, redaction |
| Contract | JVM + MockWebServer | HTTP shapes, retries, digesting, allowlist, auth error matrix |
| Storage | JVM temp dirs | Atomic write, commit/rollback, lock staleness, GC refcounts |
| Database | Robolectric/instrumented | Migrations, reconciliation, cascade deletes |
| Native | Device/emulator | `dlopen`, JVM creation, status writes, signal chaining |
| End-to-end | API 36 emulator | Install stub → launch stub main → exit → crash → recover |
| Device matrix | Physical | JVM 25, SDL3, Vulkan/Sandpaper, thermals, 16 KB future device |
| Screenshot | JVM/Robolectric | Design-system visual contract |
| Fuzz | JVM | `.mrpack` parser, manifest parser, path validator |

### 17.2 Golden fixtures

Commit, under `fixtures/`:

- `26.3-version.json` (upstream, immutable);
- `26.3-asset-index.json` (upstream);
- `fabric-0.20.1-26.3-profile.json`;
- `26.3-vanilla-plan.json`, `26.3-fabric-plan.json` with exact classpath and args;
- `mrpack-minimal/`, `mrpack-traversal/`, `mrpack-bomb/`;
- `logs/with-secrets.log` for the redaction test;
- `components/index.json` + signature fixtures (test key only).

A dependency or rule change that alters a golden plan is a deliberate diff with a review note, not an accidental test update.

### 17.3 What CI cannot prove

- GPU driver correctness on a specific phone.
- 16 KB behaviour without a 16 KB device or emulator image.
- Microsoft AppID approval.
- Thermal throttling over 30 minutes.
- Mod compatibility across the long tail.

These are device-lab and beta gates, stated as such in the release checklist.

---

## 18. Decisions for sign-off

1. **Modular monolith with the module graph in §3**, collapsing features into `:app` for a small team.
2. **Three processes: `:app`, `:game`, `:installer`; `GameActivity` and `InstallerActivity` are visible activities, no game/install foreground service.**
3. **One `libeclipse_host.so` and one JVM-creation path shared by game and installer.**
4. **No Java object crosses VMs; the only secrets that cross are in a one-shot encrypted ticket.**
5. **Filesystem is the source of truth; Room is a rebuildable index.**
6. **Structured failure taxonomy with stable codes and exactly one user action.**
7. **Mods are not sandboxed in v1; the limit is documented in the app, the privacy policy and the README.**
8. **Components are signed ECDSA P-256 manifests with pinned keys and fail-closed 16 KB checks.**
9. **Local-only observability; opt-in diagnostic export; no analytics SDK.**
10. **GitHub Actions only; no binaries in git; signed releases with SBOM and SHA-256.**
11. **Manual DI via `AppContainer` for v1; revisit Hilt only if the graph becomes unmanageable.**
12. **The 12 invariants in §1.1 are review gates.**

## 19. Open questions

- Does abstract-namespace `LocalSocket` work on every target API/SELinux combination, or is a filesystem socket the safer default?
- Does `FileChannel.tryLock` behave across processes on the emulated external volume, or must the advisory lock be the only mechanism?
- Can `:installer` keep a visible activity alive reliably on Android 16/17 while the user switches away, or does it need a `dataSync`/`specialUse` service after all?
- Is ECDSA P-256 verification available with the same provider behaviour on API 26 and API 36, or does the app need a bundled provider?
- Does `ApplicationExitInfo` report a native crash in a secondary process with enough detail to name the library, or only a signal?
- How large is `libsandpaper.so`, and does it stay in the APK or become a component?
- Is a single `:installer` process enough for a 300-mod pack, or does the processor need a larger heap than a phone can give?
- What is the smallest module graph that still enforces the invariants for a solo developer?
- Do we need a `:core:telemetry` module at all, or can local metrics live in `:app` without becoming a backdoor analytics layer?
- Can the launch ticket be avoided entirely by passing the token through a `ParcelFileDescriptor` pipe with `O_CLOEXEC`, and is that meaningfully safer than a 60-second file?
