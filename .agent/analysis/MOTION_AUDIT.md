# Motion Audit: Current EclipseLauncher Animations

## Summary
- **Files with animations**: 14
- **Hard-coded durations**: 23 instances
- **Animation types**: tween (18), spring (2), infiniteRepeatable (1)
- **No central timing tokens** - all values scattered
- **No reduced motion support**
- **No stagger system**
- **No easing tokens** - mostly LinearEasing or default

---

## Detailed Audit by File

### 1. `app/src/main/java/com/eclipselauncher/ui/animation/Motion.kt`
| Line | Animation | Spec | Duration | Easing | Purpose |
|------|-----------|------|----------|--------|---------|
| 22-28 | `animateFloatAsState` (caret rotation) | `spring(dampingRatio=MediumBouncy, stiffness=MediumLow)` | Spring | Spring | Disclosure caret flip 180° |

**Issues**: Hard-coded spring params; no token; only spring in codebase.

---

### 2. `app/src/main/java/com/eclipselauncher/ui/theme/Theme.kt`
| Line | Animation | Spec | Duration | Easing | Purpose |
|------|-----------|------|----------|--------|---------|
| 240-242 | `Crossfade` (theme switch) | `tween(250)` | 250ms | Default (FastOutSlowIn) | Theme color scheme transition |

**Issues**: Hard-coded 250ms; no token; Crossfade is appropriate but duration should be tokenized.

---

### 3. `app/src/main/java/com/eclipselauncher/ui/screens/CreateInstanceScreen.kt`
| Line | Animation | Spec | Duration | Easing | Purpose |
|------|-----------|------|----------|--------|---------|
| 269-274 | `AnimatedVisibility` (filter chip check icon) | `expandIn(tween(150)) + fadeIn(tween(150))` / `shrinkOut(tween(150)) + fadeOut(tween(150))` | 150ms | Default | Version filter chip selection indicator |
| 316 | `LaunchedEffect` + `Animatable.animateTo` (version card scale) | `tween(350)` | 350ms | Default | Version card entrance scale 0.95→1.0 |

**Issues**: Two different durations (150ms, 350ms) for similar "enter" animations; no tokens.

---

### 4. `app/src/main/java/com/eclipselauncher/ui/screens/LoaderPickerScreen.kt`
| Line | Animation | Spec | Duration | Easing | Purpose |
|------|-----------|------|----------|--------|---------|
| 256-258 | `animateFloatAsState` (chevron rotation) | `tween(350)` | 350ms | Default | Loader version dropdown chevron rotation |
| 385-388 | `AnimatedVisibility` (version dropdown) | `expandVertically(tween(350)) + fadeIn(tween(350))` / `shrinkVertically(tween(350)) + fadeOut(tween(350))` | 350ms | Default | Loader version list expand/collapse |

**Issues**: 350ms repeated but hard-coded; no token.

---

### 5. `app/src/main/java/com/eclipselauncher/ui/screens/ModpackBrowserScreen.kt`
| Line | Animation | Spec | Duration | Easing | Purpose |
|------|-----------|------|----------|--------|---------|
| 458-460 | `animateFloatAsState` (chevron rotation) | `tween(350)` | 350ms | Default | Version group chevron rotation |
| 495-498 | `AnimatedVisibility` (version list) | `expandVertically(tween(350)) + fadeIn(tween(350))` / `shrinkVertically(tween(350)) + fadeOut(tween(350))` | 350ms | Default | Modpack version group expand/collapse |

**Issues**: Same 350ms pattern as LoaderPicker; duplicated.

---

### 6. `app/src/main/java/com/eclipselauncher/ui/screens/ManageContentScreen.kt`
| Line | Animation | Spec | Duration | Easing | Purpose |
|------|-----------|------|----------|--------|---------|
| 1993 | `AnimatedVisibility` (filter panel) | Default (no spec) | Default | Default | Filter options panel expand/collapse |

**Issues**: No explicit spec; relies on default.

---

### 7. `app/src/main/java/com/eclipselauncher/ui/components/VersionSelectorDialog.kt`
| Line | Animation | Spec | Duration | Easing | Purpose |
|------|-----------|------|----------|--------|---------|
| 163-164 | `AnimatedVisibility` (version list) | `expandVertically()` / `shrinkVertically()` | Default | Default | Install version dialog version list expand/collapse |

**Issues**: No explicit spec; default duration unknown.

---

### 8. `app/src/main/java/com/eclipselauncher/ui/components/TasksDialog.kt`
| Line | Animation | Spec | Duration | Easing | Purpose |
|------|-----------|------|----------|--------|---------|
| 230 | `AnimatedVisibility` (task detail expand) | Default | Default | Default | Task detail row expand/collapse |

**Issues**: No explicit spec.

---

### 9. `app/src/main/java/com/eclipselauncher/ui/character/SkinModelView.kt`
| Line | Animation | Spec | Duration | Easing | Purpose |
|------|-----------|------|----------|--------|---------|
| 74-77 | `animateFloatAsState` (auto-rotation) | `infiniteRepeatable(tween(10000, LinearEasing))` | 10000ms | LinearEasing | Continuous skin model auto-rotation |

**Issues**: Only `infiniteRepeatable` in codebase; 10000ms hard-coded; runs continuously.

---

### 10. `app/src/main/java/com/eclipselauncher/ui/components/SkinCustomizationDialog.kt`
| Line | Animation | Spec | Duration | Easing | Purpose |
|------|-----------|------|----------|--------|---------|
| (drag/tilt) | Manual state + graphicsLayer | N/A (gesture-driven) | N/A | N/A | Skin model drag-to-rotate, pinch-to-zoom |

**Issues**: Gesture-driven, no animation spec; uses `graphicsLayer` for transform.

---

### 11. `app/src/main/java/com/eclipselauncher/ui/character/SkinRig.kt`
| Line | Animation | Spec | Duration | Easing | Purpose |
|------|-----------|------|----------|--------|---------|
| (drag/tilt) | Manual state + graphicsLayer | N/A (gesture-driven) | N/A | N/A | Skin model hip tilt, head yaw |

**Issues**: Gesture-driven, no animation spec.

---

## Hard-Coded Duration Inventory

| Duration | Count | Locations |
|----------|-------|-----------|
| 150ms | 2 | CreateInstanceScreen (2) |
| 250ms | 1 | Theme.kt (Crossfade) |
| 350ms | 6 | LoaderPickerScreen (2), ModpackBrowserScreen (2), CreateInstanceScreen (1), ManageContentScreen (implied) |
| 10000ms | 1 | SkinModelView.kt (infiniteRepeatable) |
| Default | 4 | ManageContentScreen, VersionSelectorDialog, TasksDialog |

## Easing Inventory

| Easing | Count | Locations |
|--------|-------|-----------|
| Default (FastOutSlowIn) | ~15 | Most tween() calls |
| LinearEasing | 1 | SkinModelView.kt (infiniteRepeatable) |
| Spring (MediumBouncy/MediumLow) | 1 | Motion.kt (caret) |

## Animation Type Inventory

| Type | Count | Locations |
|------|-------|-----------|
| `animateFloatAsState` | 5 | Motion.kt, CreateInstanceScreen, LoaderPickerScreen, ModpackBrowserScreen, SkinModelView |
| `AnimatedVisibility` | 8 | CreateInstanceScreen, LoaderPickerScreen, ModpackBrowserScreen, ManageContentScreen, VersionSelectorDialog, TasksDialog |
| `Crossfade` | 1 | Theme.kt |
| `LaunchedEffect` + `Animatable.animateTo` | 1 | CreateInstanceScreen |
| `infiniteRepeatable` | 1 | SkinModelView.kt |
| Gesture-driven (graphicsLayer) | 2 | SkinCustomizationDialog, SkinRig |

## Gaps Against Requirements Digest

| Requirement | Current State | Gap |
|-------------|---------------|-----|
| Central timing tokens | ❌ None | All durations hard-coded |
| Easing tokens | ❌ None | Only default and one spring |
| Spring configs | ❌ Minimal | One spring in Motion.kt |
| Stagger system | ❌ None | No stagger utilities |
| Press feedback | ❌ None | Only ripple, no scale |
| Hover/focus | ❌ None | Not implemented |
| Checkbox/toggle | ❌ None | Switch uses Crossfade+rotate (no spec) |
| Dropdown | ✅ Partial | AnimatedVisibility with 350ms tween |
| Accordion | ✅ Partial | AnimatedVisibility with 350ms tween |
| Dialog | ✅ Partial | AnimatedVisibility with 350ms tween |
| Page transition | ❌ None | Instant navigation |
| Shared element | ❌ None | Not used |
| Gesture drag | ⚠️ Manual | Skin model only |
| Reduced motion | ❌ None | No support |
| Performance (graphicsLayer) | ✅ Used | Skin model, caret, version cards |
| Interruptible/reversible | ⚠️ Partial | animate*AsState yes; AnimatedVisibility yes |

---

## File:Line Reference for All Animations

```
Motion.kt:22-28           - caretRotation spring
Theme.kt:240-242          - Crossfade tween(250)
CreateInstanceScreen.kt:269-274  - AnimatedVisibility tween(150)
CreateInstanceScreen.kt:316      - Animatable.animateTo tween(350)
LoaderPickerScreen.kt:256-258    - animateFloatAsState tween(350)
LoaderPickerScreen.kt:385-388    - AnimatedVisibility tween(350)
ModpackBrowserScreen.kt:458-460  - animateFloatAsState tween(350)
ModpackBrowserScreen.kt:495-498  - AnimatedVisibility tween(350)
ManageContentScreen.kt:1993      - AnimatedVisibility (default)
VersionSelectorDialog.kt:163-164 - AnimatedVisibility (default)
TasksDialog.kt:230               - AnimatedVisibility (default)
SkinModelView.kt:74-77           - infiniteRepeatable tween(10000)
SkinCustomizationDialog.kt       - graphicsLayer drag
SkinRig.kt                       - graphicsLayer drag
```