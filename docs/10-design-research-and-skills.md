# 10 — Design research and skills

**Status:** research and staffing draft for sign-off, 2026-10-24.
**Scope:** what we know about launcher users and interaction on Android, which claims are evidence-backed, what still needs a study, and which design skills the project must have to ship the product described in chapters 08 and 09.
**Companion chapters:** [01 — Landscape](01-landscape.md), [08 — Product design](08-product-design.md), [09 — Design system](09-design-system.md).
**Labels:** **Verified** (primary source read), **Observed** (public artifact or prior study), **Inference**, **Open**.

---

## 1. Why this chapter exists

A launcher is a **control surface for a complex system**. The user does not see the JVM, the loader, the asset store or the renderer. They see a button, a progress bar and, when something breaks, a sentence. Design research here is not about picking a shade of amber. It is about answering four questions:

1. **Can a player get from install to a world without understanding the stack?**
2. **When the stack fails, can they understand why and recover?**
3. **Can they play with the input they actually have — thumbs, a controller, a keyboard, a mouse?**
4. **Can they trust the app with an account, a world and several gigabytes of storage?**

Every design decision in chapters 08 and 09 should trace to one of these questions, a platform requirement, or an explicit product principle. Where it does not, it is taste — and taste is allowed, but it must be labelled.

---

## 2. Research questions

| # | Question | Method | Status |
|---|---|---|---|
| R1 | What do existing launchers make easy, and where do users get stuck? | Competitive teardown, release notes, issue trackers | **Observed**, partial |
| R2 | What does Android require for adaptive layout, input, back, insets and accessibility? | Platform documentation | **Verified** |
| R3 | How do people hold and touch a phone during a game? | Prior HCI research + device testing | **Observed**, needs own study |
| R4 | Which parts of the install are legible to a non-expert? | Usability test with prototype | **Open** |
| R5 | What makes an error message actionable rather than alarming? | Literature + copy testing | **Observed**, needs test |
| R6 | How much configuration do returning Java players expect? | Interviews | **Open** |
| R7 | Can a controller-only user complete onboarding? | Device test | **Open** |
| R8 | What do users need to trust a third-party launcher with Microsoft auth? | Interviews + trust probe | **Open** |
| R9 | How do 200% font scale and TalkBack change the layout? | Accessibility audit | **Open** |
| R10 | Which metrics predict “I will keep using this”? | Diary study + instrumented beta | **Open** |

The rest of this chapter records what is settled, what is inferred, and what must be tested before v1.

---

## 3. Method and evidence standard

### 3.1 Sources, in order of strength

1. **Primary platform documentation** — Android, Microsoft identity, Mojang metadata, Material 3, WCAG. Used for hard constraints.
2. **Primary artifacts** — version JSON, client jar, LWJGL native jars, public release notes. Used for technical facts.
3. **Peer-reviewed or established HCI research** — Fitts's law, Hick's law, thumb reach, progressive disclosure, error recovery. Used for interaction principles, not as proof about our users.
4. **Competitive observation** — public screenshots, documentation, issue trackers, release notes. Used to find pain, not to copy solutions.
5. **Our own studies** — interviews, usability tests, diary studies, instrumented beta. The only source that can validate our product.

### 3.2 What we will not do

- We will not claim a “clean-room” process we did not run.
- We will not copy another launcher's screens, strings, icons or code.
- We will not present a competitor's bug as a market fact without a link.
- We will not treat a single usability session as validation.
- We will not use dark patterns and call them growth.

### 3.3 Evidence labels in design docs

- **Verified** — a platform or standards document says so.
- **Observed** — seen in a public artifact, release note, issue tracker or prior study.
- **Inference** — a reasoned design decision that needs a test.
- **Open** — unknown; assigned to a study or a spike.

---

## 4. Competitive UX audit

The technical teardown is in [01](01-landscape.md). This section looks at the **experience**, not the architecture.

| Product | Strength to learn from | Pain to avoid | Evidence |
|---|---|---|---|
| **Amethyst** | Proven launch path; broad device reach; familiar to Pojav users | Legacy preference screens; settings density; weak first-run guidance; stable runtime lags Java 25 | Public docs, release notes, issue tracker |
| **FCL** | Feature depth; HMCL-derived install logic; modpack support | Hybrid XML/Compose UI; power-user defaults; error surfaces assume technical knowledge | Public screenshots, docs, issue tracker |
| **Zalith 2** | Compose M3 + Navigation 3; modern visual language; renderer detection | Dense settings; Pojav-lineage assumptions; GPL obligations | Public release notes, screenshots |
| **Pojav** | The original Android JVM launch recipe; enormous mindshare | Unmaintained; GLFW-era; crashes are opaque | Archived repository |
| **Prism Launcher (desktop)** | Instance model; clear Java/loader settings; export/import | Desktop density; not touch-first | Public docs |
| **Modrinth App (desktop)** | Content-first discovery; clean pack install; license visibility | Desktop window model; no Android runtime concerns | Public app |
| **Official Minecraft Launcher** | Account flow; version isolation; crash reporting | No Android; limited mod management; heavy installer | Public app |

### 4.1 The recurring failure pattern

Across issue trackers, the same three complaints recur:

1. **“It crashes on launch.”** No stage, no reason, no next step.
2. **“The renderer doesn't work.”** A toggle is offered without a capability check; the user tries all of them.
3. **“I lost my world/settings.”** An update or repair replaced the instance instead of migrating it.

These are not feature gaps. They are **legibility and trust** gaps. That is why the product principles put evidence, recovery and user-content safety ahead of “more settings”.

### 4.2 What not to compete on

- Number of renderers.
- Number of Java versions.
- Number of accounts.
- Number of toggles.
- Benchmark scores.

None of these is the user's job. They are implementation details leaking into the UI.

---

## 5. Platform research

### 5.1 Layout and windowing

**Verified:**

- Android 16 (API 36) enforces edge-to-edge and ignores orientation/resize restrictions on large screens unless the app is a game (`appCategory="game"`).
- Predictive back is the default; `onBackPressed()` is not called for apps targeting 36.
- `WindowSizeClass` breakpoints are the supported way to adapt; list-detail is the canonical pattern for medium/expanded.
- Foldables add posture and hinge information; the app must not assume a single contiguous rectangle.

**Design consequence:** five destinations become a bottom bar, rail or drawer; instance list becomes list-detail; the game activity is a separate, game-category, full-screen surface.

### 5.2 Touch targets and spacing

**Verified:** Android and Material guidance recommend a minimum 48 × 48 dp touch target, even when the visual icon is smaller. WCAG 2.2's Target Size (Minimum) is 24 × 24 CSS px, with a 44 × 44 enhanced target; Eclipse uses 48 dp as the floor and 56 dp for primary actions.

**Inference:** a 48 dp target is still small for a thumb on a 6.7-inch phone held one-handed; the primary Play action is 56 dp and lives in the lower third.

### 5.3 System bars, cutouts and IME

**Verified:** edge-to-edge is mandatory; insets must be consumed; the IME inset animates and can cover a chat field.

**Design consequence:** the touch overlay uses `safeDrawing`; the chat field is lifted above the IME; the game surface is edge-to-edge but no control sits under a cutout.

### 5.4 Performance and frame pacing

**Verified:** Android 15+ defaults games to 60 Hz; `Surface.setFrameRate()` requests a higher rate; the system may clamp for thermal/power reasons. ADPF exposes CPU/GPU headroom; thermal headroom should be polled no more than every 10 seconds.

**Design consequence:** performance UI reports the *actual* frame rate and the reason for a clamp, not a wish. The launcher does not promise 120 fps on a device that thermally throttles after five minutes.

### 5.5 Background work and process death

**Verified:** user-initiated data transfer jobs are the supported path for long downloads; `dataSync` foreground services are capped; process death is normal.

**Design consequence:** downloads survive process death and resume; the UI never assumes an in-memory queue is durable.

---

## 6. Interaction research

### 6.1 Touch: what the literature says

- **Fitts's law:** target acquisition time depends on distance and target width. Primary actions belong near the thumb, and small targets need more space around them.
- **Thumb reach:** Hoober's observational studies of phone grip found that most users hold a phone in one hand and operate it with the thumb, with a comfortable arc in the lower half of the screen. The exact percentages vary by device size and context; the design implication is stable: put frequent actions low, destructive actions out of the arc.
- **Hick's law:** decision time grows with the number of choices. Five destinations and one primary action are not minimalism; they are response-time engineering.
- **Occlusion:** the hand and the device cover part of the screen. The look area must not sit under the palm; controls need a margin from the bezel.

**Inference:** a virtual D-pad is more precise for Minecraft's grid-like movement than a floating stick, but a stick is more comfortable for long sessions. v1 ships both, defaulting to the D-pad, and measures which one users keep.

### 6.2 Touch in a 3D game

Minecraft expects:

- continuous look deltas (mouse);
- discrete movement keys (WASD);
- momentary actions (attack/use) with hold semantics;
- a hotbar with 9 slots;
- inventory, chat, pause, sneak, sprint, jump.

A touch overlay is therefore a **translation layer**, not a gamepad. It must:

- separate the look area from buttons by pointer ID, not by timing;
- support multi-touch (move + look + jump simultaneously);
- avoid “ghost taps” when a finger slides from a button into the look area;
- give haptic confirmation on discrete actions, not on every look delta;
- let the user resize and move controls, because hand size and device size vary.

**Observed:** desktop Minecraft's default keybindings assume a keyboard; a launcher that simply maps five buttons leaves inventory, chat and sprint unreachable. The overlay must cover the full default keybind set or provide a keyboard fallback.

### 6.3 Controller

- Focus must be spatial and stable; the D-pad moves focus, A activates, B goes back, Menu opens the overlay.
- A controller-only user must be able to complete sign-in (the device code is the hard part), install, create an instance and launch.
- Deadzones are per-stick and visible; drift is common on worn sticks.
- Rumble is feedback, not decoration; disable it in Battery Saver.

**Open:** whether the Android TV-style focus model is required for a tablet with a controller, or whether touch focus is sufficient. Test with controller-only users.

### 6.4 Keyboard and mouse

- Physical keyboards are common on Chromebooks and tablets with kickstands.
- `KEYCODE_BACK` should be Escape while the game is focused, not “finish the activity”.
- Pointer capture gives relative deltas; without it, the cursor hits the screen edge and the camera stops.
- IME text must go to the game, not to a hidden Compose field that steals focus.

**Verified:** Android pointer capture delivers all mouse events to the focused view and uses relative coordinates; request it from a click or window-focus event.

---

## 7. Cognitive and behavioural research

### 7.1 First-run: the “time to first world” problem

The first run combines four unfamiliar systems: Microsoft sign-in, a runtime download, a game install and a renderer decision. The user's working memory is the bottleneck, not the network.

Design responses:

- **One decision per step.** Version, loader and name are separate; advanced settings are collapsed.
- **Show the size before the download.** A 650 MB surprise is a trust failure.
- **Persist progress.** A process death must not restart the flow.
- **Explain the wait.** “Downloading the Java runtime (68 MB)” is better than “Preparing…”.
- **Do not ask about renderers.** The probe decides; the user sees the verdict.

**Inference:** a four-minute first run is acceptable if every stage is legible; a two-minute first run with a blank spinner feels longer.

### 7.2 Errors: recognition over recall

Nielsen's usability heuristics include “help users recognize, diagnose, and recover from errors” and “visibility of system status”. The practical rules:

- Say what happened in the user's terms.
- Say why, if we know.
- Say what to do next, with one primary action.
- Preserve the technical detail for the people who need it, behind a copy/expand affordance.
- Never blame the user.
- Never show a raw stack trace as the only message.

**Observed:** launcher issue trackers are full of “exit code 1” reports because the launcher surfaced a number without a stage. The crash sheet in [08](08-product-design.md) is a direct response.

### 7.3 Recovery: safe mode as a contract

Users fear losing worlds more than they fear a crash. Safe mode must be **additive and reversible**:

- move mods aside, do not delete them;
- change settings with a visible before/after;
- record the safe-mode flag and offer “return to normal”;
- never silently reset the instance.

**Inference:** if safe mode is framed as “start without mods”, users will try it; if it is framed as “reset”, they will uninstall.

### 7.4 Destructive actions

- Confirm only irreversible actions; use undo for reversible ones.
- Name the object in the confirmation (“Delete ‘Aether SMP’”), not “this item”.
- State what is kept (“Worlds can be exported first”).
- For a multi-gigabyte deletion, require a typed confirmation or a hold-to-confirm, not a single tap.
- Do not use a red filled button for “Cancel”.

**Verified:** confirmation dialogs are appropriate for destructive or irreversible actions; overusing them causes alert fatigue (Nielsen Norman Group guidance).

### 7.5 Trust and transparency

A third-party launcher asks for an account and several gigabytes. Trust is built by:

- showing the source of every download (Mojang, Modrinth, our components);
- showing the license of content before install;
- explaining why a permission is needed, at the moment it is needed;
- never hiding a download or a sign-in;
- making sign-out and data deletion easy to find;
- publishing the privacy policy and the source code.

**Inference:** the account screen should show “We never see your password” and “Your token stays on this device” as plain statements, not legal boilerplate.

### 7.6 Progress and perceived waiting

Nielsen's response-time guidance: 0.1 s feels instantaneous, 1 s keeps flow, 10 s is the limit for attention. Beyond 10 s, a determinate indicator and an explanation are required.

Design responses:

- staged progress with named phases;
- determinate bytes and file counts;
- ETA from a rolling average, not a instantaneous spike;
- pause/cancel always available;
- no fake progress that jumps to 90% and waits.

---

## 8. Visual research

### 8.1 Material 3 Expressive

**Verified:** Material 3 Expressive adds shape, motion-physics and emphasized-type tools, with a component set intended to make hierarchy and emotion clearer. The guidance is available in the Material 3 docs and the “Building with M3 Expressive” article.

What Eclipse takes:

- **Shape as hierarchy:** one hero radius (28 dp), one card radius (12–16 dp), full-round actions.
- **Motion physics:** springs for press/release and success, not for every transition.
- **Emphasized type:** a clear display/headline/title/body ladder, used sparingly.
- **Expressive color:** a warm brand palette, not a rainbow.

What Eclipse rejects:

- oversized type that pushes content off a 360 dp screen;
- shape morphing on every tap;
- “playful” copy that undermines a crash message;
- motion that ignores reduced-motion settings.

### 8.2 Differentiation

The competitive audit shows two visual poles: legacy XML settings (Amethyst) and custom Compose skins (Zalith). Neither owns **calm, tactile, instrument-like**. Eclipse's warm paper/ink palette, arc mark and monospace “truth” blocks are a distinct position that does not depend on copying Minecraft art.

**Inference:** a warm neutral palette is also practical: it hides fingerprint smudges less than pure black, and it keeps long log screens readable without the harshness of pure white.

### 8.3 Brand risk

“Eclipse” is a registered trademark of the Eclipse Foundation. The visual identity must avoid purple, the Eclipse IDE logo, and any implication of affiliation. The wordmark and arc are original. A trademark review is an open item before 1.0.

---

## 9. Accessibility research

### 9.1 Standards and platform

- **WCAG 2.2 AA**: contrast, target size, focus visible, dragging movements, consistent help, redundant entry.
- **Android accessibility**: TalkBack, Switch Access, font scale (non-linear from Android 14), display size, high-contrast text, reduced motion, colour correction.
- **Material 3**: state layers, focus indicators, semantic roles.

### 9.2 What tends to break in a launcher

| Risk | Mitigation |
|---|---|
| Log text at 10 sp, unscalable | 12 sp floor, scalable, selectable, copy action |
| Status conveyed by a red/green dot | Icon + label + color |
| Custom touch controls with no semantics | `customActions` for move/jump/attack; labels |
| Bottom bar labels clipped at 200% | Labels wrap or switch to rail; test at 200% |
| Progress announced every percent | Announce at 10% intervals and on stage change |
| Focus lost after a dialog | Restore focus to the invoking element |
| Drag-only control layout | Numeric position/size fields as an alternative |
| Timed device code with no warning | Countdown + “new code” button |
| Motion sickness from large transitions | Reduced-motion crossfade |

### 9.3 Research gaps

- No participant with a screen reader has tested the touch overlay.
- No participant with motor impairment has tested the control editor.
- No study has measured the cognitive load of the crash sheet.
- No RTL usability test has been run.

These are v1 blockers for the accessibility claim, not nice-to-haves.

---

## 10. Localization research

**Verified:** Android supports per-app languages, RTL mirroring, plurals and non-linear font scaling. Minecraft has its own language setting, independent of the launcher.

Design implications:

- Strings must be short enough for German and Finnish; layouts must not depend on English word length.
- The device-code screen must not put the code inside a sentence that translators can reorder incorrectly; use a separate field and a label.
- Error details (HTTP status, signal name, library) are not translated; the explanation is.
- The touch overlay is **not** mirrored in RTL, because the game world is not; the launcher chrome is.
- Date/number formatting follows the locale; sizes use binary units with localized decimal separators.
- Translation memory and a glossary (“instance”, “loader”, “runtime”) are prerequisites for community localization.

**Open:** whether the first release ships only English or English + 5 high-traffic locales. The recommendation is English first, with a string freeze and a public translation template at v1.

---

## 11. Studies to run before v1

### 11.1 Study plan

| Study | Participants | Method | Questions | When |
|---|---|---|---|---|
| **S1 — First run** | 8 (4 new to Java, 4 returning) | Moderated, device, think-aloud | Where do they hesitate? Can they sign in? Do they understand the download? | After the clickable prototype |
| **S2 — Crash recovery** | 8 | Scenario, no assistance | Do they choose safe mode? Do they understand the cause? Do they export the report? | After the crash sheet prototype |
| **S3 — Touch controls** | 6 | 30-minute play session, device | Can they survive, open inventory, chat? Which control do they remap first? | After the overlay alpha |
| **S4 — Controller-only** | 5 | Device + gamepad, no touch | Can they onboard and launch? Where does focus get lost? | After focus navigation |
| **S5 — Accessibility** | 5 (TalkBack/switch/motor/low vision) | Task-based audit | Are labels, focus, targets and log text usable? | Before beta |
| **S6 — Trust** | 10 | Short interview + sign-in observation | What worries them about a third-party launcher? What would make them stop? | Before public beta |
| **S7 — Diary study** | 12 | 2 weeks, instrumented beta | What brings them back? What makes them abandon? | Beta |

### 11.2 Recruiting

- Mix of device tiers: 3–4 GB low-end, 6–8 GB midrange, 12 GB flagship, one tablet, one foldable, one Chromebook if available.
- At least 2 participants who have used a third-party launcher before; at least 2 who have not.
- At least 1 controller-primary player.
- At least 2 participants who use a screen reader or large text in daily life.
- Compensate fairly; do not rely on friends of the team.

### 11.3 Session protocol

1. Consent and recording permission.
2. Background questions (5 min): devices, Minecraft history, launcher history.
3. Tasks (30–45 min): first run, crash recovery, touch play, settings.
4. Post-task ratings: ease (1–7), confidence (1–7), trust (1–7).
5. Short interview: what was confusing, what was missing, what would you tell a friend.
6. Debrief and compensation.

### 11.4 Analysis

- Tag observations by screen, principle and severity.
- Separate **usability** (can they do it) from **desirability** (do they want to).
- Report counts, not just quotes; one participant is an anecdote.
- Every finding gets a decision: fix, accept, or test again.
- Store recordings and notes with consent and an expiry date.

### 11.5 Success thresholds for the studies

- S1: ≥ 7/8 complete sign-in + install + world without help.
- S2: ≥ 6/8 choose a recovery action that preserves the world.
- S3: ≥ 5/6 can move, look, attack, place and open inventory within 5 minutes.
- S4: ≥ 4/5 complete onboarding with the controller alone.
- S5: zero critical blockers (unlabelled control, unreachable action, clipped text at 200%).
- S6: ≥ 7/10 say they would trust the app with a Microsoft sign-in after the explanation.

If a threshold is missed, the design iterates before the next study; the threshold does not move to fit the result.

---

## 12. Skills and staffing

### 12.1 Competency matrix

| Skill | Why it matters | Depth needed | Can be combined with |
|---|---|---|---|
| **Interaction design** | IA, flows, states, touch/controller | Expert | Product design |
| **Visual design** | Brand, type, color, layout, motion | Strong | Design systems |
| **Design systems** | Tokens, components, Compose parity | Strong | Visual design |
| **UX research** | Interviews, usability, diary, analysis | Strong | Product design |
| **Content design** | Error copy, terminology, localization-ready strings | Strong | UX writing |
| **Accessibility** | WCAG, TalkBack, switch, font scaling | Working expert | Interaction design |
| **Android platform** | Adaptive layout, insets, back, permissions, process death | Strong | Android engineering |
| **Compose** | Theme, components, previews, screenshot tests | Working | Android engineering |
| **Game UX** | Touch controls, latency, occlusion, controller | Working | Interaction design |
| **Native/graphics literacy** | Understand what the renderer UI is actually saying | Working | Graphics engineering |
| **Research ops** | Recruiting, consent, scheduling, repository | Working | UX research |
| **Community/localization** | Translator kit, glossary, support | Working | Content design |

### 12.2 Minimum team

For the 8–10 month scope in [00](00-executive-brief.md):

| Role | FTE | Notes |
|---|---|---|
| Product designer | 1.0 | IA, interaction, visual, design system |
| UX researcher | 0.5 | Can be the same person with research ops support |
| Content designer | 0.3 | Error copy, terminology, translation kit |
| Android engineer (UI) | 1.0 | Compose, adaptive, accessibility |
| Android engineer (platform) | 1.0 | Install, auth, storage, process |
| Native/graphics engineer | 1.0 | JRE, SDL, Sandpaper, Vulkan |
| QA/device lab | 0.5 | Matrix, regression, accessibility |
| Community/support | 0.3 | Beta, issues, localization |

A solo developer can ship a reduced v1 (vanilla + Fabric, Vulkan-first) in 9–12 months if they accept a smaller device matrix and defer the design-system polish. The research and accessibility work cannot be skipped without changing the product promise.

### 12.3 Hiring order

1. Native/graphics engineer (critical path).
2. Product designer with Android/Compose fluency.
3. Android platform engineer.
4. QA/device lab.
5. UX researcher/content designer (fractional is fine).

### 12.4 What the team must be able to read

- A `log4j` stack trace and a `hs_err_pid` file.
- A `dumpsys meminfo` and `ApplicationExitInfo`.
- A Vulkan capability dump and a GLES extension string.
- A WCAG contrast calculation.
- A Compose layout inspector trace.

Designers who cannot read a crash report will write crash copy that is wrong. Engineers who cannot read a usability finding will ship a settings page that only they can use.

---

## 13. Design process

### 13.1 Double diamond, adapted

```
DISCOVER                DEFINE                 DEVELOP                DELIVER
interviews              JTBD                   sketches               specs
competitive audit       journey map            wireframes             tokens
platform research       service blueprint      prototypes             component library
device telemetry        opportunity map        usability tests        QA + accessibility
```

### 13.2 Artefacts

| Artefact | Owner | When | Where |
|---|---|---|---|
| Research repository (tagged insights) | Researcher | Continuous | Internal wiki |
| JTBD + personas | Product designer | Before IA | `docs/08` |
| Journey map (first run, launch, crash, update) | Product designer | Before wireframes | Figma |
| Service blueprint (auth, install, launch) | Product + platform | Before build | Figma |
| IA + navigation model | Product designer | Before screens | `docs/08` |
| Wireframes (all five states) | Product designer | Per feature | Figma |
| Design system + tokens | Designer + Android lead | Before UI build | `:designsystem` + Figma |
| Prototype | Product designer | Before S1/S2 | Figma/Compose |
| Usability report | Researcher | After each study | Research repo |
| Accessibility audit | Designer + QA | Before beta | Checklist |
| Localization kit | Content designer | Before string freeze | Repo |
| Release notes | Product | Every release | GitHub |

### 13.3 Rituals

- **Weekly design critique:** 45 minutes, one screen, five states, accessibility note.
- **Biweekly research readout:** 30 minutes, three findings, three decisions.
- **Monthly design-system review:** new components, deprecations, token changes.
- **Release accessibility gate:** TalkBack pass, 200% font pass, contrast pass, reduced-motion pass.

### 13.4 Definition of done for a screen

- All five states designed: loading, empty, content, partial, error.
- Light/dark, compact/medium/expanded, 200% font, RTL.
- Touch and D-pad focus order.
- Copy reviewed by content design.
- Analytics events defined (local, opt-in) or explicitly none.
- Accessibility labels and roles specified.
- Screenshot in the design review.

---

## 14. Research ethics and privacy

- Informed consent before recording; participants can stop at any time.
- Do not collect Microsoft credentials, tokens or world files during a study.
- Use test accounts provisioned for research; never a participant's real account without explicit consent.
- Recordings stored encrypted, access-limited, deleted after the agreed retention period.
- Quotes anonymised; device identifiers removed.
- Compensation is not contingent on positive feedback.
- Findings are reported with counts and contrary evidence, not just the best quote.

---

## 15. Sources

### Platform and standards

- Android adaptive layouts: <https://developer.android.com/develop/ui/compose/layouts/adaptive>
- Android accessibility: <https://developer.android.com/guide/topics/ui/accessibility>
- Android pointer capture: <https://developer.android.com/develop/ui/views/touch-and-input/gestures/movement#pointer-capture>
- Android 16 behaviour changes: <https://developer.android.com/about/versions/16/behavior-changes-16>
- Android 17 behaviour changes: <https://developer.android.com/about/versions/17/behavior-changes-17>
- Material 3: <https://m3.material.io/>
- M3 Expressive: <https://m3.material.io/blog/building-with-m3-expressive>
- WCAG 2.2: <https://www.w3.org/TR/WCAG22/>
- Microsoft device code flow: <https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-device-code>
- Minecraft authentication: <https://minecraft.wiki/w/Microsoft_authentication>

### HCI and usability

- Nielsen, J. *Usability Engineering* — response-time limits (0.1 s / 1 s / 10 s).
- Nielsen, J. “10 Usability Heuristics for User Interface Design” — <https://www.nngroup.com/articles/ten-usability-heuristics/>
- Nielsen Norman Group, error-message guidelines — <https://www.nngroup.com/articles/error-message-guidelines/>
- Hoober, S. “How Do Users Really Hold Mobile Devices?” — UXmatters, 2013.
- Fitts, P. M. “The information capacity of the human motor system in controlling the amplitude of movement,” 1954.
- Hick, W. E. “On the rate of gain of information,” 1952.
- Sweller, J. *Cognitive Load Theory* — worked examples and split attention.
- Material 3 Expressive research summary (Google I/O 2025).

### Competitive artefacts

- Amethyst, FCL, Zalith 2, Pojav, Prism Launcher, Modrinth App public repositories and release notes, read 2026-10-24. Used for behaviour and pain points; no code or assets copied.

---

## 16. Open questions

- Which two device tiers should anchor the first usability study, given the lab budget?
- Can we recruit participants who have never used a third-party launcher without priming them with the research questions?
- Is a controller-only onboarding test possible on a tablet, or does it require an Android TV device?
- Which accessibility partner can provide participants for S5?
- Do we need a separate study for modpack installs, or can S1 cover it with a scenario?
- How do we measure trust without leading participants toward “yes”?
- What is the minimum localization set for v1, and who maintains the glossary?
- Which findings from the first study would change the IA, not just the copy?
