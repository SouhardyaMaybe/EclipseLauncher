package me.shadow.eclipselauncher.core.value.launcherprofiles

import androidx.annotation.Keep

@Keep
class MinecraftAuthenticationDatabase {
    @JvmField
    var accessToken: String = ""
    @JvmField
    var displayName: String = ""
    @JvmField
    var username: String = ""
    @JvmField
    var uuid: String = ""
    // public MinecraftProfile[] profiles;
}
