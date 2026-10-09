package me.shadow.eclipselauncher.utils.io;

/** Lower-case hexadecimal encoding of byte arrays. */
public final class Hex {

    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    private Hex() {
    }

    /** Encodes the data as a lower-case hex character array (two chars per byte). */
    public static char[] encodeHex(byte[] data) {
        char[] out = new char[data.length * 2];
        for (int i = 0; i < data.length; i++) {
            int value = data[i] & 0xFF;
            out[i * 2] = HEX_DIGITS[value >>> 4];
            out[i * 2 + 1] = HEX_DIGITS[value & 0x0F];
        }
        return out;
    }
}
