# GOAL

Keep the nemo-built EclipseLauncher UI (reference at `/root/nemo/Eclipse`) and migrate its surrounding implementation to Zalith Launcher 2's modern Android stack, verified only by GitHub Actions.

## IN SCOPE
- Build stack: AGP 9.3.0, Gradle 9.5.0, Kotlin 2.4.21, KSP 2.3.12, Hilt/Dagger 2.60.1, JDK 21 CI, Java/Kotlin target 17, compileSdk 37.2, targetSdk 34, minSdk 26.
- Version catalog (`gradle/libs.versions.toml`) aligned to ZL2 (`50f08ca`).
- Compose BOM 2026.09.00, Material3 1.5.0-beta02, Navigation3 1.2.0, Coil 3.6.3.
- Hilt DI (`@HiltAndroidApp`, `@AndroidEntryPoint`, `AppModule`, EntryPoint container).
- Persistence: Room 2.8.5 over SQLCipher 4.20.0 for accounts/instances/tasks; MMKV 1.3.14 for settings with a one-time SharedPreferences migration.
- Networking: shared Ktor 3.6.1 `NetworkClient` (OkHttp 5.5.1 engine, ContentNegotiation, 15 s timeout) for Modrinth, version metadata, Microsoft auth.
- Navigation: `NavBackStack<Route>` + `NavDisplay` + `entryProvider`; routes are `@Serializable` `NavKey`s.
- Preserve the existing nemo UI and prior M3 adaptations (BasicAlertDialog, ripple clickables, ExposedDropdownMenuBox).
- Keep CI-only verification: lint, unit tests, `assembleDebug`, `bundleRelease`.
- Push to `main` and leave only green workflow runs (delete failed ones).
- `.agent/` stays untracked/uncommitted; no git history rewriting.

## OUT OF SCOPE
- Visual/UX redesign or new features.
- Unused ZL2 libraries (Media3, sora-editor, RichText, Reorderable, MaterialKolor, native/runtime modules).
- Data migration from the old JSON files (no released data exists).
- Local Gradle builds.
- README refresh.

## Reference
- `/root/nemo/Eclipse` — read-only UI source of truth.
- `/tmp/opencode/zalith/zl2` — local ZL2 clone (`0c10282`); upstream `50f08ca` checked via raw files.
