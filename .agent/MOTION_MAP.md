# Motion Map: Per-Screen/Component Animation Plan

## Legend
- ✅ **ADD** - Add new purposeful animation
- 🔄 **REPLACE** - Replace hard-coded with library token
- ❌ **REMOVE** - Remove decorative/unnecessary animation
- ⏭️ **LEAVE** - Keep as-is (already good or out of scope)
- 📝 **NOTE** - Special consideration

---

## Screens

### 1. HomeScreen
| Component | Current | Action | Library Primitive | Purpose |
|-----------|---------|--------|-------------------|---------|
| Instance panel chevron | `caretRotation` spring | 🔄 REPLACE | `Modifier.pressFeedback` + `AnimatedVisibility` | Disclose instance list |
| Instance dropdown | Instant | ✅ ADD | `AnimatedVisibility` (Dropdown role) | Smooth list reveal |
| Instance list items | None | ✅ ADD | `AnimatedLazyColumn` (stagger) | Entrance choreography |
| Play button | Ripple only | ✅ ADD | `Modifier.pressFeedback` | Press scale feedback |
| Theme switch (Crossfade) | `Crossfade tween(250)` | 🔄 REPLACE | `Crossfade` with token | Theme transition |
| Account switcher | Instant | ✅ ADD | `AnimatedVisibility` (Dialog role) | Panel slide |
| Tasks button | None | ✅ ADD | `Modifier.pressFeedback` | Press feedback |

**Deliberately Still**: App mark, nametag, header icons (no interaction feedback needed)

---

### 2. CreateInstanceScreen
| Component | Current | Action | Library Primitive | Purpose |
|-----------|---------|--------|-------------------|---------|
| Version filter chips (check icon) | `AnimatedVisibility tween(150)` | 🔄 REPLACE | `AnimatedVisibility` (Accordion role) | Chip selection feedback |
| Version cards (scale entrance) | `Animatable.animateTo tween(350)` | 🔄 REPLACE | `AnimatedLazyColumn` + `Modifier.animated` | Staggered entrance |
| Version type badges | None | ✅ ADD | `AnimatedVisibility` (Default) | Badge appear |
| MC version list | None | ✅ ADD | `AnimatedLazyColumn` (stagger) | List entrance |
| Modpack button | None | ✅ ADD | `Modifier.pressFeedback` | Press feedback |

**Deliberately Still**: Version badges (static), header

---

### 3. LoaderPickerScreen
| Component | Current | Action | Library Primitive | Purpose |
|-----------|---------|--------|-------------------|---------|
| Loader row chevron | `animateFloatAsState tween(350)` | 🔄 REPLACE | `Modifier.animated` (chevron) | Disclosure rotation |
| Version dropdown | `AnimatedVisibility tween(350)` | 🔄 REPLACE | `AnimatedVisibility` (Dropdown role) | Version list expand |
| Loader cards | None | ✅ ADD | `AnimatedLazyColumn` (stagger) | Entrance choreography |
| Install button | None | ✅ ADD | `Modifier.pressFeedback` | Press feedback |

**Deliberately Still**: Loader icons (static), unavailable badges

---

### 4. ModpackBrowserScreen
| Component | Current | Action | Library Primitive | Purpose |
|-----------|---------|--------|-------------------|---------|
| Version group chevron | `animateFloatAsState tween(350)` | 🔄 REPLACE | `Modifier.animated` (chevron) | Disclosure rotation |
| Version list expand | `AnimatedVisibility tween(350)` | 🔄 REPLACE | `AnimatedVisibility` (Accordion role) | Group expand |
| Modpack cards | None | ✅ ADD | `AnimatedLazyColumn` (stagger) | Entrance choreography |
| Filter headers | None | ✅ ADD | `AnimatedVisibility` (Dropdown role) | Filter panel expand |
| Pagination buttons | None | ✅ ADD | `Modifier.pressFeedback` | Press feedback |

**Deliberately Still**: Modpack icons, header

---

### 5. ManageContentScreen
| Component | Current | Action | Library Primitive | Purpose |
|-----------|---------|--------|-------------------|---------|
| Filter panel | `AnimatedVisibility (default)` | 🔄 REPLACE | `AnimatedVisibility` (Accordion role) | Filter options expand |
| Installed/Browse tabs | Instant | ✅ ADD | `AnimatedContentSwap` (Tab role) | Tab transition |
| Content cards | None | ✅ ADD | `AnimatedLazyColumn` (stagger) | Entrance choreography |
| Action buttons (toggle/delete/update) | None | ✅ ADD | `Modifier.pressFeedback` | Press feedback |

**Deliberately Still**: Content icons, header

---

### 6. InstanceEditorScreen
| Component | Current | Action | Library Primitive | Purpose |
|-----------|---------|--------|-------------------|---------|
| Dropdown fields | Instant | ✅ ADD | `AnimatedVisibility` (Dropdown role) | Menu expand |
| Section expand/collapse | Instant | ✅ ADD | `AnimatedVisibility` (Accordion role) | Section toggle |
| Save button | None | ✅ ADD | `Modifier.pressFeedback` | Press feedback |

**Deliberately Still**: Form fields, preview image

---

### 7. AccountsScreen
| Component | Current | Action | Library Primitive | Purpose |
|-----------|---------|--------|-------------------|---------|
| Account cards | None | ✅ ADD | `AnimatedLazyColumn` (stagger) | Entrance |
| Add account button | None | ✅ ADD | `Modifier.pressFeedback` | Press feedback |
| Microsoft sign-in button | None | ✅ ADD | `Modifier.pressFeedback` | Press feedback |

**Deliberately Still**: Account avatars, header

---

### 8. SettingsScreen
| Component | Current | Action | Library Primitive | Purpose |
|-----------|---------|--------|-------------------|---------|
| Setting rows | None | ✅ ADD | `AnimatedLazyColumn` (stagger) | Entrance |
| Toggle switches | Crossfade + rotate (no spec) | 🔄 REPLACE | `AnimatedVisibility` + `Modifier.animated` (Switch role) | Toggle feedback |
| Section headers | Instant | ✅ ADD | `AnimatedVisibility` (Accordion role) | Section expand |
| Theme selector | Crossfade (theme switch) | 🔄 REPLACE | Already handled in Theme | - |

**Deliberately Still**: Slider thumbs, color previews

---

### 9. TasksDialog
| Component | Current | Action | Library Primitive | Purpose |
|-----------|---------|--------|-------------------|---------|
| Dialog entrance | "spring-open / corner-suck" (comment only) | ✅ ADD | `AnimatedVisibility` (Dialog role) | Dialog entrance |
| Task detail expand | `AnimatedVisibility (default)` | 🔄 REPLACE | `AnimatedVisibility` (Accordion role) | Detail expand |
| Task list | None | ✅ ADD | `AnimatedLazyColumn` (stagger) | Entrance |
| Minimize button | None | ✅ ADD | `Modifier.pressFeedback` | Press feedback |

**Deliberately Still**: Progress indicators (real progress)

---

### 10. VersionSelectorDialog (Manage Content → Update)
| Component | Current | Action | Library Primitive | Purpose |
|-----------|---------|--------|-------------------|---------|
| Dialog entrance | Instant | ✅ ADD | `AnimatedVisibility` (Dialog role) | Dialog entrance |
| Version groups | `AnimatedVisibility (default)` | 🔄 REPLACE | `AnimatedVisibility` (Accordion role) | Group expand |
| Version rows | None | ✅ ADD | `AnimatedLazyColumn` (stagger) | Entrance |
| Install button | None | ✅ ADD | `Modifier.pressFeedback` | Press feedback |
| Progress indicator | Real progress | ⏭️ LEAVE | - | Real progress |

**Deliberately Still**: Version badges, header

---

### 11. SkinCustomizationDialog
| Component | Current | Action | Library Primitive | Purpose |
|-----------|---------|--------|-------------------|---------|
| Dialog entrance | Instant | ✅ ADD | `AnimatedVisibility` (Dialog role) | Dialog entrance |
| 3D model drag/rotate | Manual `graphicsLayer` | ⏭️ LEAVE | - | Direct manipulation |
| Pinch zoom | Manual `graphicsLayer` | ⏭️ LEAVE | - | Direct manipulation |
| Auto-rotation | `infiniteRepeatable tween(10000)` | 🔄 REPLACE | `infiniteRepeatable` with token | Continuous rotation |

**Deliberately Still**: All 3D interactions (gesture-driven, not animated)

---

### 12. ModpackBrowserScreen (Create Instance → Modpack)
| Component | Current | Action | Library Primitive | Purpose |
|-----------|---------|--------|-------------------|---------|
| Version group chevron | `animateFloatAsState tween(350)` | 🔄 REPLACE | `Modifier.animated` (chevron) | Disclosure rotation |
| Version list expand | `AnimatedVisibility tween(350)` | 🔄 REPLACE | `AnimatedVisibility` (Accordion role) | Group expand |
| Modpack cards | None | ✅ ADD | `AnimatedLazyColumn` (stagger) | Entrance choreography |
| Filter headers | None | ✅ ADD | `AnimatedVisibility` (Dropdown role) | Filter panel expand |

**Deliberately Still**: Modpack icons, header

---

## Components (Reusable)

### Button / Interactive
| Component | Current | Action | Purpose |
|-----------|---------|--------|---------|
| All Buttons | Ripple only | ✅ ADD `Modifier.pressFeedback` | Press scale 0.95 |
| IconButton | Ripple only | ✅ ADD `Modifier.pressFeedback` | Press scale 0.95 |
| FilterChip | Ripple only | ✅ ADD `Modifier.pressFeedback` | Press scale 0.95 |
| Switch | Crossfade + rotate (no spec) | 🔄 REPLACE with `AnimatedExpansion` + `Modifier.animated` | Thumb slide + icon morph |
| Checkbox | None | ✅ ADD `AnimatedVisibility` (Checkbox role) | Check mark draw |
| RadioButton | None | ✅ ADD `AnimatedVisibility` (Radio role) | Inner circle expand |

### Lists
| Component | Current | Action | Purpose |
|-----------|---------|--------|---------|
| LazyColumn | None | ✅ ADD `AnimatedLazyColumn` (stagger) | Entrance choreography |
| Item insert | Instant | ✅ ADD `AnimatedItem` | Slide in |
| Item remove | Instant | ✅ ADD `AnimatedItem` | Slide out + fade |
| Item reorder | Instant | 📝 NOTE: Requires `Modifier.animateItemPlacement()` (Compose 1.5+) | FLIP reorder |

### Overlays
| Component | Current | Action | Purpose |
|-----------|---------|--------|---------|
| Dialog | Instant | ✅ ADD `AnimatedVisibility` (Dialog role) | Fade + scale |
| BottomSheet | N/A | ✅ ADD when used | Slide up/down + spring |
| Dropdown/Menu | Instant | ✅ ADD `AnimatedVisibility` (Dropdown role) | Fade + small translate |
| Tooltip | N/A | ✅ ADD when used | Fade + translate |
| Snackbar | N/A | ✅ ADD when used | Slide up + fade |

### Navigation
| Component | Current | Action | Purpose |
|-----------|---------|--------|---------|
| Screen transition | Instant | ✅ ADD `AnimatedContent` + `rememberPageTransition` | Directional slide + fade |
| Tab switch | Instant | ✅ ADD `AnimatedContentSwap` | Crossfade |
| Shared element | Not used | 📝 NOTE: Add when hero transitions needed | Morph |

---

## Animation Inventory by Role

### Press Feedback (80-150ms)
- All Buttons, IconButtons, FilterChips, List items, Cards
- **Token**: `pressFeedback` (100ms), `spring.press`
- **Reduced**: Instant

### Hover/Focus (100-180ms)
- Text fields, focusable cards
- **Token**: `hoverFocus` (150ms), `easing.standard`
- **Reduced**: Instant

### Checkbox/Toggle (120-200ms)
- Checkbox, Switch, RadioButton
- **Token**: `checkboxToggle` (150ms), `switchToggle` (200ms), `easing.expand`
- **Reduced**: Instant state change

### Dropdown (150-220ms)
- Filter headers, menus, version dropdowns, account switcher
- **Token**: `dropdown` (200ms), `easing.expand`, `spring.expand`
- **Reduced**: Instant (snap)

### Accordion/Expand (200-300ms)
- Version groups, filter panels, task details, settings sections
- **Token**: `accordion` (250ms), `easing.expand`, `spring.expand`
- **Reduced**: Instant height change

### Dialog (180-280ms)
- All dialogs (TasksDialog, VersionSelectorDialog, SkinCustomizationDialog, AddAccountGate, AccountSwitcherDialog)
- **Token**: `dialog` (250ms), `easing.emphasized`, `spring.emphasized`
- **Reduced**: Instant (snap)

### Page Transition (250-400ms)
- Screen-to-screen navigation
- **Token**: `pageTransition` (300ms), `easing.pageForward/back`
- **Reduced**: Crossfade instant

### Stagger (20ms/item, cap 200ms)
- All LazyColumn entrances (version lists, modpack lists, instance lists, loader lists, task lists)
- **Token**: `staggerItem` (20ms), `staggerCap` (200ms)
- **Reduced**: All simultaneous

### Continuous (10000ms)
- Skin model auto-rotation
- **Token**: `autoRotation` (10000ms), `LinearEasing`
- **Reduced**: Paused

---

## Gaps to Address (Not Currently Animated)

1. **Navigation transitions** - Currently instant; need `AnimatedContent` wrapper
2. **Tab switching** - Currently instant; need `AnimatedContentSwap`
3. **List entrances** - Currently instant; need `AnimatedLazyColumn`
4. **Press feedback** - Currently only ripple; need scale spring
5. **Switch/Checkbox/Radio** - Currently basic; need animated state change
6. **Stagger** - No stagger system; need `AnimatedLazyColumn` + `AnimatedContext`
7. **Reduced motion** - No support; need system setting + fallback
8. **Shared elements** - Not used; future enhancement

---

## Batch Plan (for Phase 3)

### Batch 1: Core Primitives + Theme + Reduced Motion
- Files: `MotionTokens.kt`, `MotionTheme.kt`, `ReducedMotion.kt`, `EclipseTheme.kt`
- Screens: None (infrastructure only)
- CI: Build + existing tests

### Batch 2: Press Feedback + Basic Appear/Disappear
- Files: `MotionModifiers.kt` (pressFeedback), `AnimatedVisibility` wrappers
- Screens: HomeScreen (play button, instance panel), CreateInstanceScreen (modpack button), LoaderPickerScreen (install button), ModpackBrowserScreen (pagination)
- Components: Button, IconButton, FilterChip
- CI: 2 runs max

### Batch 3: Dropdown/Accordion + Expand/Collapse
- Files: `MotionModifiers.kt` (dropdown, accordion), `AnimatedVisibility` role variants
- Screens: CreateInstanceScreen (version filters), LoaderPickerScreen (version dropdown), ModpackBrowserScreen (filters, version groups), ManageContentScreen (filter panel), VersionSelectorDialog (version groups), TasksDialog (task details), SettingsScreen (sections)
- CI: 2 runs max

### Batch 4: List Stagger + Entrance Choreography
- Files: `AnimatedLazyColumn`, `AnimatedContext`, `Modifier.animated`
- Screens: HomeScreen (instance list), CreateInstanceScreen (version list), LoaderPickerScreen (loader list), ModpackBrowserScreen (modpack list), ManageContentScreen (content list), AccountsScreen, TasksDialog
- CI: 2 runs max

### Batch 5: Switch/Toggle/Checkbox + Navigation Transitions
- Files: Switch/Checkbox/Radio primitives, `AnimatedContent` navigation wrapper
- Screens: SettingsScreen (toggles), Navigation (screen transitions), Tab switching
- CI: 2 runs max

### Batch 6: Dialogs + Overlays + Polish
- Files: Dialog/BottomSheet/Tooltip primitives, `AnimatedContentSwap` for tabs
- Screens: TasksDialog, VersionSelectorDialog, SkinCustomizationDialog, AddAccountGate, AccountSwitcherDialog, Theme switch (already Crossfade)
- CI: 2 runs max

### Batch 7: Gesture Helpers + Skin Model + CI Gate
- Files: `GestureMotion.kt`, `Testing.kt`, CI consistency check
- Screens: SkinCustomizationDialog (auto-rotation token), SkinModelView (drag - no change)
- CI: 2 runs max

**Total CI runs estimate**: ~14 runs (within limits)

---

## Claims Table

| Claim | Status | Evidence |
|-------|--------|----------|
| All current animations audited | VERIFIED | MOTION_AUDIT.md with file:line references |
| ZL2 patterns researched | VERIFIED | MOTION_RESEARCH.md with file:line citations |
| Material 3 APIs verified | VERIFIED | MOTION_RESEARCH.md section 3 |
| Library design complete | VERIFIED | MOTION_DESIGN.md |
| Per-screen map complete | VERIFIED | MOTION_MAP.md |
| No external deps needed | VERIFIED | MOTION_RESEARCH.md section 4 |
| GPL compliance | VERIFIED | MOTION_RESEARCH.md section 5 |
| CI consistency check designed | VERIFIED | MOTION_DESIGN.md section 8 |
| Test strategy defined | VERIFIED | MOTION_DESIGN.md section 7 |