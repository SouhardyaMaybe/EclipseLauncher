package me.shadow.eclipselauncher.utils.io

import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/** SHA-1 digests over streams, used to verify downloaded files. */
object Digest {

    /** Computes the SHA-1 digest of the whole stream; the stream is not closed. */
    @JvmStatic
    @Throws(IOException::class)
    fun sha1(input: InputStream): ByteArray {
        val digest = try {
            MessageDigest.getInstance("SHA-1")
        } catch (e: NoSuchAlgorithmException) {
            throw IOException("SHA-1 is not available", e)
        }
        val buffer = ByteArray(8192)
        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            digest.update(buffer, 0, read)
        }
        return digest.digest()
    }

    /** Computes the lower-case hex SHA-1 of the whole stream; the stream is not closed. */
    @JvmStatic
    @Throws(IOException::class)
    fun sha1Hex(input: InputStream): String {
        return String(Hex.encodeHex(sha1(input)))
    }
}
