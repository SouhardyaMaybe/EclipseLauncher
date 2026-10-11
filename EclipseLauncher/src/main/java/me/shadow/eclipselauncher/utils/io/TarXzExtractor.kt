package me.shadow.eclipselauncher.utils.io

import org.tukaani.xz.XZInputStream

import java.io.Closeable
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.Arrays

/**
 * Extracts ".tar.xz" archives (the JRE runtime distribution format used
 * by the launcher) through the org.tukaani:xz decompressor.
 *
 * The tar reader understands ustar headers, GNU long name/link entries and
 * pax "path"/"linkpath" records - everything the OpenJDK runtime
 * tarballs require. Entry types are exposed to the [Listener], which
 * keeps the extraction policy (progress reporting, symlinks, skip-if-unchanged)
 * at the call site, exactly like before.
 */
object TarXzExtractor {

    /** Receives the entries of an archive in stream order. */
    interface Listener {
        /** Called once per entry before it is dispatched, with the entry name. */
        fun onEntry(name: String)

        @Throws(IOException::class)
        fun onDirectory(name: String, destPath: File)

        @Throws(IOException::class)
        fun onSymbolicLink(name: String, linkName: String, destPath: File)

        /**
         * Called for regular files. The data stream yields exactly the entry
         * contents; it does not need to be closed by the listener.
         */
        @Throws(IOException::class)
        fun onFile(name: String, size: Long, destPath: File, data: InputStream)
    }

    /** Decompresses the stream as xz and walks the contained tar archive. */
    @JvmStatic
    @Throws(IOException::class)
    fun extract(xzInput: InputStream, dest: File, listener: Listener) {
        val tar = TarInput(XZInputStream(xzInput))
        try {
            while (true) {
                val entry = tar.nextEntry() ?: break
                listener.onEntry(entry.name)

                val destPath = File(dest, entry.name)
                ensureParentDirectory(destPath)

                if (entry.symbolicLink) {
                    listener.onSymbolicLink(entry.name, entry.linkName, destPath)
                } else if (entry.directory) {
                    listener.onDirectory(entry.name, destPath)
                } else {
                    listener.onFile(entry.name, entry.size, destPath, tar.dataStream())
                }
            }
        } finally {
            tar.close()
        }
    }

    @Throws(IOException::class)
    private fun ensureParentDirectory(file: File) {
        val parent = file.parentFile
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw IOException("Failed to create directory $parent for $file")
        }
    }

    /** A single tar entry header. */
    private class TarEntry {
        var name: String = ""
        var linkName: String = ""
        var size = 0L
        var directory = false
        var symbolicLink = false
    }

    /**
     * Sequential tar reader over a decompressed stream. Entry payloads are
     * handed out as bounded streams; unread payload plus block padding is
     * skipped when the next entry is requested.
     */
    private class TarInput(private val input: InputStream) : Closeable {

        /** Name override produced by a preceding GNU long name or pax record. */
        private var pendingName: String? = null
        private var pendingLinkName: String? = null
        /** Bytes of the current entry's payload still to be consumed. */
        private var remaining = 0L
        private var padding = 0L

        @Throws(IOException::class)
        fun nextEntry(): TarEntry? {
            skipUnreadPayload()
            val firstBlock = readBlock() ?: return null
            if (isZeroBlock(firstBlock)) return null
            var header = firstBlock

            while (true) {
                val size = parseSize(header, 124, 12)
                val typeFlag = header[156].toInt() and 0xFF

                if (typeFlag == 'L'.code) {
                    // GNU long name: the payload holds the real name of the next entry
                    pendingName = readDataAsString(size)
                } else if (typeFlag == 'K'.code) {
                    // GNU long link name: the payload holds the real link target
                    pendingLinkName = readDataAsString(size)
                } else if (typeFlag == 'x'.code || typeFlag == 'g'.code) {
                    // pax extended headers with "path"/"linkpath" records
                    readPaxHeaders(size)
                } else {
                    val entry = TarEntry()
                    var name = headerString(header, 0, 100)
                    val prefix = headerString(header, 345, 155)
                    if (prefix.isNotEmpty()) name = prefix + "/" + name
                    val overrideName = pendingName
                    if (overrideName != null) {
                        name = overrideName
                        pendingName = null
                    }
                    var linkName = headerString(header, 157, 100)
                    val overrideLinkName = pendingLinkName
                    if (overrideLinkName != null) {
                        linkName = overrideLinkName
                        pendingLinkName = null
                    }
                    entry.name = name
                    entry.linkName = linkName
                    entry.size = size
                    entry.directory = typeFlag == '5'.code
                    entry.symbolicLink = typeFlag == '2'.code
                    remaining = size
                    padding = (BLOCK - (size % BLOCK)) % BLOCK
                    return entry
                }

                val nextBlock = readBlock()
                if (nextBlock == null || isZeroBlock(nextBlock)) {
                    throw IOException("Truncated tar archive: extended header without a following entry")
                }
                header = nextBlock
            }
        }

        /** Stream over the payload of the entry returned by the last [nextEntry]. */
        fun dataStream(): InputStream {
            return EntryData()
        }

        @Throws(IOException::class)
        override fun close() {
            input.close()
        }

        @Throws(IOException::class)
        private fun skipUnreadPayload() {
            if (remaining > 0) {
                var skipped = 0L
                while (skipped < remaining) {
                    var s = input.skip(remaining - skipped)
                    if (s <= 0) {
                        if (input.read() == -1) throw IOException("Unexpected end of tar entry")
                        s = 1
                    }
                    skipped += s
                }
                remaining = 0
            }
            skipFully(padding)
            padding = 0
        }

        @Throws(IOException::class)
        private fun skipFully(count: Long) {
            var skipped = 0L
            while (skipped < count) {
                var s = input.skip(count - skipped)
                if (s <= 0) {
                    if (input.read() == -1) throw IOException("Unexpected end of tar archive")
                    s = 1
                }
                skipped += s
            }
        }

        @Throws(IOException::class)
        private fun readBlock(): ByteArray? {
            val block = ByteArray(BLOCK)
            var read = 0
            while (read < BLOCK) {
                val r = input.read(block, read, BLOCK - read)
                if (r == -1) {
                    // A partial block at the end of the stream is treated as the end of the archive
                    return null
                }
                read += r
            }
            return block
        }

        private fun isZeroBlock(block: ByteArray): Boolean {
            for (b in block) {
                if (b.toInt() != 0) return false
            }
            return true
        }

        @Throws(IOException::class)
        private fun readDataAsString(size: Long): String {
            val data = readData(size)
            var length = data.size
            while (length > 0 && (data[length - 1].toInt() == 0 || data[length - 1].toInt() == '\n'.code)) {
                length--
            }
            return String(data, 0, length, StandardCharsets.UTF_8)
        }

        @Throws(IOException::class)
        private fun readPaxHeaders(size: Long) {
            val data = readData(size)
            var offset = 0
            while (offset < data.size) {
                var scan = offset
                while (scan < data.size && data[scan].toInt() != ' '.code) scan++
                if (scan == data.size) break
                val recordLength: Int
                try {
                    recordLength = java.lang.Integer.parseInt(String(data, offset, scan - offset, StandardCharsets.UTF_8))
                } catch (e: NumberFormatException) {
                    break
                }
                val end = offset + recordLength
                if (end > data.size || recordLength <= 0) break
                val record = String(data, scan + 1, end - scan - 2, StandardCharsets.UTF_8)
                val equals = record.indexOf('=')
                if (equals > 0) {
                    val key = record.substring(0, equals)
                    val value = record.substring(equals + 1)
                    if ("path" == key) pendingName = value
                    else if ("linkpath" == key) pendingLinkName = value
                }
                offset = end
            }
        }

        @Throws(IOException::class)
        private fun readData(size: Long): ByteArray {
            val data = ByteArray(size.toInt())
            var read = 0
            while (read < size) {
                val r = input.read(data, read.toInt(), (size - read).toInt())
                if (r == -1) throw IOException("Unexpected end of tar entry")
                read += r
            }
            skipFully((BLOCK - (size % BLOCK)) % BLOCK)
            return data
        }

        /** Bounded view of the current entry payload, decrementing the shared counter. */
        private inner class EntryData : InputStream() {

            @Throws(IOException::class)
            override fun read(): Int {
                if (remaining <= 0) return -1
                val b = input.read()
                if (b == -1) throw IOException("Unexpected end of tar entry")
                remaining--
                return b
            }

            @Throws(IOException::class)
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                if (remaining <= 0) return -1
                val toRead = Math.min(len.toLong(), remaining).toInt()
                val r = input.read(b, off, toRead)
                if (r == -1) throw IOException("Unexpected end of tar entry")
                remaining -= r
                return r
            }

            override fun close() {
                // Unread payload is skipped when the next entry is requested
            }
        }

        companion object {
            private const val BLOCK = 512

            private fun headerString(header: ByteArray, offset: Int, length: Int): String {
                var end = offset
                val limit = offset + length
                while (end < limit && header[end].toInt() != 0) end++
                return String(header, offset, end - offset, StandardCharsets.UTF_8)
            }

            @Throws(IOException::class)
            private fun parseSize(header: ByteArray, offset: Int, length: Int): Long {
                // GNU base-256 encoding for values that do not fit the octal field
                if ((header[offset].toInt() and 0x80) != 0) {
                    var value = (header[offset].toInt() and 0x7F).toLong()
                    for (i in offset + 1 until offset + length) {
                        value = (value shl 8) or (header[i].toInt() and 0xFF).toLong()
                    }
                    return value
                }
                var end = offset
                val limit = offset + length
                while (end < limit && header[end].toInt() != 0 && header[end].toInt() != ' '.code) end++
                if (end == offset) return 0
                try {
                    return java.lang.Long.parseLong(String(header, offset, end - offset, StandardCharsets.UTF_8).trim(), 8)
                } catch (e: NumberFormatException) {
                    throw IOException("Invalid tar entry size field: " +
                            Arrays.toString(Arrays.copyOfRange(header, offset, offset + length)), e)
                }
            }
        }
    }
}
