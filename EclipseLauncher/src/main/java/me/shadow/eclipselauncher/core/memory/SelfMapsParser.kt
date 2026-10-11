package me.shadow.eclipselauncher.core.memory

import java.io.FileInputStream
import java.io.IOException
import java.util.Scanner

class SelfMapsParser(private val mCallback: Callback) {

    @Throws(IOException::class, NumberFormatException::class)
    fun run() {
        // Like the original try-with-resources: only the stream is managed here,
        // the Scanner wrapping it is left alone exactly like before.
        FileInputStream("/proc/self/maps").use { fileInputStream ->
            val scanner = Scanner(fileInputStream)
            while (scanner.hasNextLine()) {
                if (!forEachLine(scanner.nextLine())) break
            }
        }
    }

    private fun forEachLine(line: String): Boolean {
        val firstSpaceIndex = line.indexOf(' ')
        val addresses = line.substring(0, firstSpaceIndex)
        val addressArray = addresses.split("-")
        if (addressArray.size < 2) return true
        val begin = addressArray[0].toLong(16)
        val end = addressArray[1].toLong(16)
        return mCallback.process(begin, end, line)
    }

    interface Callback {
        fun process(startAddress: Long, endAddress: Long, wholeLine: String): Boolean
    }
}
