package me.shadow.eclipselauncher

import android.content.Context
import me.shadow.eclipselauncher.InfoDistributor.APP_NAME

class InfoCenter {
    companion object {
        @JvmStatic
        fun replaceName(context: Context, resString: Int): String = context.getString(resString, APP_NAME)
    }
}
