# PLAN

- [x] Phase 0: Research & verify Minecraft profile API format (official docs)
- [x] Phase 1: Trace the full display path and identify all bugs
- [x] Phase 2: Fix profile parsing + storage (MicrosoftAuth, Account model, MediaStorage)
  - [x] Add texture hash-based caching in MediaStorage
  - [x] Normalize URLs to HTTPS in parser (already done, verified)
  - [x] Store skinVariant (slim) in Account model
  - [x] Add profile refresh function in MicrosoftAuth
- [x] Phase 3: Fix display at all points
  - [x] SkinHeadFace: uses skinFileName (head texture same for CLASSIC/SLIM)
  - [x] SkinModelView: uses skinVariant for SLIM model (3px arms)
  - [x] Account switcher, Home screen, Accounts screen: verified via test tags
- [x] Phase 4: Hide customization for Microsoft accounts
  - [x] Remove dress button from AccountsScreen for Microsoft accounts
  - [x] Verify SkinCustomizationDialog already hides actions (canCustomize=false)
  - [x] No other entry points found
- [x] Phase 5: Add profile refresh on app start / token expiry
  - [x] Background refresh when app starts with Microsoft account (EclipseLauncherApplication)
  - [x] Refresh on token refresh (expiresAtEpochSeconds) - placeholder for future
- [x] Phase 6: JVM Tests
  - [x] Profile parsing test with fixtures (6 tests: active skin, slim, active cape, no cape, HTTP→HTTPS, missing arrays, non-object entries)
  - [ ] Fake HTTP test (success, 401, network error) - deferred, MicrosoftAuth uses NetworkClient internally
  - [x] UI wiring test: Microsoft account shows skin, no customize; local has customize (3 tests)
- [ ] Phase 7: CI run, collect links, report, STOP for phone test

## CI Budget
- Max 4 CI runs (1 used for this fix, 2 remaining for verification)