/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.core.network

import java.io.IOException

/**
 * A network failure with a stable code and a retry hint. The install executor
 * maps it to a [dev.shadow.eclipselauncher.core.common.Failure]; no URL is
 * included in the message because a URL may contain credentials.
 */
class NetworkException(
    val code: String,
    val retryable: Boolean,
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause) {
    init {
        require(CODE.matches(code)) {
            "network code must be SCREAMING_SNAKE_CASE, was '$code'"
        }
        require(message.isNotBlank()) { "a network failure needs a message" }
    }

    private companion object {
        val CODE = Regex("[A-Z][A-Z0-9_]{2,63}")
    }
}
