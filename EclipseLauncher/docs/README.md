# EclipseLauncher — Research Dossier

**Status:** dossier complete (chapters 00–14); architecture signed off, implementation in progress. The current build is a single-screen launcher shell with a real Vanilla install path and no launch runtime yet.
**Research date:** 2026-10-24
**Target:** Android (phone, tablet, foldable, Chromebook; controller and touch first)
**Game target:** Minecraft: Java Edition, current release **26.3** (2026-09-15) and current snapshot **26.4-snapshot-5** (2026-10-20)

---

## Why this dossier exists

Building an Android Minecraft: Java Edition launcher is not one problem. It is at least eight coupled problems:

1. **Game metadata and installation** — Mojang's manifest, version JSON, libraries, assets, integrity.
2. **A JVM on Android** — a custom OpenJDK 25 port, launched inside an Android app process.
3. **A window and graphics stack** — EGL, SDL3, LWJGL, desktop OpenGL translation, and now Minecraft's own Vulkan backend.
4. **Input and audio** — touch, keyboard, mouse, gamepads, IME, OpenAL.
5. **Authentication** — Microsoft OAuth → Xbox Live → XSTS → Minecraft Services, plus Mojang's AppID allowlist.
6. **Mod loaders and modpacks** — Fabric, Quilt, NeoForge, Forge, OptiFine, Modrinth/CurseForge formats.
7. **Android platform law** — scoped storage, 16 KB pages, target API 36, foreground services, Play policy.
8. **Product design** — making a technical tool understandable, recoverable, and pleasant on a 6-inch touch screen.

This dossier treats each layer with primary-source evidence, then proposes a product and system architecture. It deliberately separates **verified facts**, **source-code observations**, **inferences**, and **open questions**.

---

## The ten findings that matter most

1. **Minecraft 26.3 is a platform break, not a normal update.** It requires **Java 25**, uses **SDL3 instead of GLFW**, and ships **LWJGL 3.4.3** with SDL, Shaderc, SPIR-V Cross and Vulkan modules. The old GLFW-only Android launcher recipe no longer covers current Minecraft. [26.3 JSON](https://piston-meta.mojang.com/v1/packages/61fff1316bd50312624a858bbb5eb18fa45208ed/26.3.json), [26.3 wiki](https://minecraft.wiki/w/Java_Edition_26.3)

2. **Minecraft now has an experimental Vulkan renderer.** 26.2 added a Graphics API setting (`Default`, `Prefer Vulkan`, `Prefer OpenGL`); 26.3 continues it. On Android this is both an opportunity (native Vulkan, no GL translation) and a compatibility minefield (Vulkan 1.2 features, driver quality, SDL surface integration). [26.2 wiki](https://minecraft.wiki/w/Java_Edition_26.2)

3. **The official 26.3 libraries do not contain Android natives.** The JSON has 114 library entries, but the selected Linux set is x86_64; there is no `natives-linux-arm64` classifier. An Android launcher must supply its own LWJGL/SDL/OpenAL/FreeType natives built for Android ABIs. This is the single largest native-engineering cost.

4. **Java 25 is required and the default JVM flags are hostile to phones.** 26.3's `default-user-jvm` includes `-Xms2G -Xmx4G`, `-XX:+AlwaysPreTouch`, and (on Linux) `-XX:+UseZGC`. Applied naively on Android this will OOM, over-commit memory, or fail at startup. The launcher must parse, filter, and replace these flags. [26.3 JSON](https://piston-meta.mojang.com/v1/packages/61fff1316bd50312624a858bbb5eb18fa45208ed/26.3.json)

5. **New third-party apps need Mojang AppID approval.** Since 2023, Mojang manually allowlists new applications that call Java Edition game service APIs. Unapproved Entra apps receive **HTTP 403 “Invalid app registration”** at `login_with_xbox`. Approval is weekly and requires ownership verification, a real app identity, and a compliant name. This is a **go/no-go gate**, not a paperwork detail. [Mojang notice](https://help.minecraft.net/hc/en-us/articles/16254801392141), [review form](https://aka.ms/mce-reviewappid), [2026 Q&A report](https://learn.microsoft.com/en-my/answers/questions/5984225/minecraft-services-http-403-invalid-app-registrati)

6. **Google Play is a poor fit.** Play's Device and Network Abuse policy forbids downloading executable code (dex/JAR/.so) from outside Play, with a narrow VM/interpreter exception. A launcher downloads `client.jar` and native runtimes by design. Distributing on Play would require a legal/policy review and likely a bundled-everything design; the v1 channels are **GitHub Releases and a direct APK**, with F-Droid only as a later, separately reviewed flavor. [Play policy](https://support.google.com/googleplay/android-developer/answer/16559646)

7. **Android 16 / target API 36 and 16 KB pages are mandatory for Play, and good hygiene regardless.** New apps and updates must target API 36 from 2026-08-31; 16 KB-compatible 64-bit native code is required for Play updates from 2027-02-01. AGP 8.5.1+ and NDK r28+ handle ELF alignment; every `.so` we ship (JVM, LWJGL, SDL3, renderers, OpenAL) must comply. [target API](https://developer.android.com/google/play/requirements/target-sdk), [16 KB](https://developer.android.com/guide/practices/page-sizes)

8. **The Android launcher market is technically mature but experience-immature.** Amethyst (LGPL-3.0, 1.1.7) is the Pojav successor but only claims 26.3-snapshot-1 support. FCL (GPL-3.0, 1.3.4.1) and Zalith 2 (GPL-3.0, 2.7.2) already handle Java 25, SDL3 and Vulkan detection, but they carry legacy Pojav-era architecture and settings-heavy UX. There is room for a **clean, adaptive, recovery-first** launcher.

9. **The design system opportunity is real.** Material 3 Expressive (14 new/updated components, 35 shapes, motion-physics springs, emphasized type) gives a coherent vocabulary for a launcher that feels calm rather than gamer-neon. Existing Android launchers use either legacy XML preferences (Amethyst) or custom Compose skins (Zalith), not a governed, accessible design system. [M3 Expressive](https://m3.material.io/blog/building-with-m3-expressive)

10. **The product should be honest about scope.** A polished launcher is a 6–9 month effort for a small team, with the critical path running through **AppID approval**, **JRE 25 on Android**, and **SDL3/Vulkan integration** — not through the UI. The UI is the part we can control; the external gates are the parts that can stop the project.

---

## Recommended direction (for sign-off)

| Decision | Recommendation | Why |
|---|---|---|
| Distribution | GitHub Releases + direct APK; **F-Droid later, no Play at launch** | Play executable-code policy conflicts with the core product; F-Droid's no-blob/reproducibility rules need a separate flavor because the JRE is downloaded |
| License | **Apache-2.0** for original code; LGPL/MIT components dynamically linked with notices | Clean-room avoids GPL contamination from FCL/Zalith; permissive core invites contributors and OEM/community reuse |
| UI | Kotlin + Jetpack Compose + **Material 3 (1.4.x) + M3 Expressive**, project-owned back stack, adaptive layouts | Current stable stack; M3 Expressive gives a differentiated, accessible visual language |
| Process model | UI process + isolated `:game` process | JVM crashes, native driver crashes, and OOM kills must not take down the launcher |
| Runtime | EclipseJRE, a custom **OpenJDK 25** per ABI, downloaded and installed as a verified read-only component | 26.3 requires Java 25; no official Android JRE exists, and a 120–180 MB bundled APK is not the default |
| Graphics | **Auto**: native Vulkan 1.2 when the probe passes, otherwise **Sandpaper** (desktop GL 3.3 → GLES 3.2); system drivers only | Matches 26.3's own backend selector without betting the product on driver replacement |
| Auth | **OAuth 2.0 device code** with our own Entra public client; apply for AppID allowlist immediately | No redirect hijack surface, no client secret, best for touch/controller; allowlist is mandatory |
| Content | **Modrinth only in v1**; CurseForge deferred to v1.1; no bundled game assets | Licensing and API terms; Modrinth has a clean public API and `.mrpack` format |
| Scope | Phone/tablet first; controller + touch; no Bedrock, no piracy, no iOS | Focus; avoids legal and engineering sprawl |

**Go/no-go gates**

- **G1 — AppID:** submit the Entra app + review form before Phase 3; no approval, no public auth.
- **G2 — JRE 25:** boot Java 25 on arm64 Android and run a trivial JavaFX/Swing-free class; then boot Minecraft 26.3.
- **G3 — SDL3 window:** create an SDL3 window + EGL surface + input on a real device.
- **G4 — Vulkan probe:** detect 26.3's required Vulkan features on the target device matrix; confirm fallback.
- **G5 — Design sign-off:** approve IA, visual direction, and the “one-tap launch” flow.

---

## Document map

| # | Document | What it answers |
|---|---|---|
| 00 | [Executive brief](00-executive-brief.md) | The short version, decisions, gates, and economics |
| 01 | [Landscape and competitors](01-landscape.md) | Who exists, what they do, where the gap is |
| 02 | [Minecraft platform](02-minecraft-platform.md) | Versions, 26.x changes, metadata, launch contract, Java, libraries, assets |
| 03 | [Mod loaders and content](03-mod-loaders-and-content.md) | Fabric/Quilt/NeoForge/Forge, modpacks, APIs, install algorithms |
| 04 | [Android platform](04-android-platform.md) | Storage, SAF, native code, 16 KB, Play, performance, thermals |
| 05 | [Runtime and graphics](05-runtime-and-graphics.md) | Two-VM model, EclipseJRE, JVM launch, SDL3, Vulkan, Sandpaper |
| 06 | [Authentication and compliance](06-authentication.md) | Microsoft chain, AppID allowlist, tokens, EULA, branding |
| 07 | [Install pipeline](07-install-pipeline.md) | Planner, downloader, integrity, recovery, modpack install |
| 08 | [Product design](08-product-design.md) | Strategy, IA, flows, screens, onboarding, in-game overlay |
| 09 | [Design system](09-design-system.md) | Tokens, type, color, shape, motion, components, Compose architecture |
| 10 | [Design research and skills](10-design-research-and-skills.md) | References, skills matrix, design ops, validation |
| 11 | [System architecture](11-system-architecture.md) | Modules, processes, data model, concurrency, security, observability |
| 12 | [Delivery plan](12-delivery-plan.md) | Roadmap, test matrix, risk register, decisions needed |
| 13 | [Sources](13-sources.md) | Consolidated bibliography with access dates |
| 14 | [Glossary](14-glossary.md) | Terms used across the dossier |
| — | [Release notes](release-notes.md) | User-facing notes for the current GitHub Release |

---

## Evidence standard

Every non-obvious claim is labeled one of:

- **Verified** — read from a primary source (Mojang JSON, Microsoft docs, Android docs, license text) on 2026-10-24.
- **Observed** — read from a primary artifact or measured on a device (LWJGL native jars, `client-26.3.jar`, Amethyst/FCL releases, the API 36 test device).
- **Inference** — reasoned from verified facts; needs a spike or device test.
- **Open** — unknown or changing; tracked in the risk register.

Where a fact is likely to change (versions, Play deadlines, loader versions), the document gives the **verification date** and the command/URL used to re-check it.

---

## What “sign-off” means

Sign-off is not “the research is perfect.” It means:

1. The product scope and non-goals are accepted.
2. The two external gates (AppID, distribution policy) are understood as schedule risks.
3. The recommended architecture and license are accepted, or a different option is chosen.
4. The Phase 0 spikes are approved, with real devices and a time box.

Until then, no scaffold, no dependency lock-in, and no branding assets are committed.
