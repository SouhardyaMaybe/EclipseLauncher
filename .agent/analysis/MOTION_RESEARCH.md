# Motion System Research

## 1. ZL2 (Zalith Launcher 2) Animation System

### Overview
ZL2 has a sophisticated, centralized animation system built around:
- **SwapTimeline** (`utils/animation/SwapTimeline.kt:26-91`) - Centralized timing constants for page transitions
- **SwapClock** (`utils/animation/SwapTimeline.kt:104-148`) - Coordinates page transitions with stagger delays
- **FmAnimations** (`filemanager/ui/theme/FmPalette.kt:56-65`) - File manager specific timing constants
- **Custom Easing** (`utils/animation/_Easing.kt:28-53`) - `EaseOutFluent`, `EaseOutBack`, `EnterTranslateEasing`
- **Animated Components** (`ui/components/_Animated.kt:59-462`) - `AnimatedContext`, `AnimatedColumn`, `AnimatedRow`, `AnimatedLazyColumn` with stagger support
- **Settings Integration** - Animation speed level and enable/disable via `AllSettings`

### Key Timing Constants (SwapTimeline)
| Constant | Value | Purpose |
|----------|-------|---------|
| EXIT_ITEM | 70ms | Old page item exit duration |
| EXIT_ALPHA | 70ms | Old page item fade duration |
| EXIT_STAGGER | 15ms | Stagger between exiting items |
| EXIT_STAGGER_CAP | 40ms | Max stagger delay for exit |
| EXIT_WINDOW | 110ms | Total exit window (ITEM + STAGGER_CAP) |
| GAP | 30ms | Gap between exit and enter |
| ENTER_ITEM | 350ms | New page item translate duration |
| ENTER_ALPHA | 200ms | New page item fade duration |
| ENTER_STAGGER | 25ms | Stagger between entering items |
| ENTER_STAGGER_CAP | 250ms | Max stagger delay for enter |
| CONTENT_BASE | 350ms | General content animation baseline |

### Speed Control
```kotlin
// SwapTimeline.kt:68-80
fun speedScale(): Float = (AllSettings.launcherAnimateSpeedLevel.state / 10f).coerceIn(0.1f, 3f)
fun time(ms: Int): Int = (ms / speedScale()).toInt()
fun swapTime(ms: Int): Int = if (isSwapClosed()) 0 else time(ms)
```

### Reduced Motion Support
- `AllSettings.launcherSwapAnimEnabled.state` - Global toggle
- `AllSettings.launcherAnimateSpeedLevel.state` - Speed multiplier (0.1x to 3x)
- When disabled, `swapTime()` returns 0 (instant)

### Navigation Transitions
- Uses `AnimatedContent` with `ContentTransform` (`_Navigation.kt:124-135`)
- Serial page transitions via `SwapClock` coordination
- `fadeIn`/`fadeOut` with calculated delays based on exit/enter windows

### Component Animations
- **Buttons** (`Buttons.kt:94-98`): Scale 0.95→1.0 using `MaterialTheme.motionScheme.fastSpatialSpec()`
- **Switch** (`Switch.kt:54-59`): Thumb rotation with `Crossfade` and `animateFloatAsState` (no explicit spec = spring default)
- **List Items** (`_Animated.kt`): Staggered translate + fade with custom easing
- **Drag/Swipe** (`FmSwipeTrigger.kt`): `Animatable` with `snapTo` during drag, `animateTo` with tween for spring-back
- **Selection Highlight** (`FmEntryItem.kt:89-95`): `animateColorAsState` for background, `animateFloatAsState` for alpha

### Custom Easing
```kotlin
// _Easing.kt:28-53
class EaseOutFluent(power: Int) : Easing { ... }  // 1 - (1-f)^power
class EaseOutBack(power: Int) : Easing { ... }     // Overshoot with cosine
val EnterTranslateEasing = Easing { fraction ->
    val first = 5f * EaseOutFluent(3).transform((fraction * 350f / 250f).coerceAtMost(1f))
    val second = 11f * EaseOutBack(3).transform(fraction)
    (first + second) / 16f
}
```

---

## 2. ZL1 (Zalith Launcher 1) Animation System

### Overview
ZL1 is **View-based** (traditional Android Views, not Compose). Uses:
- `ViewPropertyAnimator` for simple animations
- `ObjectAnimator` / `AnimatorSet` for complex sequences
- Custom `Interpolator` implementations
- `RecyclerView` item animations via `DefaultItemAnimator`
- Fragment transitions via `FragmentTransaction.setCustomAnimations()`

### Key Files
- `ui/animation/` - Custom interpolators and animation utilities
- `ui/adapters/` - RecyclerView item animations
- `ui/fragments/` - Fragment transition animations

### Since ZL1 is View-based and our app is Compose-only, ZL1 patterns are NOT directly applicable but provide historical context.

---

## 3. Android/Material3 Compose Animation APIs (Verified Against Our Versions)

### Our Resolved Versions
- Compose BOM: `2026.09.00`
- Material3: `1.5.0-beta02`
- Compose Animation: via BOM

### Core Animation APIs Available

#### 1. State Animation (`animate*AsState`)
```kotlin
animateFloatAsState(targetValue, animationSpec, label)
animateColorAsState(targetValue, animationSpec, label)
animateDpAsState(targetValue, animationSpec, label)
animateIntAsState(targetValue, animationSpec, label)
```

#### 2. Animatable (Coroutines)
```kotlin
val animatable = Animatable(initialValue)
animatable.animateTo(targetValue, animationSpec)
animatable.snapTo(value)
animatable.animateDecay(velocity, decayAnimationSpec)
```

#### 3. Transition
```kotlin
val transition = updateTransition(targetState, label)
val color by transition.animateColor(animationSpec) { state -> ... }
val size by transition.animateDp(animationSpec) { state -> ... }
```

#### 4. AnimatedVisibility / AnimatedContent
```kotlin
AnimatedVisibility(visible, enter, exit, modifier, label)
AnimatedContent(targetState, transitionSpec, label) { state -> ... }
```

#### 5. Crossfade
```kotlin
Crossfade(targetState, animationSpec, label) { state -> ... }
```

#### 6. Shared Element Transitions (Material 3 1.5.0+)
```kotlin
// Requires ExperimentalSharedElementTransitionApi
Modifier.sharedElement(key, SharedElementScope)
```

#### 7. Animation Specs
```kotlin
tween(durationMillis, delayMillis, easing)
spring(dampingRatio, stiffness, visibilityThreshold)
keyframes { durationMillis at value withEasing }
repeatable(iterations, animationSpec, repeatMode)
infiniteRepeatable(animationSpec, repeatMode)
snap()
```

#### 8. Easing
```kotlin
LinearEasing
FastOutSlowInEasing
FastOutLinearInEasing
LinearOutSlowInEasing
CubicBezierEasing(x1, y1, x2, y2)
// Custom: class MyEasing : Easing { override fun transform(fraction: Float): Float }
```

#### 9. Material 3 Motion Scheme (1.5.0+)
```kotlin
MaterialTheme.motionScheme
// Properties (all FiniteAnimationSpec<*>):
// - standard: standardEasing / standardDuration
// - medium: mediumEasing / mediumDuration  
// - large: largeEasing / largeDuration
// - extraLarge: extraLargeEasing / extraLargeDuration
// - fastSpatialSpec(): For quick spatial changes (button press, etc.)
// - mediumSpatialSpec(): For medium spatial changes
// - slowSpatialSpec(): For slow spatial changes
```

#### 10. Size/Content Animation
```kotlin
Modifier.animateContentSize(animationSpec, label)
Modifier.contentSizeInMinHeight()
```

#### 11. Gesture/Drag
```kotlin
Modifier.draggable(state, orientation)
Modifier.pointerInput { detectDragGestures { ... } }
Modifier.pointerInput { detectHorizontalDragGestures { ... } }
Modifier.swipeable(state, anchors, orientation, thresholds, resistance)
```

#### 12. Reduced Motion
```kotlin
// System setting: Settings.Global.ANIMATOR_DURATION_SCALE
// Compose: LocalContentAlpha, or read via PlatformConfiguration
val configuration = LocalConfiguration.current
val animatorScale = configuration.animatorDurationScale // 0f = off
```

---

## 4. Third-Party Library Evaluation

| Library | Status | Compatibility | Verdict |
|---------|--------|---------------|---------|
| **Lottie Compose** | Active | Compose 1.6+, Kotlin 2.0 | ❌ Overkill for our needs; runtime parsing overhead |
| **Rive** | Active | Compose via interop | ❌ Binary format, runtime, overkill |
| **Orbital** | Archived | N/A | ❌ Unmaintained |
| **Compose MotionLayout** | Experimental | Compose 1.6+ | ❌ Not stable, complex XML-based |
| **Native Compose Animation** | Stable | ✅ Our versions | ✅ **Primary choice** |

**Recommendation**: Use **native Compose Animation APIs only**. No external animation dependencies needed. Material 3 1.5.0 provides `motionScheme` with expressive specs.

---

## 5. License Compliance

| Source | License | Compliance |
|--------|---------|------------|
| ZL2 (GPL-3.0) | GPL-3.0 | Study only; re-implement cleanly with own code. No verbatim copying. |
| Android/Compose | Apache-2.0 | ✅ Compatible |
| Material 3 | Apache-2.0 | ✅ Compatible |

**Action**: Re-implement patterns from ZL2 in our own code with our own token names. No GPL code in our repo.

---

## 6. Comparison Table

| Aspect | ZL2 | Android/M3 | Our App (Current) | Target |
|--------|-----|------------|-------------------|--------|
| **Central Timing** | SwapTimeline (object) | M3 motionScheme | Scattered hard-coded | Central tokens |
| **Easing** | Custom (EaseOutFluent, EnterTranslateEasing) | M3 standard/emphasized | LinearEasing, tween default | Tokenized |
| **Springs** | Not used (tween only) | Spring API available | Only in Motion.kt (caret) | Spring tokens |
| **Stagger** | SwapTimeline enter/exit stagger | Manual via delayMillis | Not used | Stagger tokens |
| **Speed Control** | Settings (0.1x-3x) | Manual | None | Settings integration |
| **Reduced Motion** | Settings toggle | System animator scale | None | System + setting |
| **Navigation** | AnimatedContent + SwapClock | Navigation Compose transitions | None (instant) | Consistent directional |
| **Press Feedback** | M3 motionScheme.fastSpatialSpec() | M3 motionScheme | None (ripple only) | Token |
| **List Stagger** | AnimatedColumn/Row/LazyColumn | Manual | Not used | Primitive |
| **Drag/Swipe** | Animatable + pointerInput | draggable/swipeable | None | Primitive |
| **Shared Elements** | Not used | Experimental API | None | Future |

---

## Source URLs
- ZL2: `/tmp/opencode/zalith/zl2` (commit `0c10282`, upstream `50f08ca`)
- Android Compose Animation: https://developer.android.com/jetpack/compose/animation
- Material 3 Motion: https://m3.material.io/foundations/motion
- Material 3 Motion Scheme API: https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary#MaterialTheme.motionScheme