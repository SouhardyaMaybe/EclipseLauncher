# USER PROMPT (verbatim)

# TASK: Our own motion/animation library, built from research (ZL1, ZL2, Android, Material 3 Expressive) and applied across the app

Read root AGENTS.md first and obey it (ACCURACY RULES, pre-flight, 2-strike rule, CI-only builds, push ONLY to `nemo`, token never printed/written/committed). If I say STOP, make no more tool calls and reply with a status report. OFF LIMITS: my GitHub repos "Eclipse" and "EclipseLauncher": never open, clone, read, search or mention them. No screenshot or emulator work.

Don't assume anything about the current state of the repo. Audit it yourself: read `.agent/`, `git log -n 30`, `git status` and the code. If something here conflicts with older notes, THIS MESSAGE WINS. Don't redo finished work. Tell me if you find something that contradicts this task. Save this task to `.agent/PROMPT.md` and write `.agent/GOAL.md` (IN/OUT of scope). Don't upgrade any dependency for this task unless I approve it.

## GOAL
Build OUR OWN motion library for this app: universally usable, driven by tokens and semantic roles instead of values scattered around the code, with explicit, documented overrides for the cases that need hard-coded values. Then apply it across the app with motion only: no layout, structure or behavior changes.

## REQUIREMENTS DIGEST (from my motion spec)
- Motion has a purpose: continuity, feedback, state change, spatial relationships. Never decoration. Don't animate everything.
- One central motion system: duration tokens, easing curves, spring configs (damping/stiffness), transition definitions, press/selection feedback rules, gesture thresholds, velocity handling, and reduced-motion alternatives. Adjustable in one place; documented with when to use and when NOT to use each pattern.
- Default timing, as starting points, adjusted by distance, size and frequency: button feedback 80-150 ms; hover/focus 100-180; checkbox/toggle 120-200; icon morph 150-220; tooltip 120-180; dropdown 150-220; accordion 200-300; bottom sheet 250-350; dialog 180-280; page transition 250-400; shared-element 250-450; large layout change 300-500. Frequent interactions feel immediate. Never delay an operation to finish an animation. Animations are interruptible and reversible.
- Springs settle naturally. Overshoot and bounce only where they communicate physical behavior.
- Gestures follow the finger directly (drag, swipe, sheets snapping by position and velocity, cancel/boundary handling). Gestures are never the only way to reach essential functionality.
- Animate the real property that changes (do not swap unrelated components). Loading -> success/error should feel like one continuous change. Skeletons mirror real content. Progress reflects REAL progress, never fake.
- Component patterns: dropdown caret rotation, accordions, dialogs, sheets, tab indicators, bottom-nav indicator, search expand/collapse, list insert/remove/reorder, image crossfade, snackbars, FAB/panel morph.
- Reduced motion: respect the system setting; fall back to simple opacity or instant; state is never communicated by motion alone; no flashing or strong parallax.
- Performance: transform/alpha via graphicsLayer, avoid layout thrash and needless recomposition, no leaked animation controllers, no loops running offscreen, test with long lists and repeated taps. No heavy animation dependency for a simple fade.

## PHASE 1: RESEARCH AND DESIGN (report only; research in parallel, subagents allowed)
1. ZL2: find the official repo (confirm it, pin a commit). How does it manage animation inside the app? Where animation code lives, the abstractions (spec objects, tokens, CompositionLocals, wrappers, modifiers), navigation transitions, shared elements, gestures and springs, per-component animations, reduced-motion and any animation settings, performance habits. Cite files and lines. ZL1: same analysis, and say whether it is View-based or Compose.
2. Android and Material sources. Verify against the official docs AND the Compose/Material3 versions our repo actually resolves (not memory): Compose animation APIs (animate*AsState, Animatable, Transition, AnimatedVisibility/AnimatedContent, shared-element transitions, lazy-list item animations, size animation, anchored dragging, predictive back); the Material 3 motion scheme (standard vs expressive, spatial vs effects specs; names I remember are UNVERIFIED) and the official Material motion guidance for easing, duration and transitions.
3. Other libraries: evaluate candidates such as Lottie, Rive, Orbital, Compose MotionLayout (names UNVERIFIED: check that each exists, is maintained, and is compatible with our Kotlin/Compose versions). Recommend native APIs plus our own library unless a dependency is clearly justified.
4. Licenses: check ZL1/ZL2 and any library you consider. Study and re-implement cleanly. Verbatim or near-verbatim copying needs license compliance and attribution.
5. `.agent/analysis/MOTION_RESEARCH.md`: comparison table (approach, API, pros/cons, fit for us, source URLs).
6. `.agent/analysis/MOTION_AUDIT.md`: audit of OUR app: every current animation, transition and gesture (search animate*, AnimatedVisibility, Crossfade, tween/spring/keyframes, graphicsLayer, drag, transitions in navigation), with file:line, every hard-coded duration or easing, and the gaps against the digest above.
7. `.agent/MOTION_DESIGN.md`: the proposed library:
   - Placement: separate Gradle module or a package in the app. Weigh CI build time and cleanliness, and recommend.
   - Token model: durations, easings, springs, distances/scales, stagger delays, gesture thresholds, expressed as SEMANTIC ROLES (for example enter, exit, press, expand, emphasize), not numbers at call sites. Defaults derived from the M3 motion scheme where available. Provided through CompositionLocals, overridable at THREE levels: theme (one place), subtree, and per call.
   - Hard-coded values: allowed only through an explicit override (for example a `MotionOverride` marker with a required reason). Add a CI consistency check that lists raw tween/spring/keyframes calls outside the library and fails on any without a documented override.
   - Primitives, in priority of real usage in our app (don't build unused code): press feedback, appear/disappear, expand/collapse, content swap, list item insert/remove/reorder, state morph (idle -> running -> done/failed), progress, skeleton, navigation transitions (consistent directional logic), drag/swipe/sheet-settle helpers, and shared elements where our navigation setup supports them (check what navigation the app really uses; don't assume).
   - Reduced-motion strategy: verify how to read the system animator scale / reduce-motion setting in Compose (cite), and whether the app already has an animation-related setting to wire in.
   - Testing strategy (JVM, no screenshots): Compose test rule with a controlled clock for durations, interruption and reversal, reduced-motion fallback, override precedence, token resolution from the theme, and cleanup on dispose.
   - `.agent/MOTION_MAP.md`: per screen and component, the purposeful animations you propose (what, why, which role) and what you deliberately leave still.
GATE 1: STOP. Show me the research summary, the audit summary, MOTION_DESIGN, MOTION_MAP and your questions. Wait for my approval before writing library code.

## PHASE 2: BUILD THE LIBRARY (after approval)
- Build exactly the approved design. Mark experimental Material APIs with proper opt-ins and don't leak them to call sites where avoidable.
- Documentation in the repo (usage examples, token catalog, when to use each primitive, how to override and hard-code).
- Tests as designed, plus the CI consistency check for raw animation specs.
- Max 6 CI runs per component group. All existing gate tests stay green; no test is deleted or weakened.

## PHASE 3: APPLY ACROSS THE APP (batches, after approval)
- Follow MOTION_MAP. Replace ad-hoc animations with library calls and remove hard-coded durations (except documented overrides).
- Motion only. Layout, structure, text and behavior stay identical. Do a code-level parity check per batch (before vs after).
- Per batch: report the animations added and their purpose, CI links, the APK link and exact phone-test steps (what I should feel and try: interrupt it, tap rapidly, rotate, turn on system reduce-motion), then STOP for my test. Max 4 CI runs per batch.

Every report ends with the claims table (claim | VERIFIED / UNVERIFIED | evidence).