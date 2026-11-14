# NOTES — noticed but NOT touched

- `gradle.properties` still contains stale, unused keys (`composeBomVersion`, `kotlinVersion`, `agpVersion`, `kspVersion`) and `android.enableJetifier=true` (unnecessary). Harmless; left untouched.
- `NavigationGraph` registers only top-level routes; sub-screen routes used by navigation strings are not registered (runtime navigation gap). Out of scope; no compile impact.
- Compose runtime resolves to 1.6.5 transitively (from lifecycle-runtime-compose 2.8.2) while all explicit Compose artifacts are 1.6.1. Same minor line, binary-compatible; no force/strictly to avoid build-script breakage.
- Unused imports remain in a few edited files (e.g., HomeScreen LayoutCoordinates, NavigationGraph NavGraphBuilder). Warnings only.
- CI emits Node 20 deprecation warnings for actions/checkout@v4, setup-java@v4, upload-artifact@v4, setup-gradle@v3; jobs still pass. Out of scope; migrating to v5 is a separate maintenance task.
- CI emits Gradle cache service 400 / "services unavailable" errors; infrastructure noise, builds unaffected.

## 2026-11-13 — Noticed but NOT touched
- `README.md` is stale: it lists M3 1.5.0-beta01 / Coil / Gradle 8.5 and the old simplified project layout. Not updated because the task was the UI port.
- `app/build.gradle.kts` still declares unused navigation/adaptive/runtime-livedata deps from the old UI. Left in place to keep the pinned stack untouched.
- Kotlin compiler reports nullable `SimpleDateFormat`/`Date` warnings in `VersionRepository.parseReleaseTime`; it compiles and CI is green, so left unchanged.
- Manifest merger warns that `tools:replace="android:allowBackup"` has no counterpart declaration; harmless, left as copied.
- `.agent/` stays untracked/uncommitted per operating rules.

## 2026-11-13 — Migration observations (NOT touched)
- `Logs(File(filesDir, "logs"))` nests logs under `files/logs/logs` because `Logs` already appends `logs`; copied from nemo, left unchanged.
- `SqlCipherPassphrase` stores the wrapped key in plain SharedPreferences (`eclipse_database`); settings themselves are MMKV as required.
- `UtilsScreen`/`MarketScreen` are currently unreferenced by `EclipseApp` (same as before the migration); kept and compile-fixed rather than deleted.
- Old JSON data files are not imported into Room; acceptable because no released data exists.
- `README.md` still describes the old stack/layout; intentionally unchanged.


## 2026-11-14 — Noticed, not touched
- `app/src/test` does not exist: CI "Run Unit Tests" executes zero tests; "gate tests stay green" is currently vacuous.
- `.github/workflows/ci.yml` has no lint step (job is still named "Lint & Test"); build.gradle only sets `lint { textReport = true }`.
- Installed tab still uses six mock `InstalledContent` rows; no jar metadata/hash icon lookup wired.
- Source selector is two weighted `FilterChip`s, not M3 `SingleChoiceSegmentedButtonRow`.
- Modrinth `project_type:world` may be unsupported; Worlds browse may show error/empty.
- `nemo` branch / off-limits repos instruction still unresolved; all work/pushes remain on `main` of `SouhardyaMaybe/EclipseLauncher` because that is the only repo with this CI/APK.

## 2026-11-14 — Install limitations (NOT hidden)
- Successful installs are appended to the in-memory Installed list only; after relaunch the files are on disk but the Installed tab is seeded with mock rows again. Real directory/jar scanning is not wired.
- Worlds are saved as archives in `saves/`; they are not extracted. Datapacks land at the instance root, not inside a specific world.
- The stable MC filter depends on the piston-meta manifest (cached or fetched). If that request fails, all groups remain visible; the badge type falls back to Releases for MC ids missing from the manifest.
- Mod loader compatibility is no longer filtered in the install sheet, by request. A Fabric mod can be listed under a Forge instance if both target the same MC version.
- Back/close cancels a running download; the `.part` file is deleted. There is no resume.
- `Installed` rows' update/toggle/delete actions are still visual placeholders.

## 2026-11-14 — Manage Content resolution
- Resolved: Installed is no longer mock-seeded; it scans real instance folders and persists metadata in sidecars.
- Resolved: update/downgrade, enable/disable and delete are wired to the filesystem.
- Not touched: worlds are still not extracted from archives; dependency installs use the dependency version's files without loader re-filtering when `version_id` is pinned.
- Not touched: files installed before this change have no sidecar, so update/downgrade reports "not installed from Modrinth"; toggle/delete still work.
- Not fixed: `app/src/test` still does not exist and CI has no lint step (same as prior notes).
- `nemo` branch / off-limits repo instruction still unresolved; all work/pushes remain on `main` of `SouhardyaMaybe/EclipseLauncher`.

## 2026-11-14 — Search/filter pass (noticed, not touched)
- Worlds on Modrinth still use `project_type:world`, which Modrinth may not support; unchanged from before.
- Installed version labels are sorted lexicographically (mod version strings are not semver-normalized); filter correctness is exact-match regardless of order.
- `browseVersionOptions` lists every cached Minecraft version in a 220dp scroll panel; there is no type-ahead inside the panel.
- Import local still only shows a "started" toast (pre-existing unimplemented path).
- `.agent/` changes remain uncommitted per operating rules.
