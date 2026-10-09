package me.shadow.eclipselauncher.feature

import me.shadow.eclipselauncher.feature.log.Logging
import me.shadow.eclipselauncher.feature.version.Version
import me.shadow.eclipselauncher.setting.AllSettings
import me.shadow.eclipselauncher.utils.MCVersionRegex
import me.shadow.eclipselauncher.utils.EclipseTools
import me.shadow.eclipselauncher.utils.version.VersionNumbers
import me.shadow.eclipselauncher.utils.version.VersionRanges

class ProfileLanguageSelector {
    companion object {
        private fun getOlderLanguage(lang: String): String {
            val underscoreIndex = lang.indexOf('_')
            return if (underscoreIndex != -1) {
                //Uppercase only the characters after the underscore
                val builder = StringBuilder(lang.substring(0, underscoreIndex + 1))
                builder.append(lang.substring(underscoreIndex + 1).uppercase())
                builder.toString()
            } else lang
        }

        private fun getLanguage(minecraftVersion: Version, rawLang: String): String {
            val lang = if (rawLang == "system") EclipseTools.getSystemLanguage() else rawLang

            val version: String = minecraftVersion.getVersionInfo()?.minecraftVersion ?: "1.11"

            val versionId = VersionNumbers.normalize(version)
            Logging.i("ProfileLanguageSelector", "Version Id : $versionId")

            return when {
                versionId.contains('.') -> {
                    if (isOlderVersionRelease(versionId)) getOlderLanguage(lang) // 1.10 -
                    else lang
                }
                MCVersionRegex.SNAPSHOT_REGEX.matcher(versionId).matches() -> { // Snapshot version "24w09a" "16w20a"
                    if (isOlderVersionSnapshot(versionId)) getOlderLanguage(lang)
                    else lang
                }
                else -> lang
            }
        }

        private fun isOlderVersionRelease(versionName: String): Boolean {
            return VersionRanges.atMost("1.10.2").contains(versionName)
        }

        private fun isOlderVersionSnapshot(versionName: String): Boolean {
            return VersionRanges.atMost("16w32a").contains(versionName)
        }

        @JvmStatic
        fun setGameLanguage(version: Version, overridden: Boolean) {
            if (MCOptions.containsKey("lang") && !overridden) return
            val language = getLanguage(version, AllSettings.setGameLanguage.getValue())
            Logging.i("ProfileLanguageSelector", "The game language has been set to: $language")
            MCOptions.set("lang", language)
        }
    }
}
