package bitcoin.walllet.kit.common.util;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.spongycastle.util.Arrays;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import bitcoin.walllet.kit.common.Digest1;

//import org.bouncycastle.jcajce.provider.digest.RIPEMD160;

public class HashUtils {

    static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    /**
     * Get RipeMD160 hash.
     */
    public static byte[] ripeMd160(byte[] input) {
        // MessageDigest digest = new RIPEMD160.Digest1();
        // digest.update(input);
        // return digest.digest();

        return Digest1.SHA256RIPEMD160(input);

        // MessageDigest d = new RIPEMD160Digest();
        // d.update(input, 0, input.length);
        // return d;
    }

    /**
     * Get SHA-256 hash.
     */
    public static byte[] sha256(byte[] input) {
        Digest d = new SHA256Digest();
        d.update(input, 0, input.length);
        byte[] out = new byte[d.getDigestSize()];
        d.doFinal(out, 0);
        return out;
    }

    /**
     * Get double SHA-256 hash.
     */
    public static byte[] doubleSha256(byte[] input) {
        byte[] round1 = sha256(input);
        return sha256(round1);
    }

    /**
     * Convert byte array to hex string.
     */
    public static String toHexString(byte[] b) {
        return toHexString(b, false);
    }

    public static String toHexString(byte[] b, boolean sep) {
        StringBuilder sb = new StringBuilder(b.length << 2);
        for (byte x : b) {
            int hi = (x & 0xf0) >> 4;
            int lo = x & 0x0f;
            sb.append(HEX_CHARS[hi]);
            sb.append(HEX_CHARS[lo]);
            if (sep) {
                sb.append(' ');
            }
        }
        return sb.toString().trim();
    }

    /**
     * Convert byte array (little endian) to hex string.
     */
    public static String toHexStringAsLittleEndian(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length << 2);
        for (int i = b.length - 1; i >= 0; i--) {
            byte x = b[i];
            int hi = (x & 0xf0) >> 4;
            int lo = x & 0x0f;
            sb.append(HEX_CHARS[hi]);
            sb.append(HEX_CHARS[lo]);
        }
        return sb.toString();
    }

    public static byte[] toBytesAsLittleEndian(String hash) {
        byte[] r = toBytes(hash);
        return Arrays.reverse(r);
    }

    public static byte[] toBytes(String hash) {
        if (hash.length() % 2 == 1) {
            throw new IllegalArgumentException("Invalid hash length.");
        }
        byte[] data = new byte[hash.length() / 2];
        for (int i = 0; i < data.length; i++) {
            char c1 = hash.charAt(2 * i);
            char c2 = hash.charAt(2 * i + 1);
            int n1 = char2int(c1);
            int n2 = char2int(c2);
            int n = n1 << 4 | n2;
            data[i] = (byte) n;
        }
        return data;
    }

    public static String hmacSha1(String data, String key) {
        SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1");
        Mac mac = null;
        try {
            mac = Mac.getInstance("HmacSHA1");
            mac.init(signingKey);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
        byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return rawHmac.toString(); // remove
        // return Base64.getEncoder().encodeToString(rawHmac);
    }

    /**
     * Get Hmac-SHA512 of data using specified key.
     *
     * @param data Bytes of data.
     * @param key  String as key.
     * @return Raw bytes (64) of Hmac-SHA512.
     */
    public static byte[] hmacSha512(byte[] data, String key) {
        SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        Mac mac = null;
        try {
            mac = Mac.getInstance("HmacSHA512");
            mac.init(signingKey);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
        return mac.doFinal(data);
    }

    static int char2int(char ch) {
        if (ch >= '0' && ch <= '9') {
            return ch - '0';
        }
        if (ch >= 'a' && ch <= 'f') {
            return ch - 'a' + 10;
        }
        if (ch >= 'A' && ch <= 'F') {
            return ch - 'A' + 10;
        }
        throw new IllegalArgumentException("Bad char.");
    }

}
