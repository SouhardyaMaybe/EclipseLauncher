# 06 — Authentication and compliance

**Status:** architecture draft for sign-off, 2026-10-24.
**Scope:** how a player signs in, how the launcher proves game ownership, how tokens are stored and refreshed, how failures are explained, and what the launcher must never do. It covers Microsoft Entra registration, the Xbox Live/XSTS chain, Minecraft Services, the Mojang AppID allowlist, and the legal boundary around game files and branding.
**Evidence:** Microsoft identity platform device-code documentation (updated 2026-06-15), the Minecraft Wiki authentication contract, Mojang's AppID notice and review form, and a live inspection of the `26.3` version JSON and client arguments.
**Labels:** **Verified** (primary source inspected), **Observed** (artifact or device measurement), **Inference** (reasoned, needs a test), **Open** (unknown).

---

## 1. The contract in one page

| Question | Answer |
|---|---|
| Which accounts work? | Microsoft accounts for a real Minecraft session. A **Local account** is a device-local display profile for the launcher UI; it does not authenticate, prove ownership or launch online. Ely.by and other third-party providers are removed. |
| Which OAuth flow? | **Device authorization grant** (RFC 8628) against the `consumers` tenant, public client, no secret. |
| Which scope? | `XboxLive.signin offline_access`. `openid profile` is not needed for launch. |
| Where does the client ID come from? | The project's own Entra public-client registration, supplied as `ECLIPSE_MS_CLIENT_ID` at build time. The default is empty, and no third-party launcher client ID is bundled. |
| Where does the chain end? | A Minecraft Services access token, a profile UUID/name, and an entitlement check. |
| What is the hard gate? | Mojang's **AppID allowlist**. An unapproved client gets HTTP 403 `Invalid app registration` at `login_with_xbox`. |
| What is stored? | The Microsoft refresh token, encrypted with an Android Keystore key; the profile and expiry metadata in plain JSON. |
| What is never stored? | The Microsoft password, the Xbox/XSTS tokens, the Minecraft access token after the session ends. |
| What happens offline? | The launcher can open, manage instances and show logs. Launching requires a valid Minecraft token; there is no offline-auth bypass. |
| What about demo mode? | A separate, clearly labelled mode using the client's own `--demo` flag. It is not a substitute for ownership. |
| What about piracy? | Not supported, not documented, not smuggled in as “offline mode”. |

---

## 2. Account facts that shape the design

### 2.1 One identity, two games

A Microsoft account can own Java Edition, Bedrock Edition, both, or neither. The entitlement endpoint returns separate items:

- `game_minecraft` / `product_minecraft` for Java Edition;
- `game_minecraft_bedrock` / `product_minecraft_bedrock` for Bedrock;
- `product_game_pass_pc` / `product_game_pass_ultimate` for Game Pass access.

Eclipse Launcher must check **Java Edition** entitlement specifically. A Microsoft account that only owns Bedrock must get “This account does not own Minecraft: Java Edition”, not a generic login failure.

**Verified:** the entitlement item names and the 404 profile behaviour are documented in the Minecraft authentication contract ([Minecraft Wiki](https://minecraft.wiki/w/Microsoft_authentication#Checking_game_ownership)).

### 2.2 The profile is not the account

- The Microsoft account has an object ID and a refresh token.
- Xbox Live has a user hash (`uhs`) and an XUID.
- Minecraft has a **profile UUID** (32 hex characters, no dashes in the API response) and a **profile name**.
- The `username` field returned by `login_with_xbox` is **not** the Minecraft UUID; it is an internal identifier. Do not use it as the profile ID.

**Verified:** `login_with_xbox` returns `username` described as “not the uuid”; `GET /minecraft/profile` returns `id` and `name`.

### 2.3 The account may exist without a profile

A Game Pass user who has never opened the official launcher may authenticate and own the game but have no Minecraft profile yet. The profile endpoint returns `404 NOT_FOUND`. The launcher must explain: “Open Minecraft once with the official launcher (or create a profile at minecraft.net) and try again.” It must not treat this as “you don't own the game”.

**Verified:** the Game Pass/profile caveat is documented in the same contract.

### 2.4 Child and family accounts

XSTS returns `2148916238` for a child account that is not in a Microsoft Family. This is common with a custom Entra app, and the `Redirect` URL points at Xbox's family page. The launcher shows the code, the meaning, and a button that opens the redirect in a Custom Tab. It does not retry in a loop.

**Verified:** XErr meanings and the `Redirect` field ([Minecraft Wiki](https://minecraft.wiki/w/Microsoft_authentication#Obtain_XSTS_token_for_Minecraft)).

---

## 3. Why the device-code flow

| Flow | Redirect surface | Secret | Touch UX | Verdict |
|---|---|---|---|---|
| Authorization code + PKCE | Custom scheme / loopback | None | Leaves the app, returns via intent | Good, but more moving parts |
| **Device code** | None | None | Code + “Open browser”, poll | **Chosen** |
| Embedded WebView | None | None | Feels native | **Rejected** — Microsoft explicitly discourages embedded browsers for sign-in; cookie/session isolation and phishing risk |
| Resource-owner password | None | None | Username/password in our UI | **Rejected** — deprecated, unsafe, often blocked |

Device code is the best fit for a controller-first, TV/tablet-capable launcher: the user can complete sign-in on a phone or PC while the game device shows a short code. It also avoids a custom redirect scheme that another app could register.

**Verified:** the device-code grant, its 15-minute default expiry, its polling interval and its error set are documented by Microsoft ([device code flow](https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-device-code)).

### 3.1 The one non-obvious constraint

The `XboxLive.signin` scope only works against the **`consumers`** tenant. Using `/common`, `/organizations`, or a tenant ID fails. This means the Entra app must allow personal Microsoft accounts, and the token endpoint must be `https://login.microsoftonline.com/consumers/oauth2/v2.0/...`.

**Verified:** the Minecraft contract states this explicitly; it matches the behaviour of the official launcher.

---

## 4. Entra application registration

The launcher needs one public client registration, owned by the project, not by an individual.

| Setting | Value | Why |
|---|---|---|
| Supported account types | **Personal Microsoft accounts only** | Java Edition accounts are consumer MSA; work/school accounts are not the target |
| Platform | **Mobile and desktop applications** | Public client, no secret |
| Redirect URI | none required for device code; add `http://localhost` for a future PKCE flow | Device code does not redirect |
| `allowPublicClient` | **true** (“Allow public client flows”) | Device code is a public-client flow |
| Client secret | **none** | A secret in an APK is not a secret |
| API permissions | delegated `XboxLive.signin`, `offline_access` | The chain and refresh token |
| Token configuration | no optional claims needed | We do not read the Microsoft ID token |
| Owner | project group, at least two humans | Bus factor and review continuity |

The client ID is public and ships in `BuildConfig`. That is expected. The security boundary is the user consent screen and the refresh token, not the client ID.

**Inference:** Microsoft may show the app name and publisher domain on the consent screen. The publisher domain must be verified before public release; an unverified app shows “unverified publisher”, which looks like phishing and will hurt conversion.

### 4.1 Branding constraints on the app registration

- The app name must not imply official Mojang/Microsoft affiliation. “Eclipse Launcher” is acceptable; “Minecraft Launcher Pro” is not.
- The privacy policy URL and terms of use URL must resolve and must describe token handling.
- The logo must be original; do not use Mojang artwork or the Minecraft grass block.
- The support contact must be a monitored address.

**Open:** Microsoft's exact publisher-verification requirements for a consumer-only public client in 2026; confirm in the Entra portal during Phase 0.

---

## 5. The chain, step by step

Every request below is HTTPS with a pinned trust store. No request carries a client secret. Tokens are redacted in logs.

### 5.1 Device authorization

```http
POST https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode
Content-Type: application/x-www-form-urlencoded

client_id=<ECLIPSE_CLIENT_ID>
&scope=XboxLive.signin%20offline_access
```

Response:

```json
{
  "user_code": "ABCD1234",
  "device_code": "<opaque>",
  "verification_uri": "https://microsoft.com/devicelogin",
  "expires_in": 900,
  "interval": 5,
  "message": "To sign in, use a web browser to open ..."
}
```

The app shows `user_code` in a large, selectable, monospace field, a **Copy** button, and an **Open browser** button. The code is not a secret in the threat model, but it is single-use and short-lived.

### 5.2 Polling

```http
POST https://login.microsoftonline.com/consumers/oauth2/v2.0/token
Content-Type: application/x-www-form-urlencoded

grant_type=urn:ietf:params:oauth:grant-type:device_code
&client_id=<ECLIPSE_CLIENT_ID>
&device_code=<opaque>
```

Polling rules:

- wait `interval` seconds between requests; on `slow_down`, add 5 seconds and keep the new interval for the rest of the flow;
- stop on `authorization_declined`, `expired_token`, or `bad_verification_code`;
- cancel the coroutine when the user leaves the screen, but keep the `device_code` until expiry so a return can resume;
- never poll from a background service; the flow is user-initiated and short.

Success returns `access_token` (about 1 hour) and `refresh_token` (because of `offline_access`). The Microsoft access token is used **once**, to get an Xbox Live token, and is then dropped.

### 5.3 Xbox Live user authentication

```http
POST https://user.auth.xboxlive.com/user/authenticate
Content-Type: application/json
Accept: application/json

{
  "Properties": {
    "AuthMethod": "RPS",
    "SiteName": "user.auth.xboxlive.com",
    "RpsTicket": "d=<microsoft_access_token>"
  },
  "RelyingParty": "http://auth.xboxlive.com",
  "TokenType": "JWT"
}
```

Response:

```json
{
  "Token": "<xbl_token>",
  "NotAfter": "2026-...",
  "DisplayClaims": { "xui": [ { "uhs": "<user_hash>" } ] }
}
```

Notes:

- The `d=` prefix is required for Microsoft identity platform tokens. Sending the raw token is a common cause of `401`.
- `Content-Type` and `Accept` must both be `application/json`; some HTTP clients omit `Accept` and get an unhelpful error.
- The user hash is stable for the account and is not a secret, but it is PII-adjacent; do not put it in analytics.

**Verified:** request shape and the `d=` prefix ([Minecraft Wiki](https://minecraft.wiki/w/Microsoft_authentication#Authenticate_with_Xbox_Live)).

### 5.4 XSTS authorization

```http
POST https://xsts.auth.xboxlive.com/xsts/authorize
Content-Type: application/json
Accept: application/json

{
  "Properties": {
    "SandboxId": "RETAIL",
    "UserTokens": [ "<xbl_token>" ]
  },
  "RelyingParty": "rp://api.minecraftservices.com/",
  "TokenType": "JWT"
}
```

Response: `Token` (XSTS) and `DisplayClaims.xui[0].uhs`. The `RelyingParty` must be exactly `rp://api.minecraftservices.com/` for Java Edition. A Bedrock/Realms relying party will produce a token that Minecraft Services rejects.

### 5.5 Minecraft login

```http
POST https://api.minecraftservices.com/authentication/login_with_xbox
Content-Type: application/json
Accept: application/json

{ "identityToken": "XBL3.0 x=<uhs>;<xsts_token>" }
```

Response:

```json
{
  "username": "<internal>",
  "roles": [],
  "access_token": "<minecraft_token>",
  "token_type": "Bearer",
  "expires_in": 86400
}
```

The Minecraft token is a bearer token valid for about **24 hours**. It is not a JWT the launcher should parse for identity; use the profile endpoint.

**Verified:** request and response shape ([Minecraft Wiki](https://minecraft.wiki/w/Microsoft_authentication#Authenticate_with_Minecraft)).

### 5.6 Entitlement

```http
GET https://api.minecraftservices.com/entitlements/mcstore
Authorization: Bearer <minecraft_token>
```

- If `items` contains `game_minecraft`, the account owns Java Edition.
- If `items` is empty, the account does not own it.
- Do not trust the JWT signature inside `items` without Mojang's public key. For a first-party launcher, the HTTPS response from `api.minecraftservices.com` is sufficient; verifying the signature is defense in depth, not a substitute for TLS.

### 5.7 Profile

```http
GET https://api.minecraftservices.com/minecraft/profile
Authorization: Bearer <minecraft_token>
```

Response: `id` (profile UUID), `name`, `skins`, `capes`. Cache the UUID and name; refresh them on every new Minecraft token, because the user can rename.

### 5.8 Launch arguments

The 26.3 game arguments include:

```
--username <profile name>
--uuid <profile uuid, no dashes>
--accessToken <minecraft token>
--clientId <launcher client id>
--xuid <xuid>
--version 26.3
--gameDir <instance>/game
--assetsDir <assets>
--assetIndex 34
--versionType release
```

**Open:** the exact `--xuid` source in 26.3. The XSTS `DisplayClaims.xui[0]` may include `xid`; the XSTS JWT contains an `xuid` claim. The launcher must extract it from the token claims, not invent it. **Open:** whether `--clientId` must be the Entra client ID, the official launcher's ID, or may be omitted for a third-party launch. Test against 26.3 with a real account before shipping; a wrong value may affect telemetry, Realms, or social features but should not block single-player.

**Security:** the access token appears in the process argument vector. On Android, `/proc/<pid>/cmdline` is readable only by the same UID (and root), so another app cannot read it. Crash reporters and logs must still scrub `--accessToken` and `Authorization` headers.

---

## 6. Failure matrix

| Stage | Signal | User message | Action |
|---|---|---|---|
| Device code | `authorization_pending` | “Waiting for you to finish signing in…” | Keep polling at `interval` |
| Device code | `slow_down` | — | Add 5 s to the interval |
| Device code | `authorization_declined` | “Sign-in was cancelled.” | Return to account screen |
| Device code | `expired_token` | “The code expired.” | Offer a new code |
| Device code | `bad_verification_code` | Internal error | Regenerate; log without the code |
| XBL | HTTP 401 | “Microsoft accepted the sign-in but Xbox Live rejected it.” | Re-run from the Microsoft token; if persistent, sign out and retry |
| XSTS | `XErr 2148916227` | “This Xbox account is banned.” | No retry; link to Xbox support |
| XSTS | `XErr 2148916233` | “This Microsoft account has no Xbox profile.” | Button: create one at xbox.com |
| XSTS | `XErr 2148916235` | “Xbox Live is not available in this country/region.” | No retry |
| XSTS | `XErr 2148916236/6237` | “This account needs adult verification.” | Open the Xbox verification page |
| XSTS | `XErr 2148916238` | “This is a child account. A parent must add it to a Microsoft Family.” | Open the `Redirect` URL |
| XSTS | other 401 | “Xbox Live could not authorize this account.” | Show `XErr`, log request ID |
| Minecraft login | HTTP 403 `Invalid app registration` | “Mojang has not approved this launcher's app ID yet.” | This is a build/release bug, not a user error; link to status page |
| Minecraft login | HTTP 401 | “Minecraft rejected the session.” | Re-run XSTS → login once |
| Entitlement | empty `items` | “This account does not own Minecraft: Java Edition.” | Offer “Switch account”; link to minecraft.net |
| Profile | HTTP 404 | “This account has no Minecraft profile yet.” | Explain the Game Pass/first-login case |
| Any | HTTP 429 | “Microsoft is rate-limiting sign-in.” | Exponential backoff with jitter; never hammer |
| Any | TLS/DNS failure | “Can't reach Microsoft.” | Distinguish offline from server outage; retry button |

**Rule:** every failure names the stage and the next action. “Login failed” is not an error message; it is an abdication.

### 6.1 The 403 that is not the user's fault

`403 Invalid app registration` means the Entra client ID is not on Mojang's allowlist. It is a **release gate**, not a runtime condition. The launcher should still handle it gracefully because users may run a development build, but the release checklist must prove the production client ID is approved.

**Verified:** new Entra apps must apply for Minecraft API permission; unapproved apps get 403 ([Mojang notice](https://help.minecraft.net/hc/en-us/articles/16254801392141), [review form](https://aka.ms/mce-reviewappid)).

---

## 7. Token lifecycle

### 7.1 Lifetimes

| Token | Lifetime | Storage |
|---|---|---|
| Microsoft access token | ~1 hour | Memory only |
| Microsoft refresh token | 90 days sliding, rotated on use | Keystore-encrypted at rest |
| Xbox Live token | ~14 days (server-defined) | Memory only |
| XSTS token | ~12 hours (server-defined) | Memory only |
| Minecraft access token | ~24 hours (`expires_in` 86400) | Memory; persisted only if “stay signed in” needs a warm launch, then encrypted |
| Profile UUID/name | until renamed | Plain JSON |

Do not hard-code these; read `expires_in` and `NotAfter` and refresh with a 5-minute safety margin.

### 7.2 Refresh state machine

```
IDLE
 ├─ add account ──► DEVICE_CODE ──► MICROSOFT_TOKEN ──► XBL ──► XSTS ──► MC_LOGIN ──► READY
READY
 ├─ launch (token valid) ──► READY
 ├─ launch (token < 5 min) ──► REFRESH_MC
 │      ├─ ok ──► READY
 │      └─ 401 ──► XBL ──► XSTS ──► MC_LOGIN ──► READY
 ├─ refresh token invalid ──► SIGNED_OUT (re-auth prompt)
 └─ sign out ──► SIGNED_OUT (delete refresh token and profile cache)
```

Rules:

- **Single-flight:** one refresh per account at a time. Two coroutines refreshing the same rotating refresh token can invalidate each other.
- **Atomic write:** write the new refresh token to a temp file, fsync, rename; a crash must not lose the only valid token.
- **Rotation:** Microsoft may return a new refresh token on every refresh. Always replace the stored one.
- **Revocation:** on `invalid_grant` / `interaction_required`, mark the account as needing re-authentication. Do not delete the instance or the profile name; the user should recognise the account.
- **No silent account switching:** if the active account fails, ask; do not silently use another stored account.

### 7.3 Offline launches

Minecraft's client requires a valid session token for online play. There is no supported offline-authentication mode in a third-party launcher, and Eclipse does not implement one. The launcher can still:

- open and manage instances;
- browse installed content;
- read logs and crash reports;
- prepare a launch and show “Sign in to play”.

**Inference:** a very short network outage during an otherwise valid 24-hour token should not block a launch; the client can start and reconnect. If the token is expired and the network is down, the launcher explains both facts.

---

## 8. Storage and cryptography

### 8.1 What is stored where

```
files/accounts/
  <account-id>/
    account.json          # profile name, uuid, xuid, client id, avatar hint
    token.bin             # AES-GCM ciphertext of the refresh token
    token.meta.json       # key alias, nonce, version, created/updated
files/accounts/index.json # ordered account list, active account id
```

- `account-id` is a random UUID generated by the launcher, not the Microsoft object ID and not the Minecraft UUID. It is the stable key if the profile is renamed.
- `account.json` contains no tokens.
- `token.bin` is encrypted with an AES-256-GCM key in the **Android Keystore**. The key is non-exportable, generated with `KeyGenParameterSpec` and `setRandomizedEncryptionRequired(true)`.
- The AAD binds the ciphertext to the account ID and a schema version, so a token file copied to another account fails authentication.
- The keystore key is **not** user-authentication-bound by default. Requiring biometrics on every launch would break the “one tap to play” promise; the threat model is offline device access, not a locked-screen attacker.

### 8.2 Backup and screenshots

- `android:allowBackup="false"` and explicit `dataExtractionRules` exclude `files/accounts/` and `files/instances/`.
- `FLAG_SECURE` is **not** set on the account screen, because users may want to screenshot the device code. The code is short-lived and single-use.
- The device code is not written to logs. The refresh token, access token, XSTS token and `Authorization` header are never logged. A redaction filter runs on every log line and crash report.

### 8.3 Threat model

| Threat | Mitigation |
|---|---|
| Another app reads the token | Android sandbox; tokens in private storage; no world-readable files |
| Device theft, offline | Keystore encryption; optional app lock later |
| Malicious mod reads launcher files | Mods run in `:game` with the same UID; they can read app-private files. **Open:** run untrusted mods with a separate UID or accept the risk and document it |
| Token leaked in a crash report | Scrubber removes `accessToken`, `Authorization`, `refresh_token`, `XSTS`, `identityToken`; user reviews before upload |
| Phishing via a fake consent screen | Always use the system browser/Custom Tabs to `microsoft.com`; never render a Microsoft login form in-app |
| Clipboard snooping | The device code is copied only on explicit tap; Android 13+ shows a clipboard preview; no auto-copy |
| Replay of the device code | Single-use, 15-minute expiry, bound to the client ID |
| Refresh-token rotation race | Single-flight + atomic replacement |

**Open:** whether Android's Keystore StrongBox is available and performant on the minimum device set. Use the TEE-backed key by default and StrongBox only when `setIsStrongBoxBacked(true)` succeeds.

---

## 9. UX flows

### 9.1 First run

The signed-off v1 surface does not force a Microsoft sign-in:

1. **Main page** — a local account named **Player** is active, and no instance exists yet.
2. **Create Instance** — the player chooses Vanilla or Fabric and gets a local instance. Quilt, Forge, NeoForge and OptiFine are visible placeholders.
3. **Accounts** — the local account can be renamed. There is no extra warning note on it. Microsoft sign-in is a separate, clearly labelled path.
4. **Microsoft device code (when configured)** — large code, Copy, Open browser. The screen explains that the browser can be on another device.
5. **Progress** — “Waiting…”, then “Checking ownership…”, then “Loading profile…”.
6. **Success** — avatar/name, “Java Edition owned”, and the first instance suggestion.
7. **Failure** — the exact stage, the exact reason, and one primary action.

When `ECLIPSE_MS_CLIENT_ID` is empty, the Microsoft action is disabled and the screen says sign-in is not configured in this build. It does not fall back to another launcher's client ID, and it does not pretend the local account is an online session.

The flow should take under 60 seconds on a phone with a password manager. It must survive rotation, process death and a trip to the browser.

### 9.2 Account switching

- The account chip in the top bar opens a bottom sheet with all accounts, the active one checked.
- “Add account” restarts the device-code flow; it does not replace the current account until success.
- Switching accounts updates the profile and the launch arguments; it does not modify instances.
- Removing an account deletes its token and metadata after a confirmation. Instances and worlds remain.

### 9.3 Re-authentication

When the refresh token is invalid:

- the account stays in the list with a “Sign in again” badge;
- the launch button becomes “Sign in to play”;
- the re-auth flow reuses the same account ID, so settings and instances stay attached;
- if the user signs in with a different Microsoft account, it becomes a **new** account, not a silent replacement.

### 9.4 Demo mode

Minecraft's client supports a demo mode. The launcher exposes it as **“Try the demo”**, not as an account option:

- no Microsoft sign-in;
- a local placeholder profile;
- the game is launched with `--demo`;
- the UI says “Demo world, 100 minutes, no saves” (verify the exact 26.3 limits before shipping);
- no online servers, Realms or marketplace.

**Open:** whether 26.3 accepts `--demo` with an empty `--accessToken`/`--uuid`, or requires a synthetic token. Test before documenting the flag. If it requires a token, demo mode is not viable without an account and should be removed rather than faked.

### 9.5 Sign-out and data deletion

- **Sign out** removes tokens but keeps the account label and instances.
- **Remove account** removes tokens and account metadata.
- **Delete all data** is a separate, typed confirmation; it deletes instances, assets, runtimes and accounts.

---

## 10. AppID allowlist: the release gate

### 10.1 What Mojang requires

New Entra applications that call the Minecraft API must be reviewed and allowlisted. The public form asks for the application (client) ID and information about the app. Until approval, `login_with_xbox` returns 403.

**Verified:** the requirement and the 403 behaviour are documented by Mojang and reproduced across launcher projects ([Mojang notice](https://help.minecraft.net/hc/en-us/articles/16254801392141), [review form](https://aka.ms/mce-reviewappid), [2026 report](https://learn.microsoft.com/en-my/answers/questions/5984225/minecraft-services-http-403-invalid-app-registrati)).

### 10.2 Submission checklist

- [ ] Entra app is a **public client** with `allowPublicClient=true`.
- [ ] Account type is **personal Microsoft accounts only**.
- [ ] The app name and icon are original and do not imply Mojang affiliation.
- [ ] A public privacy policy describes token storage and deletion.
- [ ] A public terms page exists.
- [ ] A support contact is monitored.
- [ ] The app has a real download page and a versioned release.
- [ ] The client ID in the release build matches the one submitted (no debug/release mismatch).
- [ ] The review form is submitted **before** the first public beta.
- [ ] Approval is recorded with a date and a screenshot in the release checklist.

### 10.3 What to do while waiting

- Ship a **developer build** with a clearly labelled “auth not approved” state.
- Test the full chain with a private test client ID that is approved for development only, if Mojang grants one; otherwise test up to `login_with_xbox` and mock the rest.
- Build the account UI, token storage and error handling against a local mock server.
- Do not advertise “sign in with Microsoft” on the public site until the production ID is approved.

### 10.4 If approval is denied

The project cannot ship public authentication. Options:

1. Ask Mojang for the specific reason and remediate.
2. Re-submit with a narrower scope or a different app identity.
3. Ship as a **development/enthusiast build** that requires users to supply their own approved client ID. This is a last resort and must be documented honestly, not hidden.

There is no legitimate way to bypass the allowlist. Do not bundle the official launcher's client ID, and do not impersonate another launcher.

---

## 11. Compliance and legal boundary

### 11.1 Minecraft EULA and usage guidelines

- Do not distribute Minecraft game files, assets, libraries or the JRE as if they were ours. The launcher downloads them from Mojang's official URLs and records hashes.
- Do not sell Minecraft, accounts, capes or other entitlements.
- Do not bypass authentication, ownership checks or regional restrictions.
- Do not imply that Eclipse Launcher is official or endorsed.
- Use the required disclaimer: **“NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.”**

**Verified:** the disclaimer wording is the one Mojang's brand guidance asks third-party products to use; the EULA prohibits distributing the game and commercial exploitation of the game's assets.

### 11.2 Brand and trademark

- “Minecraft” may be used descriptively (“a launcher for Minecraft: Java Edition”) but not as the product name or logo.
- Do not use the grass block, the creeper face, the Mojang logo, or the official launcher icon.
- “Eclipse” is also a registered trademark of the Eclipse Foundation. The project must include a non-affiliation statement and must not use Eclipse Foundation marks. **Open:** a formal trademark search and, if the project grows, a name review. This is a real risk, not a hypothetical.
- The app icon, wordmark and illustration style must be original. Document the authorship and license of every asset.

### 11.3 Privacy

Data inventory:

| Data | Purpose | Storage | Shared? |
|---|---|---|---|
| Microsoft refresh token | Stay signed in | Keystore-encrypted, on device | No |
| Profile UUID/name | Launch and display | On device | No |
| Instance list, settings | Product function | On device | No |
| Crash logs | Diagnostics | On device; opt-in upload | Only with consent |
| Device model, SOC, RAM | Renderer defaults | On device; included in opt-in report | Only with consent |
| Download history | Resume/repair | On device | No |

- No analytics SDK by default.
- No advertising ID.
- No account email harvesting. The launcher does not need the email; do not request it.
- Crash upload is opt-in, shows the exact payload, and has a “copy report” path for users who prefer GitHub Issues.
- A privacy policy is required for the Entra app and for the GitHub release page.

### 11.4 Rate limits and good citizenship

- Device-code polling respects `interval` and `slow_down`.
- Token refresh is single-flight and jittered.
- Profile/entitlement calls are cached for the session.
- No scraping of Mojang APIs, no bulk account checks, no “alt manager”.
- User-Agent identifies the launcher and version, with a contact URL.

### 11.5 What we will not build

- Offline authentication or a “cracked” mode.
- Account generators, alt lists, or token sharing.
- A bundled Microsoft client secret.
- A WebView that renders Microsoft's login page.
- A “remember my password” feature.
- Telemetry that identifies a player without consent.

---

## 12. Testing strategy

### 12.1 Contract tests against a mock server

A local mock implements:

- `/devicecode`, `/token` with `authorization_pending`, `slow_down`, `expired_token`, `authorization_declined`;
- XBL 401 and success;
- XSTS 401 with each `XErr`;
- `login_with_xbox` 200, 401, 403;
- entitlements empty/full;
- profile 200/404;
- 429 with `Retry-After`;
- malformed JSON, truncated responses, TLS failure.

The launcher's HTTP layer is injected with a base URL in debug builds only; release builds hard-code the official hosts and pin the trust store.

### 12.2 Account matrix

| Case | Expected |
|---|---|
| Java-owned adult | Full success |
| Bedrock-only | “Does not own Java Edition” |
| Game Pass, no profile | Profile guidance |
| Child, not in family | XErr 6238 guidance |
| Banned Xbox account | XErr 6227, no retry |
| Renamed profile | New name/UUID reflected after refresh |
| Expired refresh token | Re-auth badge, no data loss |
| Two accounts, one expired | Active account unaffected |
| Airplane mode with valid token | Launch proceeds; reconnect inside game |
| Airplane mode with expired token | Clear offline + expired message |

### 12.3 Security tests

- Token files are unreadable by `adb backup` (backup disabled).
- A copied `token.bin` under another account ID fails GCM authentication.
- Logs never contain a token; a grep over a full debug log finds none.
- The device code is not in the clipboard unless the user taps Copy.
- The app does not respond to a foreign `eclipse://` redirect (it has none).

---

## 13. Decisions for sign-off

1. **Device authorization grant, `consumers` tenant, public client, scope `XboxLive.signin offline_access`.**
2. **One project-owned Entra registration**; client ID in `BuildConfig`; no secret.
3. **Refresh token only in Android Keystore-encrypted storage**; access/XSTS/XBL tokens in memory.
4. **Mojang AppID allowlist is a hard release gate**; no public auth before approval.
5. **Java Edition entitlement is checked explicitly**, with distinct messages for Bedrock-only, no-profile and no-ownership.
6. **No offline authentication, no alt manager, no embedded Microsoft login.**
7. **Demo mode is separate and only ships if 26.3 accepts it without a real token.**
8. **The required Minecraft disclaimer and a non-affiliation statement ship in the app, the README and the release page.**
9. **The “Eclipse” trademark risk is documented and reviewed before 1.0.**
10. **A local account is kept as a device-local display profile.** It carries no warning note, does not authenticate, and cannot launch online play.
11. **No third-party launcher client ID ships.** Microsoft auth uses the project's own Entra registration or stays visibly unconfigured; Ely.by and other alternative providers are removed.

## 14. Open questions

- Does 26.3 require `--xuid` and `--clientId`, and what values does it accept from a third-party launcher?
- Does 26.3's `--demo` work with an empty token, or does it need a synthetic session?
- What is the exact refresh-token lifetime and rotation behaviour for a `consumers` public client in 2026?
- Does Microsoft require publisher verification for this app class, and what does the consent screen show without it?
- Does Android 17 change Keystore or backup behaviour in a way that affects the token store?
- Can Mojang's review team provide a development client ID, or must every test wait for production approval?
- Is there a supported way to detect “Xbox Live unavailable in this region” before the user starts the flow?
