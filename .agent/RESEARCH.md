# RESEARCH

- Google Maven POMs (curl, Nov 2026):
  - material3-adaptive-navigation-suite:1.0.0-alpha01 exists (200) and transitively needs lifecycle-runtime-compose 2.7.0-rc01, material3 [1.2.0-alpha11].
  - material3-adaptive:1.0.0-alpha01 exists.
  - material-icons-extended:1.6.1 exists.
  - navigation-compose:2.7.7 exists; depends on activity-compose 1.7.0, no Compose 1.7.
- lifecycle-runtime-compose:2.8.2 → compose runtime 1.6.5 (highest transitive; no 1.7.x).
- lifecycle-viewmodel-compose:2.8.2 → compose runtime 1.6.0.
- activity-compose:1.9.0 → compose runtime 1.0.1 (lowest), upgraded to 1.6.1 by conflict resolution.
- M3 1.2.1 sources inspected (`material3-1.2.1-sources.jar`) for Surface, ColorScheme, TextFieldDefaults, SwitchDefaults signatures.

## 2026-11-13 — Zalith Launcher 2 exact stack (read-only research)
- Repo: https://github.com/ZalithLauncher/ZalithLauncher2, main @ `50f08ca973994e33e0ca347f289b09aa740e5b54` (2026-11-13). Latest release 2.8.2 (2026-11-06). GPL-3.0, Kotlin.
- Sources: `gradle/libs.versions.toml`, `ZalithLauncher/build.gradle.kts`, root `build.gradle.kts`, `settings.gradle.kts`, `gradle/wrapper/gradle-wrapper.properties`, `.github/workflows/build.yml`, README.
- Build: AGP 9.3.0, Kotlin 2.4.21, Gradle 9.5.0, KSP 2.3.12, JDK 21 CI, Java/Kotlin target 17, compileSdk 37 (minor 2), targetSdk 34, minSdk 26, NDK declared 25.2.9519653.
- UI: Compose BOM 2026.09.00, M3 1.5.0-beta02, activity-compose 1.13.0, Navigation3 1.2.0, lifecycle 2.11.0, Material (views) 1.14.0, MaterialKolor 5.0.1, Coil 3.6.3, Media3 1.11.1, RichText 1.0.0-alpha05, sora-editor BOM 0.24.7, Reorderable 3.1.0, Fishnet 1.1.0 (crash dumper).
- DI/data: Hilt/Dagger 2.60.1, androidx hilt-navigation-compose 1.4.0, Room 2.8.5, SQLCipher 4.20.0, MMKV 1.3.14, Gson 2.15.0, kotlinx.serialization plugin.
- Net/util: Ktor 3.6.1, OkHttp 5.5.1, Okio 3.18.2, coroutines 1.11.0, Commons IO/Codec/Compress, Zip4j 2.11.6, XZ 1.12, Toml4j 0.7.2, Maven Artifact 3.10.0, MiniDNS HLA 1.1.3, Process Phoenix 3.0.0, ByteHook 1.0.10, proxy-client-android 0.0.5, BuildKeys 1.0.0.
- Local modules: LayerController, ColorPicker, CardGrid, Terracotta (LAN multiplayer proxy), InputMap, Guide, PetLib (Rust skin/animation renderer), LWJGL 3.3.3 + 3.4.1. JREs 8/17/21/25. README credits ANGLE, gl4es, NG-GL4ES, MobileGlues, Mesa/Turnip, LTW.
- Note: README's "environment requirements" (JDK 11, target API 35) are stale vs the build files (JDK 17 target / CI 21, targetSdk 34).

## 2026-11-13 — Implementation API checks
- `compileSdk { version = release(37) { minorApiLevel = 2 } }` confirmed in ZL2 `ZalithLauncher/build.gradle.kts`.
- `platforms;android-37.2`, `android-37.1`, `android-37.0` exist in `repository2-3.xml`; runner sdkmanager can install `platforms;android-37.2`.
- AGP 9.3 compatibility: Gradle 9.5.0 min/default; Build Tools 36.0.0 min/default; JDK 17 min.
- Room 2.8.5 POM depends on androidx.sqlite 2.6.2; `sqlcipher-android` 4.20.0 declares androidx.sqlite 2.7.0, so the catalog pins `androidx.sqlite:sqlite:2.7.0`. `SupportOpenHelperFactory(byte[])` and `RoomDatabase.Builder.openHelperFactory(...)` verified with javap.
- M3 1.5.0-beta02 sources: only `Modifier.menuAnchor(ExposedDropdownMenuAnchorType, enabled = true)` exists; `BasicAlertDialog(onDismissRequest, modifier, properties, content)` unchanged.
- Navigation3 1.2.0 sources: `NavBackStack(vararg elements)`, `NavDisplay(backStack, onBack, entryProvider=...)`, `entryProvider { entry<T> {} }` confirmed.
- Coil 3.6.3: `AsyncImage` in `coil-compose`; `KtorNetworkFetcherFactory()` no-arg creates its own `HttpClient`.
- Action tags verified: checkout v6 exists (latest v7), setup-java v5 (latest v6), setup-gradle v5 used by ZL2, upload-artifact v7.
- `kotlinx.serialization`: enum classes are serializable by default; `Json` has a reified `decodeFromString` member (no extra import).


## 2026-11-14 — Modrinth versions + M3 expressive loading
- Endpoint `GET https://api.modrinth.com/v2/project/{id|slug}/version?include_changelog=false`; response is an array with `id`, `project_id`, `name`, `version_number`, `game_versions[]`, `loaders[]`, `date_published`, `version_type`, `files[]` (`url`, `filename`, `primary`, `size`, `file_type`). Verified against `https://api.modrinth.com/v2/project/sodium/version` and https://docs.modrinth.com/api/operations/getprojectversions/.
- Compose Material3 1.5.0-beta02 contains `LoadingIndicator` (expressive, opt-in `ExperimentalMaterial3ExpressiveApi`), `LinearWavyProgressIndicator`, `CircularWavyProgressIndicator`; verified by inspecting `material3-android-1.5.0-beta02.aar` from Google Maven.
- Ktor 3.6.1 `HttpResponse.bodyAsChannel()` + `ByteReadChannel.readAvailable(ByteArray, offset, length)` used for streamed downloads; verified against the resolved `ktor-io-jvm-3.6.1` / `ktor-client-core-jvm-3.6.1` jars in the Gradle cache.
