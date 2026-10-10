package me.shadow.eclipselauncher.core.authenticator.listener

/** Called when there is a complete failure, guaranteed to be on the UI Thread */
fun interface ErrorListener {
    fun onLoginError(errorMessage: Throwable)
}
