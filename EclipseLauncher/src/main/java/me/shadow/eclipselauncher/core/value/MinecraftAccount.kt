package me.shadow.eclipselauncher.core.value

import androidx.annotation.Keep

import me.shadow.eclipselauncher.json.JsonSyntaxException
import me.shadow.eclipselauncher.feature.accounts.AccountsManager
import me.shadow.eclipselauncher.feature.log.Logging
import me.shadow.eclipselauncher.utils.path.PathManager
import me.shadow.eclipselauncher.utils.skin.SkinFileDownloader

import me.shadow.eclipselauncher.core.Tools

import me.shadow.eclipselauncher.utils.io.FileUtils

import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.Objects
import java.util.UUID

@Keep
class MinecraftAccount {
    @JvmField
    var accessToken: String = "0" // access token
    @JvmField
    var clientToken: String = "0" // clientID: refresh and invalidate
    @JvmField
    var profileId: String = "00000000-0000-0000-0000-000000000000" // profile UUID, for obtaining skin
    @JvmField
    var username: String = "Steve"
    @JvmField
    var msaRefreshToken: String = "0"
    @JvmField
    var xuid: String = ""
    @JvmField
    var accountType: String = ""
    val uniqueUUID: String = UUID.randomUUID().toString().lowercase(Locale.ROOT)

    fun updateMicrosoftSkin() {
        updateSkin("https://sessionserver.mojang.com")
    }

    private fun updateSkin(url: String) {
        val skinFile = File(PathManager.DIR_USER_SKIN, "$uniqueUUID.png")
        if (skinFile.exists()) FileUtils.deleteQuietly(skinFile) //Delete the skin file once
        try {
            SkinFileDownloader().downloadProfileSkin(url, skinFile, profileId)
            Logging.i("SkinLoader", "Update skin success")
        } catch (e: Exception) {
            Logging.i("SkinLoader", "Could not update skin\n" + Tools.printToString(e))
        }
    }

    @Throws(IOException::class)
    fun save() {
        Tools.write(PathManager.DIR_ACCOUNT_NEW + "/" + uniqueUUID, Tools.JSON_CODEC.toJson(this))
    }

    companion object {
        @JvmStatic
        @Throws(JsonSyntaxException::class)
        fun parse(content: String): MinecraftAccount? {
            return Tools.JSON_CODEC.fromJson(content, MinecraftAccount::class.java)
        }

        @JvmStatic
        fun loadFromProfileID(profileID: String): MinecraftAccount? {
            for (account in AccountsManager.allAccounts) {
                if (Objects.equals(account.profileId, profileID)) return account
            }
            return null
        }

        /**
         * The null checks below mirror the original: the JSON codec reflectively overwrites the
         * declared defaults when a stored profile contains an explicit null member, so a
         * non-null-typed field can still hold null at runtime.
         */
        @JvmStatic
        @Suppress("SENSELESS_COMPARISON")
        fun loadFromUniqueUUID(uniqueUUID: String): MinecraftAccount? {
            if (!accountExists(uniqueUUID)) return null
            try {
                val acc = parse(Tools.read(PathManager.DIR_ACCOUNT_NEW + "/" + uniqueUUID))!!
                if (acc.accessToken == null) {
                    acc.accessToken = "0"
                }
                if (acc.clientToken == null) {
                    acc.clientToken = "0"
                }
                if (acc.profileId == null) {
                    acc.profileId = "00000000-0000-0000-0000-000000000000"
                }
                if (acc.username == null) {
                    acc.username = "0"
                }
                if (acc.msaRefreshToken == null) {
                    acc.msaRefreshToken = "0"
                }
                return acc
            } catch (e: IOException) {
                Logging.e(MinecraftAccount::class.java.name, "Caught an exception while loading the profile", e)
                return null
            } catch (e: JsonSyntaxException) {
                Logging.e(MinecraftAccount::class.java.name, "Caught an exception while loading the profile", e)
                return null
            }
        }

        private fun accountExists(uniqueUUID: String): Boolean {
            return uniqueUUID.isNotEmpty() && File(PathManager.DIR_ACCOUNT_NEW + "/" + uniqueUUID).exists()
        }
    }

    override fun toString(): String {
        return "MinecraftAccount{" +
                "username='$username" + '\'' +
                ", accountType=$accountType" +
                '}'
    }
}
