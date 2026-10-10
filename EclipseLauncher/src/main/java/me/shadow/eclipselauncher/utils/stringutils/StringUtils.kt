package me.shadow.eclipselauncher.utils.stringutils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Base64
import android.widget.Toast

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

import me.shadow.eclipselauncher.R

import org.commonmark.node.Node
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Objects
import java.util.StringJoiner
import java.util.TimeZone

object StringUtils {

    @JvmStatic
    fun insertSpace(prefixString: Any?, vararg suffixString: Any?): String =
        insertSpace(prefixString?.toString(), *suffixString.map { Objects.requireNonNull(it).toString() }
            .toTypedArray())

    /**
     * Insert spaces between strings
     *
     * @param prefixString the first string
     * @param suffixString the following strings
     * @return the string with spaces inserted, "string1 string2 string3"
     */
    @JvmStatic
    fun insertSpace(prefixString: String?, vararg suffixString: String): String {
        return insertString(" ", prefixString, *suffixString)
    }

    @JvmStatic
    fun insertNewline(prefixString: Any?, vararg suffixString: Any?): String =
        insertNewline(prefixString?.toString(), *suffixString.map { Objects.requireNonNull(it).toString() }
            .toTypedArray())

    /**
     * Insert newlines between strings
     *
     * @param prefixString the first string
     * @param suffixString the following strings
     * @return the string with newlines inserted
     */
    @JvmStatic
    fun insertNewline(prefixString: String?, vararg suffixString: String): String {
        return insertString("\r\n", prefixString, *suffixString)
    }

    @JvmStatic
    fun insertString(stringToInsert: String, prefixString: String?, vararg suffixString: String): String {
        val stringJoiner = StringJoiner(stringToInsert)
        if (prefixString != null) {
            stringJoiner.add(prefixString)
        }
        for (string in suffixString) {
            stringJoiner.add(string)
        }

        return stringJoiner.toString()
    }

    /**
     * @return checks whether the string is null, returning "" if it is, otherwise returns the string itself
     */
    @JvmStatic
    fun getStringNotNull(string: String?): String {
        return if (string == null) "" else string
    }

    @JvmStatic
    fun formattingTime(time: String): String {
        val t = time.indexOf('T')
        val z = time.indexOf('Z')
        if (t == -1 || z == -1) return time
        return insertSpace(time.substring(0, t), time.substring(t + 1, z))
    }

    @JvmStatic
    fun formatDate(date: Date, locale: Locale, timeZone: TimeZone): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", locale)
        formatter.timeZone = timeZone
        return formatter.format(date)
    }

    @JvmStatic
    fun markdownToHtml(markdown: String): String {
        val document: Node = Parser.builder().build().parse(markdown)
        val renderer = HtmlRenderer.builder().build()
        return renderer.render(document)
    }

    @JvmStatic
    fun copyText(label: String, text: String, context: Context) {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(ClipData.newPlainText(label, text))
        // The toast was shown on the UI thread before; Dispatchers.Main does the same.
        copyScope.launch {
            Toast.makeText(context, context.getString(R.string.generic_copied), Toast.LENGTH_SHORT).show()
        }
    }

    private val copyScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main + CoroutineName("StringUtils-copyText"))

    @JvmStatic
    fun decodeBase64(rawValue: String): String {
        val decodedBytes = Base64.decode(rawValue, Base64.DEFAULT)
        return String(decodedBytes, StandardCharsets.UTF_8)
    }
}
