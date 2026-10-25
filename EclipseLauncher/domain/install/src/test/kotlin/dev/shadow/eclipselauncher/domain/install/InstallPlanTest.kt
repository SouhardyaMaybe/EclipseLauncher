/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.domain.install

import com.google.common.truth.Truth.assertThat
import dev.shadow.eclipselauncher.core.common.AppResult
import dev.shadow.eclipselauncher.domain.mojang.AssetIndex
import dev.shadow.eclipselauncher.domain.mojang.AssetIndexRef
import dev.shadow.eclipselauncher.domain.mojang.AssetObject
import dev.shadow.eclipselauncher.domain.mojang.DownloadInfo
import dev.shadow.eclipselauncher.domain.mojang.LoggingClient
import dev.shadow.eclipselauncher.domain.mojang.MavenCoordinate
import dev.shadow.eclipselauncher.domain.mojang.MetadataException
import dev.shadow.eclipselauncher.domain.mojang.ResolvedLibrary
import dev.shadow.eclipselauncher.domain.mojang.ResolvedVersion
import org.junit.Assert.assertThrows
import org.junit.Test

class InstallPlanTest {
    private val clientHash = "1111111111111111111111111111111111111111"
    private val libraryHash = "2222222222222222222222222222222222222222"
    private val indexHash = "3333333333333333333333333333333333333333"
    private val loggingHash = "4444444444444444444444444444444444444444"
    private val assetA = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    private val assetB = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"

    private fun download(hash: String, size: Long, url: String = "https://example.invalid/$hash") =
        DownloadInfo(url = url, sha1 = hash, size = size)

    private fun version(
        libraries: List<ResolvedLibrary> = listOf(library()),
        logging: LoggingClient? = LoggingClient(
            argument = "-Dlog4j.configurationFile=\${path}",
            file = download(loggingHash, 40),
            type = "log4j2-xml",
        ),
        client: DownloadInfo = download(clientHash, 10),
    ) = ResolvedVersion(
        id = "26.3",
        mainClass = "net.minecraft.client.main.Main",
        assetIndex = AssetIndexRef(
            id = "34",
            sha1 = indexHash,
            size = 30,
            totalSize = 100,
            url = "https://piston-meta.mojang.com/v1/packages/$indexHash/34.json",
        ),
        client = client,
        libraries = libraries,
        droppedArtifacts = emptyList(),
        logging = logging,
        javaMajorVersion = 25,
        arguments = null,
        minecraftArguments = null,
    )

    private fun library(
        name: String = "com.example:lib:1.0",
        path: String = "com/example/lib/1.0/lib-1.0.jar",
        hash: String = libraryHash,
    ) = ResolvedLibrary(
        coordinate = MavenCoordinate.parse(name),
        download = download(hash, 20),
        path = path,
    )

    private fun assets() = AssetIndex(
        objects = mapOf(
            "minecraft/sounds/a.ogg" to AssetObject(assetA, 50),
            "minecraft/sounds/b.ogg" to AssetObject(assetB, 50),
            "minecraft/sounds/c.ogg" to AssetObject(assetA, 50),
        ),
    )

    @Test
    fun `builds a deterministic download list and deduplicates assets by hash`() {
        val plan = InstallPlanBuilder.buildOrThrow(version(), assets())

        assertThat(plan.downloads.map { it.destination }).containsExactly(
            "versions/26.3/client.jar",
            "libraries/com/example/lib/1.0/lib-1.0.jar",
            "assets/indexes/34.json",
            "logging/$loggingHash.xml",
            "assets/objects/aa/$assetA",
            "assets/objects/bb/$assetB",
        ).inOrder()
        assertThat(plan.assets).hasSize(2)
        assertThat(plan.totalBytes).isEqualTo(10 + 20 + 30 + 40 + 50 + 50)
        assertThat(plan.assets.first().url)
            .isEqualTo("https://resources.download.minecraft.net/aa/$assetA")
    }

    @Test
    fun `two different files may not share a destination`() {
        val result = InstallPlanBuilder.build(
            version(
                libraries = listOf(
                    library(path = "com/example/lib/1.0/lib-1.0.jar", hash = libraryHash),
                    library(
                        name = "com.example:other:1.0",
                        path = "com/example/lib/1.0/lib-1.0.jar",
                        hash = "5555555555555555555555555555555555555555",
                    ),
                ),
            ),
            assets(),
        )

        assertThat(result).isInstanceOf(AppResult.Error::class.java)
    }

    @Test
    fun `a missing client URL becomes an install failure`() {
        val result = InstallPlanBuilder.build(
            version(client = DownloadInfo(sha1 = clientHash, size = 10)),
            assets(),
        )

        assertThat(result).isInstanceOf(AppResult.Error::class.java)
        assertThat((result as AppResult.Error).failure.code)
            .isEqualTo("INSTALL_PLAN_INVALID")
    }

    @Test
    fun `a cleartext download URL is rejected`() {
        val result = InstallPlanBuilder.build(
            version(client = download(clientHash, 10, "http://example.invalid/client.jar")),
            assets(),
        )

        assertThat(result).isInstanceOf(AppResult.Error::class.java)
        assertThat((result as AppResult.Error).failure.code)
            .isEqualTo("INSTALL_PLAN_INVALID")
    }

    @Test
    fun `a URL with credentials is rejected`() {
        val result = InstallPlanBuilder.build(
            version(
                client = download(
                    clientHash,
                    10,
                    "https://user:secret@example.invalid/client.jar",
                ),
            ),
            assets(),
        )

        assertThat(result).isInstanceOf(AppResult.Error::class.java)
    }

    @Test
    fun `a non-443 port is rejected`() {
        val result = InstallPlanBuilder.build(
            version(
                client = download(clientHash, 10, "https://example.invalid:8443/client.jar"),
            ),
            assets(),
        )

        assertThat(result).isInstanceOf(AppResult.Error::class.java)
    }

    @Test
    fun `layout rejects a traversal in a version id`() {
        assertThrows(MetadataException::class.java) {
            InstallLayout().clientPath("../outside")
        }
        assertThrows(MetadataException::class.java) {
            InstallLayout().assetIndexPath("34/../../outside")
        }
    }

    @Test
    fun `layout rejects an invalid asset hash`() {
        assertThrows(MetadataException::class.java) {
            InstallLayout().assetObjectPath("not-a-sha1")
        }
    }
}
