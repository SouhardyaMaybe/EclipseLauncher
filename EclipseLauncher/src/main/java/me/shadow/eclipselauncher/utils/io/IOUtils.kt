package me.shadow.eclipselauncher.utils.io

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset

/** Stream helpers: bulk copy, full reads and text writes. */
object IOUtils {

    private const val DEFAULT_BUFFER_SIZE = 8192

    /** Copies the stream contents with an internal 8 KiB buffer; neither stream is closed. */
    @JvmStatic
    @Throws(IOException::class)
    fun copy(input: InputStream, output: OutputStream): Int {
        val copied = copyLarge(input, output, ByteArray(DEFAULT_BUFFER_SIZE))
        return if (copied > Int.MAX_VALUE) Int.MAX_VALUE else copied.toInt()
    }

    /** Copies the stream contents with an internal 8 KiB buffer; neither stream is closed. */
    @JvmStatic
    @Throws(IOException::class)
    fun copyLarge(input: InputStream, output: OutputStream): Long {
        return copyLarge(input, output, ByteArray(DEFAULT_BUFFER_SIZE))
    }

    /** Copies the stream contents with the caller's buffer; neither stream is closed. */
    @JvmStatic
    @Throws(IOException::class)
    fun copyLarge(input: InputStream, output: OutputStream, buffer: ByteArray?): Long {
        if (buffer == null || buffer.isEmpty()) {
            throw IllegalArgumentException("Buffer must not be null or empty")
        }
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            output.write(buffer, 0, read)
            total += read
        }
        return total
    }

    /** Reads the whole stream with the given charset; the stream is not closed. */
    @JvmStatic
    @Throws(IOException::class)
    fun toString(input: InputStream, charset: Charset): String {
        val output = ByteArrayOutputStream()
        copy(input, output)
        return output.toString(charset.name())
    }

    /** Writes the text with the platform default charset; the stream is not closed. */
    @JvmStatic
    @Throws(IOException::class)
    fun write(data: String?, output: OutputStream) {
        write(data, output, Charset.defaultCharset())
    }

    @JvmStatic
    @Throws(IOException::class)
    fun write(data: String?, output: OutputStream, charset: Charset) {
        if (data != null) output.write(data.toByteArray(charset))
    }
}
