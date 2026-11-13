# PLAN

- [x] Research ZL2 stack (`50f08ca`) and Android SDK 37.2 availability.
- [x] Add `gradle/libs.versions.toml` and rewrite root/settings/app Gradle scripts.
- [x] Bump Gradle wrapper to 9.5.0 and CI to JDK 21 + SDK 37.2 + current actions.
- [x] Add Room+SQLCipher entities/DAOs/database, Keystore-wrapped passphrase, Hilt module, Hilt Application/Activity.
- [x] Replace JSON AccountStore/InstanceStore/TaskStore with Room-backed Hilt singletons.
- [x] Replace SharedPreferences SettingsStore with MMKV + legacy migration.
- [x] Add shared Ktor `NetworkClient`; migrate ModrinthClient, VersionRepository, MicrosoftAuth.
- [x] Rewrite EclipseApp on Navigation3; thread `auth`/`client` through screens.
- [x] Replace manual bitmap download with Coil `AsyncImage` (ModIcon/ModpackIcon).
- [x] Remove unused `JsonStore.kt`; update M3 1.5 `menuAnchor` call.
- [ ] Commit/push and get all three CI jobs green.
- [ ] Delete failed workflow runs; keep green ones.
- [ ] Final scope check against GOAL.md.
