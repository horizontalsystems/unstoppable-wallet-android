package bitcoin.walllet.kit.common.keypair;

import java.math.BigInteger;
import java.util.Arrays;

import bitcoin.walllet.kit.common.BitcoinException;
import bitcoin.walllet.kit.common.constant.BitcoinConstants;
import bitcoin.walllet.kit.common.util.Base58Utils;
import bitcoin.walllet.kit.common.util.BytesUtils;
import bitcoin.walllet.kit.common.util.HashUtils;

public class BIP32 {

    static BIP32Key generateMasterKey(BigInteger seed) {
        if (seed.signum() < 0) {
            throw new BitcoinException("Seed must be positive.");
        }
        byte[] seedBytes = seed.toByteArray();
        if (seedBytes.length % 2 == 1) {
            if (seedBytes[0] == 0x00) {
                // remove:
                seedBytes = Arrays.copyOfRange(seedBytes, 1, seedBytes.length);
            } else {
                // prepend:
                seedBytes = BytesUtils.concat(new byte[]{0x00}, seedBytes);
            }
        }
        if (seedBytes.length > 128) {
            throw new BitcoinException("Seed length exceeded than 64 bytes.");
        }
        if (seedBytes.length < 16) {
            byte[] copied = new byte[16];
            System.arraycopy(seedBytes, 0, copied, copied.length - seedBytes.length, seedBytes.length);
            seedBytes = copied;
        }
        byte[] rawHash = HashUtils.hmacSha512(seedBytes, "Bitcoin seed");
        byte[] IL = Arrays.copyOfRange(rawHash, 0, 32);
        byte[] IR = Arrays.copyOfRange(rawHash, 32, 64);
        return new BIP32Key(BitcoinConstants.MAINNET_BIP32_PRIVATE, 0, new byte[4], 0, IR,
                BytesUtils.concat(IL, BitcoinConstants.PRIVATE_KEY_SUFFIX_ARRAY));
    }

    public static class BIP32Key {
        public final int version;
        public final int depth;
        public final byte[] chainCode;
        public final byte[] key;
        public final byte[] fingerprint;
        public final int index;

        public BIP32Key(int version, int depth, byte[] fingerprint, int index, byte[] chainCode, byte[] key) {
            if (depth < 0 || depth > 255) {
                throw new BitcoinException("Invalid depth: " + depth);
            }
            if (key.length != 33) {
                throw new BitcoinException("Invalid key: expected 33 bytes but actual " + key.length + " bytes.");
            }
            if (version == BitcoinConstants.MAINNET_BIP32_PRIVATE
                    || version == BitcoinConstants.TESTNET_BIP32_PRIVATE) {
                key = BytesUtils.concat(new byte[]{0x00}, Arrays.copyOf(key, 32));
            }
            if (chainCode.length != 32) {
                throw new BitcoinException(
                        "Invalid chain code: expected 32 bytes but actual " + chainCode.length + " bytes.");
            }
            if (fingerprint.length != 4) {
                throw new BitcoinException("Invalid fingerprint: " + fingerprint.length + " bytes.");
            }
            this.version = version;
            this.depth = depth;
            this.chainCode = chainCode;
            this.key = key;
            this.fingerprint = fingerprint;
            this.index = index;
        }

        public static BIP32Key deserialize(String base58Str) {
            byte[] data = Base58Utils.decodeChecked(base58Str);
            int version = BytesUtils.bytesToInt(Arrays.copyOfRange(data, 0, 4));
            int depth = data[4];
            byte[] fingerprint = Arrays.copyOfRange(data, 5, 9);
            int index = BytesUtils.bytesToInt(Arrays.copyOfRange(data, 9, 13));
            byte[] chainCode = Arrays.copyOfRange(data, 13, 45);
            byte[] key = Arrays.copyOfRange(data, 45, 78);
            if (version == BitcoinConstants.MAINNET_BIP32_PRIVATE
                    || version == BitcoinConstants.TESTNET_BIP32_PRIVATE) {
                key = BytesUtils.concat(Arrays.copyOfRange(key, 1, 33), BitcoinConstants.PRIVATE_KEY_SUFFIX_ARRAY);
            }
            return new BIP32Key(version, depth, fingerprint, index, chainCode, key);
        }

        public String serialize() {
            byte[] b1 = BytesUtils.concat(BytesUtils.intToByteArray(this.version), new byte[]{(byte) depth},
                    this.fingerprint);
            byte[] b2 = BytesUtils.concat(BytesUtils.intToByteArray(this.index), this.chainCode, this.key);
            byte[] ser = BytesUtils.concat(b1, b2);
            byte[] checksum = HashUtils.doubleSha256(ser);
            byte[] result = BytesUtils.concat(ser, Arrays.copyOfRange(checksum, 0, 4));
            return Base58Utils.encode(result);
        }
    }
}
