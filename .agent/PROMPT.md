# USER PROMPT (verbatim, from session start)

Build a complete EclipseLauncher Android app (Kotlin, Jetpack Compose, Material 3, compileSdk 34) with full CI/CD in GitHub Actions; all builds run only in CI.

- Kotlin 2.0, Compose 1.6.1, Material 3 1.2.1, Material 3 Adaptive 1.0.0-alpha01, Adaptive Navigation Suite 1.0.0-alpha01.
- Coroutines/Flow 1.8.1; Coil 2.6.0 intended but currently disabled.
- AGP 8.9, Gradle 8.11.1, compileSdk 34, minSdk 24, targetSdk 34.
- "research about the dependencies online, then research about the implementation in github workflows online. implement with the knowledge, no assumptions."

Additional standing rule: builds are verified only via GitHub Actions.

## 2026-11-13 — New instruction (verbatim)
"umm... not what i expected. its broken. do the followings.            1. delete the failed 57 runs. the green run should only remain.             2. check out /root/nemo/Eclipse/ files and folders. it has the ui i wanted you to build. it has almost all of it but old tech stack and components. so you know what to do."

## 2026-11-13 — Research request (verbatim)
"umm... quickly check out the exact tech stack of zalith launcher 2 from online sources"

## 2026-11-14 — Instruction (verbatim)
"its good but some modifications needed. 1. increase the heights of the installed and browse tab buttons by a lil bit.     2. make the pagination like 1 page at a time. next button tap= 1 page next loads the lists and shows progress bar/loading icon m3 expressive in the whole list screen instead of just at the paginator.       3. add responsiveness like graying out when no page next or previous.           4. go for the other parts now."
