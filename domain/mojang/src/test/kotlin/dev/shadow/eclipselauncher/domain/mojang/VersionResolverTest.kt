/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.domain.mojang

import com.google.common.truth.Truth.assertThat
import dev.shadow.eclipselauncher.core.common.AppResult
import org.junit.Assert.assertThrows
import org.junit.Test

class VersionResolverTest {
    private val sha = "0123456789abcdef0123456789abcdef01234567"

    private fun download(path: String = "client.jar") = DownloadInfo(
        url = "https://piston-data.mojang.com/v1/objects/$sha/$path",
        sha1 = sha,
        size = 10,
        path = path,
    )

    private fun version(
        libraries: List<Library> = emptyList(),
        downloads: Map<String, DownloadInfo> = mapOf("client" to download()),
        assetIndex: AssetIndexRef? = AssetIndexRef(
            id = "34",
            sha1 = sha,
            size = 100,
            url = "https://piston-meta.mojang.com/v1/packages/$sha/34.json",
        ),
    ) = VersionJson(
        id = "26.3",
        mainClass = "net.minecraft.client.main.Main",
        assetIndex = assetIndex,
        downloads = downloads,
        libraries = libraries,
    )

    private fun library(
        name: String,
        path: String,
        rules: List<Rule> = emptyList(),
        natives: Map<String, String> = emptyMap(),
        classifiers: Map<String, DownloadInfo> = emptyMap(),
    ) = Library(
        name = name,
        downloads = LibraryDownloads(
            artifact = download(path),
            classifiers = classifiers,
        ),
        rules = rules,
        natives = natives,
    )

    @Test
    fun `selects Java libraries and drops desktop natives`() {
        val resolved = VersionResolver.resolveOrThrow(
            version(
                libraries = listOf(
                    library(
                        name = "com.example:client-code:1.0",
                        path = "com/example/client-code/1.0/client-code-1.0.jar",
                    ),
                    library(
                        name = "org.lwjgl:lwjgl:3.4.3",
                        path = "org/lwjgl/lwjgl/3.4.3/lwjgl-3.4.3.jar",
                        natives = mapOf("linux" to "natives-linux"),
                        classifiers = mapOf("natives-linux" to download("lwjgl-natives-linux.jar")),
                    ),
                ),
            ),
            LaunchContext(),
        )

        assertThat(resolved.libraries.map { it.coordinate.moduleKey })
            .containsExactly("com.example:client-code")
        assertThat(resolved.droppedArtifacts).hasSize(1)
        assertThat(resolved.droppedArtifacts.single().classifier).isEqualTo("natives-linux")
        assertThat(resolved.droppedArtifacts.single().kind)
            .isEqualTo(DroppedArtifactKind.NATIVE)
    }

    @Test
    fun `a native classifier in the coordinate is dropped`() {
        val resolved = VersionResolver.resolveOrThrow(
            version(
                libraries = listOf(
                    library(
                        name = "org.lwjgl:lwjgl:3.4.3:natives-linux",
                        path = "org/lwjgl/lwjgl/3.4.3/lwjgl-3.4.3-natives-linux.jar",
                        rules = listOf(Rule(action = "allow", os = OsRule(name = "linux"))),
                    ),
                ),
            ),
            LaunchContext(),
        )

        assertThat(resolved.libraries).isEmpty()
        assertThat(resolved.droppedArtifacts.single().classifier).isEqualTo("natives-linux")
    }

    @Test
    fun `the Tracy profiler is dropped as a whole`() {
        val resolved = VersionResolver.resolveOrThrow(
            version(
                libraries = listOf(
                    library(
                        name = "com.mojang:jtracy:1.14.38",
                        path = "com/mojang/jtracy/1.14.38/jtracy-1.14.38.jar",
                    ),
                ),
            ),
            LaunchContext(),
        )

        assertThat(resolved.libraries).isEmpty()
        assertThat(resolved.droppedArtifacts.single().kind)
            .isEqualTo(DroppedArtifactKind.PROFILER)
    }

    @Test
    fun `a library without rules stays on the classpath`() {
        val resolved = VersionResolver.resolveOrThrow(
            version(
                libraries = listOf(
                    library("com.example:plain:1.0", "com/example/plain/1.0/plain-1.0.jar"),
                ),
            ),
            LaunchContext(),
        )

        assertThat(resolved.libraries).hasSize(1)
    }

    @Test
    fun `a child library replaces the parent version in place`() {
        val resolved = VersionResolver.resolveOrThrow(
            version(
                libraries = listOf(
                    library("com.example:lib:1.0", "com/example/lib/1.0/lib-1.0.jar"),
                    library("com.example:other:1.0", "com/example/other/1.0/other-1.0.jar"),
                    library("com.example:lib:2.0", "com/example/lib/2.0/lib-2.0.jar"),
                ),
            ),
            LaunchContext(),
        )

        assertThat(resolved.libraries.map { it.coordinate.version })
            .containsExactly("2.0", "1.0").inOrder()
        assertThat(resolved.libraries.first().path)
            .isEqualTo("com/example/lib/2.0/lib-2.0.jar")
    }

    @Test
    fun `rules exclude a Windows-only library`() {
        val resolved = VersionResolver.resolveOrThrow(
            version(
                libraries = listOf(
                    library(
                        name = "com.example:windows:1.0",
                        path = "com/example/windows/1.0/windows-1.0.jar",
                        rules = listOf(Rule(action = "allow", os = OsRule(name = "windows"))),
                    ),
                ),
            ),
            LaunchContext(),
        )

        assertThat(resolved.libraries).isEmpty()
    }

    @Test
    fun `a version without a client download fails`() {
        val result = VersionResolver.resolve(
            version(downloads = emptyMap()),
            LaunchContext(),
        )

        assertThat(result).isInstanceOf(AppResult.Error::class.java)
        assertThat((result as AppResult.Error).failure.code)
            .isEqualTo("MOJANG_METADATA_INVALID")
    }

    @Test
    fun `a version without an asset index fails`() {
        val result = VersionResolver.resolve(version(assetIndex = null), LaunchContext())

        assertThat(result).isInstanceOf(AppResult.Error::class.java)
    }

    @Test
    fun `an artifact path that escapes the store fails`() {
        val result = VersionResolver.resolve(
            version(
                libraries = listOf(
                    library("com.example:bad:1.0", "../../outside.jar"),
                ),
            ),
            LaunchContext(),
        )

        assertThat(result).isInstanceOf(AppResult.Error::class.java)
    }

    @Test
    fun `safe relative paths reject traversal and absolute prefixes`() {
        assertThrows(MetadataException::class.java) {
            VersionResolver.safeRelativePath("../outside.jar")
        }
        assertThrows(MetadataException::class.java) {
            VersionResolver.safeRelativePath("/data/outside.jar")
        }
        assertThrows(MetadataException::class.java) {
            VersionResolver.safeRelativePath("com/example//empty.jar")
        }
        assertThat(VersionResolver.safeRelativePath("com/example/lib/1.0/lib-1.0.jar"))
            .isEqualTo("com/example/lib/1.0/lib-1.0.jar")
    }
}
