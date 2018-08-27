/*
 * Copyright 2016 Ronald W Hoffman.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bitcoin.walllet.kit.hdwallet;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import bitcoin.walllet.kit.constant.BitcoinConstants;
import bitcoin.walllet.kit.utils.Base58Utils;

/**
 * A Hierarchical Deterministic key
 */
public class HDKey extends ECKey {

    /**
     * Child is hardened
     */
    public static final int HARDENED_FLAG = 0x80000000;

    /**
     * Chain code
     */
    private final byte[] chainCode;

    /**
     * HD key parent (null if root key)
     */
    private final HDKey parent;

    /**
     * Child number
     */
    private final int childNumber;

    /**
     * Hardened key
     */
    private final boolean isHardened;

    /**
     * Depth (root key is depth 0)
     */
    private final int depth;

    /**
     * Parent fingerprint or 0 if root key
     */
    private final int parentFingerprint;

    /**
     * Create a new HD key from a private key
     *
     * @param privKey     Private key
     * @param chainCode   Chain code
     * @param parent      Parent or null if no parent
     * @param childNumber Child number (first child is 0)
     * @param isHardened  TRUE if the child is hardened
     */
    public HDKey(BigInteger privKey, byte[] chainCode, HDKey parent, int childNumber, boolean isHardened) {
        super(privKey, true);
        if (getPrivKeyBytes().length > 33)
            throw new IllegalArgumentException("Private key is longer than 33 bytes");
        if (chainCode.length != 32)
            throw new IllegalArgumentException("Chain code is not 32 bytes");
        if (getPubKey().length != 33)
            throw new IllegalStateException("Public key is not compressed");
        this.chainCode = Arrays.copyOfRange(chainCode, 0, chainCode.length);
        this.parent = parent;
        this.isHardened = isHardened;
        this.childNumber = childNumber;
        this.depth = (parent != null ? parent.getDepth() + 1 : 0);
        this.parentFingerprint = (parent != null ? parent.getFingerprint() : 0);
    }

    /**
     * Create a new HD key from a public key.  The HD key will not have a private
     * key.
     *
     * @param pubKey      Public key
     * @param chainCode   Chain code
     * @param parent      Parent or null if no parent
     * @param childNumber Child number (first child is 0)
     * @param isHardened  TRUE if the child is hardened
     */
    public HDKey(byte[] pubKey, byte[] chainCode, HDKey parent, int childNumber, boolean isHardened) {
        super(pubKey);
        if (pubKey.length != 33)
            throw new IllegalArgumentException("Public key is not compressed");
        if (chainCode.length != 32)
            throw new IllegalArgumentException("Chain code is not 32 bytes");
        this.chainCode = Arrays.copyOfRange(chainCode, 0, chainCode.length);
        this.parent = parent;
        this.isHardened = isHardened;
        this.childNumber = childNumber;
        this.depth = (parent != null ? parent.getDepth() + 1 : 0);
        this.parentFingerprint = (parent != null ? parent.getFingerprint() : 0);
    }

    /**
     * Return the parent
     *
     * @return Parent or null if this is the root key
     */
    public HDKey getParent() {
        return parent;
    }

    /**
     * Return the child number
     *
     * @return Child number
     */
    public int getChildNumber() {
        return childNumber;
    }

    /**
     * Check if the key is hardened
     *
     * @return TRUE if the key is hardened
     */
    public boolean isHardened() {
        return isHardened;
    }

    /**
     * Return the hierarchy depth
     *
     * @return Hierarchy depth (root key is depth 0)
     */
    public int getDepth() {
        return depth;
    }

    /**
     * Return private key padded to 33 bytes
     *
     * @return Padded private key
     */
    public byte[] getPaddedPrivKeyBytes() {
        byte[] privKeyBytes = getPrivKeyBytes();
        byte[] paddedBytes = new byte[33];
        System.arraycopy(privKeyBytes, 0, paddedBytes, 33 - privKeyBytes.length, privKeyBytes.length);
        return paddedBytes;
    }

    /**
     * Return the chain code
     *
     * @return Chain code
     */
    public byte[] getChainCode() {
        return chainCode;
    }

    /**
     * Return the public key fingerprint
     *
     * @return Fingerprint
     */
    public int getFingerprint() {
        //
        // The fingerprint is the first 32 bits of HASH160(pubKey)
        //
        byte[] pubKeyHash = getPubKeyHash();
        return (((int) pubKeyHash[0] & 255) << 24) | (((int) pubKeyHash[1] & 255) << 16) |
                (((int) pubKeyHash[2] & 255) << 8) | ((int) pubKeyHash[3] & 255);
    }

    /**
     * Return the parent fingerprint
     *
     * @return Parent fingerprint
     */
    public int getParentFingerprint() {
        return parentFingerprint;
    }

    /**
     * Serialize the key
     *
     * @param pubKey TRUE to serialize the public key
     */
    private byte[] serializeKey(boolean pubKey) {
        if (depth > 255)
            throw new IllegalStateException("Key depth greater than 255");
        ByteBuffer serBuffer = ByteBuffer.allocate(78);
        serBuffer.putInt(pubKey ? BitcoinConstants.HD_PUBLIC_KEY_PREFIX : BitcoinConstants.HD_PRIVATE_KEY_PREFIX);
        serBuffer.put((byte) getDepth());
        serBuffer.putInt(getParentFingerprint());
        serBuffer.putInt(isHardened() ? (getChildNumber() | HARDENED_FLAG) : getChildNumber());
        serBuffer.put(getChainCode());
        serBuffer.put(pubKey ? getPubKey() : getPaddedPrivKeyBytes());
        return serBuffer.array();
    }

    /**
     * Serialize the private key
     *
     * @return Serialized key bytes
     */
    public byte[] serializePrivKey() {
        return serializeKey(false);
    }

    /**
     * Serialize the private key and then encode it as a Base58 string
     *
     * @return Base58-encoded string
     */
    public String serializePrivKeyToString() {
        return Base58Utils.encode(addChecksum(serializeKey(false)));
    }

    /**
     * Serialize the public key
     *
     * @return Serialized key bytes
     */
    public byte[] serializePubKey() {
        return serializeKey(true);
    }

    /**
     * Serialize the public key and then encode it as a Base58 string
     *
     * @return Base58-encoded string
     */
    public String serializePubKeyToString() {
        return Base58Utils.encode(addChecksum(serializeKey(true)));
    }

    /**
     * Add the 4-byte checksum to the serialized key
     *
     * @param input Serialized key
     * @return Key plus checksum
     */
    private byte[] addChecksum(byte[] input) {
        int inputLength = input.length;
        byte[] checksummed = new byte[inputLength + 4];
        System.arraycopy(input, 0, checksummed, 0, inputLength);
        byte[] checksum = Utils.doubleDigest(input);
        System.arraycopy(checksum, 0, checksummed, inputLength, 4);
        return checksummed;
    }

    /**
     * Get the path from the root key
     *
     * @return List of node numbers
     */
    public List<Integer> getPath() {
        List<Integer> path;
        if (parent != null) {
            path = parent.getPath();
            path.add(childNumber);
        } else {
            path = new ArrayList<>();
        }
        return path;
    }

    /**
     * Get string representation of this key
     *
     * @return Path string
     */
    @Override
    public String toString() {
        List<Integer> path = getPath();
        StringBuilder sb = new StringBuilder();
        sb.append("m");
        for (Integer in : path) {
            sb.append("/").append(in.toString());
        }
        return sb.toString();
    }
}
