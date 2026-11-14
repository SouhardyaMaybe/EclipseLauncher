# USER PROMPT (verbatim)

# FIX: Microsoft sign-in ends with "Web page not available ... net::ERR_UNKNOWN_URL_SCHEME"

Read root AGENTS.md first and obey it (ACCURACY RULES, pre-flight, 2-strike rule, CI-only builds, push ONLY to `EclipseLauncher`, token never printed/written/committed). If I say STOP, make no more tool calls and reply with a status report. No screenshot or emulator work. Do this BEFORE Steps 2 and 3 of my previous message; those wait. Max 4 CI runs for this whole fix.

SYMPTOM (it existed before the package rename, from the start): in Microsoft sign-in I enter my email, then my password, tap Continue, and the sign-in WebView shows "Web page not available ... could not be loaded because net::ERR_UNKNOWN_URL_SCHEME". The address is `ms-xal-00000000402b5328://auth/?code=<one-time code>`. So Microsoft finished the sign-in and redirected to a custom scheme, and our app never intercepted it, so the WebView tried to load a URL it cannot open.

Your rename report said Microsoft auth is "Safe" because it "uses https://login.live.com/oauth20_desktop.srf". That contradicts the redirect I actually received. Quote file:line for the redirect_uri our authorize URL really sends, and for every URL our WebViewClient intercepts.

TRACE the real path with file:line: sign-in screen -> authorize URL (client_id, redirect_uri, scope, response_type) -> WebViewClient (shouldOverrideUrlLoading / onPageStarted / onPageFinished / shouldInterceptRequest / onReceivedError) -> code extraction -> token exchange -> next steps. Hypotheses to TEST, not conclusions:
a. The authorize URL uses the ms-xal redirect while the interception code only matches the desktop.srf URL (or the other way round).
b. Interception sits in a callback that doesn't fire for custom schemes (shouldInterceptRequest doesn't see them; onPageFinished is too late).
c. Interception exists but matches a different scheme string or prefix.
d. The client_id and redirect_uri pair is inconsistent (a client id only accepts its registered redirects).

STUDY how ZL1, ZL2 and PojavLauncher do the Microsoft login: which client id, which redirect, how they intercept it (cite files and URLs). Mirror the working approach and re-implement it cleanly (license rules apply).

FIX: intercept the redirect before the WebView tries to load it (return true from shouldOverrideUrlLoading for that scheme), extract `code`, `error` and `error_description`, close the WebView and continue the flow. Handle: user cancelled, error redirect, WebView closed or back pressed, double delivery (a code is single-use). Then verify the whole chain end to end: auth code -> Microsoft token -> Xbox Live -> XSTS -> Minecraft token -> profile and ownership check. Every step needs a clear user-facing error (account doesn't own Minecraft, no Xbox profile, child-account restrictions, network failure, rate limit) and must not crash.

SECURITY: never log or print the code, tokens or the full redirect URL, and never put them in `.agent/` files, test output, the debug crash overlay or error messages (redact them). Report how tokens are stored today; don't change storage in this task unless I approve.

TESTS (JVM): use the REAL WebViewClient class from the sign-in screen. Feed it the exact redirect form with a fake code and assert it returns true, the callback receives the code, and the WebView does not load it. Also test an error redirect, a cancel, an unrelated URL (must load normally) and double delivery. Test the token chain with fake HTTP responses for success and for each failure. Show the red run before the fix. If sign-in still fails on my phone after your fix, my phone is right and the test is wrong.

Report with the CI link, the APK link, exact phone-test steps and the claims table, then STOP for my test.