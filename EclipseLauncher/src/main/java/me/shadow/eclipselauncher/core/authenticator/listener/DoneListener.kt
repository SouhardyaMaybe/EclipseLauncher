package me.shadow.eclipselauncher.core.authenticator.listener

import me.shadow.eclipselauncher.core.value.MinecraftAccount

/** Called when the login is done and the account received. guaranteed to be on the UI Thread */
fun interface DoneListener {
    fun onLoginDone(account: MinecraftAccount)
}
