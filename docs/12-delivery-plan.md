# 12 — Delivery plan

**Status:** plan draft for sign-off, 2026-10-24.
**Scope:** how the architecture in [11](11-system-architecture.md) becomes a signed, tested 1.0: phase plan, gates, critical path, CI/CD, release checklist, device matrix, beta strategy, risk register, staffing and economics, and the post-v1 roadmap.
**Baseline:** a small team (2 Android, 1 native/graphics, 1 designer, part-time QA/legal), 36 working weeks plus 4 weeks contingency.
**Labels:** **Decision**, **Inference**, **Open**.

---

## 1. The plan in one page

| Item | Value |
|---|---|
| v1 scope | Vanilla + Fabric, Modrinth content and `.mrpack`, Vulkan or Sandpaper, touch/gamepad/keyboard, GitHub Releases |
| Baseline schedule | **36 weeks** from sign-off, plus **4 weeks** contingency |
| Critical path | EclipseJRE 25 → `libeclipse_host.so` → SDL3/EGL → vanilla launch → Fabric → beta |
| External gates | Mojang AppID approval; 16 KB hardware for verification; release signing key custody |
| Release cadence | Weekly internal builds, fortnightly alpha, monthly beta after Phase 5, 1.0 at week 36 |
| Distribution | GitHub Releases (signed APK + SHA-256 + SBOM); F-Droid later, separately reviewed |
| Definition of done | §7 checklist, not “it builds” |
| Team | 5.6 FTE peak; a solo variant is possible in 9–14 months with a reduced matrix |
| Budget drivers | Native/JVM engineering, device lab, legal review; CI is not the main cost |

If sign-off is **2026-11-02**, the baseline lands **2027-07-09**, with contingency through **2027-08-06**.

---

## 2. What “v1” means

### 2.1 In scope

- Android 8.0+ (API 26+), **arm64-v8a** shipped; x86_64 for the emulator.
- Minecraft **current release** at build time (26.3 is the research anchor; see §9.1 on version drift).
- Vanilla and **Fabric** creation; Forge/NeoForge profile **import** only.
- Microsoft device-code sign-in, multiple accounts, re-authentication.
- Modrinth search, project install, `.mrpack` v1 import.
- Vulkan 1.2 when the probe passes; **Sandpaper** GL 3.3 → GLES 3.2 otherwise.
- Touch overlay with presets, gamepad, physical keyboard/mouse, IME.
- Crash sheet, safe mode, diagnostics export, repair.
- Five-destination UI, design system, WCAG 2.2 AA, 200% font scale.
- Signed GitHub Releases with SHA-256, SBOM and release notes.

### 2.2 Out of scope for v1

Quilt/NeoForge creation, CurseForge, OptiFine, shader packs as a promise, cloud sync, accounts beyond Microsoft, ARM32, Play Store, iOS, a mod sandbox, and a custom AWT (`Sash`). Each has a named later release in §11.

### 2.3 The one-sentence release criterion

> A player who owns Java Edition can install Eclipse Launcher, sign in, install vanilla or Fabric 26.x, and reach a world on a supported device — or get a specific, actionable reason why not.

---

## 3. Phase plan

### 3.1 Calendar

Week 1 starts on the Monday after sign-off. The dates below assume **2026-11-02**.

| Phase | Weeks | Dates | Theme | Exit gate |
|---|---|---|---|---|
| **0 — Prove** | 1–3 | 2026-11-02 → 2026-11-20 | Spikes, AppID submission, component audits | G0–G4 |
| **1 — Skeleton** | 4–9 | 2026-11-23 → 2027-01-01 | App shell, design system, metadata, installer, downloads, instances | G5 |
| **2 — Launch** | 10–16 | 2027-01-04 → 2027-02-19 | JRE 25, host, SDL3, vanilla launch, logs | G6 |
| **3 — Identity** | 17–20 | 2027-02-22 → 2027-03-19 | Auth, accounts, token lifecycle, AppID integration | G7 |
| **4 — Content** | 21–26 | 2027-03-22 → 2027-04-30 | Fabric, Modrinth, `.mrpack`, repair, import/export | G8 |
| **5 — Experience** | 27–32 | 2027-05-03 → 2027-06-11 | Renderer matrix, touch/gamepad, overlay, accessibility, performance | G9 |
| **6 — Harden** | 33–36 | 2027-06-14 → 2027-07-09 | Beta, security review, docs, release | G10 |
| **Buffer** | 37–40 | 2027-07-12 → 2027-08-06 | Contingency only | 1.0 |

Weeks 8–9 include the northern-hemisphere holidays. The plan assumes reduced capacity there; do not move a gate into that window.

### 3.2 Phase 0 — Prove

**Goal:** retire the technical and legal unknowns before committing to a UI.

| Workstream | Deliverable | Owner |
|---|---|---|
| AppID | Entra app created, privacy/terms pages live, review form submitted, ticket id recorded | Product/legal |
| EclipseJRE | A **rebuilt** OpenJDK 25 image boots a trivial class on the API 36 arm64 device; 16 KB flags and rpath documented | Native |
| EclipseNatives | Official-upstream rebuild of LWJGL/OpenAL/FreeType/Shaderc/SPIRV-Cross; a `dlopen` smoke test on device | Native |
| EclipseSDL3 | Package rename, `pw_host` seam replacing `eclipseexec`, SDL window + EGL surface + input on device | Native |
| Vulkan probe | 26.3 extension/feature probe returns a truthful result on at least three GPUs (Adreno, Mali, PowerVR) | Native |
| Sandpaper spike | Clear a triangle and run a GL 3.3 shader through the 330→320 es rewrite on device | Graphics |
| Installer spike | Download `client.jar` + asset index + one library, verify, stage, commit, delete | Platform |
| CI spike | Clean-checkout build of a stub APK on a GitHub runner with pinned JDK/NDK | Platform |
| Legal | License decision recorded; trademark screen for “Eclipse”; component license audit | Product/legal |

**Exit criteria (G0–G4):**

- G0: dossier signed off; license, ABI scope and v1 scope recorded.
- G1: AppID application submitted with the production client ID and a real privacy policy.
- G2: Java 25 starts on device in under 2 s with the chosen flags, and a trivial `main` returns.
- G3: SDL3 creates a window and an EGL surface, receives a touch event, and swaps a frame.
- G4: the Vulkan probe produces the same verdict as a manual `vulkaninfo`-equivalent check on three devices.

If G2 or G3 fails, stop and re-plan before Phase 1. A UI built on an unproven runtime is a very expensive mockup.

### 3.3 Phase 1 — Skeleton

| Area | Deliverable |
|---|---|
| Build | Module graph, convention plugins, version catalog, dependency verification, CI jobs |
| Design system | Theme, tokens, 12 core components, preview catalog, screenshot baseline |
| Shell | Five destinations, adaptive navigation, empty/loading/error states |
| Metadata | Manifest, version JSON, rule evaluator, asset index, Maven coordinates, golden fixtures |
| Installer core | Planner, downloader, store, staging, commit/rollback, repair, GC; no loader yet |
| Instances | Create (vanilla), list, detail shell, delete/export |
| Storage | Layout, atomic write, lock, space preflight, quota |
| Database | Room schema, migrations, reconciliation |
| Observability | Structured log, redaction, diagnostic bundle skeleton |

**Exit (G5):** a vanilla 26.3 instance installs from official metadata on a device, survives a mid-download kill and a mid-commit kill, passes `FULL` verify, and can be deleted with GC reclaiming the right bytes. No JVM yet.

**Implementation note (2026-10-25):** the first Phase 1 slices are in the tree. `:domain:mojang` parses the official manifest and version JSON, evaluates ordered OS/arch/feature rules, parses Maven coordinates and argument templates, and produces the Java classpath while recording the desktop natives that EclipseNatives replaces. `:domain:install` adds the asset-index model, a deterministic content-addressed download plan for the client, libraries, assets, asset index and logging config, and a sequential artifact store that writes `<destination>.part`, hashes while streaming, checks size and SHA-1, fsyncs, atomically renames, and reports byte-accurate progress with mid-file cancellation. CI runs their unit tests. The HTTP source, parallel scheduler, materialisation and install transaction remain open.

### 3.4 Phase 2 — Launch

| Area | Deliverable |
|---|---|
| EclipseJRE | jlink profile, read-only install, 16 KB gate, rpath, `libc++_shared` |
| Host | `libeclipse_host.so`: JVM creation, status, log socket, signal hygiene, `_exit` |
| SDL | EclipseSDL3 integration, surface handover, lifecycle, input bridge |
| Planner | `LaunchPlan`, argument templates, heap policy, native substitution |
| Game | `GameActivity`, overlay shell, launch progress, result plumbing |
| Diagnostics | `hs_err`, status file, `ApplicationExitInfo`, crash sheet v1 |
| Renderer | Vulkan path first; Sandpaper only if Phase 0 proved it |

**Exit (G6):** vanilla 26.3 reaches the main menu and a world on the API 36 test device, using the Vulkan path where supported; a forced crash produces a stage-specific crash sheet; a second launch after a crash works; logs contain no token.

### 3.5 Phase 3 — Identity

| Area | Deliverable |
|---|---|
| Entra | Production client ID, redirect-free public client, publisher verification if required |
| Flow | Device code, polling, XBL, XSTS, Minecraft login, entitlement, profile |
| Vault | Keystore AES-GCM, atomic refresh-token rotation, single-flight |
| UI | Welcome, code screen, progress, success, error matrix, account switcher, re-auth |
| Launch | Token ticket, argument injection, redaction tests |
| Gate | AppID approval evidence, or a documented dev-only fallback |

**Exit (G7):** a real account signs in on a phone and on a controller-only tablet, launches the game, and survives token expiry and refresh; a Bedrock-only account, a child account and an expired code each get the specific message from doc 06.

### 3.6 Phase 4 — Content

| Area | Deliverable |
|---|---|
| Fabric | Profile merge, library dedup, loader pin, launch |
| Modrinth | Search, project detail, version selection, install sheet, license display |
| `.mrpack` | Parse, validate, resolve by SHA-512 through Modrinth, overrides, update |
| Content UI | Instance Content tab, enable/disable, dependency warnings |
| Importer | Eclipse export/import, Prism/MultiMC basic import, plain `.minecraft` copy |
| Repair | Manifest-driven repair, quarantine, safe-mode mod move |
| Installer | Foreman/Crowbar scaffolding for v1.1; Forge import only |

**Exit (G8):** a 150+ mod Fabric pack installs, launches or fails with a named file/mod, and updates without losing the world; a pack with a non-Modrinth file fails honestly; a traversal pack is rejected before extraction.

### 3.7 Phase 5 — Experience

| Area | Deliverable |
|---|---|
| Renderer matrix | Vulkan/Sandpaper selection per device and per content; probe explanation |
| Touch | Overlay presets, editor, multi-touch, mouse mode, IME |
| Gamepad | Focus navigation, mapping, deadzones, hot-plug |
| Overlay | Pause, keyboard, screenshot, performance, exit |
| Accessibility | TalkBack pass, 200% font, contrast, reduced motion, focus restore |
| Performance | Startup, scroll, install, launch budgets from doc 08; baseline profile |
| Design | Final visual QA, empty/error states, localization-ready strings |

**Exit (G9):** S3/S4/S5 study thresholds met (doc 10 §11.5); 60 fps UI on a T1 device; the touch overlay survives a 30-minute session; every critical flow is completable with a controller and with TalkBack.

### 3.8 Phase 6 — Harden

| Area | Deliverable |
|---|---|
| Beta | Closed beta cohort, crash triage, device matrix results |
| Security | Threat model review, dependency audit, signed-component verification, release-key custody |
| Legal | EULA/branding review, privacy policy, third-party notices, trademark statement |
| Docs | README, install guide, troubleshooting, known limitations, contribution guide |
| Release | Signed APK, SHA-256, SBOM, release notes, rollback plan |
| Support | Issue templates, diagnostic upload instructions, response SLA |

**Exit (G10):** §7 checklist complete; beta metrics at or above the thresholds in §8.4; no open P0/P1 security issue; 1.0 tagged.

---

## 4. Critical path and dependencies

```
AppID submit ──────────────► review ──────────────► auth beta gate
JRE 25 rebuild ──► JVM boot ──► host ──► vanilla launch ──► Fabric ──► beta
SDL3 cleanup ────► window/EGL ──► input/audio ──► overlay ──► device matrix
Sandpaper ───────► GL fallback ──► renderer matrix ──► beta
Metadata/install ─► vanilla install ─► launch ─► content ─► repair
Design system ───► screens ─► accessibility ─► visual QA
CI ──► every phase
```

The critical path is **JRE → SDL → launch → Fabric → beta**, not the UI. The AppID review runs in parallel and becomes blocking only at G7. Sandpaper is on the critical path only for devices without Vulkan; if Phase 0 shows the Vulkan coverage is good enough for the beta cohort, Sandpaper can ship as “compatibility (beta)” and improve after 1.0.

### 4.1 Parallelisation

| Weeks | Platform | Native/graphics | Design | QA |
|---|---|---|---|---|
| 1–3 | CI spike, installer spike | JRE, SDL, Vulkan, Sandpaper | IA, prototype, design system | device inventory |
| 4–9 | metadata, install, DB, downloads | host skeleton, JRE packaging | components, screens | install tests |
| 10–16 | launch plumbing, crash pipeline | JVM, SDL, renderer | launch/crash UI | launch matrix |
| 17–20 | auth, vault, accounts | token/redaction review | auth flows | account matrix |
| 21–26 | Fabric, Modrinth, import | loader processor host | content UI | pack matrix |
| 27–32 | controls, overlay, performance | renderer matrix | visual QA, accessibility | S3–S5 |
| 33–36 | beta triage, release | driver triage | polish | regression |

### 4.2 What must not be parallelised

- The JRE and the host: a JVM flag change and a host change in the same week make failures unattributable.
- The installer commit path and the launch verify path: both touch the same manifests.
- Auth and token storage: the vault must land before the flow is wired to a real account.
- The design system and the first screen: tokens first, then components, then screens.

---

## 5. Gates

| Gate | When | Evidence | If it fails |
|---|---|---|---|
| **G0 — Scope** | Week 0 | Signed decision record: license, ABI, v1 scope, design direction | Do not start; the plan is not funded |
| **G1 — AppID submitted** | Week 2 | Form receipt, client ID, privacy URL, support contact | Fix the submission; it gates G7, not Phase 1 |
| **G2 — JRE boots** | Week 3 | Device video/log: Java 25 `main` returns, 16 KB check passes | Re-scope to a JRE workstream; do not build UI on it |
| **G3 — SDL window** | Week 3 | Device video/log: window, EGL surface, touch, swap | Re-scope; a GLFW fallback is not in v1 |
| **G4 — Vulkan truth** | Week 3 | Probe output vs. independent check on 3 GPUs | Ship Sandpaper-only beta; document |
| **G5 — Install** | Week 9 | Kill/resume/commit/rollback test report | Fix storage before launch work |
| **G6 — Vanilla launch** | Week 16 | World reached; crash sheet; second launch | The hard gate; re-plan the schedule |
| **G7 — Auth** | Week 20 | Real account end-to-end; AppID approval evidence | Public beta blocked; continue dev builds |
| **G8 — Content** | Week 26 | Pack install/launch/update; traversal tests | Cut `.mrpack` from 1.0 if unsafe; keep Modrinth mods |
| **G9 — Experience** | Week 32 | Study thresholds; accessibility audit; perf budgets | Delay 1.0; do not ship an inaccessible build |
| **G10 — Release** | Week 36 | §7 checklist | Use the 4-week buffer |

**Rule:** a gate is passed by evidence attached to the gate record (log, video, report, signed checklist), not by a status meeting.

---

## 6. CI/CD

### 6.1 Principles

- **GitHub Actions only.** No local build is authoritative; a release is whatever CI produced from a tag.
- **Pinned toolchains.** JDK 25, Gradle 9.6.0, NDK r28c, CMake, AGP 9.4.0, Kotlin 2.4.21. The Gradle wrapper with `distributionSha256Sum` lands before the first release build; until then the version is pinned in the workflow and provided by `setup-gradle`.
- **Actions pinned by commit SHA**, not by tag, with the release tag in a trailing comment. Dependabot updates them with review.
- **No binaries in git.** APKs, `.so`, JRE archives, `client.jar` and packs are build outputs or downloads.
- **Fail closed.** A missing hash, a 4 KB `.so`, an unsigned APK or an unapproved AppID stops the release.

### 6.2 Workflows

Two workflows exist today; the rest of the table is the target state, and a
workflow is only listed as planned until it has run green.

| Workflow | Trigger | Jobs |
|---|---|---|
| `ci.yml` | push to `main`, pull request, manual | Docs + provenance + Python syntax; JDK 25 and SDK 37; `:core:common:test`; debug APK; **binary-manifest gate**; **16 KB gate**; unsigned release dry run; the same two gates on the release APK; both APKs uploaded for 7 days |
| `release.yml` | tag `v*`, manual with an existing tag and `dry_run` | Signing secrets required unless dry-run; signed release APK; binary-manifest and 16 KB gates; `SHA256SUMS.txt` and `BUILD-MANIFEST.json`; `gh release create --verify-tag` |
| `docs.yml` (planned) | push, PR | Split the documentation checks out of `ci.yml` once they take longer than a minute; link check and chapter index stay here |
| `native.yml` (planned) | push touching `native/`, `sandpaper/` | CMake build, symbol allowlist, `readelf` 16 KB gate, size report |
| `instrumented.yml` (planned) | nightly, manual | API 36 emulator: install, launch, crash, recover, screenshot |
| `components.yml` (planned, each component repo) | push, tag | Build, 16 KB gate, manifest, signature, release |

What `ci.yml` proves today: the app compiles against API 37, the JVM core
tests pass, both variants package, the merged manifest names the three
activities and their `:game` / `:installer` processes, `extractNativeLibs` is
true, release is not debuggable, only the two declared ABIs ship, and every
64-bit `.so` in both APKs passes the structural 16 KB rules.

What it does not prove yet: that the app launches, that the manifest gate
sees a signed APK, or that lint/ktlint pass — those are deliberately not
claimed. The first two arrive with `instrumented.yml` and the signing secrets;
lint joins `ci.yml` with the first feature module, when its output stops being
dominated by scaffold warnings.

### 6.3 `release.yml` as implemented

The sketch below was replaced by the real pipeline; keeping the sketch in sync
with the file is cheaper than rediscovering the differences during a release.

1. Check out the tag (`workflow_dispatch` takes an existing tag name, so a
   dry run builds exactly what a tag would).
2. Install JDK 25, SDK platform `android-37.0`, build-tools 36.0.0.
3. Unless `dry_run` is set, fail before compiling when
   `ECLIPSE_KEYSTORE_BASE64` is absent. The key material is never echoed.
4. `:app:assembleRelease`; `scripts/check-apk-manifest.py` asserts the final
   package id, version, activities, processes, `extractNativeLibs` and the
   absence of `debuggable`; `scripts/check-elf-alignment.py` checks every
   packaged `.so`.
5. Upload the APK as an artifact, then in the publish job download it, write
   `SHA256SUMS.txt` and `BUILD-MANIFEST.json` (version, commit, timestamp,
   per-file SHA-256 and size) and create the GitHub Release with
   `--verify-tag`.

Still open before the first public release: a tag/`versionName` match check,
an APK signature verification step (`apksigner verify --print-certs`), an
SBOM, and a required-reviewer environment around the signing step.

### 6.4 Signing and key custody

- One release key, generated once, 4096-bit RSA or EC P-256; Android requires the same key for every update of the package.
- The keystore and passwords live in GitHub Actions secrets **and** an offline backup in two physically separate places.
- The certificate SHA-256 is published in the README and on the release page.
- **Key loss means a new package name and a migration**, because Android will not install an update signed by a different key. Treat the backup as a release artifact.
- CI never prints the keystore or passwords; the signing step runs in a protected environment.
- Debug keys never sign a release; `assembleRelease` fails if `signingConfig` is null.

### 6.5 Version codes

```
versionCode = major * 1_000_000 + minor * 10_000 + patch * 100 + channel
channel: 99 = stable; 1..98 = pre-release, increasing with each build
```

Examples: `1.0.0` → `1000099`; `1.0.0-beta.3` → `1000003`; `1.1.0` → `1010099`.

The stable code must be higher than every pre-release of the same patch, or Android will refuse the final update after a beta.

`versionName` is the semver string. CI derives both from `gradle.properties` and fails if the tag does not match.

### 6.6 Update path

- The app checks the GitHub Releases API at most once per 24 h, on user-visible screens only.
- It compares `versionCode`, never a string.
- It downloads the APK, verifies SHA-256 **and** the signing certificate before handing it to `PackageInstaller`.
- The user sees the size, the changelog and a consent button. No silent install, no background download without a setting.
- A failed update leaves the current install untouched.
- Downgrades are not supported by Android; the UI explains that a rollback needs an uninstall and an export.

### 6.7 CI cost

Public repositories get generous GitHub-hosted runner allowances, but native builds and emulator jobs are the expensive part. Budget for:

- a larger runner for native builds if the standard runner times out;
- a device farm for the physical matrix (the largest recurring cost);
- artifact storage for release APKs and mappings (small);
- no macOS runners in v1 (no iOS target).

**Inference:** CI compute is a minor line compared with native engineering and devices; do not optimise it before measuring.

---

## 7. Release checklist

### 7.1 Legal and policy

- [ ] License files present: Apache-2.0 for original code; GPL-2.0+CE for EclipseJRE; zlib for SDL; LGPL for OpenAL; BSD/Apache/FTL for natives.
- [ ] `PROVENANCE.md` matches the dependency tree and the shipped notices.
- [ ] No copied code, assets, strings or icons from another launcher.
- [ ] Minecraft disclaimer on About, first run and the release page.
- [ ] Eclipse Foundation non-affiliation statement present.
- [ ] Privacy policy and terms URLs resolve.
- [ ] Mojang AppID approval recorded (or the build is explicitly labelled development-only).
- [ ] No game files, assets or JRE binaries in git.

### 7.2 Security

- [ ] `allowBackup=false`; extraction rules exclude tokens, DB and instances.
- [ ] Only `MainActivity` exported; `PendingIntent`s immutable.
- [ ] No user CAs in release; cleartext disabled; host allowlist tested.
- [ ] Redaction test passes on a log containing every secret shape.
- [ ] Component signatures and hashes verified; 16 KB gate fail-closed.
- [ ] `apksigner verify` passes; certificate SHA-256 published.
- [ ] Dependency audit has no unpatched critical CVE.
- [ ] Release keystore backed up offline in two places.

### 7.3 Quality

- [ ] Vanilla and Fabric install + launch on T0/T1/T2 devices.
- [ ] `.mrpack` install, update and traversal rejection.
- [ ] Crash → crash sheet → safe mode → recovery.
- [ ] Account add, switch, re-auth, remove.
- [ ] Offline shell, low storage, no-Vulkan, no-GLES3.2 paths.
- [ ] Touch, gamepad, keyboard, mouse, IME.
- [ ] TalkBack and 200% font pass on critical flows.
- [ ] Contrast and reduced-motion pass.
- [ ] No ANR in a 30-minute session; UI memory under budget.
- [ ] Golden plans unchanged, or the diff is reviewed.

### 7.4 Release mechanics

- [ ] `versionCode` and `versionName` match the tag.
- [ ] Clean checkout builds on CI with no local artifacts.
- [ ] APK, SHA-256, mapping, SBOM and notices attached.
- [ ] Release notes: user-facing changes, known issues, device caveats.
- [ ] Download the published APK on a clean device and install it.
- [ ] Verify the update path from the previous release.
- [ ] Tag is immutable; a hotfix gets a new tag.
- [ ] Support issue template and diagnostic instructions are live.

---

## 8. Device matrix and beta

### 8.1 Tiers

| Tier | Device class | Examples | What it proves |
|---|---|---|---|
| **T0** | Android 16/17, arm64, 8 GB+, Adreno/Mali flagship | Pixel 9/10, Galaxy S25/S26, OnePlus 13 | Vulkan, 120 Hz, gamepad, thermals |
| **T1** | Android 14/15, arm64, 6–8 GB | Pixel 7a, Galaxy A55, Redmi Note | Sandpaper, 60 Hz, midrange thermals |
| **T2** | Android 12/13, arm64, 4–6 GB | Galaxy A33, Moto G | Low-memory launch, G1 fallback, SAF |
| **T3** | Android 8–11, arm64, 3–4 GB | older midrange | Minimum SDK, no UIDT, `dataSync` |
| **Emulator** | x86_64 API 36, 16 KB image if available | CI | Smoke, no GPU claims |

### 8.2 Minimum lab

- 1 × T0 flagship (Adreno)
- 1 × T0/T1 Mali device
- 1 × T1 midrange
- 1 × T2 low-memory
- 1 × T3 or API 26 emulator
- 1 × tablet/foldable
- 1 × Chromebook or desktop-class Android (keyboard/mouse)
- 1 × controller (Xbox/8BitDo) and 1 × Bluetooth keyboard
- **1 × 16 KB page-size device or emulator** — currently missing; see §9 R3

### 8.3 Beta stages

| Stage | Cohort | Entry | Focus | Exit |
|---|---|---|---|---|
| Pre-alpha | team | G6 | Vanilla launch, crash reports | Stable on 3 devices |
| Alpha | 20–50 | G7 (dev AppID or approved) | Auth, Fabric, install | 80% launch success |
| Closed beta | 200–500 | G8 | Packs, device matrix, controls | 85% launch success, no P0 |
| Public beta | GitHub pre-release | G9 | Scale, support load | 90% launch success, crash-free ≥ 95% |
| 1.0 | all | G10 | — | Checklist complete |

### 8.4 Beta metrics

From doc 08, with beta thresholds:

| Metric | Beta threshold | 1.0 target |
|---|---|---|
| First-launch success (supported device) | ≥ 80% | ≥ 85% |
| Steady-state launch success | ≥ 90% | ≥ 95% |
| Crash-free game sessions | ≥ 90% | ≥ 95% |
| Install completion (excluding cancel) | ≥ 85% | ≥ 90% |
| Auth completion (excluding cancel) | ≥ 75% | ≥ 80% |
| Recovery success after safe mode | ≥ 60% | ≥ 70% |
| ANR rate | < 1% | < 0.5% |

A metric below threshold blocks 1.0 only if it is a **product** failure (for example, an avoidable crash or a misleading error). A device-specific driver failure is documented and gated by the supported-device list.

---

## 9. Risk register

Scoring: likelihood (L) and impact (I) on 1–5; score = L × I. Anything ≥ 12 has a named owner and a rehearsed contingency.

| # | Risk | L | I | Score | Trigger | Mitigation | Contingency |
|---|---|---|---|---|---|---|---|
| R1 | **AppID denied or delayed** | 4 | 5 | 20 | No approval by week 16 | Submit in week 2 with a complete, honest application; build demo mode and installer in parallel | Public beta blocked; ship a developer build with “auth not approved”; appeal once; do not bypass |
| R2 | **EclipseJRE 25 does not stabilise** | 3 | 5 | 15 | G2 slips > 2 weeks | Rebuild from official jdk25u; pin a known-good Amethyst reference only for measurement, not shipping; fund native time | Reduce to a single ABI; delay 1.0; do not ship 4 KB or unstable JVM |
| R3 | **No 16 KB verification hardware** | 4 | 4 | 16 | Still no device by week 8 | Static `readelf` gate; emulator image; borrow/buy a device | Label 16 KB “statically verified, not device-tested”; Play deadline is 2027-02, so this is a 1.0 gate |
| R4 | **GPU driver incompatibility** | 4 | 4 | 16 | Vulkan probe passes but game crashes | Conservative probe; Sandpaper fallback; per-GPU safe list | Ship Sandpaper-default on affected GPUs; publish a device list |
| R5 | **Sandpaper coverage gaps** | 3 | 4 | 12 | Shader or state test fails | Phase 0 triangle + shader spike; contract from 126 GL entry points; per-frame validation | Beta-label GL path; Vulkan-first; cut devices that cannot run either |
| R6 | **SDL 3.4 ABI drift / bridge cleanup** | 3 | 4 | 12 | LWJGL 3.4.3 fails against EclipseSDL3 | Pin 3.4.16, ABI tests, symbol diff against upstream 3.4.14 | Rebuild SDL at the exact LWJGL pin; drop the custom seam |
| R7 | **Midrange performance/thermals** | 4 | 3 | 12 | < 30 fps after 10 min on T1 | Heap/GC policy; render distance presets; thermal-aware defaults; baseline profile | Lower default settings; declare T1 “compatible, reduced” |
| R8 | **Mod compatibility long tail** | 5 | 3 | 15 | > 20% of packs fail | Safe mode; per-mod disable; loader pin; clear “unsupported mod” message | Curate a tested pack list; do not promise arbitrary packs |
| R9 | **“Eclipse” trademark conflict** | 3 | 4 | 12 | Counsel or USPTO search finds a conflict | Trademark screen in Phase 0; non-affiliation statement; original mark | Rename before 1.0; budget for a rebrand |
| R10 | **License contamination in infra repos** | 3 | 4 | 12 | Audit finds copied code | Provenance ledger; official-upstream rebuild; notices; review gate | Replace the component; delay release |
| R11 | **Supply-chain compromise** | 2 | 5 | 10 | Dependency/action anomaly | Pinned actions and deps; verification metadata; signed components; least-privilege tokens | Revoke, rebuild, publish an advisory, rotate keys |
| R12 | **Release key loss** | 2 | 5 | 10 | Keystore unavailable | Two offline backups; documented custody; CI protected environment | New package name and migration; painful, hence the backup |
| R13 | **Bus factor / solo capacity** | 4 | 4 | 16 | Key person unavailable | Document decisions; pair on native; keep the dossier current; avoid single-owner knowledge | Reduce scope to vanilla + Fabric; extend schedule |
| R14 | **Modrinth API/terms change** | 3 | 3 | 9 | API deprecation or rate change | Versioned client, allowlist, caching, contact | Disable Discover; keep direct `.mrpack` import |
| R15 | **Android 17 behaviour change** | 3 | 3 | 9 | New restriction in beta | Target 36; test on 17; read behaviour-changes each release | Raise min/target as needed; document |
| R16 | **Storage/SAF edge cases** | 3 | 3 | 9 | Export/import fails on a vendor | App-specific external storage; SAF only at the boundary; tests on OEMs | Copy through a cache file; avoid direct paths |
| R17 | **Mod steals account token** | 3 | 4 | 12 | Report or audit | Short-lived access token; ticket TTL; encrypted refresh token; warning | Revoke sessions; force re-auth; accelerate separate-UID work |
| R18 | **Version drift (26.3 → 27.x)** | 5 | 3 | 15 | New release changes Java/SDL/libraries | Version-agnostic metadata/planner; test the current release each phase; watch snapshots | Pin a supported version; ship an update before 1.0 |

### 9.1 Version drift is not a footnote

26.3 is the **research anchor**, not a permanent target. By a mid-2027 release, Minecraft will likely be on a newer drop with a different Java or SDL requirement. The metadata and planner layers are deliberately version-agnostic; the native stack is not. Every phase gate includes “current release still launches”. If a new drop changes the JVM or SDL, that is a schedule event, not a surprise.

---

## 10. Staffing and economics

### 10.1 Roles

| Role | FTE | Phase peak | Notes |
|---|---|---|---|
| Native/graphics engineer | 1.0 | 0–6 | JRE, host, SDL, Sandpaper; the critical path |
| Android platform engineer | 1.0 | 1–6 | install, storage, auth, CI |
| Android UI engineer | 1.0 | 1–6 | Compose, adaptive, accessibility |
| Product designer | 1.0 | 0–6 | IA, visual, design system, research |
| UX researcher | 0.5 | 0, 5–6 | Studies S1–S7 |
| Content designer | 0.3 | 1–6 | Error copy, terminology, localization kit |
| QA/device lab | 0.5 | 2–6 | Matrix, regression, accessibility |
| Community/support | 0.3 | 5–6 | Beta, issues, release notes |
| **Peak** | **5.6** | | |

### 10.2 Effort

| Phase | Person-weeks (estimate) |
|---|---|
| 0 | 14 |
| 1 | 21 |
| 2 | 18 |
| 3 | 8 |
| 4 | 18 |
| 5 | 24 |
| 6 | 30 |
| **Build total** | **133 person-weeks ≈ 31 person-months** |
| Research, legal, support, contingency | +12–18 person-months |
| **Programme total** | **43–49 person-months** |

At 5.6 FTE over 36 weeks (≈ 46 person-months of capacity), the plan has limited slack. That is deliberate: the contingency is schedule, not hidden overtime.

### 10.3 Solo variant

| Scope | Estimate | What is cut |
|---|---|---|
| Vanilla + Fabric + Modrinth, Vulkan-only, arm64, English | 9–12 months | Sandpaper, touch editor, controller polish, multiple accounts |
| Full v1 | 14–18 months | Nothing, but slower |

A solo developer should not attempt the full design system and the full device matrix in the same release. Ship a smaller promise and keep the recovery and accessibility bars.

### 10.4 Cost drivers

| Item | Nature | Notes |
|---|---|---|
| Native/JVM engineering | People | The largest line; do not underfund |
| Device lab | Capex/opex | 8–10 devices plus a 16 KB device; a farm can substitute for some |
| Legal review | One-time | EULA, trademark, privacy, LGPL/GPL notices |
| Trademark search | One-time | “Eclipse” is the highest-risk name in the project |
| GitHub Actions | Opex | Public-repo allowances; larger runners for native |
| Domain/email | Opex | Privacy policy, support contact |
| Design assets | One-time | Original icon/illustration; no stock Minecraft art |
| Beta support | Opex | Scales with device diversity |

**Open:** exact currency costs depend on team location and device sourcing; this plan budgets in person-months and device count rather than pretending to know local rates.

---

## 11. Post-v1 roadmap

| Release | Theme | Contents | Explicitly not |
|---|---|---|---|
| **1.0.x** | Stability | Crash fixes, device list, component updates, docs | New loaders |
| **1.1** | More ways to play | Quilt + NeoForge creation, Sash (AWT shim), AAudio via SDL3, background installs with a reviewed service type, mod updates | CurseForge, cloud sync |
| **1.2** | Content depth | CurseForge via official API/key, shader-pack support where Iris/Sodium compatibility is proven, pack update policies | A general mod marketplace |
| **1.3** | Living room | Android TV/desktop focus model, multi-window, external display, controller-first onboarding | A TV-only fork |
| **2.0** | Trust and scale | Mod sandbox (separate UID/companion), optional end-to-end encrypted cloud sync, multi-instance concurrency, incremental component updates | Piracy, alt managers, Play-first |

Each release gets its own mini-dossier update; the architecture invariants in doc 11 do not change without an ADR.

---

## 12. Decisions for sign-off

1. **Baseline 36 weeks + 4 weeks contingency**, with the calendar in §3.1.
2. **v1 scope as §2.1**, with the cuts in §2.2.
3. **G0–G10 are evidence gates**, not status meetings.
4. **The critical path is JRE → SDL → launch**, and Phase 1 does not start until G2/G3 pass.
5. **CI/CD is GitHub Actions only; releases are tag-driven, signed, and include SHA-256 + SBOM.**
6. **The release key is backed up offline in two places; key loss means a new package.**
7. **Beta thresholds in §8.4 are release gates.**
8. **The risk register is reviewed at every phase gate; R1, R2, R3 and R13 have named owners.**
9. **Version drift is handled by testing the current release at every gate, not by freezing 26.3.**
10. **A solo build is a reduced product, not the same product slower.**

## 13. Open questions

- Who owns the Entra tenant and the AppID submission, and can they respond to Mojang within a week?
- Which 16 KB device or emulator image can the lab obtain before week 8?
- Is a device farm acceptable for T0/T1 coverage, or is physical hardware required for GPU driver work?
- Does the team have an offline signing ceremony, or is a CI-only key acceptable with two backups?
- Which beta cohort can be recruited without depending on friends of the team?
- Is a 36-week baseline credible with the actual team, or should v1 be cut to vanilla + Fabric + Vulkan first?
- What is the hotfix SLA for a broken 1.0 release, and who staffs it?
- Does the project need a legal entity before it accepts donations or pays for a device lab?
- Should the first public release be a beta with a supported-device list, or a 1.0 with a narrower device promise?
- How often can component releases ship without forcing an app update?
