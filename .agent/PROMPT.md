# USER PROMPT (verbatim)

# FIX: Microsoft account shows no skin or cape, and skin customization must not be offered for Microsoft accounts

Read root AGENTS.md first and obey it (ACCURACY RULES, pre-flight, 2-strike rule, CI-only builds, push ONLY to `EclipseLauncher`, token never printed/written/committed). If I say STOP, make no more tool calls and reply with a status report. No screenshot or emulator work. Do this BEFORE Steps 2 and 3 of my earlier message; those wait. Max 4 CI runs. If anything here is unclear, ask me one short question instead of guessing.

SYMPTOM: Microsoft sign-in now completes and the account is added, but its real skin and cape don't show anywhere in the app. Separately, skin/cape CUSTOMIZATION (choosing a local skin or cape) is meant for offline/local accounts only and must not be offered for Microsoft accounts.

## A. DISPLAY: load the real skin and cape of the Microsoft account
1. Trace the real path with file:line: Minecraft token -> profile request -> response parsing -> account model/store -> EVERY place that displays an account's skin, avatar or cape (Accounts screen, Home account card or switcher, any preview/rig/model view) -> image loading. Find all those places yourself.
2. Verify the REAL profile API response format from official or reliable documentation (cite URLs, don't answer from memory): the endpoint, the auth header, and the fields for skins (id, state, url, variant) and capes (id, state, url, alias). Handle: which skin/cape is ACTIVE, no cape, classic vs slim model, legacy 64x32 skins, malformed or oversized PNGs.
3. Hypotheses to TEST, not conclusions:
   a. the profile is requested but the skins/capes fields are dropped or never stored
   b. the profile request fails and the error is swallowed
   c. texture URLs come as http:// and Android blocks cleartext traffic by default, so the image load fails silently. Check the real URL scheme. As far as I know the texture host also serves https, so normalize to https instead of enabling cleartext traffic globally.
   d. the display code only handles offline accounts or a UUID-based avatar service
   e. the skin is loaded once at sign-in and never refreshed or cached
4. Behavior: fetch the profile after sign-in and on token refresh / app start (skins change outside the launcher). Cache the skin and cape PNGs by texture hash (content-addressed) in the app cache so they work offline. Show a proper placeholder while loading and a clear fallback on failure. Update the displays when the profile changes.
5. Never log or print tokens, codes or full redirect URLs, and keep them out of `.agent/` files and error messages.

## B. CUSTOMIZATION: NOT for Microsoft accounts
Find every entry point to skin/cape customization (dialogs, menu items, buttons). For Microsoft accounts, hide or disable them, following the app's existing pattern for unavailable actions (with a short reason if the app shows one). They stay available for offline/local accounts. Don't remove the feature, and don't add skin uploading to the Microsoft API.

## TESTS (JVM)
- A realistic profile fixture (a skin, a slim variant, an active cape, plus a profile without any cape) -> parsed model.
- Fake HTTP for success, 401 and network error.
- A test through the REAL UI wiring: for a Microsoft account the skin appears (non-placeholder state) and the customization entry is absent; for an offline account the customization entry is present.
- Show the red run before the fix. If my phone still shows nothing after your fix, my phone is right and the test is wrong.
Report with the CI link, the APK link, exact phone-test steps and the claims table, then STOP for my test.