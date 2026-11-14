# GOAL

Build OUR OWN motion/animation library for EclipseLauncher, driven by tokens and semantic roles, then apply it across the app with motion-only changes.

## IN SCOPE
- Research ZL1/ZL2 animation patterns, Android/Material3 Compose animation APIs, and evaluate animation libraries
- Audit all current animations in the app (animate*AsState, AnimatedVisibility, Crossfade, tween/spring/keyframes, graphicsLayer, drag, navigation transitions)
- Design a central motion system with duration tokens, easing curves, spring configs, transition definitions, press/selection feedback rules, gesture thresholds, velocity handling, reduced-motion alternatives
- Token model: durations, easings, springs, distances/scales, stagger delays, gesture thresholds as SEMANTIC ROLES (enter, exit, press, expand, emphasize, etc.)
- Three-level override: theme, subtree, per-call with explicit MotionOverride marker
- CI consistency check for raw animation specs outside the library
- Primitives for real usage: press feedback, appear/disappear, expand/collapse, content swap, list item insert/remove/reorder, state morph, progress, skeleton, navigation transitions, drag/swipe/sheet-settle helpers
- Reduced-motion strategy: system animator scale / reduce-motion setting in Compose
- Testing strategy: JVM with controlled clock for durations, interruption, reversal, reduced-motion fallback, override precedence, token resolution, cleanup
- MOTION_MAP: per screen/component, purposeful animations and what to leave still
- Build library as package in app (not separate module to minimize CI overhead)
- Apply library across app in batches, motion-only, with parity checks

## OUT OF SCOPE
- Visual/UX redesign or new features
- Layout, structure, text or behavior changes
- Heavy animation dependencies (Lottie, Rive, etc.) unless clearly justified
- Separate Gradle module for the library (keep in-app package)
- Local Gradle builds (CI-only verification)
- Emulator/screenshot testing
- Data migration

## Reference
- App codebase: `/root/Launcher/EclipseLauncher/app/src/main/java`
- ZL2 clone: `/tmp/opencode/zalith/zl2` (commit `0c10282`, upstream `50f08ca`)
- Current app stack: Compose BOM 2026.09.00, Material3 1.5.0-beta02, Navigation3 1.2.0