/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Microsoft (Xbox Live) sign-in for Minecraft, using the OAuth client ID the
 * reference launcher registers with login.live.com. The chain is the standard
 * documented one: authorize -> live token -> Xbox Live -> XSTS -> Minecraft
 * services -> profile. Deliberately free of android.* imports so the JVM unit
 * tests can exercise it.
 */
package com.eclipselauncher.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

enum class MsAuthStage {
    LIVE,
    XBOX,
    XSTS,
    PROFILE,
}

enum class MsAuthError {
    DENIED,
    NETWORK,
    NO_XBOX_PROFILE,
    REGION,
    ADULT_VERIFY,
    CHILD,
    NO_MINECRAFT,
    UNKNOWN,
}

class MsAuthException(val error: MsAuthError, message: String) : Exception(message)

/** Outcome of parsing the OAuth redirect URL. */
sealed interface RedirectResult {
    data class Success(val code: String) : RedirectResult
    data class Denied(val error: String) : RedirectResult
    data class Failure(val error: String, val description: String?) : RedirectResult
}

data class MsLoginResult(
    val refreshToken: String,
    val xuid: String,
    val username: String,
    val uuid: String,
    val expiresAtEpochSeconds: Long,
)

object MicrosoftAuth {
    const val CLIENT_ID = "00000000402b5328"
    const val REDIRECT_URL = "https://login.live.com/oauth20_desktop.srf"
    const val SCOPE = "service::user.auth.xboxlive.com::MBI_SSL"

    private const val AUTHORIZE_URL = "https://login.live.com/oauth20_authorize.srf"
    private const val TOKEN_URL = "https://login.live.com/oauth20_token.srf"
    private const val XBL_URL = "https://user.auth.xboxlive.com/user/authenticate"
    private const val XSTS_URL = "https://xsts.auth.xboxlive.com/xsts/authorize"
    private const val MC_LOGIN_URL = "https://api.minecraftservices.com/authentication/login_with_xbox"
    private const val MC_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile"

    private const val TIMEOUT_MS = 20_000
    private val json = Json { ignoreUnknownKeys = true }

    /** The authorize URL opened in the sign-in WebView. */
    fun authorizeUrl(): String = buildString {
        append(AUTHORIZE_URL)
        append("?client_id=").append(CLIENT_ID)
        append("&response_type=code")
        append("&scope=").append(URLEncoder.encode(SCOPE, "UTF-8"))
        append("&redirect_url=").append(URLEncoder.encode(REDIRECT_URL, "UTF-8"))
    }

    /** Classifies a URL the WebView lands on; null when it is not our redirect. */
    fun parseRedirect(url: String): RedirectResult? {
        if (!url.startsWith(REDIRECT_URL)) return null
        val query = url.substringAfter('?', "").substringBefore('#')
        val params = query.split('&').mapNotNull { part ->
            val idx = part.indexOf('=')
            if (idx <= 0) null
            else part.take(idx) to java.net.URLDecoder.decode(part.substring(idx + 1), "UTF-8")
        }.toMap()
        val error = params["error"]
        return when {
            params["code"] != null -> RedirectResult.Success(params.getValue("code"))
            error == "access_denied" -> RedirectResult.Denied(error)
            else -> RedirectResult.Failure(error ?: "unknown", params["error_description"])
        }
    }

    /** Runs the whole chain for an authorization code. */
    suspend fun login(
        code: String,
        onStage: (MsAuthStage) -> Unit = {},
    ): MsLoginResult = withContext(Dispatchers.IO) {
        onStage(MsAuthStage.LIVE)
        val live = exchangeCode(code)

        onStage(MsAuthStage.XBOX)
        val xblToken = xblAuthenticate(live.accessToken)

        onStage(MsAuthStage.XSTS)
        val (xuid, xstsToken) = xstsAuthorize(xblToken)

        onStage(MsAuthStage.PROFILE)
        val minecraft = minecraftLogin(xstsToken, xuid)
        val profile = minecraftProfile(minecraft.accessToken)

        MsLoginResult(
            refreshToken = live.refreshToken,
            xuid = xuid,
            username = profile.name,
            uuid = profile.uuid,
            expiresAtEpochSeconds = nowEpochSeconds() + minecraft.expiresIn,
        )
    }

    /** Exchanges a refresh token for a fresh access token (for future use). */
    suspend fun refreshAccessToken(refreshToken: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val fields = tokenFields("refresh_token") + ("refresh_token" to refreshToken)
                postForm(TOKEN_URL, fields).str("access_token")
            }.getOrNull()
        }

    // ------------------------------------------------------------------ steps

    private data class LiveToken(val accessToken: String, val refreshToken: String)

    private data class MinecraftToken(val accessToken: String, val expiresIn: Long)

    private data class Profile(val name: String, val uuid: String)

    private fun exchangeCode(code: String): LiveToken {
        val response = postForm(
            TOKEN_URL,
            tokenFields("authorization_code") + ("code" to code),
        )
        return LiveToken(
            accessToken = response.str("access_token"),
            refreshToken = response.str("refresh_token"),
        )
    }

    private fun tokenFields(grantType: String): Map<String, String> = mapOf(
        "client_id" to CLIENT_ID,
        "grant_type" to grantType,
        "redirect_url" to REDIRECT_URL,
        "scope" to SCOPE,
    )

    private fun xblAuthenticate(accessToken: String): String {
        val body = buildJsonObject {
            putJsonObject("Properties") {
                put("AuthMethod", "RPS")
                put("SiteName", "user.auth.xboxlive.com")
                put("RpsTicket", accessToken)
            }
            put("RelyingParty", "http://auth.xboxlive.com")
            put("TokenType", "JWT")
        }
        return postJson(XBL_URL, body.toString()).str("Token")
    }

    private fun xstsAuthorize(xblToken: String): Pair<String, String> {
        val body = buildJsonObject {
            putJsonObject("Properties") {
                put("SandboxId", "RETAIL")
                putJsonArray("UserTokens") { add(JsonPrimitive(xblToken)) }
            }
            put("RelyingParty", "rp://api.minecraftservices.com/")
            put("TokenType", "JWT")
        }
        val (code, text) = request(XSTS_URL, body.toString(), "application/json")
        if (code !in 200..299) {
            val parsed = runCatching { json.parseToJsonElement(text) as? JsonObject }.getOrNull()
            val xerr = (parsed?.get("XErr") as? JsonPrimitive)?.longOrNull
            throw MsAuthException(
                xerr.toAuthError(),
                "XSTS authorization failed (XErr=$xerr, http=$code)",
            )
        }
        val response = runCatching { json.parseToJsonElement(text) as JsonObject }.getOrNull()
            ?: throw MsAuthException(MsAuthError.UNKNOWN, "Malformed XSTS response")
        val displayClaims = response["DisplayClaims"] as? JsonObject
        val xuiArray = displayClaims?.get("xui") as? JsonArray
        val firstClaim = xuiArray?.firstOrNull() as? JsonObject
        val xuid = (firstClaim?.get("uhs") as? JsonPrimitive)?.content
            ?: throw MsAuthException(MsAuthError.UNKNOWN, "XSTS response had no user hash")
        return xuid to response.str("Token")
    }

    private fun minecraftLogin(xstsToken: String, xuid: String): MinecraftToken {
        val body = buildJsonObject {
            put("identityToken", "XBL3.0 x=$xuid;$xstsToken")
        }
        val response = postJson(MC_LOGIN_URL, body.toString())
        return MinecraftToken(
            accessToken = response.str("access_token"),
            expiresIn = response.long("expires_in") ?: 86_400L,
        )
    }

    private fun minecraftProfile(accessToken: String): Profile {
        val (code, text) = request(MC_PROFILE_URL, null, null, accessToken)
        if (code == 404) {
            throw MsAuthException(
                MsAuthError.NO_MINECRAFT,
                "No Minecraft profile on this account",
            )
        }
        if (code !in 200..299) {
            throw MsAuthException(MsAuthError.UNKNOWN, "Profile fetch failed (http=$code)")
        }
        val response = runCatching { json.parseToJsonElement(text) as? JsonObject }.getOrNull()
            ?: throw MsAuthException(MsAuthError.UNKNOWN, "Malformed profile response")
        return Profile(
            name = response.str("name"),
            uuid = response.str("id"),
        )
    }

    private fun Long?.toAuthError(): MsAuthError = when (this) {
        2148916233L -> MsAuthError.NO_XBOX_PROFILE
        2148916235L -> MsAuthError.REGION
        2148916236L, 2148916237L -> MsAuthError.ADULT_VERIFY
        2148916238L -> MsAuthError.CHILD
        else -> MsAuthError.UNKNOWN
    }

    private fun nowEpochSeconds(): Long = System.currentTimeMillis() / 1000

    // ------------------------------------------------------------------ http

    private fun postForm(url: String, fields: Map<String, String>): JsonObject {
        val body = fields.entries.joinToString("&") { (k, v) ->
            URLEncoder.encode(k, "UTF-8") + "=" + URLEncoder.encode(v, "UTF-8")
        }
        val (code, text) = request(url, body, "application/x-www-form-urlencoded")
        if (code !in 200..299) {
            val denied = text.contains("\"error\":\"invalid_grant\"") ||
                text.contains("\"error\":\"interaction_required\"")
            if (denied) throw MsAuthException(MsAuthError.DENIED, "Token exchange rejected")
            throw MsAuthException(MsAuthError.UNKNOWN, "Token exchange failed (http=$code)")
        }
        return parseObject(text, url)
    }

    private fun postJson(url: String, body: String): JsonObject {
        val (code, text) = request(url, body, "application/json")
        if (code !in 200..299) {
            throw MsAuthException(MsAuthError.UNKNOWN, "Request failed (http=$code): $text")
        }
        return parseObject(text, url)
    }

    private fun request(
        url: String,
        body: String?,
        contentType: String?,
        bearer: String? = null,
    ): Pair<Int, String> {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.requestMethod = if (body != null) "POST" else "GET"
            connection.setRequestProperty("Accept", "application/json")
            contentType?.let { connection.setRequestProperty("Content-Type", it) }
            bearer?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
            if (body != null) {
                connection.doOutput = true
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            return code to text
        } catch (io: IOException) {
            throw MsAuthException(MsAuthError.NETWORK, io.message ?: "Network error")
        } finally {
            connection.disconnect()
        }
    }

    private fun parseObject(text: String, url: String): JsonObject =
        runCatching { json.parseToJsonElement(text) as? JsonObject }.getOrNull()
            ?: throw MsAuthException(MsAuthError.UNKNOWN, "Malformed response from $url")

    private fun JsonObject.str(key: String): String =
        (get(key) as? JsonPrimitive)?.content
            ?: throw MsAuthException(MsAuthError.UNKNOWN, "Missing field '$key'")

    private fun JsonObject.long(key: String): Long? =
        (get(key) as? JsonPrimitive)?.jsonPrimitive?.longOrNull
}
