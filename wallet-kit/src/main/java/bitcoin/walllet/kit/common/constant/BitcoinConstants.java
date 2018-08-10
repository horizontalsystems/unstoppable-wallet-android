package bitcoin.walllet.kit.common.constant;

import java.math.BigInteger;

import bitcoin.walllet.kit.common.util.HashUtils;

public final class BitcoinConstants {

    /**
     * Magic number for bitcoin message.
     */
    public static final int MAGIC = 0xd9b4bef9;

    /**
     * Default port for bitcoin peer.
     */
    public static final int PORT = 8333;

    /**
     * Bitcoin protocol version.
     */
    public static final int PROTOCOL_VERSION = 70014;

    /**
     * Network services.
     */
    public static final long NETWORK_SERVICES = 1L;

    /**
     * Legal base58 characters.
     */
    public static final String BASE58_CHARS = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

    /**
     * Network ID: 0x00 = main network.
     */
    public static final byte NETWORK_ID = 0x00;
    public static final byte[] NETWORK_ID_ARRAY = {NETWORK_ID};

    /**
     * Public key prefix: 0x04.
     */
    public static final byte PUBLIC_KEY_PREFIX = 0x04;
    public static final byte[] PUBLIC_KEY_PREFIX_ARRAY = {PUBLIC_KEY_PREFIX};

    public static final byte[] PUBLIC_KEY_COMPRESSED_02 = {0x02};
    public static final byte[] PUBLIC_KEY_COMPRESSED_03 = {0x03};

    /**
     * Private key prefix: 0x80.
     */
    public static final byte PRIVATE_KEY_PREFIX = (byte) 0x80;
    public static final byte[] PRIVATE_KEY_PREFIX_ARRAY = {PRIVATE_KEY_PREFIX};

    public static final byte PRIVATE_KEY_SUFFIX = 0x01;
    public static final byte[] PRIVATE_KEY_SUFFIX_ARRAY = {PRIVATE_KEY_SUFFIX};

    public static final int MAINNET_BIP32_PUBLIC = 0x0488b21e;
    public static final int MAINNET_BIP32_PRIVATE = 0x0488ade4;

    public static final int TESTNET_BIP32_PUBLIC = 0x043587cf;
    public static final int TESTNET_BIP32_PRIVATE = 0x04358394;

    /**
     * Minimum value of private key.
     */
    public static final BigInteger MIN_PRIVATE_KEY = new BigInteger("ffffffffffffffff", 16);

    /**
     * Maximum value of private key.
     */
    public static final BigInteger MAX_PRIVATE_KEY = new BigInteger(
            "fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364140", 16);

    /**
     * Random node id generated at startup.
     */
    public static final long NODE_ID = (long) (Math.random() * Long.MAX_VALUE);

    /**
     * Bitcoin client subversion.
     */
	public static final String SUB_VERSION = "/Satoshi:0.7.2/";

    /**
     * Genesis block hash as string.
     */
    public static final String GENESIS_HASH = "000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26f";

    /**
     * Genesis block hash as bytes.
     */
    public static final byte[] GENESIS_HASH_BYTES = HashUtils.toBytesAsLittleEndian(GENESIS_HASH);

    /**
     * Hardcoded genesis block data.
     */
    public static final byte[] GENESIS_BLOCK_DATA = HashUtils.toBytes(
            "0100000000000000000000000000000000000000000000000000000000000000000000003ba3edfd7a7b12b27ac72c3e67768f617fc81bc3888a51323a9fb8aa4b1e5e4a29ab5f49ffff001d1dac2b7c0101000000010000000000000000000000000000000000000000000000000000000000000000ffffffff4d04ffff001d0104455468652054696d65732030332f4a616e2f32303039204368616e63656c6c6f72206f6e206272696e6b206f66207365636f6e64206261696c6f757420666f722062616e6b73ffffffff0100f2052a01000000434104678afdb0fe5548271967f1a67130b7105cd6a828e03909a67962e0ea1f61deb649f6bc3f4cef38c4f35504e51ec112de5c384df7ba0b8d578a4c702b6bf11d5fac00000000");

    /**
     * Hash bytes as "00000000...0000"
     */
    public static final String ZERO_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    /**
     * Hash bytes as "00000000...0000"
     */
    public static final byte[] ZERO_HASH_BYTES = HashUtils
            .toBytesAsLittleEndian("0000000000000000000000000000000000000000000000000000000000000000");

    /**
     * Transaction version.
     */
    public static final int TX_VERSION = 0x01;

    /**
     * Signature type: SIGHASH_ALL.
     */
    public static final int SIGHASH_ALL = 0x01;
}
