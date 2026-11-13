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

