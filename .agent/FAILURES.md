# FAILURES

## 2026-11-13 — `strictly(...)` in app/build.gradle.kts (HEAD 3f67213)
- Error: `e: app/build.gradle.kts:80:9 Unresolved reference: strictly` (repeated).
- Tried: `resolutionStrategy { strictly("group:artifact:version") ... }` plus `failOnVersionConflict()`.
- Why failed: `strictly` is not a `ResolutionStrategy` member; it exists on dependency constraints (`DependencyConstraint.strictly`), so calls at this level don't resolve.
- Result: build script compilation failed before any module build. Abandoned forcing entirely; rely on normal Gradle highest-version conflict resolution and explicit direct dependencies.

## 2026-11-13 — Lint report not fully visible in CI
- Symptom: `Lint found 6 errors`, only first printed; upload step found no `lint-results.html`.
- Cause: AGP writes `lint-results-debug.html`; console only summarizes first failure.
- Resolution: `android { lint { textReport = true } }` prints full report to stdout; CI upload globs corrected to `lint-results*.html` + intermediates text report.

## 2026-11-13 — Port compile pass 1 (run 43410068232, HEAD 7f2dd53)
- Error signatures:
  - `TaskStore.kt:19 Unresolved reference 'serializer'` + `Not enough information to infer type argument for 'T'`.
  - `Motion.kt:168 This is an experimental animation API for Transition`.
  - `SelectField.kt:161 No parameter with name 'shape' found` (also containerColor, tonalElevation).
  - `HomeScreen.kt:610 No parameter with name 'shape' found` (same).
- Tried: copied the reference UI unchanged, assuming its M3 calls were portable.
- Why failed: M3 1.5 added `DropdownMenu` shape/containerColor/tonalElevation; M3 1.2.1 has no such params. `LauncherTask` was a Room entity shape, not `@Serializable`. `Transition.animateFloat` is still experimental in Compose 1.6.1.
- Fix: added `@Serializable` to `LauncherTask`/`TaskStatus`, local `@OptIn(ExperimentalAnimationApi::class)` on `AnimatedDialog`, removed the newer DropdownMenu params and let M3 1.2.1 defaults apply.
- Result: pushed as f97865b; awaiting CI.

## 2026-11-13 — CI run 43451147660: `sdkmanager: command not found`
- Signature: `Setup Android SDK ... line 1: sdkmanager: command not found` (exit 127).
- Tried: assumed `sdkmanager` is on PATH on `ubuntu-latest` (as ZL2's workflow does).
- Why failed: the runner's preinstalled cmdline-tools are not on PATH (Ubuntu 26 image migration), so the bare command and `$ANDROID_HOME` symlink never ran.
- Fix approach: locate `sdkmanager` under `$ANDROID_HOME/cmdline-tools/*/bin`; if absent, download the official cmdline-tools zip; prepend its bin dir to `$GITHUB_PATH`, then install `platforms;android-37.2` + `build-tools;36.0.0`. Evidence: this is the same manual cmdline-tools bootstrap that the repo's previously green CI used, adapted to the new SDK packages.

## 2026-11-13 — CI run 43451709825: `./gradlew` prints the Java usage
- Signature: `Run Lint ... Usage: java [options] <mainclass>` with no Gradle output, exit 1; same on the earlier wrapper run.
- Cause: the tracked `gradlew` had a mangled final eval (`eval set -- <jvm opts>` without re-appending the main class and `$@`), so it exec'd `java` with no main class. The wrapper jar itself was untracked earlier, which masked the script defect.
- Fix: restore the standard Gradle 9.5 `gradlew`/`gradlew.bat` from ZL2 (same wrapper version) and track `gradle-wrapper.jar` (added in `8836229`). Evidence: ZL2 CI builds with these exact wrapper scripts.

## 2026-11-13 — CI run 43452026824: `Unresolved reference 'ExposedDropdownMenu'`
- Signature: `e: .../SelectField.kt:125:9 Unresolved reference 'ExposedDropdownMenu'` (+ cascading composable error at 130).
- Cause: M3 1.5 moved `ExposedDropdownMenu` to a top-level `ExposedDropdownMenuBoxScope` extension; the file only imported the box/anchor/defaults. It resolved as a scope member under M3 1.2.1 without an import.
- Fix: add `import androidx.compose.material3.ExposedDropdownMenu` (verified against M3 1.5.0-beta02 sources).

## 2026-11-13 — CI run 43452605239: 7x `LocalContextGetResourceValueCall` lint errors
- Signature: `Error: Querying resource values using LocalContext.current is not configuration-aware...` at 7 sites (EclipseApp, InstanceEditorScreen x4, ManageContentScreen, MarketScreen).
- Cause: using `context.getString(R.string.xxx)` inside composables instead of `stringResource(R.string.xxx)`.
- Fix: replaced all 7 occurrences with `stringResource()` (Compose's locale/configuration-aware accessor). Evidence: Compose lint rule flags direct `Context.getString` from `LocalContext.current`.
