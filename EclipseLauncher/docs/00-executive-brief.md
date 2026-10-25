# 00 — Executive brief

**Date:** 2026-10-24
**Purpose:** give a decision-maker the whole picture in 15 minutes, without hiding the hard parts.

---

## 1. What we are building

**EclipseLauncher** is a from-scratch, Android-first launcher for **Minecraft: Java Edition**. It installs game versions and mod loaders, authenticates Microsoft accounts, manages instances and content, launches the game inside a bundled Java 25 runtime, and provides touch/gamepad controls and an in-game overlay.

It is **not**:

- a Bedrock launcher;
- a piracy tool or an offline-auth bypass;
- a server host;
- a cheat client;
- an iOS app (separate problem);
- a Play-Store-first product (see §6).

The product promise: **“One tap to play. If it breaks, we tell you why and fix it.”**

---

## 2. The technical truth in one page

A launcher is a small operating system for one game. The layers are:

```
┌──────────────────────────────────────────────────────────────┐
│ Compose UI (home, instances, downloads, settings, overlay)   │
├──────────────────────────────────────────────────────────────┤
│ Launcher core: metadata, installer, auth, mod loaders, logs  │
├──────────────────────────────────────────────────────────────┤
│ Launch planner: classpath, JVM args, natives, game args      │
├──────────────────────────────────────────────────────────────┤
│ Game process: JNI_CreateJavaVM → HotSpot 25 → Main.main     │
├──────────────────────────────────────────────────────────────┤
│ Window/input: EclipseSDL3 3.4.x → EGL / Vulkan WSI          │
├──────────────────────────────────────────────────────────────┤
│ Graphics: native Vulkan 1.2, else Sandpaper (GL 3.3 → GLES) │
├──────────────────────────────────────────────────────────────┤
│ Android: Surface, Audio, Input, Storage, Keystore, LMK       │
└──────────────────────────────────────────────────────────────┘
```

Each layer has a failure mode that looks like “the launcher is broken” to a user. The design work is largely about **making failures legible and recoverable**.

### Current game facts (verified 2026-10-24)

| Fact | Value | Source |
|---|---|---|
| Latest release | 26.3, 2026-09-15 | Mojang manifest |
| Latest snapshot | 26.4-snapshot-5, 2026-10-20 | Mojang manifest |
| Java required | **25** (`java-runtime-epsilon`, 25.0.1) | 26.3 JSON, runtime manifest |
| Window/input | **SDL3**, replacing GLFW | 26.3 wiki |
| LWJGL | **3.4.3** (SDL 3.4.14, Vulkan 1.4.360 headers) | 26.3 JSON, LWJGL release notes |
| Graphics APIs | OpenGL + experimental **Vulkan 1.2** backend | 26.2/26.3 wiki |
| Default heap | `-Xms2G -Xmx4G`, `AlwaysPreTouch`, ZGC on Linux | 26.3 JSON `default-user-jvm` |
| Vanilla footprint (Linux x64) | ~582 MiB libraries+client+assets, plus JRE | computed from 26.3 JSON |
| Libraries | 114 entries; 66 selected on Linux x64; **no Android ARM64 natives** | 26.3 JSON |

**Implication:** the old Pojav/Amethyst recipe (GLFW shim + GL4ES + Java 8/17/21) is no longer sufficient for current Minecraft. A new launcher must own the **SDL3 + Java 25 + Vulkan-capable** path.

---

## 3. The five hard problems, ranked

| Rank | Problem | Why it is hard | Can we control it? |
|---|---|---|---|
| 1 | **Mojang AppID allowlist** | New apps get HTTP 403 until manually approved; weekly review; ownership verification; name restrictions | Partly — submit early, build a compliant app |
| 2 | **JRE 25 on Android** | No official Android build; custom OpenJDK port; bionic/SELinux/signal/AWT patches; per-ABI | Yes — engineering, but expensive |
| 3 | **SDL3 + Vulkan integration** | Minecraft now owns the window via SDL3; Android surface, input, IME, gamepad, Vulkan WSI; experimental backend | Yes — but requires device spikes |
| 4 | **Renderer/driver fragmentation** | Adreno/Mali/PowerVR/Xclipse; GLES 3.x; Vulkan 1.1 vs 1.2; thermal throttling | Partly — auto-detection + fallbacks |
| 5 | **Distribution policy** | Play forbids downloading executable code outside Play | Yes — ship signed GitHub Releases and a direct APK; F-Droid later, separately reviewed |

The UI, installer, and mod manager are **large but well-understood**. The top three are the schedule.

---

## 4. Competitive position

| Launcher | License | Latest | MC 26.3 | Java 25 | SDL3 | Vulkan | UI | Core weakness |
|---|---|---|---|---|---|---|---|---|
| **Amethyst** | LGPL-3.0 | 1.1.7 (2026-07-31) | snapshot-1 only | No (8/17/21) | Partial (LWJGL 3.4.1) | No | Legacy XML preferences | Behind 26.3; Pojav-era architecture |
| **FCL** | GPL-3.0 | 1.3.4.1 (2026-10-20) | Yes | Yes (8/17/21/25) | Yes | Detection + game backend | Mixed XML/Compose | Settings-heavy; GPL; legacy core |
| **Zalith 2** | GPL-3.0 | 2.7.2 (2026-10-20) | Yes | Yes | Yes | Detection + game backend | Compose M3 + Navigation 3 | GPL; power-user density; Pojav core |
| **Pojav** | LGPL-3.0 | archived 2025-09-23 | No | No | No | No | Legacy | Unmaintained |
| **Eclipse (target)** | Apache-2.0 | — | **Yes** | **Yes** | **Yes** | **Auto + fallback** | **M3 Expressive, adaptive** | Must earn trust; no ecosystem yet |

**Gap:** nobody has combined (a) current 26.3 support, (b) a coherent accessibility-first design system, (c) a recovery-first onboarding and diagnostics model, and (d) a clean, permissively licensed architecture. FCL/Zalith are closest technically; their UX is built for power users.

**Do not compete on “more settings.”** Compete on **first-launch success**, **time-to-play**, **clarity when things fail**, and **touch/controller ergonomics**.

---

## 5. Recommended architecture (decision summary)

### 5.1 Clean-room core, proven native components

- Write the app, installer, auth, launcher planner, and UI from scratch in Kotlin.
- Use **OpenJDK 25 mobile** (GPLv2+CE) as the runtime.
- Use **LWJGL 3.4.3** (BSD-3) with Android builds of the official native libraries.
- Use **SDL 3.4.x** (zlib) for the 26.3 window/input path.
- Write **Sandpaper**, an original GL 3.3-core-on-GLES-3.2 compatibility runtime; no MobileGlues, NG-GL4ES, Zink or Turnip.
- Do **not** copy FCL/Zalith (GPL-3.0) code. Study behavior, reimplement.
- Keep the original code **Apache-2.0**; ship third-party notices and LGPL source/relink rights.

### 5.2 Two processes

```
:app process                          :game process
┌─────────────────────┐   Intent      ┌──────────────────────────┐
│ Compose UI          │ ────────────► │ GameActivity + Surface   │
│ ViewModels          │               │ ART: SDL3 Java bridge    │
│ Installer/Auth      │ ◄──────────── │ HotSpot 25: Minecraft    │
│ Download service    │   logs/state  │ SDL3 + EGL/Vulkan        │
└─────────────────────┘               └──────────────────────────┘
```

- The UI survives JVM/native crashes.
- The game process gets its own heap/LMK class.
- Launch config is immutable and serialized; no shared mutable state.
- Logs stream over a local socket/pipe with redaction.

### 5.3 Graphics policy: “Auto, with evidence”

```
Device probe:
  Vulkan 1.2 + all 26.3 extensions/features? ──yes──► Minecraft Vulkan backend
        │ no
        ▼
  GLES 3.2 present? ──yes──► Sandpaper (desktop GL 3.3 → GLES 3.2)
        │ no
        ▼
  Explain why; offer reduced settings and a compatibility screen
```

Never show six renderer names on first launch. Show **“Recommended for your device”**, a one-line reason, and an Advanced door. There is no Zink, Turnip or ANGLE rung: v1 uses the system drivers only, and Sandpaper is the single GL fallback.

### 5.4 Auth policy

- **OAuth 2.0 device code** against `login.microsoftonline.com/consumers`, public client, no secret.
- Scope: `XboxLive.signin offline_access`.
- Then Xbox Live → XSTS → `login_with_xbox` → entitlements → profile.
- Store only the refresh token, encrypted with an Android Keystore key.
- **Apply for Mojang AppID approval in Phase 0.** Without it, the auth chain ends in 403.
- Demo mode is a candidate only if 26.3 accepts `--demo` without a real token; piracy and offline authentication are not supported either way.

### 5.5 Distribution

- GitHub Releases (signed APK + SHA-256 + SBOM + changelog).
- Direct APK with an in-app update check (download + certificate/SHA-256 verification + `PackageInstaller`, user consent).
- **No Play at launch.** Revisit only with a legal opinion and a bundled-everything variant.
- **F-Droid later, as a separately reviewed flavor.** Its no-blob/reproducibility rules conflict with a downloaded JRE, so it is not a v1 promise.

---

## 6. Economics and schedule

### 6.1 Effort (small team: 2 Android, 1 native/graphics, 1 designer, part-time QA/legal)

| Phase | Scope | Estimate |
|---|---|---|
| 0 | Research sign-off, AppID application, JRE/SDL/Vulkan spikes | 3–4 weeks |
| 1 | App shell, design system, metadata, installer, downloads, instances | 5–7 weeks |
| 2 | JVM 25 launch, LWJGL/SDL window, vanilla 26.3, logs | 6–8 weeks |
| 3 | Auth + accounts + AppID integration | 3–4 weeks |
| 4 | Fabric/Quilt/NeoForge/Forge, Modrinth modpacks, content browser | 5–7 weeks |
| 5 | Renderer matrix, Vulkan auto, controls editor, overlay, performance | 6–8 weeks |
| 6 | Beta, hardening, docs, release | 4–5 weeks |
| **Total** | | **32–43 weeks** (8–10 months) |

A solo developer should expect **14–18 months** for the same scope, or a reduced v1 (vanilla + Fabric + Vulkan-first) in **9–12 months**.

### 6.2 Cost drivers

- **Native/JVM engineering** is the most expensive line; a JRE 25 Android port is not a weekend patch.
- **Device matrix** (Adreno/Mali/PowerVR, Android 12–16, 6–12 GB RAM) requires real hardware or a device farm.
- **Legal/compliance** (AppID, EULA, branding, LGPL/GPL, privacy) is non-trivial and should be budgeted.
- **Support** scales with device diversity; invest in diagnostics to reduce tickets.

### 6.3 Build vs buy

| Option | Pros | Cons | Verdict |
|---|---|---|---|
| Fork Amethyst | Fastest path to a working JVM/GL stack; LGPL | Behind 26.3; legacy architecture; LGPL obligations; hard to differentiate | Not recommended |
| Fork FCL/Zalith | Current 26.3, Java 25, Vulkan detection | GPL-3.0 forces our app GPL; legacy Pojav core; huge codebase | Not recommended for a new brand |
| Clean-room app + open native components | Control, license freedom, modern UX, maintainable | Highest up-front native cost | **Recommended** |
| Wrapper around official launcher | Minimal work | Not possible on Android; no official Android Java launcher | Rejected |

---

## 7. The two gates that can kill the schedule

### G1 — AppID allowlist

Mojang's notice: new third-party apps must request access via the review form; existing apps keep access; new apps are manually allowlisted. A 2026 developer report shows the exact failure: OAuth/XBL/XSTS succeed, then `login_with_xbox` returns **403 Invalid app registration** until the AppID is approved. [S9][S10][S30]

**Actions (Phase 0):**

1. Create the Entra app: personal accounts only, public client, no secret, name **EclipseLauncher** (no Minecraft/Mojang/Microsoft/Xbox/Live/Discord/Hypixel in the name).
2. Publish a real website/privacy policy/support contact.
3. Submit the AppID review form with client ID, tenant ID, website, justification, ownership-verification description.
4. Wait for weekly review; do not resubmit repeatedly.
5. In parallel, build the installer and a no-auth test path with a stub main class so the product is testable without auth.

**Fallback if denied:** the product cannot ship public Microsoft auth. Options: appeal with evidence; pivot to a “bring your own approved AppID” enterprise/developer build (not consumer-friendly); or stop. Do not ship a bypass or borrow another launcher's client ID.

### G2 — Distribution policy

Play's policy text is explicit: apps may not download executable code (dex, JAR, .so) from a source other than Play, except code that runs in a VM/interpreter providing indirect Android API access. A Minecraft launcher downloads `client.jar` and native runtime components. The VM exception is arguable but not a safe assumption. [S13]

**Actions:** ship signed GitHub Releases and a direct APK; keep the APK self-contained where possible; publish hashes/signatures; provide an in-app updater with explicit consent; revisit Play only with counsel.

---

## 8. Product design thesis

The category has a **trust and comprehension problem**. Users are told to pick a renderer, a JVM, a heap size, a driver, and a mod loader before they can play. Most failures are configuration failures, not code failures.

EclipseLauncher's design thesis:

1. **One primary action per screen.** Home = “Play” on the last instance. Everything else is secondary.
2. **Progressive disclosure.** Auto by default; advanced settings behind a clearly labeled door, with explanations.
3. **Evidence, not options.** “Vulkan is unavailable because your driver lacks `VK_KHR_push_descriptor`” beats a disabled toggle.
4. **Recovery is a feature.** Every error screen has: what happened, why, what we tried, and one recommended fix.
5. **Calm visual language.** Warm paper-and-ink surfaces, one amber accent, generous spacing, expressive but not noisy motion.
6. **Touch and controller are equal citizens.** 48 dp targets, safe areas, focus order, no hover-only affordances.
7. **Privacy is visible.** “Your token is encrypted on this device; we never see your password” is part of onboarding, not a policy page.

Visual direction: **“field instrument”** — warm neutral surfaces, deep ink, a thin eclipse arc, amber primary and teal secondary; rounded expressive shapes; restrained motion; no Minecraft textures or logos. Dynamic color is an opt-in, not the default.

---

## 9. Success metrics

| Metric | Target (beta) | Why |
|---|---|---|
| First-launch success (vanilla 26.3, supported device) | ≥ 90% | The core promise |
| Time-to-play after install (cached) | ≤ 20 s | Perceived speed |
| Crash-free game sessions | ≥ 95% | Stability |
| Download completion (10 MB+ assets) | ≥ 98% | Installer quality |
| Auth success (approved app, good network) | ≥ 97% | Trust |
| Support tickets per 1,000 launches | < 5 | Diagnostics quality |
| ANR rate | < 0.5% | Main-thread discipline |
| Accessibility: TalkBack-complete critical flows | 100% | Inclusion |
| Renderer fallback rate | tracked, not targeted | Learn device reality |

---

## 10. What we need from the user before scaffolding

1. **License choice:** Apache-2.0 clean-room (recommended) vs GPL-3.0 ecosystem reuse.
2. **Distribution:** GitHub Releases + direct APK (recommended), F-Droid later, no Play at launch.
3. **Auth applicant:** who owns the Entra tenant/domain and will submit the AppID form.
4. **Device priority:** arm64-only v1 (recommended) vs arm64 + armeabi-v7a + x86_64.
5. **Design direction:** approve the “field instrument” thesis and the Home/Instances/Discover/Downloads/Settings IA.
6. **Scope:** vanilla + Fabric + Modrinth for v1 (recommended) vs all loaders/CurseForge at launch.

If these are accepted, Phase 0 starts with the five spikes and the AppID submission — not with UI code.

---

## Sources for this brief

- [S1] Mojang version manifest — https://piston-meta.mojang.com/mc/game/version_manifest_v2.json
- [S2] 26.3 version JSON — https://piston-meta.mojang.com/v1/packages/61fff1316bd50312624a858bbb5eb18fa45208ed/26.3.json
- [S3] Java Edition 26.3 — https://minecraft.wiki/w/Java_Edition_26.3
- [S4] Java Edition 26.2 — https://minecraft.wiki/w/Java_Edition_26.2
- [S5] Java Edition 26.1 — https://minecraft.wiki/w/Java_Edition_26.1
- [S6] Microsoft authentication — https://minecraft.wiki/w/Microsoft_authentication
- [S7] Device code flow — https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-device-code
- [S9] AppID review notice — https://help.minecraft.net/hc/en-us/articles/16254801392141
- [S10] AppID review form — https://aka.ms/mce-reviewappid
- [S11] EULA — https://www.minecraft.net/en-us/eula
- [S12] Usage guidelines — https://www.minecraft.net/en-us/usage-guidelines
- [S13] Play Device and Network Abuse — https://support.google.com/googleplay/android-developer/answer/16559646
- [S14] Android 16 KB pages — https://developer.android.com/guide/practices/page-sizes
- [S15] Play target API — https://developer.android.com/google/play/requirements/target-sdk
- [S18] M3 Expressive — https://m3.material.io/blog/building-with-m3-expressive
- [S23] LWJGL 3.4.3 — https://github.com/LWJGL/lwjgl3/releases/tag/3.4.3
- [S24] Amethyst — https://github.com/AngelAuraMC/Amethyst-Android
- [S25] FCL — https://github.com/FCL-Team/FoldCraftLauncher
- [S26] Zalith 2 — https://github.com/ZalithLauncher/ZalithLauncher2
- [S30] 2026 403 report — https://learn.microsoft.com/en-my/answers/questions/5984225/minecraft-services-http-403-invalid-app-registrati
