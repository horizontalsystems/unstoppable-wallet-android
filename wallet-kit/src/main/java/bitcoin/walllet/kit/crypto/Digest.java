package bitcoin.walllet.kit.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

public class Digest {

    public final static String SHA256 = "SHA-256";
    public final static String RIPEMD160 = "RIPEMD160";
    private final static String[] SHARIPE = new String[]{SHA256, RIPEMD160};
    private final static int[] SHARIPERepeat = new int[]{1, 1};
    private final static ThreadLocal<HashMap<String, MessageDigest>> digests = new ThreadLocal<HashMap<String, MessageDigest>>() {
        @Override
        public HashMap<String, MessageDigest> initialValue() {
            return new HashMap<String, MessageDigest>();
        }
    };

    static {
        Crypt.init();
    }

    public static byte[] SHA256(byte[] message) {
        return hash(message, SHA256, 1);
    }

    public static byte[] doubleSHA256(byte[] message) {
        return hash(message, SHA256, 2);
    }

    public static byte[] RIPEMD160(byte[] message) {
        return hash(message, RIPEMD160, 1);
    }

    public static byte[] SHA256RIPEMD160(byte[] message) {
        return hash(message, SHARIPE, SHARIPERepeat);
    }

    private static MessageDigest getDigestLocal(String digest) {
        MessageDigest d = digests.get().get(digest);
        if (d != null) {
            return d;
        }
        d = getDigest(digest);
        digests.get().put(digest, d);
        return d;
    }

    public static MessageDigest getDigest(String digest) {
        MessageDigest d;
        try {
            d = MessageDigest.getInstance(digest);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
        return d;
    }

    private static byte[] hash(byte[] message, String[] digest, int[] n) {
        if (digest.length != n.length) {
            return null;
        }
        for (int i = 0; i < n.length; i++) {
            message = hash(message, digest[i], n[i]);
        }
        return message;
    }

    private static byte[] hash(byte[] message, String digest, int n) {
        if (message == null) {
            return null;
        }
        MessageDigest d = getDigestLocal(digest);
        for (int i = 0; i < n; i++) {
            d.reset();
            d.update(message);
            message = d.digest();
        }
        return message;
    }

}

