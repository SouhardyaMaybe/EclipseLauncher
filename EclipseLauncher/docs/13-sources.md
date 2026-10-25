# 13 — Sources

**Status:** bibliography for the dossier, 2026-10-24.
**Scope:** every source that materially supports a claim in chapters 00–12, what it can and cannot prove, when it must be re-checked, and which claims remain unverified.
**Access date:** all URLs were opened on **2026-10-24** unless a row says otherwise.
**Rule:** a source is listed because a chapter relies on it, not to decorate the bibliography. Competitive repositories are listed as **behavioural evidence only**; no code or asset from them is shipped.

---

## 1. How to read this chapter

Three kinds of evidence appear in the dossier:

| Kind | Example | What it proves |
|---|---|---|
| **Primary document** | Microsoft device-code docs, Android 16 behaviour changes, Mojang version JSON | The contract or platform rule, at the access date |
| **Primary artifact** | `client-26.3.jar`, LWJGL native jars, the asset index, a device measurement | What the software actually contains or does |
| **Secondary observation** | Minecraft Wiki, a competitor's release notes, an issue tracker | Where to look and what to test; never sufficient alone for a hard claim |

A wiki is a map, not the territory. Where the dossier makes a hard claim about Minecraft behaviour, it cites the version JSON, the client artifact or Mojang's own documentation. The Minecraft Wiki is used to find the endpoint and the error code, then the endpoint or a primary document is cited where possible.

**Licensing note:** the Minecraft Wiki is CC BY-SA 3.0. The dossier paraphrases facts (endpoint names, error codes) and does not copy article text. Any future reuse of wiki text must carry the license and attribution.

---

## 2. Verification log (2026-10-24)

These are the checks actually run while writing the dossier. They are reproducible with `curl` and Python; no local build was used.

| Check | Command / URL | Result |
|---|---|---|
| Current release and snapshot | `GET https://piston-meta.mojang.com/mc/game/version_manifest_v2.json` | release `26.3`; snapshot `26.4-snapshot-5`; snapshot JSON SHA-1 `2ee2d20f…` |
| 26.3 version JSON | `GET https://piston-meta.mojang.com/v1/packages/61fff1316bd50312624a858bbb5eb18fa45208ed/26.3.json` | `javaVersion.majorVersion=25`; `mainClass=net.minecraft.client.main.Main`; 114 libraries |
| Client jar | JSON `downloads.client` | 41,483,720 B; SHA-1 `e877b6a07acd633fb3bb475002175cec036e7b87` |
| Asset index | `GET https://piston-meta.mojang.com/v1/packages/0c8f43b28f837869c1e54a6aeb22c396a1548946/34.json` | 5,147 objects; all hashes unique; 484,894,038 B; index 597,035 B |
| Linux library selection | Python rule evaluator over the 26.3 JSON | 66 selected; 55 Java artifacts (69.6 MiB); 11 native artifacts (9.9 MiB) |
| Fabric loader | `GET https://meta.fabricmc.net/v2/versions/loader/26.3` | 255 builds; latest stable `0.20.1` |
| Fabric profile | `GET https://meta.fabricmc.net/v2/versions/loader/26.3/0.20.1/profile/json` | id `fabric-loader-0.20.1-26.3`; main `…knot.KnotClient`; 7 libraries; `-DFabricMcEmu= net.minecraft.client.main.Main` |
| Quilt loader | `GET https://meta.quiltmc.org/v3/versions/loader/26.3` | latest `0.20.0-beta.9`; profile main `org.quiltmc.loader.impl.launch.knot.KnotClient`; 12 libraries |
| NeoForge | `GET https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml` | newest 26.3 line `26.3.0.97`; metadata updated 2026-10-24 |
| Forge | `GET https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json` (earlier in the research) | 26.3 line observed; import-only, not re-verified in this pass |
| LWJGL native pins | Inspected the native jars inside LWJGL 3.4.3 | SDL `147a8ee` (3.4.14), OpenAL `7f9c6de`, FreeType 2.14.3 `0a0221a`, Shaderc `2c8cae7`, SPIRV-Cross `6c09849` |
| EclipseJRE reference | Amethyst OpenJDK 25 Android image, measured with `readelf`/`stat` | 159 MB; `lib/modules` 126,800,736 B; `libjvm.so` 19,387,112 B; all `PT_LOAD p_align 0x1000`; needs `libc++_shared.so`; no `DT_RUNPATH` |
| Test device | API 36, arm64, Android 16 | 5.3 GiB RAM; 4 KB pages; Amethyst JRE 25 boots; **not 16 KB** |
| Docs gate | `python3 scripts/check-docs.py` | passed (16 files: chapters 00–14 plus release notes) |

**What this log does not prove:** that any of it works on a 16 KB device, that the AppID is approved, that a given GPU driver is conformant, or that a 2027 Minecraft release keeps the same contract. Those are gates in [12](12-delivery-plan.md).

---

## 3. Minecraft, Mojang and the launcher contract

### 3.1 Primary metadata

| Source | URL | Used for |
|---|---|---|
| Version manifest v2 | <https://piston-meta.mojang.com/mc/game/version_manifest_v2.json> | Current release/snapshot, per-version JSON URL and SHA-1 |
| 26.3 version JSON | <https://piston-meta.mojang.com/v1/packages/61fff1316bd50312624a858bbb5eb18fa45208ed/26.3.json> | Libraries, arguments, asset index, Java 25, logging, downloads |
| 26.3 asset index `34` | <https://piston-meta.mojang.com/v1/packages/0c8f43b28f837869c1e54a6aeb22c396a1548946/34.json> | Object count, sizes, hashes, CDN layout |
| Asset CDN | <https://resources.download.minecraft.net/> | `objects/<2>/<hash>` download shape |
| Java runtime manifest | <https://launchermeta.mojang.com/v1/products/java-runtime/2ec0cc96c44e5a76b9c8b7c39df7210883d12871/all.json> | Mojang's own JRE component naming (`java-runtime-epsilon`, Java 25) |
| Launcher metadata root | <https://piston-meta.mojang.com/mc/game/version_manifest_v2.json> | The entry point for every version lookup |

**Why the JSON is the primary source:** the wiki can lag a release; the JSON is what the official launcher consumes. Every size, hash and argument in chapters 02 and 07 is read from it.

### 3.2 Release notes and behaviour

| Source | URL | Used for | Caveat |
|---|---|---|---|
| Java Edition 26.3 | <https://minecraft.wiki/w/Java_Edition_26.3> | Release date, headline changes, Vulkan/GL context | Wiki; corroborate with JSON where possible |
| Java Edition 26.2 | <https://minecraft.wiki/w/Java_Edition_26.2> | Graphics API selector, Vulkan introduction | Wiki |
| Java Edition 26.1 | <https://minecraft.wiki/w/Java_Edition_26.1> | Java 25 requirement, unobfuscated client | Wiki |
| Microsoft authentication | <https://minecraft.wiki/w/Microsoft_authentication> | XBL/XSTS/Minecraft chain, XErr codes, entitlements, profile | Wiki; endpoints and codes cross-checked against live requests where possible |
| Minecraft EULA | <https://www.minecraft.net/en-us/eula> | Distribution and commercial boundaries | Blocked from the research network on 2026-10-24; known text, re-read before release |
| Usage guidelines | <https://www.minecraft.net/en-us/usage-guidelines> | Brand/disclaimer requirements | Blocked from the research network; confirm the exact disclaimer before 1.0 |
| Mojang AppID notice | <https://help.minecraft.net/hc/en-us/articles/16254801392141> | New apps must be allowlisted; 403 behaviour | Blocked from the research network; content corroborated by the review form and a 2026 Q&A report |
| AppID review form | <https://aka.ms/mce-reviewappid> | The actual submission path | Redirect target can change; verify in Phase 0 |

**Blocked-source note:** `help.minecraft.net` and `www.minecraft.net` returned connection timeouts from the research network on 2026-10-24 (HTTP `000`), not 404s. The claims they support are treated as **Verified with a caveat** and must be re-read from an unblocked network before the release checklist is signed.

### 3.3 The 2026 AppID failure report

| Source | URL | Used for |
|---|---|---|
| Microsoft Q&A report | <https://learn.microsoft.com/en-my/answers/questions/5984225/minecraft-services-http-403-invalid-app-registrati> | The exact 403 `Invalid app registration` symptom after a successful OAuth/XBL/XSTS chain |

This is a secondary report, but it matches Mojang's notice and is the clearest public description of the failure mode. It is used to design the error state, not to prove the policy.

---

## 4. Loaders and content platforms

### 4.1 Fabric

| Source | URL | Used for |
|---|---|---|
| Loader list for 26.3 | <https://meta.fabricmc.net/v2/versions/loader/26.3> | Build count, stable version, Maven coordinates |
| Profile JSON | <https://meta.fabricmc.net/v2/versions/loader/26.3/0.20.1/profile/json> | Main class, libraries, JVM args, `inheritsFrom` |
| Meta API root | <https://meta.fabricmc.net/v2/versions/loader> | Version discovery shape |
| Fabric Loader repository | <https://github.com/FabricMC/fabric-loader> | Behaviour and license reference |
| Fabric Loader license | <https://github.com/FabricMC/fabric-loader/blob/master/LICENSE> | Apache-2.0 |

### 4.2 Quilt

| Source | URL | Used for |
|---|---|---|
| Loader list for 26.3 | <https://meta.quiltmc.org/v3/versions/loader/26.3> | Latest `0.20.0-beta.9` |
| Profile JSON | <https://meta.quiltmc.org/v3/versions/loader/26.3/0.20.0-beta.9/profile/json> | Main class, 12 libraries, empty game args |
| Quilt Maven | <https://maven.quiltmc.org/repository/release/> | Artifact host |
| Quilt Loader license | <https://github.com/QuiltMC/quilt-loader/blob/master/LICENSE> | Apache-2.0 |

### 4.3 NeoForge and Forge

| Source | URL | Used for |
|---|---|---|
| NeoForge Maven metadata | <https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml> | Version list; `26.3.0.97` latest on 2026-10-24 |
| NeoForge installer | <https://maven.neoforged.net/releases/net/neoforged/neoforge/26.3.0.97/neoforge-26.3.0.97-installer.jar> | Installer/processor model (not downloaded in full in this pass) |
| NeoForge docs/FAQ | <https://docs.neoforged.net/user/docs/faq> | Loader behaviour and support boundaries |
| NeoForge license | <https://github.com/neoforged/NeoForge/blob/1.21.x/LICENSE.txt> | LGPL-2.1 (verify the exact file at the pinned tag) |
| Forge promotions | <https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json> | Recommended/latest Forge builds |
| Forge installer | <https://maven.minecraftforge.net/net/minecraftforge/forge/26.3-66.1.0/forge-26.3-66.1.0-installer.jar> | Import-only profile shape |
| Forge license | <https://github.com/MinecraftForge/MinecraftForge/blob/1.20.x/LICENSE.txt> | LGPL-2.1 (verify at the pinned tag) |

**Caveat:** the Forge 26.3 installer and its processor list were not fully audited. v1 is import-only for Forge, so the risk is contained; a full audit is a v1.1 task.

### 4.4 Modrinth

| Source | URL | Used for |
|---|---|---|
| API docs | <https://docs.modrinth.com/api/> | Base URL, auth, rate-limit shape |
| Search operation | <https://docs.modrinth.com/api/operations/searchprojects/> | Search parameters and facets |
| Project versions | <https://docs.modrinth.com/api/operations/getprojectversions/> | Version selection, loaders, game versions |
| `.mrpack` format | <https://support.modrinth.com/en/articles/8802351-modrinth-modpack-format-mrpack> | `modrinth.index.json`, `files[]`, `overrides/`, dependencies |
| Modrinth code | <https://github.com/modrinth/code> | Format implementation reference (AGPL for the platform; **not** copied into the app) |
| CDN | <https://cdn.modrinth.com/> | File host allowlist |

**Licensing caution:** the Modrinth platform code is AGPL-3.0. The launcher uses the **public API and format**, not the code. No Modrinth source is copied; the format is implemented from the published specification.

### 4.5 CurseForge and other pack formats

| Source | URL | Used for |
|---|---|---|
| CurseForge API docs | <https://docs.curseforge.com/rest-api/> | Why v1 defers CurseForge (key, terms, rate limits) |
| API key policy | <https://support.curseforge.com/support/solutions/articles/9000208346-about-the-curseforge-api-and-how-to-apply-for-a-key> | Application process and obligations |
| CurseForge export help | <https://support.curseforge.com/support/solutions/articles/9000197908-exporting-a-modpack-for-curseforge-project-submission> | Pack structure |
| GDLauncher manifest format | <https://gdlauncher.com/docs/modpack-manifest-format> | Comparison only; not implemented in v1 |

### 4.6 Renderer-sensitive content

| Source | URL | Used for |
|---|---|---|
| Iris changelog | <https://modrinth.com/mod/iris/changelog> | Iris states Vulkan is not supported; Sodium/Iris are GL renderers |
| Iris site | <https://irisshaders.dev/> | Compatibility context |

**Use:** this is product evidence for the rule “a Vulkan-capable device may still need the GL path because of mods”, not a technical contract.

---

## 5. Microsoft identity, Xbox Live and Minecraft Services

| Source | URL | Used for |
|---|---|---|
| Device authorization grant | <https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-device-code> | Endpoint, polling, `authorization_pending`, `slow_down`, `expired_token`, `interval`, 15-minute default |
| Device login page | <https://microsoft.com/devicelogin> | User-facing verification URL |
| Microsoft token endpoint | <https://login.microsoftonline.com/consumers/oauth2/v2.0/token> | `consumers` tenant, public client, no secret |
| Device code endpoint | <https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode> | Request shape |
| Xbox Live user auth | <https://user.auth.xboxlive.com/user/authenticate> | `RpsTicket=d=<token>`, `RelyingParty=http://auth.xboxlive.com` |
| XSTS authorize | <https://xsts.auth.xboxlive.com/xsts/authorize> | `SandboxId=RETAIL`, `RelyingParty=rp://api.minecraftservices.com/` |
| Minecraft login | <https://api.minecraftservices.com/authentication/login_with_xbox> | `identityToken=XBL3.0 x=<uhs>;<xsts>` |
| Entitlements | <https://api.minecraftservices.com/entitlements/mcstore> | `game_minecraft` / `product_minecraft` |
| Profile | <https://api.minecraftservices.com/minecraft/profile> | UUID, name, skins, capes; 404 for no profile |

**Status:** the Microsoft document is primary and current (page updated 2026-06-15). The Xbox and Minecraft endpoints are documented publicly through the Minecraft Wiki and are used by every third-party launcher; they are not a versioned Microsoft contract and can change. The dossier treats response shapes as **Verified by the wiki plus common implementation**, and adds a contract-test suite against a mock server (doc 06 §12) so a change is detected at build time.

**Open:** the exact `--xuid` and `--clientId` requirements of 26.3 are not documented by Mojang. They are listed as open questions in doc 06 and must be tested with a real account before 1.0.

---

## 6. Android platform

### 6.1 Packaging, pages and native code

| Source | URL | Used for |
|---|---|---|
| 16 KB page sizes | <https://developer.android.com/guide/practices/page-sizes> | ELF `p_align`, zip alignment, Play deadline 2027-02-01, testing |
| Target API requirements | <https://developer.android.com/google/play/requirements/target-sdk> | API 36 deadline |
| `<application>` | <https://developer.android.com/guide/topics/manifest/application-element> | `extractNativeLibs`, `largeHeap`, `allowBackup` |
| Bionic changes for NDK | <https://android.googlesource.com/platform/bionic/+/master/android-changes-for-ndk-developers.md> | `dlopen`/`execve` restrictions, linker namespaces |
| App-specific storage | <https://developer.android.com/training/data-storage/app-specific> | `getExternalFilesDir`, no permission, USB visibility |
| Document provider / SAF | <https://developer.android.com/guide/topics/providers/document-provider> | Import/export boundary |

### 6.2 Lifecycle, processes and background work

| Source | URL | Used for |
|---|---|---|
| Process lifecycle | <https://developer.android.com/guide/components/activities/process-lifecycle> | LMK, process importance, why `:game` is separate |
| User-initiated data transfer | <https://developer.android.com/develop/background-work/background-tasks/uidt> | API 34+ download path |
| FGS types | <https://developer.android.com/develop/background-work/services/fgs/service-types> | `dataSync` vs `shortService`; why the installer is an activity |
| FGS timeouts | <https://developer.android.com/develop/background-work/services/fgs/timeout> | 6-hour `dataSync` cap, ~3-minute `shortService` |
| Android 16 changes | <https://developer.android.com/about/versions/16/behavior-changes-16> | Edge-to-edge, large-screen orientation, predictive back |
| Android 17 changes | <https://developer.android.com/about/versions/17/behavior-changes-17> | Unmodifiable `static final`, native DCL, local-network permission, touchpad, background audio |
| Background audio (17) | <https://developer.android.com/about/versions/17/behavior-changes-17#background-audio> | Why the game stops audio when backgrounded |

### 6.3 Display, input and performance

| Source | URL | Used for |
|---|---|---|
| Display refresh rate | <https://developer.android.com/games/optimize/display-refresh-rate-change> | `Surface.setFrameRate`, 60 Hz default, system clamping |
| ADPF thermal | <https://developer.android.com/games/optimize/adpf/thermal> | Thermal headroom polling |
| Pointer capture | <https://developer.android.com/develop/ui/views/touch-and-input/gestures/movement#pointer-capture> | Relative mouse deltas, capture from click/focus |
| Adaptive layouts | <https://developer.android.com/develop/ui/compose/layouts/adaptive> | Window size classes, list-detail |
| Accessibility | <https://developer.android.com/guide/topics/ui/accessibility> | TalkBack, switch access, font scale, semantics |
| Non-linear font scaling | <https://developer.android.com/about/versions/14/behavior-changes-14#non-linear-font-scaling> | 200% font testing |

---

## 7. JVM, graphics and native upstreams

### 7.1 EclipseJRE and OpenJDK

| Source | URL | Used for |
|---|---|---|
| OpenJDK 25 updates | <https://github.com/openjdk/jdk25u> | Upstream tag `jdk-25.0.4+4`, GPL-2.0+CE |
| EclipseJRE repository | <https://github.com/SouhardyaMaybe/EclipseJRE> | Project-owned build; current artifact is **not releasable** until rebuilt |
| Amethyst Android port | <https://github.com/AngelAuraMC/Amethyst-Android> | **Reference measurement only**; not shipped, not a source of code |
| Boardwalk | <https://github.com/zhuowei/Boardwalk> | Historical proof that a JVM can run on Android; not shipped |

**Provenance note:** Amethyst is LGPL-3.0 and FCL/Zalith are GPL-3.0. They are studied for behaviour and measured for facts (image size, `readelf` output). No code, patches, build scripts or assets are copied. The EclipseJRE build is re-derived from OpenJDK and the Android NDK.

### 7.2 LWJGL and the native stack

| Source | URL | Used for |
|---|---|---|
| LWJGL 3.4.3 | <https://github.com/LWJGL/lwjgl3/releases/tag/3.4.3> | Modules, native pins, BSD-3-Clause |
| LWJGL repository | <https://github.com/LWJGL/lwjgl3> | Library contract |
| SDL | <https://github.com/libsdl-org/SDL> | SDL 3.4.x source, zlib license, Android backend |
| OpenAL Soft | <https://github.com/kcat/openal-soft> | OpenSL backend, LGPL-2.0+ |
| OpenAL Soft site | <https://openal-soft.org> | Backend documentation |
| FreeType | <https://gitlab.freedesktop.org/freetype/freetype> | 2.14.3, FTL |
| Shaderc | <https://github.com/google/shaderc> | GLSL → SPIR-V, Apache-2.0 |
| SPIRV-Cross | <https://github.com/KhronosGroup/SPIRV-Cross> | SPIR-V → GLSL 330, Apache-2.0 |
| Vulkan Memory Allocator | <https://github.com/GPUOpen-LibrariesAndSDKs/VulkanMemoryAllocator> | VMA, MIT |
| Vulkan specification | <https://registry.khronos.org/vulkan/specs/latest/html/vkspec.html> | 1.2 feature and extension semantics |
| FFmpeg | <https://ffmpeg.org/> | Optional media plugin; LGPL/GPL boundary |

**Native pin policy:** the pins are read from LWJGL 3.4.3's native jars, not from memory. They are recorded in [doc 05 §5.1](05-runtime-and-graphics.md). EclipseNatives rebuilds those official upstreams for Android; it does not fork them.

### 7.3 Graphics contracts extracted from the client

| Artifact | Path | Used for |
|---|---|---|
| GL entry points | [`contracts/26.3-gl-functions.txt`](contracts/26.3-gl-functions.txt) | The 126 functions Sandpaper must provide |
| Vulkan contract | [`contracts/26.3-vulkan.json`](contracts/26.3-vulkan.json) | API 1.2, 5 required extensions, feature list |

These are original extractions from `client-26.3.jar` (class and string inspection), not copied source. They are the reviewable contract between the game and the renderer.

---

## 8. Design, accessibility and HCI

### 8.1 Platform and standards

| Source | URL | Used for |
|---|---|---|
| Material 3 | <https://m3.material.io/> | Components, color roles, shape, motion |
| M3 Expressive | <https://m3.material.io/blog/building-with-m3-expressive> | Expressive components, springs, emphasized type |
| WCAG 2.2 | <https://www.w3.org/TR/WCAG22/> | Contrast, target size, focus, dragging, consistent help |
| Android accessibility | <https://developer.android.com/guide/topics/ui/accessibility> | TalkBack, switch access, semantics |
| Android adaptive layouts | <https://developer.android.com/develop/ui/compose/layouts/adaptive> | Breakpoints and list-detail |

### 8.2 Usability and cognition

| Source | Reference | Used for |
|---|---|---|
| Nielsen, 10 heuristics | <https://www.nngroup.com/articles/ten-usability-heuristics/> | Visibility of status, error recovery, recognition over recall |
| Nielsen, error messages | <https://www.nngroup.com/articles/error-message-guidelines/> | Error copy structure |
| Nielsen, response times | <https://www.nngroup.com/articles/response-times-3-important-limits/> | 0.1 s / 1 s / 10 s thresholds |
| Hoober, how users hold phones | <https://www.uxmatters.com/mt/archives/2013/02/how-do-users-really-hold-mobile-devices.php> | Thumb reach; primary actions low |
| Fitts (1954) | *Journal of Experimental Psychology* 47(6), 381–391, doi:10.1037/h0055392 | Target size/distance |
| Hick (1952) | *Quarterly Journal of Experimental Psychology* 4(1), 11–26, doi:10.1080/17470215208416600 | Choice reaction time; five destinations |
| Sweller (1988) | *Cognitive Science* 12(2), 257–285, doi:10.1207/s15516709cog1202_4 | Cognitive load; progressive disclosure |

**Use:** these sources support interaction principles, not claims about Eclipse's specific users. The user studies in [doc 10 §11](10-design-research-and-skills.md) are the only evidence that can validate the product.

---

## 9. Distribution, licensing and legal

### 9.1 Distribution policy

| Source | URL | Used for |
|---|---|---|
| Play Device and Network Abuse | <https://support.google.com/googleplay/android-developer/answer/16559646> | Executable-code download restriction; why no Play at launch |
| Play target API | <https://developer.android.com/google/play/requirements/target-sdk> | API 36 deadline |
| Play data-safety / user data | <https://support.google.com/googleplay/android-developer/answer/11926878> | Privacy surface if Play is revisited |
| F-Droid inclusion policy | <https://f-droid.org/en/docs/Inclusion_Policy/> | Reproducibility, no blobs, licensing |
| F-Droid anti-features | <https://f-droid.org/en/docs/Anti-Features/> | Why a downloaded JRE is a policy question |

### 9.2 Licenses

| License | URL | Applies to |
|---|---|---|
| Apache-2.0 | <https://www.apache.org/licenses/LICENSE-2.0> | Original app, Sandpaper, Sash, Foreman, Crowbar |
| GPL-2.0 | <https://www.gnu.org/licenses/old-licenses/gpl-2.0.html> | OpenJDK |
| GPL-2.0 + Classpath Exception | <https://openjdk.org/legal/gplv2+ce.html> | EclipseJRE |
| LGPL-2.0 | <https://www.gnu.org/licenses/old-licenses/lgpl-2.0.html> | OpenAL Soft (and NeoForge/Forge, per their files) |
| zlib | <https://opensource.org/license/zlib> | SDL3 |
| BSD-3-Clause | <https://opensource.org/license/bsd-3-clause> | LWJGL |
| FTL | <https://gitlab.freedesktop.org/freetype/freetype/-/blob/master/docs/FTL.TXT> | FreeType |
| MIT | <https://opensource.org/license/mit> | VMA and other small dependencies |

**Rule:** every dependency in the shipped APK has a row in [`PROVENANCE.md`](../PROVENANCE.md), a notice file, and a source URL. A dependency without a row fails `scripts/check-provenance.sh`.

### 9.3 Brand and trademark

| Source | URL | Used for |
|---|---|---|
| Minecraft usage guidelines | <https://www.minecraft.net/en-us/usage-guidelines> | Disclaimer and brand boundaries |
| Eclipse Foundation trademark usage policy | <https://www.eclipse.org/legal/logo-guidelines> | The “Eclipse” name risk; non-affiliation statement |
| Eclipse Foundation trademarks list | <https://www.eclipse.org/legal/trademarks> | What the Foundation claims, and the non-affiliation wording |
| USPTO trademark search | <https://tmsearch.uspto.gov/> | Phase 0 name screen |

**Caveat:** the Eclipse Foundation pages were not re-fetched in this pass. The name risk is documented in doc 06 and doc 10; a formal search is a Phase 0 task, not a claim that the name is available.

---

## 10. Competitive sources (behavioural evidence only)

| Project | URL | License | What it was used for |
|---|---|---|---|
| PojavLauncher | <https://github.com/PojavLauncherTeam/PojavLauncher> | GPL-3.0 | The original Android JVM launch recipe; historical context |
| Amethyst | <https://github.com/AngelAuraMC/Amethyst-Android> | LGPL-3.0 | Current launch behaviour; JRE image measurement; renderer options |
| Fold Craft Launcher | <https://github.com/FCL-Team/FoldCraftLauncher> | GPL-3.0 | Feature depth, modpack UX, failure modes |
| Zalith Launcher 2 | <https://github.com/ZalithLauncher/ZalithLauncher2> | GPL-3.0 | Compose/Navigation 3 UI, renderer detection |
| HMCL | <https://github.com/HMCL-dev/HMCL> | GPL-3.0 | Instance model, installer logic concepts |
| Prism Launcher | <https://github.com/PrismLauncher/PrismLauncher> | GPL-3.0 | Instance/Java/loader model, export/import |
| Modrinth code | <https://github.com/modrinth/code> | AGPL-3.0 | `.mrpack` format implementation reference |
| Boardwalk | <https://github.com/zhuowei/Boardwalk> | MIT/Apache-2.0 (verify) | Historical JVM-on-Android proof |

**Provenance statement:** these projects were read to understand behaviour, failure modes and market expectations. No source code, patches, build scripts, strings, icons, screenshots or assets were copied into Eclipse Launcher. The clean-room claim is a **provenance-audited original implementation**, not a formal legal clean-room; see [`PROVENANCE.md`](../PROVENANCE.md).

---

## 11. Source quality and re-check schedule

| Source | Volatility | Re-check | Automatable? |
|---|---|---|---|
| Mojang version manifest | Every release/snapshot | Before every release; watch the manifest | Yes — `scripts/refresh-metadata.py` (planned) |
| 26.x version JSON | Immutable per version; new versions monthly | Per release | Yes |
| Asset index | Immutable per version | Per release | Yes |
| Fabric meta | Weekly builds | Weekly + before release | Yes |
| Quilt meta | Weekly/beta | v1.1 | Yes |
| NeoForge/Forge Maven | Weekly | v1.1 | Yes |
| Microsoft device-code docs | Rarely | Quarterly | No (manual read) |
| Xbox/Minecraft endpoints | Unversioned; can change | Every release + contract tests | Partial (mock contract tests) |
| Android behaviour changes | Yearly (API) | Each API; quarterly for Play rules | No |
| 16 KB requirements | Deadline-driven | Quarterly until 2027-02 | No |
| Modrinth API | Occasional | Quarterly | Partial |
| Component pins | Per upstream release | Per component release | Yes |
| Licenses | Per dependency change | Every release | Yes (`check-provenance.sh`) |
| HCI research | Stable | Not scheduled | No |

### 11.1 Planned automation

- **`scripts/refresh-metadata.py`** — fetch the manifest, compare `26.3`/current release hashes, and open a PR when a pinned fixture changes. Never auto-merge.
- **`scripts/check-sources.py`** — HEAD/GET the bibliography, report 4xx/5xx and redirects to a different host. Treat 403/000 (bot blocking) as a warning, not a failure.
- **`source-drift.yml`** — monthly scheduled workflow that runs the two scripts and opens one issue with the diff.
- **License gate** — `scripts/check-provenance.sh` fails when a dependency has no ledger row.

### 11.2 What is deliberately not automated

- AppID status: only a human can read the review response.
- Trademark availability: counsel, not a script.
- GPU driver conformance: a device, not a URL.
- HCI findings: participants, not scraping.

---

## 12. Known gaps and unverified claims

| Claim | Status | Why it matters | How to close |
|---|---|---|---|
| Mojang AppID approval for this app | **Open** | Blocks public auth | Submit in Phase 0; record the response |
| 26.3 `--xuid` and `--clientId` requirements | **Open** | Could break launch/social features | Test with a real account before 1.0 |
| 26.3 `--demo` with an empty token | **Open** | Demo mode may not be viable | Test; remove the feature if it needs a fake session |
| 16 KB behaviour on a real device | **Open** | Play deadline; native crashes | Obtain a 16 KB device/emulator by week 8 |
| Microsoft publisher verification for this client class | **Open** | Consent-screen trust and conversion | Check the Entra portal in Phase 0 |
| Refresh-token lifetime/rotation in 2026 | **Open** | Token lifecycle correctness | Observe a real account over 90 days |
| Modrinth rate limits and User-Agent policy | **Open** | Discover reliability | Read the current docs and measure |
| Forge 26.3 processor model | **Open** | v1.1 import/creation | Audit the installer in v1.1 |
| Quilt 0.20.0-beta.9 stability on 26.3 | **Open** | v1.1 scope | Test in the v1.1 cycle |
| EclipseJRE 25 stability on Android 17 | **Open** | Runtime gate | Phase 0/2 device tests |
| “Eclipse” trademark availability | **Open** | Rebrand risk | Phase 0 counsel search |
| F-Droid reproducibility with a downloaded JRE | **Open** | Later distribution | Separate F-Droid review |

**Rule:** an open claim may shape a design, but it may not appear as a promise in the UI, the README or a release note until it is closed.

---

## 13. Citation map by chapter

| Chapter | Principal sources |
|---|---|
| 00 Executive brief | 26.3 JSON, Play policy, Mojang AppID notice, Android 16/17, competitor repos |
| 01 Landscape | Pojav/Amethyst/FCL/Zalith/HMCL/Prism repos, Play policy |
| 02 Minecraft platform | Version manifest, 26.3 JSON, 26.1–26.3 wiki, asset index, Java runtime manifest |
| 03 Loaders and content | Fabric/Quilt meta and licenses, NeoForge/Forge Maven and licenses, Modrinth/CurseForge docs, Iris changelog |
| 04 Android platform | 16 KB, target API, app-specific storage, SAF, process lifecycle, UIDT, FGS types, Android 16/17, pointer capture, ADPF |
| 05 Runtime and graphics | OpenJDK 25, LWJGL 3.4.3 native pins, SDL, OpenAL, FreeType, Shaderc, SPIRV-Cross, VMA, Vulkan spec, GL/Vulkan contracts |
| 06 Authentication | Microsoft device-code docs, Minecraft Wiki auth chain, AppID notice/form, 2026 Q&A report, EULA/usage guidelines |
| 07 Install pipeline | 26.3 JSON/asset index, Fabric/Quilt profiles, NeoForge Maven, Modrinth `.mrpack` spec, Android storage/SAF |
| 08 Product design | Android adaptive/accessibility, Material 3, WCAG 2.2, NN/g, competitor UX |
| 09 Design system | Material 3, M3 Expressive, WCAG 2.2, Android accessibility, type/contrast guidance |
| 10 Design research | Platform docs, HCI literature, competitor artefacts, study methods |
| 11 System architecture | Android process lifecycle, 16 KB, Keystore/backup docs, component upstreams, this chapter |
| 12 Delivery plan | 16 KB/target API deadlines, Play/F-Droid policies, CI/toolchain versions, risk evidence from chapters 04–11 |

---

## 14. Open questions about the sources themselves

- Which sources need an offline mirror so the dossier can be reviewed without network access?
- Do we need a formal citation style (for example, a `SOURCES.bib`) or is this chapter enough?
- Should the monthly drift job open one issue per source family, or one combined report?
- How do we record a source that was read through a blocked network (the Minecraft.net pages) without overstating verification?
- Who owns the AppID correspondence and the trademark search, and where are the responses stored?
- Do we publish this bibliography with the app, or only in the repository?
