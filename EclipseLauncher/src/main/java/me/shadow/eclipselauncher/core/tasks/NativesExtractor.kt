package me.shadow.eclipselauncher.core.tasks

import me.shadow.eclipselauncher.core.Architecture
import me.shadow.eclipselauncher.core.utils.FileUtils
import me.shadow.eclipselauncher.utils.path.PathManager
import java.io.File
import java.io.FileInputStream
import java.io.FilterInputStream
import java.io.InputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class NativesExtractor(private val mDestinationDir: File) {
    private val mLibraryLocation = "jni/" + getAarArchitectureName() + "/"

    /**
     * Extract the native libraries of the current device architecture out of an AAR archive.
     * Blocking file I/O; runs inside the caller's coroutine.
     */
    fun extractFromAar(source: File) {
        val buffer = ByteArray(8192)
        FileInputStream(source).use { fileInputStream ->
            ZipInputStream(fileInputStream).use { zipInputStream ->
                // Wrap the ZIP input stream into a non-closeable stream to
                // avoid it being closed by processEntry()
                val entryCopyStream = NonCloseableInputStream(zipInputStream)
                while (true) {
                    val entry = zipInputStream.nextEntry ?: break
                    if (!entry.name.startsWith(mLibraryLocation) || entry.isDirectory) continue
                    // Entry name is actually the full path, so we need to strip the path before extraction
                    val entryName = FileUtils.getFileName(entry.name)
                    // getFileName may make the file name null, avoid that case.
                    if (entryName == null || LIBRARY_BLACKLIST.contains(entryName)) continue

                    processEntry(entryCopyStream, entry, File(mDestinationDir, entryName), buffer)
                }
            }
        }
    }

    private fun processEntry(sourceStream: InputStream, zipEntry: ZipEntry, entryDestination: File, buffer: ByteArray) {
        if (entryDestination.exists()) {
            val expectedSize = zipEntry.size
            val expectedCrc32 = zipEntry.crc
            val realSize = entryDestination.length()
            val realCrc32 = fileCrc32(entryDestination, buffer)
            // File in archive is the same as the local one, don't extract
            if (realSize == expectedSize && realCrc32 == expectedCrc32) return
        }
        // copyInputStreamToFile copies the stream to a file and then closes it.
        me.shadow.eclipselauncher.utils.io.FileUtils.copyInputStreamToFile(sourceStream, entryDestination)
    }

    private class NonCloseableInputStream(inputStream: InputStream) : FilterInputStream(inputStream) {
        override fun close() {
            // Do nothing (the point of this class)
        }
    }

    companion object {
        private val LIBRARY_BLACKLIST = createLibraryBlacklist()

        /**
         * Create a library blacklist so that downloaded natives are not able to
         * override built-in libraries.
         * @return the resulting blacklist of library file names
         */
        private fun createLibraryBlacklist(): ArrayList<String> {
            val includedLibraryNames = File(PathManager.DIR_NATIVE_LIB).list()
            val blacklist = ArrayList<String>(includedLibraryNames.size)
            for (libraryName in includedLibraryNames) {
                // allow overriding jnidispatch (as the integrated version may be too old)
                if (libraryName == "libjnidispatch.so") continue
                blacklist.add(libraryName)
            }
            blacklist.trimToSize()
            return blacklist
        }

        private fun getAarArchitectureName(): String {
            val architecture = Architecture.getDeviceArchitecture()
            return when (architecture) {
                Architecture.ARCH_ARM -> "armeabi-v7a"
                Architecture.ARCH_ARM64 -> "arm64-v8a"
                Architecture.ARCH_X86 -> "x86"
                Architecture.ARCH_X86_64 -> "x86_64"
                else -> throw RuntimeException("Unknown CPU architecture: $architecture")
            }
        }

        private fun fileCrc32(target: File, buffer: ByteArray): Long {
            val crc32 = CRC32()
            FileInputStream(target).use { fileInputStream ->
                while (true) {
                    val len = fileInputStream.read(buffer)
                    if (len == -1) break
                    crc32.update(buffer, 0, len)
                }
            }
            return crc32.value
        }
    }
}
