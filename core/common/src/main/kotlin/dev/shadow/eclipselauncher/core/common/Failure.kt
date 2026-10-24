/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.core.common

/**
 * The stage a failure belongs to. The stage is not the same thing as the
 * subsystem that raised it: it is the step of the user-visible operation that
 * could not continue, which is what the UI has to explain.
 */
enum class FailureStage {
    AUTH,
    RESOLVE,
    DOWNLOAD,
    VERIFY,
    EXTRACT,
    INSTALL,
    LAUNCH,
    RUNTIME,
    STORAGE,
    NETWORK,
    UNKNOWN,
}

/** The one thing the UI should offer after a failure. */
enum class UserAction {
    RETRY,
    SIGN_IN,
    FREE_SPACE,
    CHOOSE_VERSION,
    OPEN_SETTINGS,
    EXPORT_LOGS,
    NONE,
}

/**
 * A failure that is safe to show, log and compare.
 *
 * [code] is a stable machine identifier in SCREAMING_SNAKE_CASE. It is part of
 * the product contract: support notes, tests and the diagnostic bundle all use
 * it, so it must not be renamed casually. [technicalDetail] is for the log and
 * the diagnostic bundle, not the first line of the UI.
 */
data class Failure(
    val code: String,
    val stage: FailureStage,
    val retryable: Boolean,
    val userMessage: String,
    val userAction: UserAction = UserAction.NONE,
    val technicalDetail: String? = null,
) {
    init {
        require(CODE_PATTERN.matches(code)) {
            "failure code must be SCREAMING_SNAKE_CASE, was '$code'"
        }
        require(userMessage.isNotBlank()) { "a failure needs a user-facing message" }
    }

    companion object {
        private val CODE_PATTERN = Regex("[A-Z][A-Z0-9_]{2,63}")
    }
}

/** A result that either carries a value or one [Failure]. */
sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Error(val failure: Failure) : AppResult<Nothing>
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(value))
    is AppResult.Error -> this
}

inline fun <T, R> AppResult<T>.fold(
    onSuccess: (T) -> R,
    onError: (Failure) -> R,
): R = when (this) {
    is AppResult.Success -> onSuccess(value)
    is AppResult.Error -> onError(failure)
}
