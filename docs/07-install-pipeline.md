# 07 — Install pipeline

**Status:** architecture draft for sign-off, 2026-10-24.
**Scope:** how Eclipse Launcher turns a version id, a loader choice and optional modpack into a verified, launchable instance: metadata resolution, library/native selection, asset download, transactional materialisation, repair, and import/export.
**Evidence:** the live `26.3` version JSON and asset index, Fabric and Quilt meta responses fetched 2026-10-24, the NeoForge Maven metadata, the `.mrpack` format specification, and Android storage behaviour from doc 04.
**Labels:** **Verified**, **Observed**, **Inference**, **Open**.

---

## 1. The contract in one page

| Layer | Decision | Why |
|---|---|---|
| Metadata | Mojang `version_manifest_v2.json` → per-version JSON → asset index | Official, versioned, SHA-1 anchored |
| Library selection | Evaluate Mojang rules for `linux`/`arm64`, then **replace every native artifact** with an EclipseNatives build | Mojang ships x86_64 glibc natives; none load on Android |
| Java libraries | Download from Maven exactly as the JSON says | Pure JVM code is portable; do not re-host |
| Client | Download `client.jar` from `piston-data.mojang.com`, verify SHA-1 | Required; never redistribute |
| Assets | Content-addressed `objects/<2>/<hash>`, shared across instances | 5,147 unique objects in 26.3; no duplication |
| Loader | Fabric/Quilt by profile merge; NeoForge/Forge by installer processors | Different mechanisms, different risk |
| Modpacks | Modrinth `.mrpack` v1 only in v1; resolve files through Modrinth's API and CDN | Tight host allowlist; hash-checked |
| Commit | Stage → fsync → journal → atomic rename | A crash never leaves a half-installed instance |
| Repair | Manifest-driven re-download; quarantine corrupt files | Never silently run a damaged jar |
| Dedup | Global object/library store, instance manifests reference hashes | Several instances share one 485 MB asset set |
| Deletion | Refcounted GC, 7-day grace, never while locked | No “disk full because old instances” |
| Integrity | SHA-1 for Mojang, SHA-512 for Modrinth, SHA-256 for our components | The hash is the contract, not the URL |

**Baseline measured from 26.3:**

| Item | Size |
|---|---|
| `client.jar` (`e877b6a0…`) | 41,483,720 B / 39.6 MiB |
| Java libraries (55 artifacts) | 69.6 MiB |
| Mojang Linux x86_64 natives (11 artifacts, discarded) | 9.9 MiB |
| Asset index `34` | 597,035 B, 5,147 objects, all hashes unique |
| Assets | 484,894,038 B / 462.4 MiB |
| **Vanilla total before JRE** | **~581.5 MiB** |
| EclipseJRE 25 (jlink target) | ~50–75 MiB compressed, 60–90 MiB installed |
| First-run total | ~650–700 MiB |

---

## 2. Metadata resolution

### 2.1 The manifest

```
GET https://piston-meta.mojang.com/mc/game/version_manifest_v2.json
```

Each entry has `id`, `type`, `url`, `time`, `releaseTime`, `sha1` and `complianceLevel`. The `latest` object names the current `release` and `snapshot`.

Rules:

- Cache the manifest for 10 minutes; force refresh when the user pulls to refresh or a version lookup misses.
- Fetch a version JSON only after verifying its SHA-1 against the manifest entry.
- Keep `release` and `snapshot` in the normal UI; put `old_beta`/`old_alpha` behind a developer toggle.
- A version JSON is immutable in practice, but cache it by SHA-1, not by id, so a re-publish cannot poison the cache.

**Verified:** 26.3 is `release`, published 2026-09-15; 26.4-snapshot-5 is the current snapshot at the research date.

### 2.2 What the version JSON gives us

| Field | Use |
|---|---|
| `downloads.client` | URL, SHA-1, size of `client.jar` |
| `libraries[]` | Maven coordinates, rules, artifact/classifier downloads, `extract` |
| `assetIndex` | Index id, URL, SHA-1, size, `totalSize` |
| `assets` | Asset index id (26.3: `34`) |
| `mainClass` | `net.minecraft.client.main.Main` |
| `arguments.jvm` / `arguments.game` | Argument templates with rules and `${macros}` |
| `logging.client` | Log4j config URL, SHA-1, `-Dlog4j.configurationFile=${path}` |
| `javaVersion` | `java-runtime-epsilon`, major 25 |
| `complianceLevel` | 1; informational |
| `minimumLauncherVersion` | 21; we are not a legacy launcher |

Older versions may use the string `minecraftArguments` instead of `arguments`. The planner normalises both into one `LaunchPlan`; 26.3 uses the modern form.

### 2.3 Rules evaluation

A rule list is evaluated in order. A rule matches when:

- its `os.name` is absent or equals `linux`;
- its `os.arch` is absent or in our accepted set (`arm64`, `aarch64`; `x86_64` for the emulator);
- every entry in `features` matches the launch context (`is_demo_user`, `has_custom_resolution`, quick-play flags).

A library with **no rules** is allowed. Once a rule list is present, the last matching rule's `action` wins, and **no match means disallow**; that is how Mojang marks the Windows, macOS and Linux variants of the same artifact. Argument objects also default to disallow when no rule matches. Implementations that treat a present-but-unmatched rule list as allow put the macOS Objective-C bridge and Windows natives on Android; implementations that treat an empty rule list as deny silently drop every ordinary library. Both bugs exist in the wild.

**Verified:** the rule semantics are the launcher contract described by the version JSON and the Minecraft Wiki.

### 2.4 Native substitution

26.3 selects 11 native artifacts for Linux x86_64 (`jtracy`, `lwjgl`, `lwjgl-opengl`, `lwjgl-stb`, `lwjgl-vma`, `lwjgl-jemalloc`, `lwjgl-openal`, `lwjgl-sdl`, `lwjgl-shaderc`, `lwjgl-spvc`, `lwjgl-freetype`). None of them is loadable on Android:

- they are ELF x86_64, not AArch64;
- they link glibc, not bionic;
- `libSDL3.so` expects a desktop video backend;
- `libopenal.so` expects ALSA/PulseAudio.

The planner therefore **drops every `:natives-*` artifact** and records a substitution:

| Mojang native | EclipseNatives replacement | Notes |
|---|---|---|
| `lwjgl` | `liblwjgl.so` 3.4.3 | JNI core, memory, library loading |
| `lwjgl-opengl` | `liblwjgl_opengl.so` 3.4.3 | GL JNI support |
| `lwjgl-stb` | `liblwjgl_stb.so` 3.4.3 | Image/font decode |
| `lwjgl-vma` | `liblwjgl_vma.so` 3.4.3 | Vulkan allocator |
| `lwjgl-jemalloc` | `libjemalloc.so` 3.4.3 | Allocator; optional but keep parity |
| `lwjgl-sdl` | **EclipseSDL3 3.4.16** `libSDL3.so` | Must export the same SDL 3.4 ABI |
| `lwjgl-openal` | official OpenAL Soft `7f9c6de`, OpenSL | No Oboe fork |
| `lwjgl-freetype` | official FreeType 2.14.3 | `0a0221a` |
| `lwjgl-shaderc` | official Shaderc v2026.3 | `2c8cae7` |
| `lwjgl-spvc` | official SPIRV-Cross `6c09849` | GLSL emission |
| `jtracy` | **dropped** | Profiling only; no Android build in the official set |

The Java artifacts for those modules are still downloaded; only the `.so` payload is replaced. `-Dorg.lwjgl.librarypath=<nativeLibraryDir>` makes LWJGL resolve the replacement by name.

**Inference:** 26.3 does not call Tracy unless a debug flag is set; dropping `jtracy` is safe for normal play. Verify no `NoClassDefFoundError` appears in the first 10 minutes of a profiler-free session.

### 2.5 Asset index

`assetIndex.url` returns a JSON map:

```json
{
  "objects": {
    "icons/icon_16x16.png": {
      "hash": "5ff04807c356f1beed0b86ccf659b44b9983e3fa",
      "size": 781
    }
  }
}
```

- Download from `https://resources.download.minecraft.net/<first two hex>/<hash>`.
- Store at `<assets>/objects/<first two hex>/<hash>`.
- Write `<assets>/indexes/<id>.json` exactly as fetched.
- 26.3: 5,147 objects, 484,894,038 B, all hashes unique. **Observed.**
- Legacy `virtual`/`resources` layouts are only needed for pre-1.7 versions; not in v1.

### 2.6 Logging config

The `logging.client` block supplies a Log4j XML file and the JVM argument `-Dlog4j.configurationFile=${path}`. Download and verify it, rewrite `${path}` to the absolute local path, and keep the file under the shared store. If it is missing, Minecraft still starts with a default logger, but the launcher loses the structured log file. Treat it as required.

---

## 3. Loader resolution

### 3.1 Fabric (v1)

```
GET https://meta.fabricmc.net/v2/versions/loader/26.3
GET https://meta.fabricmc.net/v2/versions/loader/26.3/<loader>/profile/json
```

**Observed 2026-10-24:** 255 loader builds for 26.3; latest stable `0.20.1`; profile id `fabric-loader-0.20.1-26.3`; main class `net.fabricmc.loader.impl.launch.knot.KnotClient`; 7 libraries; JVM arg `-DFabricMcEmu= net.minecraft.client.main.Main`.

Merge rules:

1. Start from the parent 26.3 plan.
2. Append Fabric libraries, deduplicating by `group:artifact` with the child winning.
3. Append child JVM/game arguments after the parent's, preserving order.
4. Override `mainClass` with the loader's.
5. Keep the parent's asset index and logging config.
6. Pin the loader version in `instance.json`; do not silently upgrade on repair.

Intermediary is reported as `0.0.0` for 26.3 because the game is unobfuscated from 26.1 onward. Do not add a remap step that has nothing to remap.

### 3.2 Quilt (v1.1)

```
GET https://meta.quiltmc.org/v3/versions/loader/26.3/<loader>/profile/json
```

**Observed:** latest `0.20.0-beta.9`; main class `org.quiltmc.loader.impl.launch.knot.KnotClient`; 12 libraries; `arguments.game` empty. Same merge algorithm. Quilt is out of v1 scope per doc 03, but the planner is loader-agnostic and the profile merge is shared.

### 3.3 NeoForge (v1.1)

NeoForge publishes installer artifacts under `net.neoforged:neoforge` on `maven.neoforged.net`. **Observed:** the newest 26.3 line is `26.3.0.97` (2026-10-24).

Unlike Fabric, NeoForge needs **binary patch processors**:

- `install_profile.json` lists `data` entries, `processors[]` (main class, classpath, args, outputs) and libraries.
- `version.json` (or the installer's `version` entry) carries the launch profile.
- Processors are Java programs that must run with a JVM, not with ART.

**Decision:** run processors in a dedicated `:installer` process using EclipseJRE 25, a restricted classpath, `-Djava.awt.headless=true`, no network except the Maven allowlist, and a staging directory. The UI process never loads installer code.

Why a separate process:

- a malicious or broken processor cannot crash the UI;
- the JVM can be created once and exited without contaminating `:game`;
- the installer's file writes are confined and auditable;
- the same runner serves Forge import and future loaders.

Outputs are verified against the installer's `outputs` map before the instance is committed.

### 3.4 Forge (v1.1, import-only)

Forge uses the same processor model with a different profile and, on modern versions, `data/client.lzma` binary patches. v1 does not offer a “create Forge instance” button; it imports an existing Forge instance or a CurseForge pack and runs **Foreman** only if the profile is complete and the processor outputs are reproducible. Anything that requires downloading Forge's ad-hoc installer UI is out of scope.

### 3.5 OptiFine

Unsupported. It is proprietary, its installer mutates the client jar, and its licensing does not permit redistribution. The launcher detects an OptiFine profile and explains why it will not launch it. It does not silently strip it and pretend.

---

## 4. Modrinth modpacks (v1)

### 4.1 Format

A `.mrpack` is a ZIP with `modrinth.index.json` at the root:

```json
{
  "formatVersion": 1,
  "game": "minecraft",
  "versionId": "2.1.0",
  "name": "Example Pack",
  "summary": "…",
  "files": [
    {
      "path": "mods/example.jar",
      "hashes": { "sha1": "…", "sha512": "…" },
      "env": { "client": "required", "server": "unsupported" },
      "downloads": ["https://cdn.modrinth.com/data/…"],
      "fileSize": 123456
    }
  ],
  "dependencies": {
    "minecraft": "26.3",
    "fabric-loader": "0.20.1"
  }
}
```

Plus an optional `overrides/` directory copied into the instance root.

Validation:

- `formatVersion` must be `1`; `game` must be `minecraft`.
- `dependencies.minecraft` must be an exact version id we support, not a range.
- Exactly one loader dependency (`fabric-loader`, `quilt-loader`, `neoforge`, or `forge`). Multiple loaders are a hard error.
- `files[].path` must be a relative path under the instance, with no `..`, no absolute prefix, no backslash, no NUL.
- `env.client == "unsupported"` files are skipped; `optional` files are shown in a checklist; `required` files are mandatory.
- The declared `fileSize` must match the downloaded size, and SHA-512 must match; SHA-1 is a fallback only when SHA-512 is absent.

### 4.2 Resolve through Modrinth, not arbitrary URLs

A manifest can contain any download URL. Trusting it turns the launcher into an open downloader. Instead:

1. For each file, query Modrinth by SHA-512 (`/v2/version_file/<hash>?algorithm=sha512`).
2. Use the returned version's official CDN URL for that hash.
3. Download only from `cdn.modrinth.com`.
4. If Modrinth does not know the hash, fail that file with “This pack contains a file that is not hosted on Modrinth” and list it. Do not fall back to the manifest URL.

This is stricter than the format requires, and it is the right trade for a launcher that handles untrusted packs. It also gives us the project/version metadata for the UI.

**Inference:** some packs bundle a file that is not on Modrinth (a private build, a GitHub release). Those packs will not install in v1. The error must name the file and suggest asking the pack author to publish it on Modrinth.

### 4.3 Overrides

- Extract `overrides/` **after** the declared files, so a pack can replace a default config.
- Refuse symlinks, absolute paths, `..`, and entries above the size/ratio caps.
- If an override would overwrite an existing user file, move the old file to `overrides-backup/<timestamp>/` and record it.
- Never let an override write outside the instance root.

### 4.4 Updates

- Keep the pack's `versionId` and `projectId` in `instance.json`.
- An update is a new transactional install into a staging instance, then a switch; the old instance stays until the user confirms.
- User files (saves, options, screenshots) are copied forward; pack-managed files are replaced.
- A downgrade is allowed but warns about world format changes.

---

## 5. The install transaction

### 5.1 Stages

```
RESOLVE → PLAN → PREFLIGHT → ACQUIRE → MATERIALISE → PATCH → VERIFY → COMMIT → GC
```

| Stage | Output | Failure leaves |
|---|---|---|
| RESOLVE | version JSON, loader profile, asset index, component manifest | cache only |
| PLAN | immutable `LaunchPlan` + file list + estimated bytes | nothing on disk |
| PREFLIGHT | free-space and network checks | nothing |
| ACQUIRE | verified objects in the shared store, `.part` for incomplete | resumable `.part` |
| MATERIALISE | instance tree, manifests, config templates | staging dir |
| PATCH | loader processors, renames, overrides | staging dir |
| VERIFY | full hash pass + launch-plan sanity | staging dir |
| COMMIT | journal + atomic rename + instance registry entry | old instance intact |
| GC | unreferenced objects removed | store consistent |

### 5.2 The `LaunchPlan`

The planner emits a deterministic JSON document:

```json
{
  "schema": 1,
  "instanceId": "…",
  "minecraft": "26.3",
  "loader": { "type": "fabric", "version": "0.20.1" },
  "mainClass": "net.fabricmc.loader.impl.launch.knot.KnotClient",
  "classpath": ["/…/client.jar", "/…/asm-9.10.1.jar", "…"],
  "jvmArgs": ["-Xms512M", "…"],
  "gameArgs": ["--username", "…", "…"],
  "assetsDir": "/…/assets",
  "gameDir": "/…/instances/…/game",
  "nativeDir": "/…/lib/arm64",
  "javaHome": "/…/runtimes/eclipsejre-25…",
  "env": { "ALSOFT_DRIVERS": "opensl" },
  "planHash": "sha256:…"
}
```

- `planHash` is SHA-256 over the length-prefixed plan fields: the version and every download's kind, URL, SHA-1, size and destination. Two identical installs produce the same hash, and secrets are never in the plan.
- The install plan is written into the committed instance tree and re-read by repair and launch; the game process never re-resolves metadata. A pending plan is held in memory while a download runs, not yet persisted across process death.
- Access tokens are **not** in the plan file. They are injected at launch from the account vault into the argument vector in memory.

### 5.3 Preflight

- Required bytes = sum of missing objects + 15% scratch + 512 MiB headroom.
- `StatFs` on the target volume; if short, show the exact shortfall and offer to free space or pick another volume.
- Check ABI: `Build.SUPPORTED_ABIS` must contain `arm64-v8a` (or `x86_64` on the emulator).
- Check the runtime component: installed, hash-valid, 16 KB ELF gate passed.
- Check the renderer: Vulkan probe or GLES 3.2; warn before a 650 MB download if the device cannot run the game.
- Check the account: signed in, Java entitlement present.

The last two checks are product decisions: it is cheaper to say “this device cannot run 26.3” before the download than after it.

**Implementation note (2026-10-25):** the Android coordinator checks the selected version exists in the official manifest, rejects a device whose `SUPPORTED_ABIS` has neither `arm64-v8a` nor `x86_64`, and runs the space contract before the first download. `ArtifactStore.missingBytes` counts absent or wrong-sized objects with a fast existence/size check, `InstallPreflight` adds the 15% scratch and 512 MiB headroom, and a shortfall becomes `STORAGE_NOT_ENOUGH_SPACE` with `FREE_SPACE` as the offered action. Runtime and account preflight remain open because this build has no runtime or online account. A missing file or a failed request becomes a task failure with a stable code instead of a silent partial install.

### 5.4 Acquire

- One HTTP client with a fixed User-Agent, HTTP/2, a 6-connection global semaphore, and a 4-connection per-host semaphore.
- Per-object work item: URL, expected digest, expected size, destination.
- `Range: bytes=<n>-` with `If-Range` on the stored ETag/Last-Modified; if the server ignores ranges, restart that object.
- Write to `<dest>.part`; hash while streaming; on completion verify the digest, fsync, then rename.
- A digest mismatch quarantines the file (`.bad`), re-downloads once, then fails the stage with the URL and both hashes.
- 429/503 honour `Retry-After`; 5xx retries with exponential backoff and jitter; 404 refreshes metadata once.
- A 200 response to a range request (server ignored it) truncates and restarts.
- Cancel stops scheduling, lets in-flight requests finish, and keeps `.part` files for resume.
- Pause is a first-class state, not a cancelled install.

**Implementation note (2026-10-25):** the Android coordinator now runs the whole Vanilla path: fetch the official manifest, version JSON and asset index; resolve the Linux/arm64 (or x86_64) launch context; build the deterministic plan; execute it through the strict HTTP source; then commit through `InstanceTransaction`. The Tasks dialog shows the real phase, completed files and bytes, and drives pause, resume, retry and cancel. Pause is polled between 64 KiB chunks, so a large client jar stops promptly. Resume reuses the in-memory plan and skips every object already verified in the store; after process death the coordinator re-fetches metadata and the store still skips verified objects. The store and executor remain sequential, delete the `.part` on pause, and do not retry a digest mismatch. Range requests, `.part` retention, the 6-connection scheduler and digest-mismatch retry remain open.

**Observed:** 26.3's assets are 5,147 small objects. Per-request overhead dominates; batching is impossible (one hash per object), so concurrency and connection reuse matter more than raw bandwidth.

### 5.5 Materialise

- Create the instance skeleton from a template: `game/`, `game/mods/`, `game/config/`, `game/resourcepacks/`, `game/shaderpacks/`, `game/saves/`, `logs/`, `cache/`, `crash/`.
- Write `instance.json` with the plan hash, loader, component versions, pack metadata and creation time.
- Write `manifest.json`: every managed file with its store digest, size and origin. User files are not listed.
- Write `options.txt` only if absent; never overwrite a user's settings on repair.
- Create `servers.dat` only if absent.
- Set `eula.txt`? The client does not require it; do not fabricate consent.

**Implementation note (2026-10-25):** `InstanceTransaction` writes the skeleton and the three JSON documents into `.staging/<txid>`, fsyncs them, and atomically renames the tree. Before the rename it hard-links the existing `game/` tree into staging (copy fallback; symlinks skipped), so a reinstall or version change preserves worlds, options, screenshots and manually added mods. The app creates `datapacks/` and `modpacks/` on first use. Free-space preflight and the `options.txt`/`servers.dat` creation rules remain open.

### 5.6 Commit

```
<instances>/.staging/<txid>/         complete tree
<instances>/<id>/                    absent or old version
journal.json                         {txid, id, state:"prepared"}
```

Commit sequence:

1. fsync the staging tree (files and directories).
2. write `journal.json` with `state:"committing"`.
3. `rename(staging, <id>)` — atomic within the same filesystem.
4. update the instance registry (Room/SQLite) in a transaction.
5. write `state:"committed"`, then delete the journal.

On startup, a `committing` journal with a missing final directory means the rename did not happen: redo it or roll back to the old directory. A `prepared` journal is discarded. The registry reconciles against the filesystem; the filesystem is the source of truth for what exists.

### 5.7 Verify

| Level | What it checks | When |
|---|---|---|
| `FAST` | every managed path exists and size matches | after commit, before every launch |
| `FULL` | every managed file matches its digest | first install, after a crash, on demand |
| `LAUNCH` | main class present, classpath files readable, native dir populated, asset index present | before every launch |

A `FULL` pass over 5,147 assets on a midrange phone takes tens of seconds; do not run it on every start. `FAST` is a stat storm but tolerable; cache the result for the session.

### 5.8 Garbage collection

- Refcount = union of all instance manifests + in-flight installs + the last known good plan.
- Delete unreferenced objects older than 7 days.
- Never delete an object while an install or a launch holds a lock.
- Run GC after a successful uninstall, on idle charging, or when free space is low.
- Report reclaimed bytes; never silently delete a world or a user file.

**Implementation note (2026-10-25):** `StoreGc` reads every committed instance manifest, unions those paths with caller-pinned in-flight plan paths, and walks only `versions/`, `libraries/`, `assets/` and `logging/`. It deletes unreferenced regular files older than seven days, removes stale `.part` and `.bad` files on the same rule, prunes empty directories, skips symbolic links, and stops without deleting when the caller's install/launch lock is held. A malformed, missing or unreadable manifest aborts the pass with `STORAGE_GC_REFERENCES_INVALID`; it never deletes with an incomplete reference set. Wiring the collector to the Android lock and to uninstall is still open.

---

## 6. Storage layout

```
files/                                  (internal, backed up: never)
  runtimes/eclipsejre-25.0.4-arm64/     read-only JRE, 0444 .so
  accounts/…                            Keystore-encrypted tokens
  installs.db                           instance registry
  logs/

<external-files>/                       (app-specific external)
  instances/<id>/
    instance.json
    manifest.json
    game/                               cwd: saves, options, mods, resourcepacks
    logs/
    cache/
    crash/
  shared/
    objects/<2>/<sha1>                  Mojang assets and libraries
    assets/indexes/34.json
    assets/objects/<2>/<sha1>           hard-linked or referenced from objects
    libraries/<maven path>              Java artifacts
    mods/<sha512>.jar                   Modrinth content
  staging/<txid>/
```

Why this split:

- the JRE contains executable code and must live on internal storage, where `dlopen` is permitted and the path is stable;
- instances and assets are large and benefit from app-specific external storage, which is still private to the app but visible over USB/SAF for backup;
- `cache/` is evictable by the system; no managed file lives only there.

**Open:** whether the emulated external volume supports hard links on the target API levels. If not, `assets/objects` is the single copy and the index points at it; no duplication is needed because Minecraft reads `assets/objects/<2>/<hash>` directly. Libraries are referenced by absolute classpath, so they also need no per-instance copy.

---

## 7. Repair and diagnostics

### 7.1 What repair does

1. Read `manifest.json` and the plan.
2. For each managed file: missing → re-download; size mismatch → re-download; hash mismatch (FULL) → quarantine and re-download.
3. Re-run loader processors only if their outputs are missing or the loader version changed.
4. Rebuild `instance.json` if it is corrupt, from the manifest.
5. Never touch `game/saves`, `game/options.txt`, `game/mods` files the user added, or screenshots.

### 7.2 What repair does not do

- It does not “upgrade” Minecraft or the loader.
- It does not delete mods to make the game start; safe mode moves them aside and says so.
- It does not re-download the JRE silently; it offers the component repair with a size.

### 7.3 Diagnostic bundle

`Export diagnostics` writes a ZIP with:

- `instance.json`, `manifest.json`, `plan.json` (secrets removed);
- `logs/latest.log`, `logs/game-stdout.log`, `logs/sandpaper.log`;
- `crash-report.json`, `hs_err_pid*.log` if present;
- device info, ABI, API, RAM, GPU strings, renderer probe;
- component versions and hashes;
- **no** account tokens, no world files, no screenshots.

The user sees the file list before sharing.

---

## 8. Import and export

### 8.1 Instance export

`.eclipseinstance` is a ZIP containing:

```
eclipse-instance.json      schema, name, MC version, loader, component locks
game/                      saves, options, config, mods, resourcepacks (user content)
manifest.json              managed files are excluded; hashes let the importer rebuild them
```

- Exclude `client.jar`, libraries, assets and the JRE; they are reproducible.
- Cap the archive at a size the user confirms; warn about worlds with large region files.
- Import validates paths and refuses symlinks.

### 8.2 Foreign imports

| Source | v1 | Notes |
|---|---|---|
| `.mrpack` | ✅ | Native path |
| Eclipse `.eclipseinstance` | ✅ | Round-trip |
| Prism/MultiMC instance | ✅ (basic) | `instance.cfg`, `mmc-pack.json`, `minecraft/` |
| CurseForge `.zip` | ❌ v1 | Requires the CurseForge API and key; v1.1 |
| ATLauncher/Technic | ❌ | Different metadata; not planned |
| Plain `.minecraft` folder | ✅ (copy) | Detect `versions/`, `mods/`, `saves/`; no loader inference |

Import never trusts the source's `client.jar` or libraries; it re-resolves from official metadata and hashes.

---

## 9. Security

### 9.1 URL allowlist

Only these hosts, checked after every redirect:

```
piston-meta.mojang.com
piston-data.mojang.com
resources.download.minecraft.net
libraries.minecraft.net
maven.fabricmc.net
meta.fabricmc.net
maven.quiltmc.org
meta.quiltmc.org
maven.neoforged.net
api.modrinth.com
cdn.modrinth.com
api.github.com
objects.githubusercontent.com
github.com            (release assets only, via the API-resolved URL)
```

- HTTPS only; no userinfo; port 443 only.
- Redirects are followed manually (max 5) and each hop is re-checked.
- `file://`, `content://`, `http://`, `ftp://` are rejected.
- A pack's arbitrary `downloads[]` URL is never fetched; see §4.2.

**Implementation note (2026-10-25):** `HostAllowlist.Minecraft` mirrors this list exactly. `StrictUrlGuard` checks HTTPS, no credentials, port 443 and the host before every connection and after every redirect; `HttpDownloadSource` follows at most five redirects manually and never hands a disallowed URL to the connection factory. The host check is exact, with no wildcard syntax.

**Open:** whether Mojang's CDN uses additional hostnames per region. The allowlist must be tested from several regions; a false rejection is better than an open fetcher, but it must be debuggable.

### 9.2 Archive handling

- Reject entries with absolute paths, `..`, backslashes, NUL, or a resolved path outside the destination.
- Reject symlinks and hardlinks.
- Caps: 20,000 entries, 2 GiB total uncompressed, 512 MiB per file, 100:1 compression ratio per entry, 200:1 overall.
- Stream to disk; never load a whole entry into memory.
- Validate `modrinth.index.json` size (≤ 8 MiB) and JSON depth before parsing.
- Treat the pack name and summary as untrusted text; no HTML rendering, no file names from them.

### 9.3 Code execution

- The only code executed during install is the loader processor, in `:installer`, with a classpath restricted to the installer's declared libraries.
- No shell. Argument vectors only.
- Processor output paths must stay inside the staging directory; verify after each processor.
- A processor that writes outside staging, spawns a process, or opens a network socket is a bug; the sandbox is a separate UID at v1.1, filesystem confinement plus review at v1.

### 9.4 Supply chain

- Mojang metadata: HTTPS + SHA-1 from the manifest + SHA-1 per artifact.
- Modrinth: SHA-512 from the version file, resolved through the API.
- Our components: SHA-256 from a signed `BUILD-MANIFEST.json`; the app pins the release index hash.
- No “skip hash check” toggle, not even in developer settings. A debug build may point at a local mirror, but it still verifies.

---

## 10. Performance budget

| Operation | Target | Notes |
|---|---|---|
| Manifest + version + loader resolve | < 2 s | Cached; parallel |
| Plan generation | < 300 ms | Pure Kotlin, no I/O except stat |
| Vanilla first install (100 Mbit/s) | 3–6 min | 650 MB; CDN-bound |
| Vanilla first install (20 Mbit/s) | 15–25 min | Show ETA, allow pause |
| Repair (FAST) | < 2 s | 5,000 stats |
| Repair (FULL) | 20–60 s | Hash 500 MB |
| Instance switch | < 500 ms | Plan is precomputed |
| Export (2 GB world) | 1–3 min | ZIP deflate; stream |
| GC scan | < 1 s | Registry + manifests |

**Observed:** the asset set is 5,147 unique objects; there is no intra-version dedup to exploit. The win is cross-instance sharing, not within one install.

---

## 11. Test plan

### 11.1 Unit

- Maven coordinate parser (classifier, extension, missing version).
- Rule evaluator: allow/deny order, `os.arch`, features, no-match default.
- Macro substitution: `${classpath}`, `${natives_directory}`, `${launcher_name}`, unknown macro → error.
- Manifest merge: duplicate library, child override, argument order.
- Path safety: `..`, absolute, UNC, NUL, symlink, case-insensitive collisions.
- Digest streaming: correct hash, truncated stream, wrong size.

### 11.2 Integration (mock HTTP)

- 404 → metadata refresh once → fail with a clear message.
- 429 with `Retry-After` → waits, then succeeds.
- 500 → retry, then fail.
- Truncated body → resumes with Range.
- Server ignores Range → restarts cleanly.
- Corrupt object → quarantine, re-download, succeed; second corruption → fail.
- Redirect to a non-allowlisted host → rejected.
- Pack with `../` entry → rejected before extraction.
- Zip bomb → rejected at the ratio cap.
- Concurrent installs of two instances → shared objects downloaded once.

### 11.3 Golden plans

Commit `fixtures/plans/26.3-vanilla.json`, `26.3-fabric-0.20.1.json` and `26.3-neoforge-26.3.0.97.json` with the exact classpath order, argument vector and plan hash. A dependency or rule change that alters the plan is a deliberate diff, not an accident.

### 11.4 Device

- Install, kill the app mid-download, reopen, restart the install, verify. (The current build re-fetches metadata and reuses verified store objects; a pending plan is not persisted yet.)
- Install, kill the app during commit, reopen, recover.
- Fill the disk mid-install, confirm a clean failure and no partial instance.
- Install two instances, confirm assets are shared once.
- Uninstall one, confirm GC does not remove the other's objects.
- Import a 2 GB export, confirm no path escapes.

---

## 12. Decisions for sign-off

1. **Official Mojang metadata only; no third-party version mirrors.**
2. **Replace all native artifacts with EclipseNatives; drop `jtracy`.**
3. **Fabric profile merge is v1; Quilt and NeoForge are v1.1; Forge is import-only.**
4. **Modrinth `.mrpack` v1 only, resolved through the Modrinth API/CDN, not arbitrary URLs.**
5. **Global content-addressed store with per-instance manifests; assets and libraries shared across instances.**
6. **Staging → fsync → journal → atomic rename; the filesystem is the source of truth.**
7. **Three verification levels; FULL is not on the launch path.**
8. **Installer processors run in a dedicated `:installer` process with EclipseJRE.**
9. **Strict host allowlist; no “skip hash” escape hatch.**
10. **No CurseForge, ATLauncher or Technic install in v1.**

## 13. Open questions

- Does the external app-specific volume support hard links on API 26–37, or must every consumer read `shared/objects` directly?
- Does 26.3's `client.jar` expect a `assets/virtual` layout for any legacy resource path?
- Which `libraries.minecraft.net` artifacts are still referenced by 26.3, and are they all available over HTTPS with the same hashes?
- Does NeoForge 26.3.0.97 still need `client.lzma` binary patches, or are the processors output-only?
- What is the exact Modrinth API rate limit and User-Agent policy in 2026?
- Are there regional Mojang CDN hostnames missing from the allowlist?
- Can a `FULL` verify run in parallel with a launch without thrashing the flash storage?
