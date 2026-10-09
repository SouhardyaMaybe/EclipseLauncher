package me.shadow.eclipselauncher.utils.io;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** SHA-1 digests over streams, used to verify downloaded files. */
public final class Digest {

    private Digest() {
    }

    /** Computes the SHA-1 digest of the whole stream; the stream is not closed. */
    public static byte[] sha1(InputStream input) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-1 is not available", e);
        }
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
            digest.update(buffer, 0, read);
        }
        return digest.digest();
    }

    /** Computes the lower-case hex SHA-1 of the whole stream; the stream is not closed. */
    public static String sha1Hex(InputStream input) throws IOException {
        return new String(Hex.encodeHex(sha1(input)));
    }
}
