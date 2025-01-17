package com.chinamobile.cmos.PduParser;


import java.io.ByteArrayOutputStream;

public class QuotedPrintable {
    private static byte ESCAPE_CHAR = '=';

    /**
     * Decodes an array quoted-printable characters into an array of original bytes.
     * Escaped characters are converted back to their original representation.
     *
     * <p>
     * This function implements a subset of
     * quoted-printable encoding specification (rule #1 and rule #2)
     * as defined in RFC 1521.
     * </p>
     *
     * @param bytes array of quoted-printable characters
     * @return array of original bytes,
     *         null if quoted-printable decoding is unsuccessful.
     */
    public static final byte[] decodeQuotedPrintable(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i];
            if (b == ESCAPE_CHAR) {
                try {
                    if('\r' == (char)bytes[i + 1] &&
                            '\n' == (char)bytes[i + 2]) {
                        i += 2;
                        continue;
                    }
                    int u = Character.digit((char) bytes[++i], 16);
                    int l = Character.digit((char) bytes[++i], 16);
                    if (u == -1 || l == -1) {
                        return null;
                    }
                    buffer.write((char) ((u << 4) + l));
                } catch (ArrayIndexOutOfBoundsException e) {
                    return null;
                }
            } else {
                buffer.write(b);
            }
        }
        return buffer.toByteArray();
    }
}
