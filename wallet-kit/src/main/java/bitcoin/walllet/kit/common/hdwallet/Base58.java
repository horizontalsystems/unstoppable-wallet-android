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

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * Provides Base-58 encoding and decoding
 */
public class Base58 {

    /** Alphabet used for encoding and decoding */
    private static final char[] ALPHABET =
            "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray();

    /** Lookup index for US-ASCII characters (code points 0-127) */
    private static final int[] INDEXES = new int[128];
    static {
        for (int i=0; i<INDEXES.length; i++)
            INDEXES[i] = -1;
        for (int i=0; i<ALPHABET.length; i++)
            INDEXES[ALPHABET[i]] = i;
    }

    /**
     * Encodes a byte array as a Base58 string
     *
     * @param       bytes           Array to be encoded
     * @return                      Encoded string
     */
    public static String encode(byte[] bytes) {
        //
        // Nothing to do for an empty array
        //
        if (bytes.length == 0)
            return "";
        //
        // Make a copy of the input since we will be modifying it as we go along
        //
        byte[] input = Arrays.copyOf(bytes, bytes.length);
        //
        // Count the number of leading zeroes (we will need to prefix the encoded result
        // with this many zero characters)
        //
        int zeroCount = 0;
        while (zeroCount < input.length && input[zeroCount] == 0)
            zeroCount++;
        //
        // Encode the input starting with the first non-zero byte
        //
        int offset = zeroCount;
        byte[] encoded = new byte[input.length*2];
        int encodedOffset = encoded.length;
        while (offset < input.length) {
            byte mod = divMod58(input, offset);
            if (input[offset] == 0)
                offset++;
            encoded[--encodedOffset] = (byte)ALPHABET[mod];
        }
        //
        // Strip any leading zero values in the encoded result
        //
        while (encodedOffset < encoded.length && encoded[encodedOffset] == (byte)ALPHABET[0])
            encodedOffset++;
        //
        // Now add the number of leading zeroes that we found in the input array
        //
        for (int i=0; i<zeroCount; i++)
            encoded[--encodedOffset] = (byte)ALPHABET[0];
        //
        // Create the return string from the encoded bytes
        //
        String encodedResult;
        try {
            byte[] stringBytes = Arrays.copyOfRange(encoded, encodedOffset, encoded.length);
            encodedResult = new String(stringBytes, "US-ASCII");
        } catch (UnsupportedEncodingException exc) {
            encodedResult = "";             // Should never happen
        }
        return encodedResult;
    }

    /**
     * Decodes a Base58 string
     *
     * @param       string                  Encoded string
     * @return                              Decoded bytes
     * @throws      AddressFormatException  Invalid Base-58 encoded string
     */
    public static byte[] decode(String string) throws AddressFormatException {
        //
        // Nothing to do if we have an empty string
        //
        if (string.length() == 0)
            return new byte[0];
        //
        // Convert the input string to a byte sequence
        //
        byte[] input = new byte[string.length()];
        for (int i=0; i<string.length(); i++) {
            int codePoint = string.codePointAt(i);
            int digit = -1;
            if (codePoint>=0 && codePoint<INDEXES.length)
                digit = INDEXES[codePoint];
            if (digit < 0)
                throw new AddressFormatException(String.format("Illegal character %c at index %d",
                                                               string.charAt(i), i));
            input[i] = (byte)digit;
        }
        //
        // Count the number of leading zero characters
        //
        int zeroCount = 0;
        while (zeroCount < input.length && input[zeroCount] == 0)
            zeroCount++;
        //
        // Convert from Base58 encoding starting with the first non-zero character
        //
        byte[] decoded = new byte[input.length];
        int decodedOffset = decoded.length;
        int offset = zeroCount;
        while (offset < input.length) {
            byte mod = divMod256(input, offset);
            if (input[offset] == 0)
                offset++;
            decoded[--decodedOffset] = mod;
        }
        //
        // Strip leading zeroes from the decoded result
        //
        while (decodedOffset < decoded.length && decoded[decodedOffset] == 0)
            decodedOffset++;
        //
        // Return the decoded result prefixed with the number of leading zeroes
        // that were in the original string
        //
        byte[] output = Arrays.copyOfRange(decoded, decodedOffset-zeroCount, decoded.length);
        return output;
    }

    /**
     * Decode a Base58-encoded checksummed string and verify the checksum.  The
     * checksum will then be removed from the decoded value.
     *
     * @param       string                  Base-58 encoded checksummed string
     * @return                              Decoded value
     * @throws      AddressFormatException  The string is not valid or the checksum is incorrect
     */
    public static byte[] decodeChecked(String string) throws AddressFormatException {
        //
        // Decode the string
        //
        byte[] decoded = decode(string);
        if (decoded.length < 4)
            throw new AddressFormatException("Decoded string is too short");
        //
        // Verify the checksum contained in the last 4 bytes
        //
        byte[] bytes = Arrays.copyOfRange(decoded, 0, decoded.length-4);
        byte[] checksum = Arrays.copyOfRange(decoded, decoded.length-4, decoded.length);
        byte[] hash = Arrays.copyOfRange(Utils.doubleDigest(bytes), 0, 4);
        if (!Arrays.equals(hash, checksum))
            throw new AddressFormatException("Checksum is not correct");
        //
        // Return the result without the checksum bytes
        //
        return bytes;
    }

    /**
     * Divide the current number by 58 and return the remainder.  The input array
     * is updated for the next round.
     *
     * @param       number          Number array
     * @param       offset          Offset within the array
     * @return                      The remainder
     */
    private static byte divMod58(byte[] number, int offset) {
        int remainder = 0;
        for (int i=offset; i<number.length; i++) {
            int digit = (int)number[i]&0xff;
            int temp = remainder*256 + digit;
            number[i] = (byte)(temp/58);
            remainder = temp%58;
        }
        return (byte)remainder;
    }

    /**
     * Divide the current number by 256 and return the remainder.  The input array
     * is updated for the next round.
     *
     * @param       number          Number array
     * @param       offset          Offset within the array
     * @return                      The remainder
     */
    private static byte divMod256(byte[] number, int offset) {
        int remainder = 0;
        for (int i=offset; i<number.length; i++) {
            int digit = (int)number[i]&0xff;
            int temp = remainder*58 + digit;
            number[i] = (byte)(temp/256);
            remainder = temp%256;
        }
        return (byte)remainder;
    }
}
