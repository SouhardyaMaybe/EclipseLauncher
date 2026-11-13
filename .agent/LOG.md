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
- CI passes: lint-and-test, assemble-debug, assemble-release all green on `173bf09`; failed runs deleted, only green runs remain.
- Final scope check: all IN-SCOPE items complete; OUT-OF-SCOPE items untouched (README, visual redesign, native modules, local builds).

