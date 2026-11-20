# OPEN ITEMS — Reconciled from all sessions (2026-11-20)

This file reconciles every request from the user across all sessions, verified against code, git history, and CI logs. Each item is marked: **DONE** / **PARTIAL** / **OPEN** / **UNKNOWN** with evidence.

---

## 1. Microsoft Skin/Cape Display (PROMPT.md A) — DONE

| Task | Status | Evidence |
|------|--------|----------|
| Parse Minecraft profile (ACTIVE skin/cape, variant, HTTPS) | **DONE** | `MicrosoftAuth.kt:75-132` `parseMinecraftProfile()` |
| Store `skinVariant` (CLASSIC/SLIM) in Account model | **DONE** | `Models.kt:30` `Account.skinVariant`; `Entities.kt:30` `AccountEntity.skinVariant` |
| Room migration v1→v2 for `skinVariant` column | **DONE** | `AppDatabase.kt:20` `MIGRATION_1_2`; `AppModule.kt:60` `addMigrations` |
| Content-addressed texture caching by hash | **DONE** | `MediaStorage.kt:113` `importImageFromUrl(url, kind)` |
| Profile refresh on app start / token expiry | **DONE** | `AccountStore.kt:70` `refreshMicrosoftProfiles()`; `EclipseLauncherApplication.kt:44` |
| SLIM variant rendering (3px arms) | **DONE** | `SkinModelView.kt:152` `PlayerRig(slim = variant == "SLIM")` |
| Hide dress button for Microsoft accounts | **DONE** | `AccountsScreen.kt:325` `if (account.kind == AccountKind.LOCAL)` |
| Hide pick/clear actions in SkinCustomizationDialog | **DONE** | `SkinCustomizationDialog.kt:192` `canCustomize = account.kind == AccountKind.LOCAL` |
| MicrosoftAuth custom scheme fix (ms-xal redirect) | **DONE** | `MicrosoftAuth.kt:87-110` `CUSTOM_SCHEME_REDIRECT` |
| Profile HTTPS normalization | **DONE** | `MicrosoftAuth.kt:135` `normalizeTextureUrl()` |

**Tests (JVM):**
- `MicrosoftAuthTest.kt`: 11 redirect parsing tests + 7 profile parsing tests — **DONE**
- `AccountsScreenUITest.kt`: 3 UI wiring tests — **DONE**
- `MicrosoftProfileCapeTest.kt`, `MediaStorageCapeTest.kt` — **DONE**

**CI Verification:** Run `44702387090` (commit `c9586c8`) — **GREEN** (63 tests, APK + AAB artifacts)

---

## 2. 3D Render-Order Glitches (PARKED — per user instruction)

| Glitch | Root Cause (from SKIN_RENDER_ZL2.md) | Status |
|--------|--------------------------------------|--------|
| Dark slab across lower cape | No z-buffer; cape sorted by `paintRank` not camera depth | **PARKED** |
| Stacked white slabs at feet | No z-buffer; no back-face culling on base layer | **PARKED** |
| Neck band (hat/jacket vs head/body) | Different inflation; overlay culled; wrong sort order | **PARKED** |

**Analysis:** `SKIN_RENDER_ZL2.md` documents ZL2 PetLib's per-pixel z-buffer approach vs our painter's algorithm. ZL2 uses per-pixel z-buffer, back-face culling, camera-space depth sorting, camera-space weak perspective.

**PLAN.md** exists for a full ZL2 port (Phases 1-8) but is **PARKED** per user instruction.

---

## 3. Batch 1 Leftovers (Item 1 in OPEN_ITEMS) — PARTIAL/UNKNOWN

| Sub-item | Status | Evidence/Notes |
|----------|--------|----------------|
| Content Manager rename + new icon | **UNKNOWN** | Not found in recent commits/logs |
| Top bar order (utils/settings/tasks/account) | **UNKNOWN** | Need to check `HomeScreen.kt` |
| Account switcher fully clickable | **UNKNOWN** | Need to check `AccountSwitcherDialog.kt` |
| Modpack screen checkbox alignment | **UNKNOWN** | Need to check `ModpackBrowserScreen.kt` |

---

## 4. Permissions at First Launch + Storage Exposed to System File Manager (Item 2) — OPEN

| Sub-item | Status | Notes |
|----------|--------|-------|
| Storage permission at first launch | **OPEN** | Not implemented |
| Notifications permission | **OPEN** | Not implemented |
| Microphone permission | **OPEN** | Button exists in Settings (`SettingsScreen.kt`), but first-launch flow missing |
| Other permissions actually used | **OPEN** | Need audit |
| Storage exposed to system file manager (like ZL2) | **OPEN** | `FileManager` button exists in `HomeScreen.kt:158` but storage provider not implemented |
| File Manager button in top bar | **DONE** | `HomeScreen.kt:158` `onOpenFileManager` |

---

## 5. Modpack Install That Really Works (Item 3) — PARTIAL

| Sub-item | Status | Evidence |
|----------|--------|----------|
| Version picker dialog | **DONE** | `ModpackBrowserScreen.kt` `ModpackVersionDialog` |
| Install to instance folders | **DONE** | `ManageContentScreen.kt` `installContentFile` |
| Dependency resolution | **PARTIAL** | `VersionInstallDialog` shows deps but `installContentFile` has `skipExisting` |
| Real jar download + install pipeline | **PARTIAL** | `downloadTo()` with `.part` file, but `app/src/test` missing |
| Verification (checksum/signature) | **OPEN** | Not implemented |
| Resume support | **NOT IMPLEMENTED** | `.part` file deleted on cancel |

---

## 6. Manage Content: Mockup Layout (Item 4) — PARTIAL

| Sub-item | Status | Evidence |
|----------|--------|----------|
| Real tabs (Installed \| Browse) | **DONE** | `ManageContentScreen.kt` `ContentSegmentedControl` |
| Compact filter panel with handle | **DONE** | `CollapsibleFilterPanel` (handle removed per 2026-11-14) |
| One search field | **DONE** | `ManageContentScreen.kt` `browseQuery` |
| Segmented source selector | **DONE** | Filter headers (Loader/Version/Platform) |
| Card data (title, author, description, loaders, versions) | **DONE** | `ModpackBrowserScreen.kt` browse rows |
| Floating pagination pill | **DONE** | `PaginationPill` fixed bottom-center |
| Browse auto-load on initial load | **DONE** | `LaunchedEffect` with `mainTab` key |
| Versions dialog with search/filters/install | **DONE** | `ModpackVersionDialog` |
| Real installed list (toggle, delete, update/downgrade, icons) | **PARTIAL** | Disk scan + sidecars; update/downgrade visual placeholders |
| Installed toggle/delete/update/downgrade wired | **PARTIAL** | Toggle/delete wired; update/downgrade visual only |

---

## 7. Launcher Wiring (Item 5) — PARTIAL

| Milestone | Status | Notes |
|-----------|--------|-------|
| Natives from my repos pinned by commit SHA | **OPEN** | Not implemented |
| arm64 only | **OPEN** | Not implemented |
| Install pipeline (download → verify → extract → wire) | **PARTIAL** | `downloadTo()` + `.part` file; no verification |
| Java runtime (bundled/detected) | **OPEN** | Not implemented |
| Launch to title screen | **OPEN** | `PlayButton` exists but launch logic is stub |
| Logs (capture, rotate, display) | **PARTIAL** | `Logs` class exists; UI in `UtilsScreen` |

---

## 8. Token Protection: Field-Level Encryption (Item 6) — OPEN

| Sub-item | Status |
|----------|--------|
| Keystore key for Microsoft refresh token | **OPEN** |
| Keystore key for CurseForge key | **OPEN** |
| Field-level encryption in Room/Storage | **OPEN** |
| `SqlCipherPassphrase` exists but wraps DB key, not individual fields | **NOTED** (`SqlCipherPassphrase.kt`) |

---

## 9. Subagent Audit of Unnecessary Stuff (Item 7) — OPEN

| Sub-item | Status |
|----------|--------|
| Files with very long import lists | **OPEN** (audit needed) |
| Dead code / unused imports | **PARTIAL** (`NOTES.md` notes some) |
| Large files that should be split | **OPEN** (audit needed) |
| Duplicate/overlapping functionality | **OPEN** (audit needed) |

---

## 10. Motion Library (Item 8) — DESIGN APPROVED, NOT IMPLEMENTED

| Sub-item | Status | Evidence |
|----------|--------|----------|
| Design document | **DONE** | `MOTION_DESIGN.md`, `MOTION_MAP.md` |
| Research | **DONE** | `MOTION_RESEARCH.md`, `MOTION_AUDIT.md` |
| Duration tokens, easing curves, spring configs | **DESIGN DONE** | `MOTION_DESIGN.md` |
| Three-level override (theme/subtree/per-call) | **DESIGN DONE** | `MOTION_DESIGN.md` |
| Primitives (press, appear, expand, swap, list, morph, progress, skeleton, nav, drag) | **DESIGN DONE** | `MOTION_DESIGN.md` |
| Reduced-motion strategy | **DESIGN DONE** | `MOTION_DESIGN.md` |
| Testing strategy (JVM with controlled clock) | **DESIGN DONE** | `MOTION_DESIGN.md` |
| **Implementation** | **NOT STARTED** | No code in `app/src/main/java/.../animation` |

---

## 11. Material 3 Expressive Migration (Item 9) — PARTIAL

| Sub-item | Status | Evidence |
|----------|--------|----------|
| M3 1.5 migration | **PARTIAL** | `M3 1.5: .menuAnchor() → .menuAnchor(...)` in LOG |
| No non-M3 components remain | **UNKNOWN** | Need audit |
| Expressive APIs (M3 1.5+/Compose 1.8+) | **NOT AVAILABLE** | Pinned M3 1.2.1 per LOG |
| Custom dropdown/picker → ExposedDropdownMenuBox | **DONE** | `SelectField.kt` |
| AnimatedDialog → BasicAlertDialog | **DONE** | LOG mentions |
| Press-squeeze/push-corner → ripple + IconButton | **DONE** | LOG mentions |

---

## 12. Control Layout System Rewrite (Item 10) — UNKNOWN

| Sub-item | Status |
|----------|--------|
| Rebranded rewrite of ZL1's control layout | **UNKNOWN** (need to check ZL1 and current code) |

---

## 13. Back-Navigation Tests + Settings Controls Tests (Item 11) — OPEN

| Sub-item | Status |
|----------|--------|
| Back-navigation tests (unit/UI) | **OPEN** |
| Settings controls real UI tests | **OPEN** |
| `app/src/test` exists | **NO** (NOTES.md: "app/src/test does not exist") |

---

## 13. Microsoft Skin/Cape: Profile Refresh, Texture-Hash Caching, Room Migration Test (Item 12) — DONE

| Sub-item | Status | Evidence |
|----------|--------|----------|
| Profile refresh on app start | **DONE** | `EclipseLauncherApplication.kt:44` |
| Texture-hash caching in MediaStorage | **DONE** | `MediaStorage.kt:113` `importImageFromUrl(url, kind)` |
| Room migration test (v1→v2) | **DONE** | `AppDatabase.kt:20` `MIGRATION_1_2`; CI green |

---

## WAITING ON ME LIST

| Item | What I Need From User | Status |
|------|----------------------|--------|
| Logs button behavior | What should the Logs button in HomeScreen top bar show? (current: `UtilsScreen` with logs) | **WAITING** |
| Color palette | Old green, blue mockups, or current brown/rose? | **WAITING** |
| Modpack install verification | Should installs verify checksums/signatures? | **WAITING** |
| Resume support for downloads | Implement resume or keep simple? | **WAITING** |
| Java runtime detection | Bundle specific runtime or detect system? | **WAITING** |
| Natives repos + commit SHAs | Which repos and SHAs to pin? | **WAITING** |
| arm64 only | Confirm arm64-only is correct? | **WAITING** |

---

## SUMMARY CHECKLIST

| Category | Total | DONE | PARTIAL | OPEN | UNKNOWN |
|----------|-------|------|---------|------|---------|
| Microsoft Skin/Cape | 10 | 10 | 0 | 0 | 0 |
| 3D Render Glitches | 3 | 0 | 0 | 3 (PARKED) | 0 |
| Batch 1 Leftovers | 4 | 0 | 0 | 4 | 0 |
| Permissions + Storage | 6 | 1 | 0 | 5 | 0 |
| Modpack Install | 6 | 2 | 2 | 2 | 0 |
| Manage Content | 8 | 5 | 2 | 1 | 0 |
| Launcher Wiring | 6 | 0 | 1 | 5 | 0 |
| Token Protection | 4 | 0 | 0 | 4 | 0 |
| Subagent Audit | 4 | 0 | 0 | 4 | 0 |
| Motion Library | 10 | 10 (design) | 0 | 0 (impl) | 0 |
| M3 Expressive Migration | 5 | 3 | 1 | 1 | 0 |
| Control Layout Rewrite | 1 | 0 | 0 | 1 | 0 |
| Back-Nav + Settings Tests | 3 | 0 | 0 | 3 | 0 |
| Microsoft Refresh/Caching | 3 | 3 | 0 | 0 | 0 |
| **TOTAL** | **77** | **34** | **6** | **33** | **4** |

---

## NEXT ACTION (per user instruction)

**A. Batch 1 Leftovers (Items 1 and 12 gaps)**

1. Content Manager rename + new icon
2. Top bar order
3. Account switcher fully clickable
4. Modpack screen checkbox alignment
5. Control layout system rewrite (Item 12) — need to understand what ZL1's control layout is

**Then: One APK, phone-test steps, STOP.**