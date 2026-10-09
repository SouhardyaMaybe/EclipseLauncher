package me.shadow.eclipselauncher.feature.accounts

import android.content.Context
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.core.authenticator.listener.DoneListener
import me.shadow.eclipselauncher.core.authenticator.listener.ErrorListener
import me.shadow.eclipselauncher.core.authenticator.microsoft.MicrosoftBackgroundLogin
import me.shadow.eclipselauncher.core.value.MinecraftAccount

class AccountUtils {
    companion object {
        @JvmStatic
        fun microsoftLogin(context: Context, account: MinecraftAccount, doneListener: DoneListener, errorListener: ErrorListener) {
            MicrosoftBackgroundLogin(true, account.msaRefreshToken)
                .performLogin(context, account, doneListener, errorListener)
        }

        @JvmStatic
        fun isMicrosoftAccount(account: MinecraftAccount): Boolean {
            return account.accountType == AccountType.MICROSOFT.type
        }

        @JvmStatic
        fun isNoLoginRequired(account: MinecraftAccount?): Boolean {
            return account == null || account.accountType == AccountType.LOCAL.type
        }

        @JvmStatic
        fun getAccountTypeName(context: Context, account: MinecraftAccount): String {
            return if (isMicrosoftAccount(account)) {
                context.getString(R.string.account_microsoft_account)
            } else {
                context.getString(R.string.account_local_account)
            }
        }
    }
}
