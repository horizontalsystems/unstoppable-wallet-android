/**
 * Copyright 2011 Google Inc.
 * Copyright 2013-2014 Ronald W Hoffman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bitcoin.walllet.kit.common.hdwallet;

import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * A Sha256Hash wraps a byte[] so that equals and hashCode work correctly, allowing it to
 * be used as a key in a map.
 */
public class Sha256Hash {

    /** A zero hash */
    public static final Sha256Hash ZERO_HASH = new Sha256Hash();

    /** The byte value of the SHA-256 digest */
    private byte[] bytes;

    /**
     * Creates a Sha256Hash with a zero value
     */
    public Sha256Hash() {
        bytes = new byte[32];
    }

    /** Creates a Sha256Hash by wrapping the given byte array
     *
     * @param       rawHashBytes    32-byte hash digest
     */
    public Sha256Hash(byte[] rawHashBytes) {
        this(rawHashBytes, 0, rawHashBytes.length);
    }

    /**
     * Creates a Sha256Hash by wrapping the given byte array
     *
     * @param       rawHashBytes    32-byte hash digest
     * @param       offset          Offset within the byte array
     * @param       length          Number of bytes
     */
    public Sha256Hash(byte[] rawHashBytes, int offset, int length) {
        if (length != 32)
            throw new IllegalArgumentException("SHA-256 hash must be 32 bytes");
        if (offset+length > rawHashBytes.length)
            throw new IllegalArgumentException("Hash byte array overflow");
        bytes = new byte[32];
        System.arraycopy(rawHashBytes, offset, bytes, 0, length);
    }

    /**
     * Creates a Sha256Hash by decoding the given hex string. It must be 64 characters long.
     *
     * @param       hexString           64-character hex string
     */
    public Sha256Hash(String hexString) {
        if (hexString.length() != 64)
            throw new IllegalArgumentException("SHA-256 hash string must be 64 characters");
        bytes = Hex.decode(hexString);
    }

    /**
     * Returns the bytes interpreted as a positive integer.
     *
     * @return                      The integer representation of the hash digest
     */
    public BigInteger toBigInteger() {
        return new BigInteger(1, bytes);
    }

    /**
     * Returns the hash digest
     *
     * @return                      The hash digest
     */
    public byte[] getBytes() {
        return bytes;
    }

    /**
     * Compares this Sha256Hash object to another one
     *
     * @param       other           The object to compare
     * @return                      TRUE if the objects are equal
     */
    @Override
    public boolean equals(Object other) {
        return (other!=null && (other instanceof Sha256Hash) && Arrays.equals(bytes, ((Sha256Hash)other).bytes));
    }

    /**
     * Generates the hash code for this object.  We use the last 4 bytes of the value to form the hash because
     * the first 4 bytes often contain zero values in the Bitcoin protocol.
     *
     * @return                      Hash code
     */
    @Override
    public int hashCode() {
        return (((int)bytes[28]&0xff)<<24) | (((int)bytes[29]&0xff)<<16) |
                    (((int)bytes[30]&0xff)<<8) | ((int)bytes[31]&0xff);
    }

    /**
     * Creates a string representation of the hash value of this object
     *
     * @return                      The string representation
     */
    @Override
    public String toString() {
        return Utils.bytesToHexString(bytes);
    }
}
