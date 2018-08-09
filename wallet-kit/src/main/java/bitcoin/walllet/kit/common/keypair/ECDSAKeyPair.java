package bitcoin.walllet.kit.common.keypair;

import org.spongycastle.util.Arrays;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import bitcoin.walllet.kit.common.constant.BitcoinConstants;
import bitcoin.walllet.kit.common.util.Base58Utils;
import bitcoin.walllet.kit.common.util.BytesUtils;
import bitcoin.walllet.kit.common.util.HashUtils;
import bitcoin.walllet.kit.common.util.Secp256k1Utils;

/**
 * The ECDSAKeyPair which contains a private key. Public key can be calculated
 * by private key.
 *
 * @author Michael Liao
 */
public class ECDSAKeyPair {

    private final BigInteger privateKey;
    private final boolean isCompressed;

    // public key can be cacluated by private key:
    private BigInteger[] publicKey = null;

    /**
     * Construct a keypair with private key.
     */
    private ECDSAKeyPair(BigInteger privateKey, boolean isCompressed) {
        this.privateKey = privateKey;
        this.isCompressed = isCompressed;
    }

    /**
     * Create KeyPair with specified WIF string.
     */
    public static ECDSAKeyPair of(String wif) {
        byte[] key = parseWIF(wif);
        return new ECDSAKeyPair(new BigInteger(1, key), !wif.startsWith("5"));
    }

    /**
     * Create KeyPair with specified private key (compressed).
     */
    public static ECDSAKeyPair of(byte[] privateKey) {
        return of(new BigInteger(1, privateKey));
    }

    /**
     * Create KeyPair with specified private key (compressed).
     */
    public static ECDSAKeyPair of(BigInteger privateKey) {
        checkPrivateKey(privateKey);
        return new ECDSAKeyPair(privateKey, true);
    }

    /**
     * Create a new KeyPair with secure random private key (compressed).
     */
    public static ECDSAKeyPair createNewKeyPair() {
        return of(generatePrivateKey());
    }

    /**
     * Parse WIF string as private key.
     */
    public static byte[] parseWIF(String wif) {
        byte[] data = Base58Utils.decodeChecked(wif);
        if (data[0] != BitcoinConstants.PRIVATE_KEY_PREFIX) {
            throw new IllegalArgumentException("Leading byte is not 0x80.");
        }
        if (wif.charAt(0) == '5') {
            // remove first 0x80:
            return Arrays.copyOfRange(data, 1, data.length);
        } else {
            if (data[data.length - 1] != BitcoinConstants.PRIVATE_KEY_SUFFIX) {
                throw new IllegalArgumentException("Ending byte is not 0x01.");
            }
            // remove first 0x80 and last 0x01:
            return Arrays.copyOfRange(data, 1, data.length - 1);
        }
    }

    /**
     * generate random private key between 0x00ffff... ~ 0xff0000...
     *
     * @return Private key as byte[32].
     */
    public static byte[] generatePrivateKey() {
        SecureRandom sr;
        try {
            sr = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            sr = new SecureRandom();
        }
        int first;
        byte[] key = new byte[32];
        do {
            sr.nextBytes(key);
            first = key[0] & 0xff;
        } while (first == 0x00 || first == 0xff);
        return key;
    }

    static byte[] bigIntegerToBytes(BigInteger bi, int length) {
        byte[] data = bi.toByteArray();
        if (data.length == length) {
            return data;
        }
        // remove leading zero:
        if (data[0] == 0) {
            data = Arrays.copyOfRange(data, 1, data.length);
        }
        if (data.length > length) {
            throw new IllegalArgumentException("BigInteger is too large.");
        }
        byte[] copy = new byte[length];
        System.arraycopy(data, 0, copy, length - data.length, data.length);
        return copy;
    }

    static void checkPrivateKey(BigInteger bi) {
        if (bi == null) {
            throw new IllegalArgumentException("Private key is null.");
        }
        if (bi.compareTo(BitcoinConstants.MIN_PRIVATE_KEY) == (-1)) {
            throw new IllegalArgumentException("Private key is too small.");
        }
        if (bi.compareTo(BitcoinConstants.MAX_PRIVATE_KEY) == 1) {
            throw new IllegalArgumentException("Private key is too large.");
        }
    }

    /**
     * Get private key as BigInteger.
     */
    public BigInteger getPrivateKey() {
        return this.privateKey;
    }

    /**
     * Convert to public key as uncompressed byte[].
     */
    public byte[] toUncompressedPublicKey() {
        BigInteger[] keys = getPublicKey();
        byte[] xs = bigIntegerToBytes(keys[0], 32);
        byte[] ys = bigIntegerToBytes(keys[1], 32);
        return BytesUtils.concat(BitcoinConstants.PUBLIC_KEY_PREFIX_ARRAY, xs, ys);
    }

    public String toEncodedUncompressedPublicKey() {
        return Secp256k1Utils.uncompressedPublicKeyToAddress(toUncompressedPublicKey());
    }

    /**
     * Convert to public key as compressed byte[].
     */
    public byte[] toCompressedPublicKey() {
        BigInteger[] keys = getPublicKey();
        byte[] xs = bigIntegerToBytes(keys[0], 32);
        byte[] ys = bigIntegerToBytes(keys[1], 32);
        if ((ys[31] & 0xff) % 2 == 0) {
            return BytesUtils.concat(BitcoinConstants.PUBLIC_KEY_COMPRESSED_02, xs);
        } else {
            return BytesUtils.concat(BitcoinConstants.PUBLIC_KEY_COMPRESSED_03, xs);
        }
    }

    public String toEncodedCompressedPublicKey() {
        return Secp256k1Utils.compressedPublicKeyToAddress(toCompressedPublicKey());
    }

    public String toEncodedPublicKey() {
        return this.isCompressed ? toEncodedCompressedPublicKey() : toEncodedUncompressedPublicKey();
    }

    public byte[] toPublicKey() {
        return this.isCompressed ? toCompressedPublicKey() : toUncompressedPublicKey();
    }

    /**
     * Get public key as BigInteger[] with 2 elements.
     */
    public BigInteger[] getPublicKey() {
        if (this.publicKey == null) {
            // ECPoint point = Secp256k1Utils.getG().multiply(privateKey);
            // ECPoint normed = point.normalize();
            // byte[] x = normed.getXCoord().getEncoded();
            // byte[] y = normed.getYCoord().getEncoded();
            // this.publicKey = new BigInteger[] { new BigInteger(1, x), new BigInteger(1, y) };
        }
        return Arrays.clone(this.publicKey);
    }

    /**
     * Get uncompressed Wallet Import Format string defined in:
     * https://en.bitcoin.it/wiki/Wallet_import_format
     */
    public String toUncompressedWIF() {
        byte[] key = bigIntegerToBytes(this.privateKey, 32);
        byte[] extendedKey = BytesUtils.concat(BitcoinConstants.PRIVATE_KEY_PREFIX_ARRAY, key);
        byte[] hash = HashUtils.doubleSha256(extendedKey);
        byte[] checksum = Arrays.copyOfRange(hash, 0, 4);
        byte[] extendedKeyWithChecksum = BytesUtils.concat(extendedKey, checksum);
        return Base58Utils.encode(extendedKeyWithChecksum);
    }

    /**
     * Get Compressed Wallet Import Format string defined in:
     * https://en.bitcoin.it/wiki/Wallet_import_format
     */
    public String toCompressedWIF() {
        byte[] key = bigIntegerToBytes(this.privateKey, 32);
        byte[] extendedKey = BytesUtils.concat(BitcoinConstants.PRIVATE_KEY_PREFIX_ARRAY, key,
                BitcoinConstants.PRIVATE_KEY_SUFFIX_ARRAY);
        byte[] hash = HashUtils.doubleSha256(extendedKey);
        byte[] checksum = Arrays.copyOfRange(hash, 0, 4);
        byte[] extendedKeyWithChecksum = BytesUtils.concat(extendedKey, checksum);
        return Base58Utils.encode(extendedKeyWithChecksum);
    }

    @Override
    public String toString() {
        return "KeyPair<" + this.getPrivateKey().toString(16) + ">";
    }
}
