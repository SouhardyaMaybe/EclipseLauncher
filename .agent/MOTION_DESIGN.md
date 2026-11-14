# Motion Library Design

## 1. Placement: In-App Package (Not Separate Module)

### Decision: Package in `app/src/main/java/com/eclipselauncher/ui/motion/`

**Rationale:**
- Single-module app; adding a module increases CI complexity (extra Gradle project, dependency graph)
- Motion library is app-specific (tokens tied to our theme, components, settings)
- No reuse across apps planned
- Keeps CI fast: one `assembleDebug` task, no inter-module dependencies
- Easy to extract later if needed

### Structure
```
app/src/main/java/com/eclipselauncher/ui/motion/
├── MotionTokens.kt           # Duration, easing, spring tokens
├── MotionTheme.kt            # CompositionLocalProvider for tokens
├── MotionModifiers.kt        # Modifier extensions (pressFeedback, enterExit, etc.)
├── MotionComponents.kt       # High-level primitives (PressableCard, Expandable, etc.)
├── GestureMotion.kt          # Drag/swipe/sheet helpers
├── ReducedMotion.kt          # Reduced motion detection & fallbacks
├── Testing.kt                # Test utilities (TestMotionTheme, controlled clock)
└── Override.kt               # MotionOverride marker for CI check
```

---

## 2. Token Model

### 2.1 Duration Tokens (Semantic Roles)

```kotlin
// MotionTokens.kt
object MotionDuration {
    // Press / immediate feedback
    const val PRESS_FEEDBACK = 100      // Button press scale, ripple
    const val HOVER_FOCUS = 150         // Hover/focus transitions
    
    // Selection / toggle
    const val CHECKBOX_TOGGLE = 150     // Checkbox check/uncheck
    const val SWITCH_TOGGLE = 200       // Switch thumb slide
    const val ICON_MORPH = 200          // Icon transformation (search→close)
    
    // Disclosure / expand
    const val TOOLTIP = 150             // Tooltip show/hide
    const val DROPDOWN = 200            // Dropdown/menu expand
    const val ACCORDION = 250           // Accordion expand/collapse
    
    // Overlays
    const val BOTTOM_SHEET = 300        // Bottom sheet enter/exit
    const val DIALOG = 250              // Dialog enter/exit
    const val SNACKBAR = 200            // Snackbar enter/exit
    
    // Navigation
    const val PAGE_TRANSITION = 300     // Screen-to-screen
    const val SHARED_ELEMENT = 350      // Shared element morph
    
    // Layout
    const val LARGE_LAYOUT = 400        // Major layout restructure
    
    // Continuous
    const val AUTO_ROTATION = 10000     // Skin model auto-rotation
    
    // Stagger
    const val STAGGER_ITEM = 20         // Per-item stagger delay
    const val STAGGER_CAP = 200         // Max stagger delay
}

// Derived from Material 3 motionScheme (1.5.0+)
internal object MotionDurationM3 {
    val standard = MaterialTheme.motionScheme.standard.duration
    val medium = MaterialTheme.motionScheme.medium.duration
    val large = MaterialTheme.motionScheme.large.duration
    val fastSpatial = MaterialTheme.motionScheme.fastSpatialSpec()
    val mediumSpatial = MaterialTheme.motionScheme.mediumSpatialSpec()
}
```

### 2.2 Easing Tokens

```kotlin
object MotionEasing {
    // Standard M3 easings (via motionScheme)
    val standard = MaterialTheme.motionScheme.standard.easing
    val emphasized = MaterialTheme.motionScheme.emphasized.easing
    
    // Custom for specific cases
    val enterTranslate = EnterTranslateEasing  // From ZL2, for staggered list enter
    val easeOutFluent(power: Int = 3) = EaseOutFluent(power)  // 1-(1-f)^power
    val easeOutBack(power: Int = 3) = EaseOutBack(power)      // Slight overshoot
    
    // Semantic aliases
    val press = standard
    val expand = emphasized
    val collapse = standard
    val pageForward = standard
    val pageBack = standard
}
```

### 2.3 Spring Tokens

```kotlin
object MotionSpring {
    // Gentle press feedback (button scale)
    val press = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
    
    // Expand/collapse (accordion, dropdown, sheet)
    val expand = spring(
        dampingRatio = Spring.DampingRatioMedium,
        stiffness = Spring.StiffnessMedium
    )
    
    // Settle (drag release, swipe back)
    val settle = spring(
        dampingRatio = Spring.DampingRatioHighMedium,
        stiffness = Spring.StiffnessMedium
    )
    
    // Emphasized (shared element, hero)
    val emphasized = spring(
        dampingRatio = Spring.DampingRatioMedium,
        stiffness = Spring.StiffnessHigh
    )
}
```

### 2.4 Distance/Scale Tokens

```kotlin
object MotionDistance {
    const val PRESS_SCALE = 0.95f
    const val HOVER_SCALE = 1.02f
    const val ENTER_TRANSLATE_DP = 40.dp
    const val EXIT_TRANSLATE_SCALE = 6f / 16f  // From ZL2
    const val CHEVRON_ROTATION_DEG = 180f
}
```

### 2.5 Gesture Thresholds

```kotlin
object MotionGesture {
    const val SWIPE_TRIGGER_DP = 80.dp
    const val SWIPE_MAX_DRAG_FACTOR = 1.5f
    const val SWIPE_BACK_DURATION = 220  // ms
    const val DRAG_VELOCITY_THRESHOLD = 500f  // px/s
    const val LONG_PRESS_MS = 500
}
```

---

## 3. Theme Integration (CompositionLocal)

### 3.1 MotionTheme.kt

```kotlin
@Immutable
data class MotionTheme(
    val durations: MotionDurations = MotionDurations(),
    val easings: MotionEasings = MotionEasings(),
    val springs: MotionSprings = MotionSprings(),
    val distances: MotionDistances = MotionDistances(),
    val gestures: MotionGestures = MotionGestures(),
    val reducedMotion: ReducedMotionStrategy = ReducedMotionStrategy.System
) {
    // Factory for M3 alignment
    companion object {
        fun fromMaterial3(): MotionTheme = MotionTheme(
            durations = MotionDurations.fromM3(),
            easings = MotionEasings.fromM3(),
            springs = MotionSprings(),
            distances = MotionDistances(),
            gestures = MotionGestures(),
            reducedMotion = ReducedMotionStrategy.System
        )
    }
}

// Semantic token holders (overrideable per-field)
@Immutable
data class MotionDurations(
    val pressFeedback: Int = MotionDuration.PRESS_FEEDBACK,
    val hoverFocus: Int = MotionDuration.HOVER_FOCUS,
    val checkboxToggle: Int = MotionDuration.CHECKBOX_TOGGLE,
    val switchToggle: Int = MotionDuration.SWITCH_TOGGLE,
    val iconMorph: Int = MotionDuration.ICON_MORPH,
    val tooltip: Int = MotionDuration.TOOLTIP,
    val dropdown: Int = MotionDuration.DROPDOWN,
    val accordion: Int = MotionDuration.ACCORDION,
    val bottomSheet: Int = MotionDuration.BOTTOM_SHEET,
    val dialog: Int = MotionDuration.DIALOG,
    val snackbar: Int = MotionDuration.SNACKBAR,
    val pageTransition: Int = MotionDuration.PAGE_TRANSITION,
    val sharedElement: Int = MotionDuration.SHARED_ELEMENT,
    val largeLayout: Int = MotionDuration.LARGE_LAYOUT,
    val autoRotation: Int = MotionDuration.AUTO_ROTATION,
    val staggerItem: Int = MotionDuration.STAGGER_ITEM,
    val staggerCap: Int = MotionDuration.STAGGER_CAP,
) {
    fun tweenMillis(role: DurationRole): Int = when (role) {
        DurationRole.PressFeedback -> pressFeedback
        DurationRole.HoverFocus -> hoverFocus
        // ... etc
    }
}

@Immutable
data class MotionEasings(
    val standard: Easing = MotionEasing.standard,
    val emphasized: Easing = MotionEasing.emphasized,
    val enterTranslate: Easing = MotionEasing.enterTranslate,
    val press: Easing = MotionEasing.press,
    val expand: Easing = MotionEasing.expand,
    val collapse: Easing = MotionEasing.collapse,
    val pageForward: Easing = MotionEasing.pageForward,
    val pageBack: Easing = MotionEasing.pageBack,
)

@Immutable
data class MotionSprings(
    val press: SpringSpec = MotionSpring.press,
    val expand: SpringSpec = MotionSpring.expand,
    val settle: SpringSpec = MotionSpring.settle,
    val emphasized: SpringSpec = MotionSpring.emphasized,
)

@Immutable
data class MotionDistances(
    val pressScale: Float = MotionDistance.PRESS_SCALE,
    val hoverScale: Float = MotionDistance.HOVER_SCALE,
    val enterTranslateDp: Dp = MotionDistance.ENTER_TRANSLATE_DP,
    val exitTranslateScale: Float = MotionDistance.EXIT_TRANSLATE_SCALE,
    val chevronRotationDeg: Float = MotionDistance.CHEVRON_ROTATION_DEG,
)

@Immutable
data class MotionGestures(
    val swipeTriggerDp: Dp = MotionGesture.SWIPE_TRIGGER_DP,
    val swipeMaxDragFactor: Float = MotionGesture.SWIPE_MAX_DRAG_FACTOR,
    val swipeBackDurationMs: Int = MotionGesture.SWIPE_BACK_DURATION,
    val dragVelocityThreshold: Float = MotionGesture.DRAG_VELOCITY_THRESHOLD,
    val longPressMs: Int = MotionGesture.LONG_PRESS_MS,
)

enum class ReducedMotionStrategy {
    System,      // Follow system animator duration scale
    ForceOn,     // Always reduced (for testing)
    ForceOff     // Never reduced (for testing)
}
```

### 3.2 CompositionLocal Provider

```kotlin
// MotionTheme.kt (continued)
val LocalMotionTheme = compositionLocalOf { MotionTheme.fromMaterial3() }
val LocalReducedMotion = compositionLocalOf { false }

@Composable
fun MotionTheme(
    theme: MotionTheme = MotionTheme.fromMaterial3(),
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalMotionTheme provides theme,
        LocalReducedMotion provides reducedMotion
    ) {
        content()
    }
}

// Access in components
@Composable
fun rememberMotionTheme(): MotionTheme = LocalMotionTheme.current
@Composable
fun isReducedMotion(): Boolean = LocalReducedMotion.current
```

### 3.3 EclipseTheme Integration

```kotlin
// In Theme.kt - wrap EclipseTheme with MotionTheme
@Composable
fun EclipseTheme(
    themeMode: ThemeMode = ThemeMode.DYNAMIC,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val reducedMotion = rememberReducedMotion(context) // Read system setting
    
    MotionTheme(
        theme = MotionTheme.fromMaterial3(),
        reducedMotion = reducedMotion
    ) {
        Crossfade(
            targetState = themeMode,
            animationSpec = tween(LocalMotionTheme.current.durations.themeSwitch)
        ) { mode ->
            MaterialTheme(
                colorScheme = eclipseColorScheme(context, mode),
                typography = EclipseTypography,
                shapes = EclipseShapes,
                content = content
            )
        }
    }
}
```

---

## 4. Three-Level Override System

### 4.1 Level 1: Theme (Global)
Set once in `EclipseTheme` - affects entire app.

### 4.2 Level 2: Subtree (CompositionLocalProvider)
```kotlin
MotionTheme(
    theme = LocalMotionTheme.current.copy(
        durations = LocalMotionTheme.current.durations.copy(
            dialog = 100  // Faster dialogs in this subtree
        )
    )
) {
    DialogContent()
}
```

### 4.3 Level 3: Per-Call (Explicit Override)
```kotlin
Modifier.pressFeedback(
    override = MotionOverride(
        duration = 50,
        reason = "High-frequency action; must feel instant"
    )
)
```

### 4.4 MotionOverride Marker

```kotlin
@Immutable
@TargetAnnotationTarget(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class MotionOverride(
    val durationMs: Int? = null,
    val easing: Easing? = null,
    val spring: SpringSpec? = null,
    val reason: String  // Required documentation
)

// Usage in modifiers
fun Modifier.pressFeedback(
    enabled: Boolean = true,
    override: MotionOverride? = null
): Modifier = composed {
    val theme = LocalMotionTheme.current
    val reduced = isReducedMotion()
    
    val duration = override?.durationMs ?? theme.durations.pressFeedback
    val springSpec = override?.spring ?? theme.springs.press
    
    if (reduced && !theme.reducedMotion.allowPressFeedback) {
        this // No animation
    } else {
        // ... apply spring with duration/spec
    }
}
```

---

## 5. Primitives (Priority Order)

### 5.1 Press Feedback
```kotlin
fun Modifier.pressFeedback(
    enabled: Boolean = true,
    scale: Float = LocalMotionTheme.current.distances.pressScale,
    spring: SpringSpec = LocalMotionTheme.current.springs.press,
    override: MotionOverride? = null
): Modifier
```

### 5.2 Appear/Disappear (Fade + Optional Translate)
```kotlin
@Composable
fun AnimatedVisibility(
    visible: Boolean,
    role: EnterExitRole = EnterExitRole.Default,
    enter: EnterTransition = fadeIn() + slideInVertically(),
    exit: ExitTransition = fadeOut() + slideOutVertically(),
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) { ... }

enum class EnterExitRole {
    Default,           // Standard fade + slide
    Dropdown,          // Fade + small translate
    Dialog,            // Fade + scale
    BottomSheet,       // Slide up/down
    Tooltip,           // Fade + small translate
    Page,              // Crossfade + directional slide
}
```

### 5.3 Expand/Collapse (Height + Fade)
```kotlin
@Composable
fun AnimatedExpansion(
    expanded: Boolean,
    role: ExpandRole = ExpandRole.Default,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) { ... }

enum class ExpandRole {
    Default,       // expandVertically + fade
    Accordion,     // Accordion timing
    Dropdown,      // Dropdown timing
    BottomSheet,   // Spring settle
    Dialog,        // Dialog timing
}
```

### 5.4 Content Swap (Crossfade)
```kotlin
@Composable
fun AnimatedContentSwap<T>(
    targetState: T,
    role: SwapRole = SwapRole.Default,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) { ... }
```

### 5.5 List Item Insert/Remove/Reorder
```kotlin
@Composable
fun AnimatedLazyColumn(
    items: List<T>,
    isVisible: Boolean = true,
    stagger: Boolean = true,
    role: ListRole = ListRole.Default,
    content: @Composable (index: Int, item: T, ctx: AnimatedContext) -> Unit
) { ... }

@Stable
class AnimatedContext(
    val progress: Float,          // 0→1 enter, 1→0 exit
    val alphaProgress: Float,     // Separate alpha curve
    val distanceScale: Float,     // For exit distance scaling
    val index: Int,               // Item index for stagger
    val defaultDirection: AnimatedDirection
)

fun Modifier.animated(
    ctx: AnimatedContext,
    direction: AnimatedDirection = AnimatedDirection.Auto,
    distance: Dp = LocalMotionTheme.current.distances.enterTranslateDp,
    fade: Boolean = true
): Modifier
```

### 5.6 State Morph (Idle → Loading → Success/Error)
```kotlin
@Composable
fun StateMorph(
    state: StateType,
    role: StateRole = StateRole.Default,
    modifier: Modifier = Modifier,
    content: @Composable (StateType) -> Unit
) { ... }

enum class StateType { Idle, Loading, Success, Error }
enum class StateRole { Default, Button, Card, Page }
```

### 5.7 Progress / Skeleton
```kotlin
@Composable
fun Skeleton(
    shape: Shape = RectangleShape,
    animation: SkeletonAnimation = SkeletonAnimation.Shimmer,
    modifier: Modifier = Modifier
) { ... }

enum class SkeletonAnimation {
    None,           // Static
    Pulse,          // Alpha pulse
    Shimmer,        // Gradient sweep
    Wave            // Wave propagation
}
```

### 5.8 Navigation Transitions
```kotlin
// For Navigation 3 / AnimatedContent
fun rememberPageTransition(): AnimatedContentTransitionScope<Scene>.() -> ContentTransform = {
    val theme = LocalMotionTheme.current
    val isReduced = isReducedMotion()
    
    if (isReduced) {
        ContentTransform(fadeIn(snap()), fadeOut(snap()))
    } else {
        ContentTransform(
            fadeIn(tween(theme.durations.pageTransition, easing = theme.easings.pageForward))
                togetherWith slideInHorizontally(theme.durations.pageTransition),
            fadeOut(tween(theme.durations.pageTransition, easing = theme.easings.pageBack))
                togetherWith slideOutHorizontally(theme.durations.pageTransition)
        )
    }
}
```

### 5.9 Drag/Swipe/Sheet Helpers
```kotlin
@Stable
class DraggableState(
    val offset: Animatable<Float, AnimationVector1D>,
    val isDragging: Boolean,
    val dragFraction: Float  // 0..1
)

fun rememberDraggableState(
    triggerDistance: Dp = LocalMotionTheme.current.gestures.swipeTriggerDp
): DraggableState

fun Modifier.draggableSwipe(
    state: DraggableState,
    onTriggered: () -> Unit,
    enabled: Boolean = true
): Modifier = composed {
    val theme = LocalMotionTheme.current
    this
        .offset { IntOffset(state.offset.value.roundToInt(), 0) }
        .pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragStart = { state.isDragging = true; state.offset.snapTo(0f) },
                onHorizontalDrag = { _, delta -> state.offset.snapTo((state.offset.value + delta).coerceIn(-max, max)) },
                onDragEnd = { 
                    if (abs(state.offset.value) >= triggerPx) onTriggered()
                    state.offset.animateTo(0f, tween(theme.gestures.swipeBackDurationMs))
                    state.isDragging = false
                },
                onDragCancel = { state.offset.animateTo(0f, tween(theme.gestures.swipeBackDurationMs)); state.isDragging = false }
            )
        }
}
```

---

## 6. Reduced Motion Strategy

### 6.1 System Setting Detection
```kotlin
@Composable
fun rememberReducedMotion(context: Context = LocalContext.current): Boolean {
    val configuration = LocalConfiguration.current
    val animatorScale = configuration.animatorDurationScale
    return animatorScale == 0f || LocalMotionTheme.current.reducedMotion == ReducedMotionStrategy.ForceOn
}
```

### 6.2 Fallback Behavior
| Animation Type | Reduced Motion Fallback |
|----------------|------------------------|
| Press feedback | None (instant) |
| Fade in/out | Instant (snap) |
| Slide/translate | None (snap) |
| Expand/collapse | Instant height change |
| Scale | None (snap) |
| Rotation | None (snap) |
| Spring | Instant (snapTo) |
| Stagger | All simultaneous |
| Continuous (auto-rotation) | Paused/stopped |

### 6.3 Implementation
```kotlin
// In each primitive
if (isReducedMotion()) {
    // Return instant/snap version
    AnimatedVisibility(visible, enter = snap(), exit = snap()) { ... }
    // OR
    Modifier.graphicsLayer { scaleX = if (pressed) 0.95f else 1f } // No animation
} else {
    // Full animation
}
```

---

## 7. Testing Strategy

### 7.1 TestMotionTheme
```kotlin
object TestMotionTheme {
    val instant = MotionTheme(
        durations = MotionDurations(
            pressFeedback = 0, hoverFocus = 0, checkboxToggle = 0,
            switchToggle = 0, iconMorph = 0, tooltip = 0, dropdown = 0,
            accordion = 0, bottomSheet = 0, dialog = 0, snackbar = 0,
            pageTransition = 0, sharedElement = 0, largeLayout = 0,
            autoRotation = 0, staggerItem = 0, staggerCap = 0
        ),
        easings = MotionEasings(standard = LinearEasing, ...),
        springs = MotionSprings(press = snap(), ...),
        reducedMotion = ReducedMotionStrategy.ForceOn
    )
}
```

### 7.2 Test Utilities
```kotlin
@Composable
fun TestMotionThemeProvider(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMotionTheme provides TestMotionTheme.instant) {
        content()
    }
}

// Controlled clock for virtual time
class TestAnimationClock : MonotonicFrameClock {
    var currentTime = 0L
    override fun withFrameNanos(callback: (Long) -> Unit) {
        callback(currentTime * 1_000_000)
    }
}

// Test helpers
fun advanceTime(millis: Int) { ... }
fun assertAnimationCompleted<T>(state: State<T>) { ... }
fun assertAnimationReversed(state: State<Float>) { ... }
```

### 7.3 Test Cases
1. **Duration resolution** - Theme level, subtree level, per-call override precedence
2. **Reduced motion** - System setting respected; ForceOn/ForceOff work
3. **Interruption** - Press during animation reverses cleanly
4. **Reversal** - Expand→collapse mid-animation reverses direction
5. **Stagger** - List items animate with correct delays
6. **Cleanup** - Animatable coroutines cancelled on dispose
6. **Override CI check** - Raw `tween()`/`spring()` calls flagged

---

## 8. CI Consistency Check

### 8.1 Detekt/Lint Rule
```kotlin
// Custom detekt rule or ktlint rule
// Fails on raw animation specs outside motion package
// Allowed: 
//   - com.eclipselauncher.ui.motion.* (library internals)
//   - @MotionOverride annotated calls
//   - Test files
```

### 8.2 Gradle Task
```kotlin
tasks.register("checkMotionSpecs") {
    doLast {
        val violations = fileTree("app/src/main/java")
            .include("**/*.kt")
            .filter { !it.path.contains("/motion/") && !it.path.contains("/test/") }
            .map { file ->
                file.readText().findAll(/(tween|spring|keyframes|infiniteRepeatable)\s*\(/) 
                    .filter { !it.contains("@MotionOverride") }
                    .map { "$file: raw animation spec without override" }
            }.flatten()
        
        if (violations.isNotEmpty()) {
            throw GradleException("Raw animation specs found:\n${violations.joinToString("\n")}")
        }
    }
}
```

---

## 9. Migration Path

### Phase 1: Library + Tokens (No Behavior Change)
1. Create `ui/motion/` package with tokens, theme, primitives
2. Add `MotionTheme` wrapper in `EclipseTheme`
3. Add `rememberReducedMotion()` 
4. Verify build passes

### Phase 2: Replace Ad-Hoc Animations (Per Screen)
See `MOTION_MAP.md` for screen-by-screen plan.

### Phase 3: CI Gate + Tests
1. Add CI consistency check
2. Add JVM tests for token resolution, override precedence, reduced motion
3. Ensure all existing tests pass