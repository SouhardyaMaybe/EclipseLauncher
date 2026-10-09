package me.shadow.eclipselauncher.utils.io;

import org.tukaani.xz.XZCompressorInputStream;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Extracts {@code .tar.xz} archives (the JRE runtime distribution format used
 * by the launcher) through the {@link org.tukaani:xz} decompressor.
 *
 * <p>The tar reader understands ustar headers, GNU long name/link entries and
 * pax {@code path}/{@code linkpath} records - everything the OpenJDK runtime
 * tarballs require. Entry types are exposed to the {@link Listener}, which
 * keeps the extraction policy (progress reporting, symlinks, skip-if-unchanged)
 * at the call site, exactly like before.</p>
 */
public final class TarXzExtractor {

    private TarXzExtractor() {
    }

    /** Receives the entries of an archive in stream order. */
    public interface Listener {
        /** Called once per entry before it is dispatched, with the entry name. */
        void onEntry(String name);

        void onDirectory(String name, File destPath) throws IOException;

        void onSymbolicLink(String name, String linkName, File destPath) throws IOException;

        /**
         * Called for regular files. The data stream yields exactly the entry
         * contents; it does not need to be closed by the listener.
         */
        void onFile(String name, long size, File destPath, InputStream data) throws IOException;
    }

    /** Decompresses the stream as xz and walks the contained tar archive. */
    public static void extract(InputStream xzInput, File dest, Listener listener) throws IOException {
        TarInput tar = new TarInput(new XZCompressorInputStream(xzInput));
        try {
            TarEntry entry;
            while ((entry = tar.nextEntry()) != null) {
                listener.onEntry(entry.name);

                File destPath = new File(dest, entry.name);
                ensureParentDirectory(destPath);

                if (entry.symbolicLink) {
                    listener.onSymbolicLink(entry.name, entry.linkName, destPath);
                } else if (entry.directory) {
                    listener.onDirectory(entry.name, destPath);
                } else {
                    listener.onFile(entry.name, entry.size, destPath, tar.dataStream());
                }
            }
        } finally {
            tar.close();
        }
    }

    private static void ensureParentDirectory(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Failed to create directory " + parent + " for " + file);
        }
    }

    /** A single tar entry header. */
    private static final class TarEntry {
        String name;
        String linkName;
        long size;
        boolean directory;
        boolean symbolicLink;
    }

    /**
     * Sequential tar reader over a decompressed stream. Entry payloads are
     * handed out as bounded streams; unread payload plus block padding is
     * skipped when the next entry is requested.
     */
    private static final class TarInput implements Closeable {

        private static final int BLOCK = 512;

        private final InputStream in;
        /** Name override produced by a preceding GNU long name or pax record. */
        private String pendingName;
        private String pendingLinkName;
        /** Bytes of the current entry's payload still to be consumed. */
        private long remaining;
        private long padding;

        TarInput(InputStream in) {
            this.in = in;
        }

        TarEntry nextEntry() throws IOException {
            skipUnreadPayload();
            byte[] header = readBlock();
            if (header == null || isZeroBlock(header)) return null;

            while (true) {
                long size = parseSize(header, 124, 12);
                int typeFlag = header[156] & 0xFF;

                if (typeFlag == 'L') {
                    // GNU long name: the payload holds the real name of the next entry
                    pendingName = readDataAsString(size);
                } else if (typeFlag == 'K') {
                    // GNU long link name: the payload holds the real link target
                    pendingLinkName = readDataAsString(size);
                } else if (typeFlag == 'x' || typeFlag == 'g') {
                    // pax extended headers with "path"/"linkpath" records
                    readPaxHeaders(size);
                } else {
                    TarEntry entry = new TarEntry();
                    String name = headerString(header, 0, 100);
                    String prefix = headerString(header, 345, 155);
                    if (!prefix.isEmpty()) name = prefix + "/" + name;
                    if (pendingName != null) {
                        name = pendingName;
                        pendingName = null;
                    }
                    String linkName = headerString(header, 157, 100);
                    if (pendingLinkName != null) {
                        linkName = pendingLinkName;
                        pendingLinkName = null;
                    }
                    entry.name = name;
                    entry.linkName = linkName;
                    entry.size = size;
                    entry.directory = typeFlag == '5';
                    entry.symbolicLink = typeFlag == '2';
                    remaining = size;
                    padding = (BLOCK - (size % BLOCK)) % BLOCK;
                    return entry;
                }

                header = readBlock();
                if (header == null || isZeroBlock(header)) {
                    throw new IOException("Truncated tar archive: extended header without a following entry");
                }
            }
        }

        /** Stream over the payload of the entry returned by the last {@link #nextEntry()}. */
        InputStream dataStream() {
            return new EntryData();
        }

        @Override
        public void close() throws IOException {
            in.close();
        }

        private void skipUnreadPayload() throws IOException {
            if (remaining > 0) {
                long skipped = 0;
                while (skipped < remaining) {
                    long s = in.skip(remaining - skipped);
                    if (s <= 0) {
                        if (in.read() == -1) throw new IOException("Unexpected end of tar entry");
                        s = 1;
                    }
                    skipped += s;
                }
                remaining = 0;
            }
            skipFully(padding);
            padding = 0;
        }

        private void skipFully(long count) throws IOException {
            long skipped = 0;
            while (skipped < count) {
                long s = in.skip(count - skipped);
                if (s <= 0) {
                    if (in.read() == -1) throw new IOException("Unexpected end of tar archive");
                    s = 1;
                }
                skipped += s;
            }
        }

        private byte[] readBlock() throws IOException {
            byte[] block = new byte[BLOCK];
            int read = 0;
            while (read < BLOCK) {
                int r = in.read(block, read, BLOCK - read);
                if (r == -1) {
                    // A partial block at the end of the stream is treated as the end of the archive
                    return null;
                }
                read += r;
            }
            return block;
        }

        private boolean isZeroBlock(byte[] block) {
            for (byte b : block) {
                if (b != 0) return false;
            }
            return true;
        }

        private String readDataAsString(long size) throws IOException {
            byte[] data = readData(size);
            int length = data.length;
            while (length > 0 && (data[length - 1] == 0 || data[length - 1] == '\n')) {
                length--;
            }
            return new String(data, 0, length, StandardCharsets.UTF_8);
        }

        private void readPaxHeaders(long size) throws IOException {
            byte[] data = readData(size);
            int offset = 0;
            while (offset < data.length) {
                int space = offset;
                while (space < data.length && data[space] != ' ') space++;
                if (space == data.length) break;
                int recordLength;
                try {
                    recordLength = Integer.parseInt(new String(data, offset, space - offset, StandardCharsets.UTF_8));
                } catch (NumberFormatException e) {
                    break;
                }
                int end = offset + recordLength;
                if (end > data.length || recordLength <= 0) break;
                String record = new String(data, space + 1, end - space - 2, StandardCharsets.UTF_8);
                int equals = record.indexOf('=');
                if (equals > 0) {
                    String key = record.substring(0, equals);
                    String value = record.substring(equals + 1);
                    if ("path".equals(key)) pendingName = value;
                    else if ("linkpath".equals(key)) pendingLinkName = value;
                }
                offset = end;
            }
        }

        private byte[] readData(long size) throws IOException {
            byte[] data = new byte[(int) size];
            int read = 0;
            while (read < size) {
                int r = in.read(data, read, (int) (size - read));
                if (r == -1) throw new IOException("Unexpected end of tar entry");
                read += r;
            }
            skipFully((BLOCK - (size % BLOCK)) % BLOCK);
            return data;
        }

        private static String headerString(byte[] header, int offset, int length) {
            int end = offset;
            int limit = offset + length;
            while (end < limit && header[end] != 0) end++;
            return new String(header, offset, end - offset, StandardCharsets.UTF_8);
        }

        private static long parseSize(byte[] header, int offset, int length) throws IOException {
            // GNU base-256 encoding for values that do not fit the octal field
            if ((header[offset] & 0x80) != 0) {
                long value = header[offset] & 0x7F;
                for (int i = offset + 1; i < offset + length; i++) {
                    value = (value << 8) | (header[i] & 0xFF);
                }
                return value;
            }
            int end = offset;
            int limit = offset + length;
            while (end < limit && header[end] != 0 && header[end] != ' ') end++;
            if (end == offset) return 0;
            try {
                return Long.parseLong(new String(header, offset, end - offset, StandardCharsets.UTF_8).trim(), 8);
            } catch (NumberFormatException e) {
                throw new IOException("Invalid tar entry size field: "
                        + Arrays.toString(Arrays.copyOfRange(header, offset, offset + length)), e);
            }
        }

        /** Bounded view of the current entry payload, decrementing the shared counter. */
        private final class EntryData extends InputStream {

            @Override
            public int read() throws IOException {
                if (remaining <= 0) return -1;
                int b = in.read();
                if (b == -1) throw new IOException("Unexpected end of tar entry");
                remaining--;
                return b;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (remaining <= 0) return -1;
                int toRead = (int) Math.min(len, remaining);
                int r = in.read(b, off, toRead);
                if (r == -1) throw new IOException("Unexpected end of tar entry");
                remaining -= r;
                return r;
            }

            @Override
            public void close() {
                // Unread payload is skipped when the next entry is requested
            }
        }
    }
}
