/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.domain.install

import java.io.IOException
import java.nio.file.Files

/** How much work a verification pass is allowed to do. */
enum class VerifyLevel {
    /** Existence and size only. Cheap enough before every launch. */
    FAST,

    /** Every managed object is re-hashed. Used after an install or a crash. */
    FULL,
}

enum class VerifyFailureReason {
    PATH,
    MISSING,
    SIZE,
    DIGEST,
    IO,
}

data class VerifyFailure(
    val path: String,
    val reason: VerifyFailureReason,
    val detail: String? = null,
)

data class VerifyReport(
    val level: VerifyLevel,
    val checkedFiles: Int,
    val failures: List<VerifyFailure>,
) {
    val healthy: Boolean
        get() = failures.isEmpty()
}

/**
 * Checks an instance manifest against the shared store. It never repairs and
 * never downloads: the caller decides whether a failure means re-download,
 * quarantine or a warning.
 */
object InstanceVerifier {
    fun verify(
        manifest: InstanceManifest,
        store: ArtifactStore,
        level: VerifyLevel,
    ): VerifyReport {
        val failures = mutableListOf<VerifyFailure>()
        var checked = 0

        for (file in manifest.files) {
            checked++
            val path = try {
                store.pathFor(file.path)
            } catch (error: StoreException) {
                failures += VerifyFailure(
                    path = file.path,
                    reason = VerifyFailureReason.PATH,
                    detail = error.message,
                )
                continue
            }

            try {
                if (!Files.isRegularFile(path)) {
                    failures += VerifyFailure(
                        path = file.path,
                        reason = VerifyFailureReason.MISSING,
                    )
                    continue
                }
                val actualSize = Files.size(path)
                if (actualSize != file.size) {
                    failures += VerifyFailure(
                        path = file.path,
                        reason = VerifyFailureReason.SIZE,
                        detail = "expected ${file.size} bytes, found $actualSize",
                    )
                    continue
                }
                if (level == VerifyLevel.FULL) {
                    val item = DownloadItem(
                        kind = file.kind,
                        url = file.origin,
                        sha1 = file.sha1,
                        size = file.size,
                        destination = file.path,
                    )
                    if (!store.isVerified(item)) {
                        failures += VerifyFailure(
                            path = file.path,
                            reason = VerifyFailureReason.DIGEST,
                            detail = "expected SHA-1 ${file.sha1}",
                        )
                    }
                }
            } catch (error: IOException) {
                failures += VerifyFailure(
                    path = file.path,
                    reason = VerifyFailureReason.IO,
                    detail = error.message,
                )
            }
        }

        return VerifyReport(level = level, checkedFiles = checked, failures = failures)
    }
}
