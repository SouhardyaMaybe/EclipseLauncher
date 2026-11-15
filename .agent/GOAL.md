# GOAL

Fix Microsoft account skin/cape display and hide customization for Microsoft accounts.

## IN SCOPE
- Trace and fix the full path: Microsoft OAuth token -> Minecraft profile request -> response parsing -> Account model -> Room store -> ALL display points (Home screen head, Account switcher, Accounts screen, Skin dialog 3D model)
- Verify REAL Minecraft profile API format from official docs (cite URLs): endpoint, auth header, skins[] (id, state, url, variant), capes[] (id, state, url, alias)
- Handle ACTIVE skin/cape selection, no cape, classic vs slim (variant), legacy 64x32 skins, malformed/oversized PNGs
- Normalize texture URLs to HTTPS (texture host serves HTTPS)
- Fetch profile on sign-in, on token refresh, on app start (skins change outside launcher)
- Cache skin/cape PNGs by texture hash (content-addressed) in app cache for offline use
- Show placeholder while loading, clear fallback on failure
- Update displays when profile changes
- Hide/disable ALL skin/cape customization entry points for Microsoft accounts (dress button, dialog actions)
- Keep customization available for LOCAL/offline accounts
- JVM tests: realistic profile fixtures, fake HTTP (success/401/network error), REAL UI wiring test (Microsoft account shows skin, no customize; local account has customize)

## OUT OF SCOPE
- Visual/UX redesign or new features
- Layout, structure, text or behavior changes unrelated to skin/cape
- Skin uploading to Microsoft API
- Emulator/screenshot testing
- Local Gradle builds (CI-only verification)
- The 3D render-order glitches (separate task)

## REFERENCE
- Official Minecraft profile API: https://api.minecraftservices.com/minecraft/profile (Bearer token auth)
- Texture host: https://textures.minecraft.net (serves HTTPS)
- App codebase: `/root/Launcher/EclipseLauncher/app/src/main/java`
- MicrosoftAuth.kt lines 75-132 (parsing), 301-315 (profile fetch)
- Display points: SkinHeadFace.kt, SkinModelView.kt, AccountsScreen.kt, HomeScreen.kt, AccountSwitcherDialog.kt, SkinCustomizationDialog.kt