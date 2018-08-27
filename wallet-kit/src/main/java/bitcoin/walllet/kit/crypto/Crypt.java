package bitcoin.walllet.kit.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Crypt {

    private final static AtomicBoolean init = new AtomicBoolean(false);

    static {
        init();
    }

    public static OutputStream encrypt(byte[] keyIv, OutputStream out) {
        if (keyIv == null || keyIv.length != 32) {
            return null;
        }
        byte[] key = new byte[16];
        byte[] iv = new byte[16];
        System.arraycopy(keyIv, 0, key, 0, 16);
        System.arraycopy(keyIv, 16, iv, 0, 16);
        return encrypt(key, iv, out);
    }

    public static OutputStream encrypt(byte[] key, byte[] iv, OutputStream out) {
        Key outKey = new SecretKeySpec(key, "AES");
        Cipher outgoing;
        try {
            outgoing = Cipher.getInstance("AES/CFB8/NoPadding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
            return null;
        }

        try {
            outgoing.init(Cipher.ENCRYPT_MODE, outKey, new IvParameterSpec(iv));
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
            return null;
        }
        return new CipherOutputStream(out, outgoing);
    }

    public static InputStream decrypt(byte[] keyIv, InputStream in) {
        byte[] key = new byte[16];
        byte[] iv = new byte[16];
        System.arraycopy(keyIv, 0, key, 0, 16);
        System.arraycopy(keyIv, 16, iv, 0, 16);
        return decrypt(key, iv, in);
    }

    public static InputStream decrypt(byte[] key, byte[] iv, InputStream in) {
        try {
            Key inKey = new SecretKeySpec(key, "AES");
            Cipher incoming = Cipher.getInstance("AES/CFB8/NoPadding");
            incoming.init(Cipher.DECRYPT_MODE, inKey, new IvParameterSpec(iv));
            return new CipherInputStream(in, incoming);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void init() {
        if (init.compareAndSet(false, false)) {
            synchronized (init) {
                Security.addProvider(new BouncyCastleProvider());
            }
            init.set(true);
        }
    }

}
