# PLAN

- [x] Phase 0: Research & verify Minecraft profile API format (official docs)
- [x] Phase 1: Trace the full display path and identify all bugs
- [ ] Phase 2: Fix profile parsing + storage (MicrosoftAuth, Account model, MediaStorage)
  - [ ] Add texture hash-based caching in MediaStorage
  - [ ] Normalize URLs to HTTPS in parser (already done, verify)
  - [ ] Store skinVariant (slim) in Account model
  - [ ] Add profile refresh function in MicrosoftAuth
- [ ] Phase 3: Fix display at all points
  - [ ] SkinHeadFace: use skinVariant for slim rendering
  - [ ] SkinModelView: use skinVariant for slim model
  - [ ] Account switcher, Home screen, Accounts screen: verify they work
- [ ] Phase 4: Hide customization for Microsoft accounts
  - [ ] Remove dress button from AccountsScreen for Microsoft accounts
  - [ ] Verify SkinCustomizationDialog already hides actions (canCustomize=false)
  - [ ] Check any other entry points
- [ ] Phase 5: Add profile refresh on app start / token expiry
  - [ ] Background refresh when app starts with Microsoft account
  - [ ] Refresh on token refresh (expiresAtEpochSeconds)
- [ ] Phase 6: JVM Tests
  - [ ] Profile parsing test with fixtures (active skin, slim, active cape, no cape)
  - [ ] Fake HTTP test (success, 401, network error)
  - [ ] UI wiring test: Microsoft account shows skin, no customize; local has customize
- [ ] Phase 7: CI run, collect links, report, STOP for phone test

## CI Budget
- Max 4 CI runs (3 for fixes, 1 for final verification)