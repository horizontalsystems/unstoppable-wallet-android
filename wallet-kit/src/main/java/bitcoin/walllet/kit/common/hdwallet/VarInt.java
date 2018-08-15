/**
 * Copyright 2011 Google Inc.
 * Copyright 2013 Ronald W Hoffman
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

import java.io.EOFException;
import java.io.InputStream;
import java.io.IOException;

/**
 * A VarInt is an unsigned variable-length encoded integer using the bitcoin encoding (called the 'compact size'
 * in the reference client).  It consists of a marker byte and zero or more data bytes as follows:
 * <pre>
 *      Value           Marker      Data
 *      ======          ======      ====
 *      0-252           0-252       0 bytes
 *      253 to 2^16-1   253         2 bytes
 *      2^16 to 2^32-1  254         4 bytes
 *      2^32 to 2^64-1  255         8 bytes
 * </pre>
 */
public final class VarInt {

    /** The value of this VarInt */
    private final long value;

    /** The encoded size of this VarInt */
    private int encodedSize;

    /**
     * Creates a new VarInt with the requested value
     *
     * @param       value           Requested value
     */
    public VarInt(long value) {
        this.value = value;
        encodedSize = sizeOf(value);
    }

    /**
     * Creates a new VarInt from a byte array in little-endian format
     *
     * @param       buf             Byte array
     * @param       offset          Starting offset into the array
     * @throws      EOFException    Buffer is too small
     */
    public VarInt(byte[]buf, int offset) throws EOFException {
        if (offset > buf.length)
            throw new EOFException("End-of-data while processing VarInt");
        int first = 0x00FF&(int)buf[offset];
        if (first < 253) {
            // 8 bits.
            value = first;
            encodedSize = 1;
        } else if (first == 253) {
            // 16 bits.
            if (offset+2 > buf.length)
                throw new EOFException("End-of-data while processing VarInt");
            value = (0x00FF&(int)buf[offset+1]) | ((0x00FF&(int)buf[offset+2])<<8);
            encodedSize = 3;
        } else if (first == 254) {
            // 32 bits.
            if (offset+5 > buf.length)
                throw new EOFException("End-of-data while processing VarInt");
            value = Utils.readUint32LE(buf, offset+1);
            encodedSize = 5;
        } else {
            // 64 bits.
            if (offset+9 > buf.length)
                throw new EOFException("End-of-data while processing VarInt");
            value = Utils.readUint64LE(buf, offset+1);
            encodedSize = 9;
        }
    }

    /**
     * Creates a new VarInt from an input stream encoded in little-endian format
     *
     * @param       in              Input stream
     * @throws      EOFException    End-of-data processing stream
     * @throws      IOException     I/O error processing stream
     */
    public VarInt(InputStream in) throws EOFException, IOException {
        int count;
        int first = in.read();
        if (first < 0)
            throw new EOFException("End-of-data while processing VarInt");

        if (first < 253) {
            // 8 bits.
            value = first;
            encodedSize = 1;
        } else if (first == 253) {
            // 16 bits.
            byte[] buf = new byte[2];
            count = in.read(buf, 0, 2);
            if (count < 2)
                throw new EOFException("End-of-data while processing VarInt");

            value = (0x00FF&(int)buf[0]) | ((0x00FF&(int)buf[1])<<8);
            encodedSize = 3;
        } else if (first == 254) {
            // 32 bits.
            byte[] buf = new byte[4];
            count = in.read(buf, 0, 4);
            if (count < 4)
                throw new EOFException("End-of-data while processing VarInt");

            value = Utils.readUint32LE(buf, 0);
            encodedSize = 5;
        } else {
            // 64 bits.
            byte[] buf = new byte[8];
            count = in.read(buf, 0, 8);
            if (count < 8)
                throw new EOFException("End-of-data while processing VarInt");

            value = Utils.readUint64LE(buf, 0);
            encodedSize = 9;
        }
    }

    /**
     * Returns the value of thie VarInt as an int
     *
     * @return                      Integer value
     */
    public int toInt() {
        return (int)value;
    }

    /**
     * Returns the value of this VarInt as a long
     *
     * @return                      Long value
     */
    public long toLong() {
        return value;
    }

    /**
     * Returns the encoded size of this VarInt
     *
     * @return      Encoded size
     */
    public int getEncodedSize() {
        return encodedSize;
    }

    /**
     * Returns the encoded VarInt size
     *
     * @param       bytes           Encoded byte stream
     * @param       offset          Offset of the encoded VarInt
     * @return      Encoded size
     */
    public static int sizeOf(byte[] bytes, int offset) {
        int length;
        int varLength = (int)bytes[offset]&0xff;
        if (varLength < 253)
            length = 1;
        else if (varLength == 253)
            length = 3;
        else if (varLength == 254)
            length = 5;
        else
            length = 9;
        return length;
    }

    /**
     * Returns the encoded size of the given unsigned integer value.
     *
     * @param       value           Value to be encoded
     * @return      Encoded size
     */
    public static int sizeOf(int value) {
        int minSize;
        long tValue = ((long)value)&0xffffffffL;

        if (tValue < 253L)
            minSize = 1;            // Single data byte
        else if (tValue < 65536L)
            minSize = 3;            // 1 marker + 2 data bytes
        else
            minSize = 5;            // 1 marker + 4 data bytes

        return minSize;
    }

    /**
     * Returns the encoded size of the given unsigned long value
     *
     * @param       value           Value to be encoded
     * @return      Encoded size
     */
    public static int sizeOf(long value) {
        int minSize;
        if ((value&0xFFFFFFFF00000000L) != 0) {
            // 1 marker + 8 data bytes
            minSize = 9;
        } else if ((value&0x00000000FFFF0000L) != 0) {
            // 1 marker + 4 data bytes
            minSize = 5;
        } else if (value >= 253L) {
            // 1 marker + 2 data bytes
            minSize = 3;
        } else {
            // Single data byte
            minSize = 1;
        }

        return minSize;
    }

    /**
     * Encode the value in little-endian format
     *
     * @return                      Encoded byte stream
     */
    public byte[] encode() {
        return encode(value);
    }

    /**
     * Encode the value in little-endian format
     *
     * @param       value           Value to encode
     * @return                      Byte array
     */
    public static byte[] encode(long value) {
        byte[] bytes;
        if ((value&0xFFFFFFFF00000000L) != 0) {
            // 1 marker + 8 data bytes
            bytes = new byte[9];
            bytes[0] = (byte)255;
            Utils.uint64ToByteArrayLE(value, bytes, 1);
        } else if ((value&0x00000000FFFF0000L) != 0) {
            // 1 marker + 4 data bytes
            bytes = new byte[5];
            bytes[0] = (byte)254;
            Utils.uint32ToByteArrayLE(value, bytes, 1);
        } else if (value >= 253L) {
            // 1 marker + 2 data bytes
            bytes = new byte[]{(byte)253, (byte)value, (byte)(value>>8)};
        } else {
            // Single data byte
            bytes = new byte[]{(byte)value};
        }
        return bytes;
    }
}
