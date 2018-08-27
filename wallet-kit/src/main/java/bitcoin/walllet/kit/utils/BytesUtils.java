package bitcoin.walllet.kit.utils;

import org.spongycastle.util.Arrays;

public class BytesUtils {

    /**
     * Is byte array ALL zeros?
     */
    public static boolean isZeros(byte[] bs) {
        for (byte b : bs) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Join two byte arrays to a new byte array.
     */
    public static byte[] concat(byte[] buf1, byte[] buf2) {
        byte[] buffer = new byte[buf1.length + buf2.length];
        int offset = 0;
        System.arraycopy(buf1, 0, buffer, offset, buf1.length);
        offset += buf1.length;
        System.arraycopy(buf2, 0, buffer, offset, buf2.length);
        return buffer;
    }

    /**
     * Join three byte arrays to a new byte array.
     */
    public static byte[] concat(byte[] buf1, byte[] buf2, byte[] buf3) {
        byte[] buffer = new byte[buf1.length + buf2.length + buf3.length];
        int offset = 0;
        System.arraycopy(buf1, 0, buffer, offset, buf1.length);
        offset += buf1.length;
        System.arraycopy(buf2, 0, buffer, offset, buf2.length);
        offset += buf2.length;
        System.arraycopy(buf3, 0, buffer, offset, buf3.length);
        return buffer;
    }

    /**
     * Is array equals? (length equals and every byte equals)
     */
    public static boolean equals(byte[] b1, byte[] b2) {
        if (b1 == null || b2 == null) {
            throw new IllegalArgumentException("one of the arguments is null");
        }
        if (b1.length != b2.length) {
            return false;
        }
        for (int i = 0; i < b1.length; i++) {
            if (b1[i] != b2[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Reverse the byte array. Return new reversed array.
     */
    public static byte[] reverse(byte[] msgHash) {
        return Arrays.reverse(msgHash);
    }

    /**
     * Convert int to 4 bytes.
     *
     * @param value Int value.
     * @return 4 bytes.
     */
    public static byte[] intToByteArray(int value) {
        return new byte[]{(byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) value};
    }

    /**
     * Convert 4 bytes to int.
     *
     * @param bs Byte array with length of 4.
     * @return int value.
     */
    public static int bytesToInt(byte[] bs) {
        if (bs == null || bs.length != 4) {
            throw new IllegalArgumentException("Invalid bytes.");
        }
        return bs[0] << 24 | (bs[1] & 0xff) << 16 | (bs[2] & 0xff) << 8 | (bs[3] & 0xff);
    }
}
