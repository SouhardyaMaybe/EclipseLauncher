# Package Rename Report: com.eclipselauncher → me.shadow.eclipselauncher

## Commit
- **Commit**: `a42a741` (main branch)
- **CI Run**: 43624909844 (GREEN - all 3 jobs passed)
- **Artifacts**:
  - Debug APK: `14125408998`
  - Release AAB: `14125379217`

## Changes Made
1. **Package declarations & directories**: All 49 Kotlin files moved from `com.eclipselauncher` to `me.shadow.eclipselauncher` with `git mv` (history preserved)
2. **Imports**: All `import com.eclipselauncher.*` → `import me.shadow.eclipselauncher.*`
3. **Package declarations**: All `package com.eclipselauncher.*` → `package me.shadow.eclipselauncher.*`
4. **Gradle**: `namespace` and `applicationId` both updated to `me.shadow.eclipselauncher`
4. **ProGuard**: Keep rules updated
5. **AndroidManifest**: Uses relative class names (`.EclipseLauncherApplication`, `.MainActivity`) - works with new namespace

## Files Changed (61 files)
- 49 Kotlin source files (renamed + updated)
- `app/build.gradle.kts` (namespace + applicationId)
- `app/proguard-rules.pro`
- `app/src/main/AndroidManifest.xml`

## Verification
- ✅ No remaining `com.eclipselauncher` references in source/config (verified with grep)
- ✅ CI GREEN: Lint & Test, Assemble Debug APK, Assemble Release AAB
- ✅ All 3 CI jobs passed (Run 43624909844)
- ✅ Artifacts produced: Debug APK (14125408998), Release AAB (14125379217)

## ApplicationId Consequences Analysis

### What changes with new applicationId (`me.shadow.eclipselauncher`):
1. **Fresh install**: Installs as a separate app alongside any existing `com.eclipselauncher` build
2. **No data migration**: Room databases, MMKV settings, SharedPreferences, internal files - ALL lost
3. **Microsoft Auth**: Uses standard `https://login.live.com/oauth20_desktop.srf` redirect (no package in redirect URI) - **NO CHANGE NEEDED**
4. **FileProvider**: Not used - **NO CHANGE NEEDED**
5. **Deep links**: None configured - **NO CHANGE NEEDED**
6. **Notification channels**: None configured - **NO CHANGE NEEDED**
7. **JNI/Native code**: None found - **NO CHANGE NEEDED**
8. **API keys**: None restricted by package name found

### User Action Required
- **Uninstall old app** (`com.eclipselauncher`) before installing new build, OR install side-by-side
- All instances, accounts, settings, downloaded content will be reset
- This is expected behavior for applicationId change

## CI Verification
- **Run**: 43624909844 (commit `a42a741`)
- **Jobs**: Lint & Test ✅, Assemble Debug APK ✅, Assemble Release AAB ✅
- **Artifacts**: app-debug-apk (14125408998), app-release-aab (14125379217)

## Phone Test Steps
1. Uninstall any existing `com.eclipselauncher` build
2. Install debug APK (artifact 14125408998)
3. Verify: App launches, Home screen loads, navigation works, all screens open
4. Test: Create instance, add account, browse modpacks, manage content
5. Verify: Settings persist after app restart (new MMKV store)

## Motion Package Path Update
- Old: `com.eclipselauncher.ui.motion`
- New: `me.shadow.eclipselauncher.ui.motion` (to be created in Phase 2)

## Notes
- No test files existed (no migration needed)
- No native/JNI code affected
- No Microsoft auth redirect URI changes needed (uses standard desktop redirect)
- All proguard rules updated
