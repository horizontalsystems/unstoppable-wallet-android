/**
 * Copyright 2011 Google Inc.
 * Copyright 2013-2016 Ronald W Hoffman
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
package bitcoin.walllet.kit.hdwallet;

import org.spongycastle.crypto.digests.RIPEMD160Digest;
import org.spongycastle.crypto.digests.SHA512Digest;
import org.spongycastle.crypto.macs.HMac;
import org.spongycastle.crypto.params.KeyParameter;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Static utility methods
 */
public class Utils {
    /** Bit masks (Low-order bit is bit 0 and high-order bit is bit 7) */
    private static final int bitMask[] = {0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80};

    /** Instance of a SHA-256 digest which we will use as needed */
    private static final MessageDigest digest;
    static {
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);  // Can't happen.
        }
    }

    /**
     * Calculate the SHA-256 hash of the input and then hash the resulting hash again
     *
     * @param       input           Data to be hashed
     * @return                      The hash digest
     */
    public static byte[] doubleDigest(byte[] input) {
        return doubleDigest(input, 0, input.length);
    }

    /**
     * Calculate the SHA-256 hash of the input and then hash the resulting hash again
     *
     * @param       input           Data to be hashed
     * @param       offset          Starting offset within the data
     * @param       length          Number of data bytes to hash
     * @return                      The hash digest
     */
    public static byte[] doubleDigest(byte[] input, int offset, int length) {
        byte[] bytes;
        synchronized (digest) {
            digest.reset();
            digest.update(input, offset, length);
            byte[] first = digest.digest();
            bytes = digest.digest(first);
        }
        return bytes;
    }

    /**
     * Calculate RIPEMD160(SHA256(input)).  This is used in Address calculations.
     *
     * @param       input           The byte array to be hashed
     * @return                      The hashed result
     */
    public static byte[] sha256Hash160(byte[] input) {
        byte[] out = new byte[20];
        synchronized(digest) {
            digest.reset();
            byte[] sha256 = digest.digest(input);
            RIPEMD160Digest rDigest = new RIPEMD160Digest();
            rDigest.update(sha256, 0, sha256.length);
            rDigest.doFinal(out, 0);
        }
        return out;
    }

    /**
     * Calculate the HMAC-SHA512 digest for use with BIP 32
     *
     * @param       key             Key
     * @param       input           Bytes to be hashed
     * @return                      Hashed result
     */
    public static byte[] hmacSha512(byte[] key, byte[] input) {
        HMac hmac = new HMac(new SHA512Digest());
        hmac.init(new KeyParameter(key));
        hmac.update(input, 0, input.length);
        byte[] out = new byte[64];
        hmac.doFinal(out, 0);
        return out;
    }

    /**
     * Sets the specified bit
     * @param       data            Byte array
     * @param       index           Bit position
     */
    public static void setBitLE(byte[] data, int index) {
        data[index >>> 3] |= bitMask[7 & index];
    }
}
