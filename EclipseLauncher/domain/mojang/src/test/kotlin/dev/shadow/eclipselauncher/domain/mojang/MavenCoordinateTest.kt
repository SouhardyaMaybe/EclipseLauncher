/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.domain.mojang

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class MavenCoordinateTest {
    @Test
    fun `parses a plain coordinate`() {
        val coordinate = MavenCoordinate.parse("org.lwjgl:lwjgl:3.4.3")

        assertThat(coordinate.group).isEqualTo("org.lwjgl")
        assertThat(coordinate.artifact).isEqualTo("lwjgl")
        assertThat(coordinate.version).isEqualTo("3.4.3")
        assertThat(coordinate.classifier).isNull()
        assertThat(coordinate.extension).isEqualTo("jar")
        assertThat(coordinate.moduleKey).isEqualTo("org.lwjgl:lwjgl")
    }

    @Test
    fun `parses a classifier and an explicit extension`() {
        val coordinate = MavenCoordinate.parse(
            "org.lwjgl:lwjgl:3.4.3:natives-linux@zip",
        )

        assertThat(coordinate.classifier).isEqualTo("natives-linux")
        assertThat(coordinate.extension).isEqualTo("zip")
        assertThat(coordinate.fileName).isEqualTo("lwjgl-3.4.3-natives-linux.zip")
    }

    @Test
    fun `builds the Maven repository path`() {
        val coordinate = MavenCoordinate.parse("net.fabricmc:fabric-loader:0.20.1")

        assertThat(coordinate.relativePath)
            .isEqualTo("net/fabricmc/fabric-loader/0.20.1/fabric-loader-0.20.1.jar")
    }

    @Test
    fun `rejects a traversal segment`() {
        assertThrows(IllegalArgumentException::class.java) {
            MavenCoordinate.parse("org.example:../../etc/passwd:1.0")
        }
    }

    @Test
    fun `rejects an absolute path`() {
        assertThrows(IllegalArgumentException::class.java) {
            MavenCoordinate.parse("/org.example:thing:1.0")
        }
    }

    @Test
    fun `rejects an empty group segment`() {
        assertThrows(IllegalArgumentException::class.java) {
            MavenCoordinate.parse("org..example:thing:1.0")
        }
    }

    @Test
    fun `rejects a coordinate with too few parts`() {
        assertThrows(IllegalArgumentException::class.java) {
            MavenCoordinate.parse("org.example:thing")
        }
    }
}
