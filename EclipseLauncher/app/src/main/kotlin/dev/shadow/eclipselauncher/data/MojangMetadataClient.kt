/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.data

import dev.shadow.eclipselauncher.core.common.AppResult
import dev.shadow.eclipselauncher.core.common.Failure
import dev.shadow.eclipselauncher.core.common.FailureStage
import dev.shadow.eclipselauncher.core.common.UserAction
import dev.shadow.eclipselauncher.core.network.DownloadSource
import dev.shadow.eclipselauncher.core.network.NetworkException
import dev.shadow.eclipselauncher.domain.mojang.AssetIndex
import dev.shadow.eclipselauncher.domain.mojang.MojangMetadata
import dev.shadow.eclipselauncher.domain.mojang.VersionJson
import dev.shadow.eclipselauncher.domain.mojang.VersionManifest
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/** The pause/cancel result shared by metadata fetching and the executor. */
internal fun installCancelledFailure(): Failure = Failure(
    code = "INSTALL_CANCELLED",
    stage = FailureStage.INSTALL,
    retryable = true,
    userMessage = "The install was paused.",
    userAction = UserAction.NONE,
)

/**
 * Fetches the three official Mojang documents through the same strict
 * [DownloadSource] the installer uses. The documents are small; a hard cap
 * keeps a hostile or broken response from exhausting the heap.
 *
 * The parser lives in `:domain:mojang`; this class only owns transport and
 * cancellation. It never logs or echoes a URL.
 */
class MojangMetadataClient(private val source: DownloadSource) {
    private var cachedManifest: VersionManifest? = null

    fun fetchManifest(isCancelled: () -> Boolean = { false }): AppResult<VersionManifest> {
        cachedManifest?.let { return AppResult.Success(it) }
        return when (val text = fetchText(MANIFEST_URL, MAX_MANIFEST_BYTES, isCancelled)) {
            is AppResult.Error -> text
            is AppResult.Success -> when (val parsed = MojangMetadata.parseManifest(text.value)) {
                is AppResult.Error -> parsed
                is AppResult.Success -> {
                    cachedManifest = parsed.value
                    parsed
                }
            }
        }
    }

    fun fetchVersion(
        url: String,
        isCancelled: () -> Boolean = { false },
    ): AppResult<VersionJson> =
        when (val text = fetchText(url, MAX_VERSION_BYTES, isCancelled)) {
            is AppResult.Error -> text
            is AppResult.Success -> MojangMetadata.parseVersion(text.value)
        }

    fun fetchAssetIndex(
        url: String,
        isCancelled: () -> Boolean = { false },
    ): AppResult<AssetIndex> =
        when (val text = fetchText(url, MAX_ASSET_INDEX_BYTES, isCancelled)) {
            is AppResult.Error -> text
            is AppResult.Success -> MojangMetadata.parseAssetIndex(text.value)
        }

    private fun fetchText(
        url: String,
        maxBytes: Int,
        isCancelled: () -> Boolean,
    ): AppResult<String> {
        if (isCancelled()) return AppResult.Error(installCancelledFailure())
        return try {
            val bytes = source.open(url).use { input -> readLimited(input, maxBytes, isCancelled) }
            AppResult.Success(String(bytes, Charsets.UTF_8))
        } catch (_: MetadataCancelled) {
            AppResult.Error(installCancelledFailure())
        } catch (error: MetadataTooLarge) {
            AppResult.Error(
                Failure(
                    code = "MOJANG_METADATA_INVALID",
                    stage = FailureStage.RESOLVE,
                    retryable = false,
                    userMessage = "The official version metadata is too large to use.",
                    userAction = UserAction.NONE,
                    technicalDetail = error.message,
                ),
            )
        } catch (error: NetworkException) {
            AppResult.Error(
                Failure(
                    code = error.code,
                    stage = FailureStage.NETWORK,
                    retryable = error.retryable,
                    userMessage = metadataNetworkMessage(error.code),
                    userAction = if (error.retryable) UserAction.RETRY else UserAction.NONE,
                    technicalDetail = error.message,
                ),
            )
        } catch (error: IOException) {
            AppResult.Error(
                Failure(
                    code = "MOJANG_METADATA_IO",
                    stage = FailureStage.RESOLVE,
                    retryable = true,
                    userMessage = "The official version metadata could not be read.",
                    userAction = UserAction.RETRY,
                    technicalDetail = error.message,
                ),
            )
        }
    }

    private fun readLimited(
        input: InputStream,
        maxBytes: Int,
        isCancelled: () -> Boolean,
    ): ByteArray {
        val output = ByteArrayOutputStream(minOf(maxBytes, 64 * 1024))
        val buffer = ByteArray(BUFFER_SIZE)
        var total = 0
        while (true) {
            if (isCancelled()) throw MetadataCancelled()
            val read = input.read(buffer)
            if (read < 0) break
            if (read == 0) continue
            total += read
            if (total > maxBytes) throw MetadataTooLarge()
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private fun metadataNetworkMessage(code: String): String = when (code) {
        "NET_NOT_FOUND" -> "The official version metadata is no longer available."
        "NET_HOST_NOT_ALLOWED" -> "The version metadata is hosted on a server the launcher does not trust."
        "NET_TLS" -> "A secure connection to the metadata server failed."
        "NET_RETRY_EXHAUSTED" -> "The metadata server is busy. Try again later."
        "NET_URL_INVALID", "NET_REDIRECT_INVALID", "NET_REDIRECT_LIMIT" ->
            "The version metadata has an invalid address."
        else -> "The official version metadata could not be downloaded."
    }

    private class MetadataCancelled : Exception()

    private class MetadataTooLarge : Exception("the metadata response exceeded its size cap")

    private companion object {
        const val MANIFEST_URL =
            "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
        const val MAX_MANIFEST_BYTES = 8 * 1024 * 1024
        const val MAX_VERSION_BYTES = 16 * 1024 * 1024
        const val MAX_ASSET_INDEX_BYTES = 64 * 1024 * 1024
        const val BUFFER_SIZE = 16 * 1024
    }
}
