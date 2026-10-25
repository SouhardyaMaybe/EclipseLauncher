/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.data

import dev.shadow.eclipselauncher.core.common.Failure
import dev.shadow.eclipselauncher.core.common.FailureStage
import dev.shadow.eclipselauncher.core.common.UserAction

/**
 * The device checks that can be made before a download starts.
 *
 * The APK ships only arm64-v8a and x86_64. A device that reports neither
 * cannot load the native runtime or the game, so failing before a 500 MiB
 * download is cheaper and clearer than failing at launch. Runtime and account
 * checks belong here once those components exist; they are deliberately not
 * faked today.
 */
object DevicePreflight {
    private val SUPPORTED_ABIS = listOf("arm64-v8a", "x86_64")

    fun supportedAbi(abis: List<String>): String? =
        abis.firstOrNull { it in SUPPORTED_ABIS }

    fun abiFailure(abis: List<String>): Failure = Failure(
        code = "DEVICE_ABI_UNSUPPORTED",
        stage = FailureStage.INSTALL,
        retryable = false,
        userMessage =
            "This device's CPU is not supported. Eclipse Launcher needs a 64-bit ARM or x86_64 device.",
        userAction = UserAction.NONE,
        technicalDetail = "Build.SUPPORTED_ABIS was ${abis.joinToString(",")}",
    )
}
