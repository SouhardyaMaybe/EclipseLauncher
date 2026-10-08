package me.shadow.eclipselauncher.feature.notice

import me.shadow.eclipselauncher.feature.log.Logging
import me.shadow.eclipselauncher.setting.AllSettings
import me.shadow.eclipselauncher.utils.ZHTools
import me.shadow.eclipselauncher.utils.http.CallUtils
import me.shadow.eclipselauncher.utils.http.CallUtils.CallbackListener
import me.shadow.eclipselauncher.utils.path.UrlManager
import me.shadow.eclipselauncher.utils.stringutils.StringUtils
import me.shadow.eclipselauncher.pojav.Tools
import okhttp3.Call
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.Objects

class CheckNewNotice {
    companion object {
        @JvmStatic
        var noticeInfo: NoticeInfo? = null
        private var isChecking = false

        private fun checkCooling(): Boolean {
            return ZHTools.getCurrentTimeMillis() - AllSettings.noticeCheck.getValue() > 2 * 60 * 1000 //2-minute cooldown
        }

        @JvmStatic
        fun checkNewNotice(listener: CheckNoticeListener) {
            if (isChecking) {
                return
            }
            isChecking = true

            noticeInfo?.let {
                listener.onSuccessful(noticeInfo)
                isChecking = false
                return
            }

            if (!checkCooling()) {
                return
            } else {
                AllSettings.noticeCheck.put(ZHTools.getCurrentTimeMillis()).save()
            }

            CallUtils(object : CallbackListener {
                override fun onFailure(call: Call?) {
                    isChecking = false
                }

                @Throws(IOException::class)
                override fun onResponse(call: Call?, response: Response?) {
                    if (!response!!.isSuccessful) {
                        Logging.e("CheckNewNotice", "Unexpected code ${response.code}")
                    } else {
                        runCatching {
                            Objects.requireNonNull(response.body)
                            val responseBody = response.body!!.string()

                            val originJson = JSONObject(responseBody)
                            val rawBase64 = originJson.getString("content")
                            //base64 decode, because what is read here is Base64-encoded text
                            val rawJson = StringUtils.decodeBase64(rawBase64)

                            val noticeJson = Tools.GLOBAL_GSON.fromJson(rawJson, NoticeJsonObject::class.java)

                            //Get the notice message
                            val title = getLanguageText(noticeJson.title)
                            val content = getLanguageText(noticeJson.content)

                            noticeInfo = NoticeInfo(title, content, noticeJson.date, noticeJson.numbering)
                            listener.onSuccessful(noticeInfo)
                        }.getOrElse { e ->
                            Logging.e("Check New Notice", "Failed to resolve the notice.", e)
                        }
                    }
                    isChecking = false
                }
            }, "${UrlManager.URL_GITHUB_HOME}launcher_notice.json", null).enqueue()
        }

        private fun getLanguageText(text: NoticeJsonObject.Text): String {
            return text.enUS
        }
    }
}
