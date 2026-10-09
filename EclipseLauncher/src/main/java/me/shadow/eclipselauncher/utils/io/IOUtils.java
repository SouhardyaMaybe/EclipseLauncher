package me.shadow.eclipselauncher.utils.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

/** Stream helpers: bulk copy, full reads and text writes. */
public final class IOUtils {

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private IOUtils() {
    }

    /** Copies the stream contents with an internal 8 KiB buffer; neither stream is closed. */
    public static int copy(InputStream input, OutputStream output) throws IOException {
        long copied = copyLarge(input, output, new byte[DEFAULT_BUFFER_SIZE]);
        return copied > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) copied;
    }

    /** Copies the stream contents with an internal 8 KiB buffer; neither stream is closed. */
    public static long copyLarge(InputStream input, OutputStream output) throws IOException {
        return copyLarge(input, output, new byte[DEFAULT_BUFFER_SIZE]);
    }

    /** Copies the stream contents with the caller's buffer; neither stream is closed. */
    public static long copyLarge(InputStream input, OutputStream output, byte[] buffer) throws IOException {
        if (buffer == null || buffer.length == 0) {
            throw new IllegalArgumentException("Buffer must not be null or empty");
        }
        long total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
            total += read;
        }
        return total;
    }

    /** Reads the whole stream with the given charset; the stream is not closed. */
    public static String toString(InputStream input, Charset charset) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        copy(input, output);
        return output.toString(charset.name());
    }

    /** Writes the text with the platform default charset; the stream is not closed. */
    public static void write(String data, OutputStream output) throws IOException {
        write(data, output, Charset.defaultCharset());
    }

    public static void write(String data, OutputStream output, Charset charset) throws IOException {
        if (data != null) output.write(data.getBytes(charset));
    }
}
