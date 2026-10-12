package me.shadow.eclipselauncher.core.authenticator.microsoft

import android.content.Context
import android.util.ArrayMap

import me.shadow.eclipselauncher.mcgui.ProgressLayout
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.feature.accounts.AccountType
import me.shadow.eclipselauncher.feature.log.Logging
import me.shadow.eclipselauncher.utils.path.UrlManager

import me.shadow.eclipselauncher.core.Tools
import me.shadow.eclipselauncher.core.authenticator.listener.DoneListener
import me.shadow.eclipselauncher.core.authenticator.listener.ErrorListener
import me.shadow.eclipselauncher.core.value.MinecraftAccount

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import java.io.IOException
import java.net.HttpURLConnection
import java.net.ProtocolException
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Collections

/** Allow to perform a background login on a given account */
// TODO handle connection errors !
class MicrosoftBackgroundLogin(private val mIsRefresh: Boolean, private val mAuthCode: String?) {

    /* Fields used to fill the account  */
    @JvmField
    var msRefreshToken: String? = null
    @JvmField
    var mcName: String? = null
    @JvmField
    var mcToken: String? = null
    @JvmField
    var mcUuid: String? = null
    @JvmField
    var doesOwnGame: Boolean = false

    /** Performs a full login, calling back listeners appropriately  */
    fun performLogin(
        context: Context,
        account: MinecraftAccount?,
        doneListener: DoneListener?,
        errorListener: ErrorListener?
    ) {
        loginScope.launch {
            // The login itself runs on a background dispatcher, like the old task pool thread did
            var accountOrNull: MinecraftAccount? = null
            var failure: Throwable? = null
            try {
                accountOrNull = performLoginSteps(context, account)
            } catch (t: Throwable) {
                failure = t
            }

            // Snapshotted into vals so the UI lambdas can smart cast them
            val finishedAccount = accountOrNull
            val thrown = failure

            if (thrown == null) {
                // ended: delivered on the UI thread, posted without blocking this coroutine,
                // exactly like the old TaskExecutors.getAndroidUI() handler post
                launch(Dispatchers.Main) {
                    try {
                        if (doneListener != null && finishedAccount != null) doneListener.onLoginDone(finishedAccount)
                    } catch (t: Throwable) {
                        // An exception thrown by the done listener was routed to onThrowable before
                        launch(Dispatchers.Main) { deliverError(t, errorListener) }
                    }
                }
            } else {
                launch(Dispatchers.Main) { deliverError(thrown, errorListener) }
            }

            // finallyTask: ran on the background pool right after the callbacks were posted,
            // inside a runCatching that swallowed anything it threw
            runCatching { ProgressLayout.clearProgress(ProgressLayout.LOGIN_ACCOUNT) }
        }
    }

    /** The five login steps and the account bookkeeping, run on the background dispatcher */
    private suspend fun performLoginSteps(context: Context, account: MinecraftAccount?): MinecraftAccount {
        notifyProgress(1, context.getString(R.string.account_login_progress_access_token))
        val accessToken = runInterruptible { acquireAccessToken(mIsRefresh, mAuthCode) }
        notifyProgress(2, context.getString(R.string.account_login_progress_xbl_token))
        val xboxLiveToken = runInterruptible { acquireXBLToken(accessToken) }
        notifyProgress(3, context.getString(R.string.account_login_progress_xsts_token))
        val xsts = runInterruptible { acquireXsts(xboxLiveToken) }
        notifyProgress(4, context.getString(R.string.account_login_progress_minecraft_token))
        val minecraftToken = runInterruptible { acquireMinecraftToken(xsts[0], xsts[1]) }
        notifyProgress(5, context.getString(R.string.account_login_progress_checking))
        runInterruptible { fetchOwnedItems(minecraftToken) }
        runInterruptible { checkMcProfile(minecraftToken) }

        return runInterruptible {
            val acc: MinecraftAccount
            if (account == null) {
                //Try to find an existing local account with the same Profile UUID
                val acc1 = MinecraftAccount.loadFromProfileID(mcUuid!!)
                acc = acc1 ?: MinecraftAccount()
            } else {
                acc = account
            }

            if (doesOwnGame) {
                acc.xuid = xsts[0]
                acc.clientToken = "0" /* FIXME */
                acc.accessToken = minecraftToken
                acc.username = mcName!!
                acc.profileId = mcUuid!!
                acc.msaRefreshToken = msRefreshToken!!
                acc.accountType = AccountType.MICROSOFT.type
                acc.updateMicrosoftSkin()
            }
            acc.save()
            Logging.i("Account", "Saved the account : ${acc.username}")
            acc
        }
    }

    /** Error callback, posted to the UI thread like the old onThrowable handler */
    private fun deliverError(e: Throwable, errorListener: ErrorListener?) {
        // The Task wrapper swallowed anything thrown by the handler itself
        runCatching {
            Logging.e("MicroAuth", "Exception thrown during authentication", e)
            errorListener?.onLoginError(e)
        }
    }

    @Throws(IOException::class, JSONException::class)
    fun acquireAccessToken(isRefresh: Boolean, authcode: String?): String {
        val url = URL(authTokenUrl)
        Logging.i("MicrosoftLogin", "isRefresh=$isRefresh")

        val formData = convertToFormData(
            "client_id", "00000000402b5328",
            if (isRefresh) "refresh_token" else "code", authcode,
            "grant_type", if (isRefresh) "refresh_token" else "authorization_code",
            "redirect_url", "https://login.live.com/oauth20_desktop.srf",
            "scope", "service::user.auth.xboxlive.com::MBI_SSL"
        )

        val conn = UrlManager.createHttpConnection(url)
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        conn.setRequestProperty("charset", "utf-8")
        conn.setRequestProperty(
            "Content-Length",
            formData.toByteArray(StandardCharsets.UTF_8).size.toString()
        )
        conn.setRequestMethod("POST")
        conn.setUseCaches(false)
        conn.setDoInput(true)
        conn.setDoOutput(true)
        conn.connect()
        conn.getOutputStream().use { wr ->
            wr.write(formData.toByteArray(StandardCharsets.UTF_8))
        }
        if (conn.getResponseCode() >= 200 && conn.getResponseCode() < 300) {
            val jo = JSONObject(Tools.read(conn.getInputStream()))
            msRefreshToken = jo.getString("refresh_token")
            conn.disconnect()
            return jo.getString("access_token")
            //acquireXBLToken(jo.getString("access_token"));
        } else {
            throw getResponseThrowable(conn)
        }
    }

    private fun acquireXBLToken(accessToken: String): String {
        val url = URL(xblAuthUrl)

        val data = JSONObject()
        val properties = JSONObject()
        properties.put("AuthMethod", "RPS")
        properties.put("SiteName", "user.auth.xboxlive.com")
        properties.put("RpsTicket", accessToken)
        data.put("Properties", properties)
        data.put("RelyingParty", "http://auth.xboxlive.com")
        data.put("TokenType", "JWT")

        val req = data.toString()
        val conn = UrlManager.createHttpConnection(url)
        setCommonProperties(conn, req)
        conn.connect()

        conn.getOutputStream().use { wr ->
            wr.write(req.toByteArray(StandardCharsets.UTF_8))
        }
        if (conn.getResponseCode() >= 200 && conn.getResponseCode() < 300) {
            val jo = JSONObject(Tools.read(conn.getInputStream()))
            conn.disconnect()
            return jo.getString("Token")
            //acquireXsts(jo.getString("Token"));
        } else {
            throw getResponseThrowable(conn)
        }
    }

    /** @return [uhs, token] */
    private fun acquireXsts(xblToken: String): Array<String> {
        val url = URL(xstsAuthUrl)

        val data = JSONObject()
        val properties = JSONObject()
        properties.put("SandboxId", "RETAIL")
        properties.put("UserTokens", JSONArray(Collections.singleton(xblToken)))
        data.put("Properties", properties)
        data.put("RelyingParty", "rp://api.minecraftservices.com/")
        data.put("TokenType", "JWT")

        val req = data.toString()
        val conn = UrlManager.createHttpConnection(url)
        setCommonProperties(conn, req)
        conn.connect()

        conn.getOutputStream().use { wr ->
            wr.write(req.toByteArray(StandardCharsets.UTF_8))
        }

        if (conn.getResponseCode() >= 200 && conn.getResponseCode() < 300) {
            val jo = JSONObject(Tools.read(conn.getInputStream()))
            val uhs = jo.getJSONObject("DisplayClaims").getJSONArray("xui").getJSONObject(0).getString("uhs")
            val token = jo.getString("Token")
            conn.disconnect()
            return arrayOf(uhs, token)
            //acquireMinecraftToken(uhs,jo.getString("Token"));
        } else if (conn.getResponseCode() == 401) {
            val responseContents = Tools.read(conn.getErrorStream())
            val jo = JSONObject(responseContents)
            val xerr = jo.optLong("XErr", -1)
            val localeId: Int? = XSTS_ERRORS[xerr]
            if (localeId != null) {
                throw PresentedException(RuntimeException(responseContents), localeId, false)
            }
            throw PresentedException(RuntimeException(responseContents), R.string.xerr_unknown, true, xerr)
        } else {
            throw getResponseThrowable(conn)
        }
    }

    private fun acquireMinecraftToken(xblUhs: String, xblXsts: String): String {
        val url = URL(mcLoginUrl)

        val data = JSONObject()
        data.put("identityToken", "XBL3.0 x=" + xblUhs + ";" + xblXsts)

        val req = data.toString()
        val conn = UrlManager.createHttpConnection(url)
        setCommonProperties(conn, req)
        conn.connect()

        conn.getOutputStream().use { wr ->
            wr.write(req.toByteArray(StandardCharsets.UTF_8))
        }

        if (conn.getResponseCode() >= 200 && conn.getResponseCode() < 300) {
            val jo = JSONObject(Tools.read(conn.getInputStream()))
            conn.disconnect()
            mcToken = jo.getString("access_token")
            //checkMcProfile(jo.getString("access_token"));
            return jo.getString("access_token")
        } else {
            throw getResponseThrowable(conn)
        }
    }

    private fun fetchOwnedItems(mcAccessToken: String) {
        val url = URL(mcStoreUrl)

        val conn = UrlManager.createHttpConnection(url)
        conn.setRequestProperty("Authorization", "Bearer $mcAccessToken")
        conn.setUseCaches(false)
        conn.connect()
        if (conn.getResponseCode() < 200 || conn.getResponseCode() >= 300) {
            throw getResponseThrowable(conn)
        }
        // We don't need any data from this request, it just needs to happen in order for
        // the MS servers to work properly. The data from this is practically useless
        // as it does not indicate whether the user owns the game through Game Pass.
    }

    private fun checkMcProfile(mcAccessToken: String) {
        val url = URL(mcProfileUrl)

        val conn = UrlManager.createHttpConnection(url)
        conn.setRequestProperty("Authorization", "Bearer $mcAccessToken")
        conn.setUseCaches(false)
        conn.connect()

        if (conn.getResponseCode() >= 200 && conn.getResponseCode() < 300) {
            val s = Tools.read(conn.getInputStream())
            conn.disconnect()
            val jsonObject = JSONObject(s)
            val name = jsonObject.get("name") as String
            val uuid = jsonObject.get("id") as String
            val uuidDashes = uuid.replaceFirst(
                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                "\$1-\$2-\$3-\$4-\$5"
            )
            doesOwnGame = true
            Logging.i("MicrosoftLogin", "UserName = $name")
            Logging.i("MicrosoftLogin", "Uuid Minecraft = $uuidDashes")
            mcName = name
            mcUuid = uuidDashes
        } else {
            Logging.i("MicrosoftLogin", "It seems that this Microsoft Account does not own the game.")
            doesOwnGame = false
            throw PresentedException(RuntimeException(conn.getResponseMessage()), R.string.minecraft_not_owned, true)
            //throwResponseError(conn);
        }
    }

    /** Wrapper to ease notifying the listener */
    private fun notifyProgress(step: Int, stepString: String) {
        ProgressLayout.setProgress(
            ProgressLayout.LOGIN_ACCOUNT,
            step * 20,
            R.string.account_login_microsoft_progress,
            stepString
        )
    }

    private fun getResponseThrowable(conn: HttpURLConnection): RuntimeException {
        Logging.i(
            "MicrosoftLogin",
            "Error code: " + conn.getResponseCode() + ": " + conn.getResponseMessage()
        )
        if (conn.getResponseCode() == 429) {
            return PresentedException(R.string.microsoft_login_retry_later, false)
        }
        return RuntimeException(conn.getResponseMessage())
    }

    companion object {
        private const val authTokenUrl = "https://login.live.com/oauth20_token.srf"
        private const val xblAuthUrl = "https://user.auth.xboxlive.com/user/authenticate"
        private const val xstsAuthUrl = "https://xsts.auth.xboxlive.com/xsts/authorize"
        private const val mcLoginUrl = "https://api.minecraftservices.com/authentication/login_with_xbox"
        private const val mcProfileUrl = "https://api.minecraftservices.com/minecraft/profile"
        private const val mcStoreUrl = "https://api.minecraftservices.com/entitlements/mcstore"

        private val XSTS_ERRORS = ArrayMap<Long, Int>().apply {
            put(2148916233L, R.string.xerr_no_account)
            put(2148916235L, R.string.xerr_not_available)
            put(2148916236L, R.string.xerr_adult_verification)
            put(2148916237L, R.string.xerr_adult_verification)
            put(2148916238L, R.string.xerr_child)
            put(2148074255L, R.string.account_microsoft_xerr_unauthorized)
        }

        /** Process-wide login scope: background work on Dispatchers.IO, like the old task pool */
        private val loginScope =
            CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineName("MicrosoftBackgroundLogin"))

        /** Set common properties for the connection. Given that all requests are POST, interactivity is always enabled */
        private fun setCommonProperties(conn: HttpURLConnection, formData: String) {
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("charset", "utf-8")
            try {
                conn.setRequestProperty(
                    "Content-Length",
                    formData.toByteArray(StandardCharsets.UTF_8).size.toString()
                )
                conn.setRequestMethod("POST")
            } catch (e: ProtocolException) {
                Logging.e("MicrosoftAuth", e.toString())
            }
            conn.setUseCaches(false)
            conn.setDoInput(true)
            conn.setDoOutput(true)
        }

        /**
         * @param data A series a strings: key1, value1, key2, value2...
         * @return the data converted as a form string for a POST request
         */
        private fun convertToFormData(vararg data: String?): String {
            val builder = StringBuilder()
            var i = 0
            while (i < data.size) {
                if (builder.length > 0) builder.append("&")
                builder.append(URLEncoder.encode(data[i], "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(data[i + 1], "UTF-8"))
                i += 2
            }
            return builder.toString()
        }
    }
}
