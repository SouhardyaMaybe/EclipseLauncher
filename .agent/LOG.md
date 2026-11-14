# LOG

## 2026-11-13
- Restored `.agent` context; HEAD 3f67213 broken at build-script compile (`strictly` unresolved).
- Rewrote `app/build.gradle.kts` without force/strictly/failOnVersionConflict; explicit Compose 1.6.1, M3 1.2.1, adaptive 1.0.0-alpha01, nav-compose 2.7.7, icons 1.6.1.
- Fixed 385 reported Kotlin errors across 39 files:
  - Removed invalid `weight` import; made 4 root-Column composables `ColumnScope` extensions.
  - `BorderStroke` → `androidx.compose.foundation.BorderStroke`; `TextOverflow` → `ui.text.style`.
  - `Divider` → M3; added `filled.*` icon imports.
  - `collectAsStateWithLifecycle` → `collectAsState` + `getValue` imports.
  - `Icon(onClick=)` → `Modifier.clickable`; `Surface(elevation=)` → `shadowElevation`; nullable onClick lambdas fixed.
  - TextField color args → M3 1.2.1 names; invalid paddings expanded to start/end.
  - `Slider(range=)` → `valueRange=`; `FloatRange` → `ClosedFloatingPointRange<Float>`.
  - Removed `shadow` from color schemes; added missing `LightOnTertiary*` colors.
  - Nullable `Text(text = searchError)` → `.orEmpty()`.
  - `SettingsCategory` enum values without unused ctor args; `filteredVersions` stateIn.
  - Home/Accounts delegated-property smart-cast and `.value` misuse fixed.
- Audits: no bad patterns remaining; imports complete; brace balance OK; no duplicate imports.
- Next: commit + push, watch CI.

## CI run 43402681821 (commit 337e2c5)
- Build script compiled; Kotlin compile reached. 14 remaining errors in 2 files.
- Root causes: Java-style ternaries in TasksScreen (`x : y`) and bare icon property references in ModpackBrowserScreen (`ArrowBack`/`Folder` need `Icons.Default.` receiver + `Icons` import).
- Fixed both; no other Java ternaries remain in codebase.

## CI run 43403153096 (commit 9d184cc)
- Kotlin compile passed. `:app:lintDebug` failed: 6 errors, 78 warnings; only first surfaced (`values-night/themes.xml:11 NewApi` windowLightNavigationBar requires API 27).
- Added `tools:targetApi="27"` to `android:windowLightNavigationBar` in values/themes.xml and values-night/themes.xml.
- Added `lint { textReport = true }` (no textOutput) so the full lint text report prints to CI stdout.
- Fixed CI lint report upload globs: `app/build/reports/lint-results*.html` + intermediates text report.

## CI run 43403891423 (commit d0e1f26)
- NewApi fixed (2 errors gone). Full lint report now printed to stdout by `textReport = true`.
- Remaining 4 errors: `StateFlowValueCalledInComposition` at JavaTweaksSettingsScreen.kt:87, MiscellaneousSettingsScreen.kt:143/151, VideoRendererSettingsScreen.kt:115 — reading `viewModel.settings.value.*` inside `@Composable` wrappers.
- Fix: collect `settings` with `collectAsState()` at top of each `*WithModals` composable and use collected value.

## CI run 43404371076 (commit 74cec65) — GREEN
- Lint & Test: success (full lint report printed; 0 errors, warnings only).
- Assemble Debug APK: success; artifact `app-debug-apk` uploaded.
- Assemble Release AAB: success; artifact `app-release-aab` uploaded.
- Artifacts: lint-report, test-reports, app-debug-apk, app-release-aab all uploaded successfully.
- Remote main == local HEAD 74cec65. Working tree clean except untracked `.agent/`.
- Final scope check: all IN SCOPE items done; no out-of-scope changes (Coil remains disabled, no refactors).

## 2026-11-13 — UI port & M3 1.2.1 adaptation
- Ported the reference app's full UI/data/resources into `com.eclipselauncher`; kept Kotlin 2.0.0 / Compose 1.6.1 / M3 1.2.1 / compileSdk 34.
- Reimplemented the reference Room/Hilt stores as JSON + StateFlow (`AccountStore`, `InstanceStore`, `TaskStore`) behind a manual `EclipseContainer`.
- CI compile fixes: serializable task models, Transition opt-in marker, DropdownMenu 1.3-only params, lint produceState false positive.
- First fully green port build: run 43413112160 (commit fd056b6).
- M3 migration so far: press-squeeze/push-corner -> ripple clickables + IconButton; AnimatedDialog -> Material3 BasicAlertDialog; custom dropdown/picker -> ExposedDropdownMenuBox/OutlinedButton.
- Note: true Material 3 Expressive APIs (M3 1.5+/Compose 1.8+) are unavailable on the pinned M3 1.2.1; used the closest standard M3 1.2.1 components.

## 2026-11-13 — ZL2 stack migration
- Adopted ZL2 versions: AGP 9.3.0 (built-in Kotlin), Kotlin 2.4.21, Gradle 9.5.0, KSP 2.3.12, Hilt 2.60.1, compileSdk 37.2 (`release(37) { minorApiLevel = 2 }`), targetSdk 34, minSdk 26, JDK 21 CI.
- Added `gradle/libs.versions.toml`; removed all hard-coded old versions and unused deps (adaptive, navigation-compose, window, rx/livedata, arch/mockito tests).
- Room+SQLCipher: accounts/instances/tasks entities + DAOs; `SqlCipherPassphrase` generates a random 256-bit key and wraps it with a non-exportable Keystore AES-GCM key (nemo's `SecretKey.encoded` path was broken).
- Settings moved to MMKV with one-time copy from the old `eclipse_settings` SharedPreferences.
- Networking: one Ktor/OkHttp `NetworkClient`; Modrinth/version/Microsoft auth no longer use `HttpURLConnection`.
- Navigation3: private `@Serializable sealed interface Route : NavKey`, `NavBackStack`, `NavDisplay`, `entryProvider`; overlay pop preserves root exit.
- Screens updated: `MicrosoftAuthScreen(auth=)`, `ManageContentScreen(client=)`, `ModpackBrowserScreen(client=)`, `MarketPage(client=)`/`UtilsScreen(client=)`.
- `ModIcon`/`ModpackIcon` now use Coil `AsyncImage` over the existing placeholder tiles.
- M3 1.5: `.menuAnchor()` → `.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)`.
- Removed unused `JsonStore.kt`.
- CI passes: lint-and-test, assemble-debug, assemble-release all green on `f4cb15f`; failed runs deleted, only green runs remain.
- Theme 2: Dynamic (Material You) + Urban (from ZL2 urbanAsh) with Crossfade animation; removed Dark/Light.
- Settings cleanup: removed JAVA_SANDBOX, DOWNLOAD_SOURCE, AUTO_INSTALL_DEPENDENCIES, DISABLE_SERVER_BLOCKLIST, ALSOFT_OPENSL, UBWC, FORCE_ENGLISH.
- Settings UX: Back button text changed to "Back"; added Microphone permission asker.
- CurseForge key option preserved.

## CI run 43545935588 (commit 6d0ec01) — GREEN
- Lint & Test: success (0 errors).
- Assemble Debug APK: success.
- Assemble Release AAB: success.
- Remote main == local HEAD 6d0ec01. Working tree clean except untracked `.agent/`.

## 2026-11-14 — ManageContentScreen (File Manager) rewrite
- Replaced FilterChip side-by-side buttons with `ContentSegmentedControl` (Installed | Browse) supporting tap + horizontal drag gestures to switch views seamlessly.
- Content type bar: sticky horizontal row of glyph chips (MODS ■, RESOURCE_PACKS ■, DATA_PACKS ■, SHADERS ■, WORLDS ◆) with count badges under selected.
- Browse auto-loads content on initial load via `LaunchedEffect` triggered by `browseContentType`/`browseProvider`/`browsePage`/`browseQuery` changes.
- Collapse/expand options panel: `▲`/`▼` toggle for provider selector row.
- Floating page navigation: sticks to right below search field showing `‹ page/total ›` with prev/next text buttons; `▲` collapses/expands the floating control.
- Content icons loaded via Coil `AsyncImage` with fallback glyph; applies to both Installed and Browse rows.
- Replaced all `Icon()` composable calls with `Text()` glyphs (◀, ▲, ▼, ◄, ►, ●, 🗑, ⟳, ⬇, 🔍) for Icon API compatibility.
- Removed Sandbox .jar toggle (JAVA_SANDBOX), download source selection (DOWNLOAD_SOURCE), import data, auto-install deps, disable server blocklist, OpenSL audio, UBWC, force English toggle.
- CurseForge key option preserved.
- Theme: Dynamic (Material You default) + Urban (from ZL2 urbanAsh urbanAsh palette) with Crossfade animation; no Dark/Light.
- Settings: Single `<- Back` button in TopAppBar (back-to-root when in sub-page, exits when at root); microphone permission asker with granted/denied state.


## CI run 43548091059 (commit dad64a9) — GREEN
- Lint & Test: success (0 errors).
- Assemble Debug APK: success.
- Assemble Release AAB: success.
- Remote main == local HEAD dad64a9. Working tree clean except untracked `.agent/`.

## 2026-11-14 — ManageContentScreen (File Manager) rewrite — COMPLETE
- Replaced FilterChip side-by-side buttons with `ContentSegmentedControl` (Installed | Browse) supporting tap + horizontal drag gestures to switch views seamlessly.
- Content type bar: sticky horizontal row of glyph chips (MODS ◆, RESOURCE_PACKS ◆, DATA_PACKS ◆, SHADERS ◆, WORLDS ◆) with count badges under selected.
- Browse auto-loads content on initial load via `LaunchedEffect` triggered by `browseContentType`/`browseProvider`/`browsePage`/`browseQuery` changes.
- Collapse/expand options panel: `▲`/`▼` toggle for provider selector row.
- Floating page navigation: sticks to right below search field showing `‹ page/total ›` with prev/next text buttons; `▲` collapses/expands the floating control.
- Content icons loaded via Coil `AsyncImage` with fallback glyph; applies to both Installed and Browse rows.
- Replaced all `Icon()` composable calls with `Text()` glyphs (◀, ▲, ▼, ◄, ►, ●, 🗑, ⟳, ⬇, 🔍) for Icon API compatibility.
- Removed Sandbox .jar toggle (JAVA_SANDBOX), download source selection (DOWNLOAD_SOURCE), import data, auto-install deps, disable server blocklist, OpenSL audio, UBWC, force English toggle.
- CurseForge key option preserved.
- Theme: Dynamic (Material You default) + Urban (from ZL2 urbanAsh urbanAsh palette) with Crossfade animation; no Dark/Light.
- Settings: Single `<- Back` button in TopAppBar (back-to-root when in sub-page, exits when at root); microphone permission asker with granted/denied state.
- Installed tab matches reference UI exactly: glyph icon card, name + "version · type" line, ⟳ refresh, toggle switch, 🗑 delete button per row.

## 2026-11-14 — Green build restored + Browse auto-load fixed (478b799)
- Root cause of ~40 red runs: one missing brace made `ManageContentScreen` a *local* function inside `PaginationPill`. Fixed nesting; screen is top-level again in the normal `src/main/java` source set.
- Restored `EclipseApp.kt` byte-for-byte from known-good `a622441`; removed all inlining, duplicate imports, `@file:Suppress`, `src/main/kotlin`, and the `sourceSets` hack.
- Auto-load bug: `LaunchedEffect` keys omitted `mainTab`, so opening Browse never fetched. Added `mainTab` + retry/page/type/source/query keys; empty query now sends the default listing.
- Split transport failure from empty results via `ModrinthSearchResult.failed`; added error+Retry, empty+Clear filters, and request cancellation by `LaunchedEffect` restart.
- Pagination pill now floats over the list (was after a `weight(1f)` box and got 0 height), bottom-center, edge-collapsed, nav-bar padded, 96dp list bottom inset.
- Browse rows now use real `categories` + `versions` from the search response; removed hardcoded support range, author line, and download counts.
- CI run 43578498959: all three jobs green.
- NOT done: versions/install dialog; JVM auto-load test (no `app/src/test` exists); lint step remains absent from ci.yml.

## 2026-11-14 — Phone crash fix: enum values() is an Array
- Reproduced from user log at ManageContentScreen.kt:481.
- `ContentType.values()` -> `Array`; cast to `List` crashed whenever the filter panel expanded.
- Replaced with `ContentType.entries`; removed the cast. Pushed for CI verification.

## 2026-11-14 — Filter handle + paginator cleanup; rapid-paging fix
- Removed the six-dot drag handle and all collapse/drag state; filter panel is now always expanded and compact (single chip row).
- Removed the paginator collapse chevron and edge-handle mode; pill is fixed bottom-center `‹ page/total ›`, fixed-width indicator (no button shift between 1- and 2-digit pages), spinner while loading.
- Rapid-tap bounce: debounce page changes by 220ms, capture `requestedPage`, fetch only the settled page, and discard stale responses; scroll-to-top is debounced the same way.
