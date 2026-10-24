# 01 — Landscape and competitor teardown

**Verified:** 2026-10-24. Stars/releases from GitHub API; source claims from repository READMEs and code.

---

## 1. Market shape

The Android Java Edition launcher market is a **small, technical, community-driven niche** with four live products and one archived ancestor:

| Product | Role | License | Stars | Latest release | Release date |
|---|---|---|---|---|---|
| **PojavLauncher** | The ancestor; archived 2025-09-23 | LGPL-3.0 | 9,456 | — | — |
| **Amethyst** | Pojav's designated Android successor | LGPL-3.0 | 2,332 | 1.1.7 | 2026-07-31 |
| **FCL (Fold Craft Launcher)** | HMCL-core + Amethyst backend; feature leader | GPL-3.0 | 5,031 | 1.3.4.1 | 2026-10-20 |
| **Zalith Launcher 2** | Compose/M3 redesign on Pojav core | GPL-3.0 | 2,093 | 2.7.2 | 2026-10-20 |
| **Boardwalk** | Historical proof of concept (2014) | Apache-2.0/GPL-2.0 | — | — | — |

The numbers are small compared with mainstream apps, but the users are highly engaged, technically demanding, and vocal. Trust is the currency: a broken launch after a Minecraft update produces immediate reputational damage.

**Structural facts:**

- The same three native problems recur in every product: **JVM port**, **graphics translation**, **input bridge**.
- Most projects reuse Pojav/Amethyst native code or its descendants; genuinely independent stacks are rare.
- Minecraft updates now arrive on a **year.drop.hotfix** cadence (26.1, 26.2, 26.3), so launchers must ship compatibility patches several times a year.
- 26.3's SDL3 migration invalidated assumptions that had been stable since 2014.

---

## 2. Competitor matrix

Legend: ✅ supported, ◐ partial/with caveats, ❌ not supported, ? not verified.

| Capability | Amethyst | FCL | Zalith 2 | Eclipse target |
|---|---|---|---|---|
| Minecraft 26.3 release | ❌ (README claims up to 26.3-snapshot-1) | ✅ (Vulkan path; OpenGL renderers capped at 26.3-snapshot-3) | ✅ (same pattern) | ✅ |
| Java 25 runtime | ❌ (README: 8/17/21) | ✅ (8/17/21/25) | ✅ (JRE_25) | ✅ |
| SDL3 window/input | ◐ (SDL3 headers, sdl2-compat, LWJGL 3.4.1) | ✅ | ✅ | ✅ |
| Native Vulkan backend | ❌ | ✅ detection + game setting | ✅ detection + game setting | ✅ auto + fallback |
| OpenGL translation | ✅ GL4ES/MobileGlues/Zink/LTW | ✅ NG-GL4ES/GL4ES/VirGL/VGPU/Zink/Freedreno + plugins | ✅ multiple renderers | ✅ MobileGlues/NG-GL4ES/Zink |
| Custom Vulkan drivers | ◐ | ✅ driver plugins (Turnip etc.) | ✅ driver plugins | ◐ optional |
| Fabric / Quilt | ✅ / ◐ | ✅ / ✅ | ✅ / ✅ | ✅ / ✅ |
| Forge / NeoForge | ✅ / ◐ | ✅ / ✅ | ✅ / ✅ | ✅ / ✅ |
| OptiFine | ✅ | ✅ | ✅ | ◐ user-supplied |
| Modrinth modpacks | ◐ | ✅ | ✅ | ✅ v1 |
| CurseForge modpacks | ✅ | ✅ | ✅ | ◐ API/key or import |
| Content browser | ◐ | ✅ | ✅ | ✅ Modrinth first |
| Touch controls editor | ✅ | ✅ | ✅ | ✅ |
| Gamepad | ✅ | ✅ | ✅ | ✅ |
| In-game overlay | ◐ | ✅ | ✅ | ✅ |
| Adaptive tablet/foldable UI | ❌ | ◐ | ◐ | ✅ |
| M3 Expressive design system | ❌ | ❌ | ◐ custom M3 | ✅ |
| Accessibility (TalkBack, large text) | ◐ | ◐ | ◐ | ✅ first-class |
| Crash recovery / repair | ◐ | ✅ | ✅ | ✅ |
| Direct APK / F-Droid | ✅ / ◐ | ✅ / ❌ | ✅ / ❌ | ✅ / ✅ |
| Google Play | ❌ | ❌ | ❌ | ❌ (policy) |
| License | LGPL-3.0 | GPL-3.0 | GPL-3.0 | Apache-2.0 (proposed) |

> **Caution:** “supported” above means the code has a path for it, not that every device works. Android graphics is a matrix, not a boolean.

---

## 3. Teardown: Amethyst

### 3.1 What it is

Amethyst is the community successor to PojavLauncher, forked from Pojav's `v3_openjdk` branch. It keeps the Pojav architecture:

- Java/Kotlin Android app with XML layouts and `PreferenceFragment` settings.
- A `libpojavexec.so` native launcher that loads a custom `libjvm.so` and calls `JLI_Launch`.
- A custom LWJGL 3 Java layer (`jre_lwjgl3glfw`) that replaces GLFW with a bridge to Android.
- Renderer `.so` plugins: GL4ES/NG-GL4ES, MobileGlues, Zink/OSMesa, Kopper/glxshim, LTW, system GLES.
- SDL2/SDL3 integration for gamepads and, partially, for 26.3's window path.

### 3.2 Strengths

- **Proven JVM/native launch path** across Android 8–16 and four ABIs.
- **Broad renderer coverage**, including Zink and VirGL for difficult devices.
- **Mature controls system** with custom layouts, gamepad mapping, and gesture handling.
- **LGPL**, which is friendlier than GPL for reuse.

### 3.3 Weaknesses

- **Compatibility lag:** README claims support only through `26.3-snapshot-1`; 26.3 final changed SDL/window behavior further.
- **No Java 25 in the stable runtime set** (8/17/21), while 26.1+ requires 25.
- **Legacy UI:** settings-heavy preference screens, limited adaptive layout, weak onboarding.
- **Same-process JVM:** a native/JVM crash can take down the UI.
- **Native build complexity:** submodules (SDL, MobileGlues, androidnsbypass) are not in the source tarball; reproducing the build is hard.

### 3.4 Lessons for Eclipse

- The `JLI_Launch` model works; keep it, but isolate it in a `:game` process.
- Renderer plugins must be **dynamically loadable** and independently updatable.
- Do not let the compatibility matrix become a README claim; make it **runtime-detected**.

---

## 4. Teardown: FCL (Fold Craft Launcher)

### 4.1 What it is

FCL is the feature leader. It ports **HMCL's core** (`org.jackhuang.hmcl`) to Android and uses an Amethyst-derived backend for JVM launch and rendering. It is a hybrid: Java/Kotlin, XML fragments, some Compose, and a large `fclcore` module.

### 4.2 Strengths

- **Deep installer**: full version/mod-loader/modpack handling, mirror support, Java runtime management (8/17/21/25).
- **Current compatibility**: references to 26.3, NeoForge 12, SDL input, Vulkan detection, and per-version graphics backend.
- **Renderer plugin system** and **driver plugin system** (Turnip, etc.).
- **Content ecosystem**: Modrinth/CurseForge, mod updates, modpack import, resource packs, shaders, worlds.
- **Remote/local multiplayer** via Terracotta/EasyTier (differentiating, but AGPL/LGPL dependencies).
- **Large community** (5k stars, frequent releases).

### 4.3 Weaknesses

- **GPL-3.0**: any code reuse forces the entire launcher to GPL; not compatible with a permissive clean-room plan.
- **Legacy architecture**: HMCL core is desktop-oriented; Android lifecycle, process isolation, and Compose state are bolted on.
- **UI density**: many settings, long lists, inconsistent visual hierarchy; power-user oriented.
- **Build size/complexity**: huge source tree, many native dependencies, difficult to audit.
- **Same-process/JVM legacy**: crash isolation is limited.

### 4.4 Lessons for Eclipse

- The **installer is the product**. Invest in a clean planner and repair flow, not just a download button.
- Per-version **graphics backend** and **renderer** settings are necessary; hide them behind Auto.
- Driver plugins are valuable but a security/support liability; sandbox and verify them.

---

## 5. Teardown: Zalith Launcher 2

### 5.1 What it is

Zalith 2 is a **from-scratch UI** on top of the Pojav launch engine. It uses Kotlin, Jetpack Compose, Material 3, and **Navigation 3**, with a custom theme and component library. It is the closest thing to a modern Android launcher UX.

### 5.2 Strengths

- **Modern UI architecture**: Compose, Navigation 3, adaptive-ish layouts, custom design tokens.
- **Current runtime**: Java 25, SDL3, LWJGL 3.4.1, Vulkan feature detection, graphics API override (`preferredGraphicsBackend`).
- **Good power features**: task system, logs, control editor, modpack presets, driver plugins.
- **Active** (2.7.2, 2026-10-20).

### 5.3 Weaknesses

- **GPL-3.0** with additional naming terms; cannot be reused in a permissive product.
- **Pojav core** still carries legacy assumptions (GLFW stub, same-process JVM, native extraction).
- **UX is still settings-forward**: many screens, dense controls, limited onboarding/recovery storytelling.
- **Tablet/foldable** support is partial; phone layout is the primary design.
- **No design-system governance**: components are app-specific, not tokenized for reuse.

### 5.4 Lessons for Eclipse

- Compose + Navigation 3 + M3 is the right UI stack.
- A **task system** (global downloads/installs) is essential; make it a first-class surface, not a notification.
- **Vulkan detection** must be a capability model, not a single “supported” flag.

---

## 6. Teardown: PojavLauncher and Boardwalk

### 6.1 Pojav

Pojav proved the core idea: run a full desktop JVM inside an Android app, load LWJGL, translate OpenGL to GLES, and bridge input. It was archived in September 2025 after a reputational/community crisis. Its code is LGPL-3.0 and remains the ancestor of Amethyst/FCL/Zalith native layers.

**Lesson:** technical success is not enough; governance, trust, and maintenance matter.

### 6.2 Boardwalk

Boardwalk (2014) was the first credible Android Java Edition launcher. It used a custom JVM and a minimal UI. It is historically important but not a practical base.

**Lesson:** the hard part has always been the runtime, not the launcher list.

---

## 7. Desktop references (design, not code)

These are not Android competitors, but they define user expectations:

| Product | What to borrow | What not to copy |
|---|---|---|
| **Prism Launcher** | Instance-centric model, mod metadata, log viewer, portable instances | Desktop density; modal-heavy settings |
| **Modrinth App** | Content discovery, one-click install, clean visual hierarchy | Web-app spacing; account assumptions |
| **HMCL** | Installer robustness, mirror support, Java management | Desktop Java UI; configuration sprawl |
| **Official Minecraft Launcher** | “Play” as the primary action, version simplicity | Limited mod/modpack support; account coupling |
| **Steam Deck / SteamOS** | Controller-first navigation, suspend/resume, “return to game” | PC-only assumptions |
| **Xbox app** | Profile/social surfaces, remote download queue | Console account model |
| **Play Store** | Download progress, pause/resume, update semantics | Store chrome; ratings/reviews |

---

## 8. Where the gap is

### 8.1 Experience gap

The category optimizes for **capability** (“we support X renderer, Y loader, Z driver”) rather than **outcome** (“it launched”). New users are confronted with decisions they cannot evaluate:

- Which renderer?
- Which Java?
- How much RAM?
- Which graphics API?
- Which driver?
- Which loader version?

Every one of these is a support ticket waiting to happen. A launcher that makes these **automatic, explainable, and reversible** wins.

### 8.2 Trust gap

- Auth failures are opaque (“login failed”).
- Tokens and logs are often stored or displayed without redaction.
- Compliance with Mojang's AppID process is invisible to users.
- Update channels are unclear; APK provenance is weak.

A launcher that is **transparent about auth, updates, and data** can differentiate.

### 8.3 Platform gap

- No Android launcher has a coherent **tablet/foldable/Chromebook** layout.
- Accessibility is an afterthought (small targets, poor TalkBack labels, motion without reduction).
- Controller-first flows are partial; touch is primary, gamepad is mapped, but the UI is not focus-designed.
- No launcher exposes **why** a device cannot run Vulkan or a shader pack.

### 8.4 Technical gap

- 26.3's SDL3 transition is only partially absorbed by Amethyst; FCL/Zalith patch around it.
- Java 25 mobile runtimes are custom and fragile; reproducibility is poor.
- Vulkan is detected but not intelligently selected/fallback-managed.
- Native libraries are not consistently 16 KB-aligned or audited.

---

## 9. Positioning statement

> **EclipseLauncher is the calm, reliable way to play Minecraft: Java Edition on Android.**
> It picks the right runtime, renderer, and settings for your device, launches in one tap, and when something breaks it tells you exactly what happened and how to fix it.
> Built for touch, controller, and tablets — not ported from a desktop launcher.

### 9.1 Pillars

1. **One-tap launch.** Auto JVM, heap, renderer, graphics API, and loader.
2. **Recovery-first.** Every failure has a diagnosis and a repair action.
3. **Adaptive by design.** Phone, tablet, foldable, Chromebook; touch and controller.
4. **Transparent.** Open source, signed releases, redacted logs, clear auth/data story.
5. **Current.** 26.3+, Java 25, SDL3, Vulkan-aware, 16 KB-ready.

### 9.2 Non-goals (v1)

- Bedrock, iOS, console, server hosting.
- Piracy/offline auth.
- Play Store distribution.
- A general-purpose mod manager for games other than Minecraft.
- A social network/chat platform.
- A shader development IDE.

---

## 10. Competitive risks

| Risk | Likelihood | Impact | Response |
|---|---|---|---|
| FCL/Zalith ship the same UX improvements first | Medium | Medium | Move fast on design; differentiate on recovery/accessibility, not features |
| Amethyst catches up on 26.3/Java 25 | Medium | Low | Its LGPL/legacy architecture remains; compete on experience |
| A new well-funded launcher appears | Low | High | Build community trust and a permissive license; ship quality |
| Mojang changes auth/AppID rules again | Medium | High | Abstract auth; monitor notices; design demo mode |
| Minecraft changes window/graphics stack again | Medium | High | Keep renderer/window layers pluggable; track snapshots |
| Device vendors break Vulkan/GLES | High | Medium | Capability probing + fallbacks + device reports |

---

## 11. Sources

- Amethyst repository and README — https://github.com/AngelAuraMC/Amethyst-Android
- FCL repository and README — https://github.com/FCL-Team/FoldCraftLauncher
- Zalith Launcher 2 repository and README — https://github.com/ZalithLauncher/ZalithLauncher2
- PojavLauncher repository — https://github.com/PojavLauncherTeam/PojavLauncher
- Boardwalk — https://github.com/zhuowei/Boardwalk
- GitHub REST API (stars/releases), queried 2026-10-24
- FCL `RendererManager.kt`, `VulkanDependency.kt`, `FCLGameLauncher.java` (source snapshot)
- Zalith `VulkanDependency.kt`, `GameHandler.kt`, `Launcher.kt` (source snapshot)
- Amethyst `JREUtils.java`, `Tools.java`, `README.md` (source snapshot)
- Prism Launcher — https://github.com/PrismLauncher/PrismLauncher
- Modrinth App — https://github.com/modrinth/code
- HMCL — https://github.com/HMCL-dev/HMCL
