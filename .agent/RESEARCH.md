# RESEARCH: Minecraft Profile API

## Official Sources
1. **Minecraft Wiki - Mojang API**: https://minecraft.wiki/w/Mojang_API
2. **Mojang API Docs (gapple)**: https://mojang-api-docs.gapple.pw/needs-auth/change-skin
3. **OpenAPI Spec (apis.io)**: https://apis.io/apis/mojang/mojang-profile-api

## Profile API Endpoint
- **GET** `https://api.minecraftservices.com/minecraft/profile`
- **Auth**: `Authorization: Bearer <access_token>` (Minecraft access token from XSTS exchange)
- **Error**: HTTP 401 if token missing/invalid

## Response Format (AuthenticatedProfile)
```json
{
  "id": "player UUID without hyphens",
  "name": "username",
  "skins": [
    {
      "id": "skin UUID",
      "state": "ACTIVE" | "INACTIVE",
      "url": "https://textures.minecraft.net/texture/...",
      "variant": "CLASSIC" | "SLIM",
      "alias": "skin nickname (optional)"
    }
  ],
  "capes": [
    {
      "id": "cape UUID",
      "state": "ACTIVE" | "INACTIVE",
      "url": "https://textures.minecraft.net/texture/...",
      "alias": "cape display name (optional)"
    }
  ],
  "profileActions": ["FORCED_NAME_CHANGE", ...]
}
```

## Texture Host
- `textures.minecraft.net` - serves **HTTPS** (also accessible via HTTP but Android blocks cleartext)
- Current code normalizes `http://` → `https://` in `normalizeTextureUrl()` - CORRECT

## Key Fields for Launcher
- **Active skin**: first skin with `state == "ACTIVE"` → use its `url` and `variant`
- **Active cape**: first cape with `state == "ACTIVE"` → use its `url`
- **Skin variant**: `CLASSIC` = Steve model (4px arms), `SLIM` = Alex model (3px arms)
- **Legacy skins**: 64x32 supported (current validation allows 64x64 or 64x32)

## Current Code Analysis (MicrosoftAuth.kt)
- Lines 75-132: `parseMinecraftProfile()` - correctly extracts ACTIVE skin/cape, normalizes URLs
- Line 97-98: `activeSkin` / `activeCape` computed properties - CORRECT
- Line 135-136: `normalizeTextureUrl()` - normalizes http→https - CORRECT
- Line 63-72: `MsLoginResult` includes `skinVariant` but it's **NOT stored in Account model**
- Line 210-212: `skinUrl`, `skinVariant`, `capeUrl` passed to `MsLoginResult`

## Account Model Gap (Models.kt)
- `Account` has `skinFileName`, `capeFileName` but **NO `skinVariant` field**
- This means SLIM variant is lost → renders as CLASSIC (wrong model for Alex skins)

## MediaStorage Gap
- Downloads images but **no content-addressed caching by texture hash**
- No placeholder/fallback handling in UI
- No background refresh on app start/token expiry

## Display Points (All use skinFileName/capeFileName from Account)
1. **HomeScreen.kt:201-205** → `SkinHeadFace` (static head avatar)
2. **AccountSwitcherDialog.kt:130-134** → `SkinHeadFace`
3. **AccountsScreen.kt:288-292** → `SkinHeadFace` in AccountRow
4. **SkinCustomizationDialog.kt:179-186** → `SkinModelStage` → `SkinModelView` (3D model)
5. **SkinModelView.kt:120-155** → loads skin/cape bitmaps, creates `PlayerRig`

## Customization Entry Points
1. **AccountsScreen.kt:324-331** - Dress button (ALWAYS shown, should hide for Microsoft)
2. **SkinCustomizationDialog.kt:192** - Pick/clear actions guarded by `canCustomize` (correctly false for Microsoft)
3. **HomeScreen** - Head click opens AccountsScreen (not direct customize)

## Hypotheses to Test
a. ✅ Profile requested, skins/capes parsed correctly (code looks correct)
b. ❓ Profile request fails silently - need to check error handling
c. ✅ URLs normalized to HTTPS (normalizeTextureUrl does this)
d. ✅ Display code handles both account kinds (uses skinFileName from Account)
e. ✅ Skin loaded once at sign-in, never refreshed - **CONFIRMED GAP**

## Fix Priority
1. Add `skinVariant` to Account model and propagate through storage
2. Use `skinVariant` in SkinHeadFace and SkinModelView for SLIM rendering
3. Add texture-hash-based caching in MediaStorage
4. Add profile refresh on app start / token expiry for Microsoft accounts
5. Hide dress button for Microsoft accounts in AccountsScreen
6. Add JVM tests for parsing, HTTP, and UI wiring