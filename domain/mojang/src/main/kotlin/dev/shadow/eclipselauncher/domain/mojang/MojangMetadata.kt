/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.domain.mojang

import dev.shadow.eclipselauncher.core.common.AppResult
import dev.shadow.eclipselauncher.core.common.Failure
import dev.shadow.eclipselauncher.core.common.FailureStage
import dev.shadow.eclipselauncher.core.common.UserAction
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString

/**
 * Parsing entry points for the two official Mojang documents. Both return a
 * [Failure] instead of leaking a serializer exception into the UI, because a
 * malformed metadata response is a user-visible resolve failure.
 */
object MojangMetadata {
    fun parseManifest(json: String): AppResult<VersionManifest> =
        parse(json) { MojangJson.decodeFromString<VersionManifest>(it) }

    fun parseVersion(json: String): AppResult<VersionJson> =
        parse(json) { MojangJson.decodeFromString<VersionJson>(it) }

    fun findVersion(manifest: VersionManifest, id: String): VersionSummary? =
        manifest.versions.firstOrNull { it.id == id }

    fun latestRelease(manifest: VersionManifest): VersionSummary? =
        findVersion(manifest, manifest.latest.release)

    private fun <T> parse(json: String, block: (String) -> T): AppResult<T> =
        try {
            AppResult.Success(block(json))
        } catch (error: SerializationException) {
            AppResult.Error(
                Failure(
                    code = "MOJANG_METADATA_INVALID",
                    stage = FailureStage.RESOLVE,
                    retryable = true,
                    userMessage = "The version list could not be read.",
                    userAction = UserAction.RETRY,
                    technicalDetail = error.message,
                ),
            )
        } catch (error: IllegalArgumentException) {
            AppResult.Error(
                Failure(
                    code = "MOJANG_METADATA_INVALID",
                    stage = FailureStage.RESOLVE,
                    retryable = false,
                    userMessage = "The version list is not valid.",
                    userAction = UserAction.CHOOSE_VERSION,
                    technicalDetail = error.message,
                ),
            )
        }
}
