/*
 * Copyright 2014-2016 Ronald Hoffman.
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
package bitcoin.walllet.kit.common.hdwallet;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

/**
 * SerializedBuffer handles byte stream serialization and deserialization using
 * the little-endian format
 */
public class SerializedBuffer {


    /** UTF-8 character set */
    private static final Charset utf8Charset = Charset.forName("UTF-8");

    /** Default buffer size */
    private static final int defaultSize = 256;

    /** Buffer size increment */
    private static final int incrementSize = 512;

    /** Byte buffer */
    private byte[] bytes;

    /** Buffer start */
    private int bufferStart;

    /** Buffer limit */
    private int bufferLimit;

    /** Current buffer offset */
    private int offset;

    /** Segment start offset */
    private int segmentOffset;

    /**
     * Create a new serialized buffer.  The buffer start is set to the beginning of the
     * byte array and the buffer limit is set to the end of the byte array.
     *
     * @param       buffer              Backing byte array
     */
    public SerializedBuffer(byte[] buffer) {
        this(buffer, 0, buffer.length);
    }

    /**
     * Create a new serialized buffer.  The buffer start is set to the byte array offset
     * and the buffer limit is set to the buffer start plus the array length.
     *
     * @param       buffer              Backing byte array
     * @param       bufOffset           Array offset
     * @param       bufLength           Array length
     */
    public SerializedBuffer(byte[] buffer, int bufOffset, int bufLength) {
        bytes = buffer;
        bufferStart = bufOffset;
        bufferLimit = bufOffset+bufLength;
        offset = bufferStart;
        segmentOffset = bufferStart;
    }

    /**
     * Create a new serialized buffer of the specified size
     *
     * @param       initialSize         Initial buffer size
     */
    public SerializedBuffer(int initialSize) {
        this(new byte[initialSize], 0, initialSize);
    }

    /**
     * Create a new serialized buffer with the default size
     */
    public SerializedBuffer() {
        this(new byte[defaultSize], 0, defaultSize);
    }

    /**
     * Creates a new serialized buffer from an input ByteBuffer.  The buffer start is
     * set to 0 and the buffer limit is set to the ByteBuffer limit.
     *
     * @param       byteBuffer          Input ByteBuffer
     */
    public SerializedBuffer(ByteBuffer byteBuffer) {
        bytes = byteBuffer.array();
        bufferStart = 0;
        bufferLimit = byteBuffer.limit();
        offset = bufferStart;
        segmentOffset = bufferStart;
    }

    /**
     * Return the number of available bytes (buffer limit - current buffer position)
     *
     * @return                          Available byte count
     */
    public int available() {
        return bufferLimit-offset;
    }

    /**
     * Return the buffer start position relative to the backing byte array
     *
     * @return                          Buffer start position
     */
    public int getBufferStart() {
        return bufferStart;
    }

    /**
     * Return the current buffer position relative to the buffer start position
     *
     * @return                          Buffer position
     */
    public int getPosition() {
        return offset-bufferStart;
    }

    /**
     * Set the current buffer position relative to the buffer start position
     *
     * @param       position            Buffer position
     */
    public void setPosition(int position) {
        offset = Math.min(bufferStart+position, bufferLimit);
    }

    /**
     * Return the current segment start relative to the buffer start position
     *
     * @return                          Segment start
     */
    public int getSegmentStart() {
        return segmentOffset-bufferStart;
    }

    /**
     * Start a new segment at the current buffer position
     */
    public void setSegmentStart() {
        segmentOffset = offset;
    }

    /**
     * Start a new segment at the specified buffer position relative to the buffer start position
     *
     * @param       position            Buffer position
     */
    public void setSegmentStart(int position) {
        segmentOffset = Math.min(bufferStart+position, bufferLimit);
    }

    /**
     * Skip one or more bytes.  If the skip count exceeds the buffer limit, the
     * current buffer position will be set to the buffer limit.
     *
     * @param       count               Skip count
     */
    public void skip(int count) {
        offset = Math.min(offset+count, bufferLimit);
    }

    /**
     * Reset the buffer and segment offsets to the buffer start
     */
    public void rewind() {
        offset = bufferStart;
        segmentOffset = bufferStart;
    }

    /**
     * Return the backing array for this buffer
     *
     * @return                          Byte array
     */
    public byte[] array() {
        return bytes;
    }

    /**
     * Return the byte array for the serialized buffer.  A new array will be allocated
     * if the used portion of the buffer is not the same size as the backing array.
     *
     * @return                          Byte array
     */
    public byte[] toByteArray() {
        return (offset-bufferStart!=bytes.length ? Arrays.copyOfRange(bytes, bufferStart, offset) : bytes);
    }

    /**
     * Return a ByteBuffer for the serialized buffer.  The ByteBuffer will use the
     * little-endian format.
     *
     * @return                          ByteBuffer
     */
    public ByteBuffer toByteBuffer() {
        return ByteBuffer.wrap(bytes, bufferStart, offset).order(ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * Return a ByteArrayInputStream for the serialized buffer
     *
     * @return                          ByteArrayInputStream
     */
    public ByteArrayInputStream toInputStream() {
        return new ByteArrayInputStream(bytes, bufferStart, offset);
    }

    /**
     * Return a byte value
     *
     * @return                          Byte value
     * @throws      EOFException        Byte array underrun
     */
    public byte getByte() throws EOFException {
        if (offset == bufferLimit)
            throw new EOFException("Byte array underrun");
        return bytes[offset++];
    }

    /**
     * Return an unsigned byte value
     *
     * @return                          Unsigned byte value
     * @throws      EOFException        Byte array underrun
     */
    public int getUnsignedByte() throws EOFException {
        return (int)getByte()&255;
    }

    /**
     * Store a byte value
     *
     * @param       val                 Byte value
     * @return                          This buffer
     */
    public SerializedBuffer putByte(byte val) {
        if (offset == bufferLimit)
            reallocateArray(1);
        bytes[offset++] = val;
        return this;
    }

    /**
     * Store an unsigned byte value
     *
     * @param       val                 Unsigned byte value
     * @return                          This buffer
     */
    public SerializedBuffer putUnsignedByte(int val) {
        return putByte((byte)val);
    }

    /**
     * Return a 2-byte unsigned short value
     *
     * @return                          Short value
     * @throws      EOFException        Byte array underrun
     */
    public int getUnsignedShort() throws EOFException {
        if (bufferLimit-offset < 2)
            throw new EOFException("Byte array underrun");
        return ((int)bytes[offset++]&255) | (((int)bytes[offset++]&255)<<8);
    }

    /**
     * Store a 2-byte unsigned short value
     *
     * @param       val                 Short value
     * @return                          This buffer
     */
    public SerializedBuffer putUnsignedShort(int val) {
        if (bufferLimit-offset < 2)
            reallocateArray(2);
        bytes[offset++] = (byte)val;
        bytes[offset++] = (byte)(val>>>8);
        return this;
    }

    /**
     * Return a 4-byte integer value
     *
     * @return                          Integer value
     * @throws      EOFException        Byte array underrun
     */
    public int getInt() throws EOFException {
        if (bufferLimit-offset < 4)
            throw new EOFException("Byte array underrun");
        return ((int)bytes[offset++]&255) |
                        (((int)bytes[offset++]&255)<<8) |
                        (((int)bytes[offset++]&255)<<16) |
                        (((int)bytes[offset++]&255)<<24);
    }

    /**
     * Return a 4-byte unsigned integer value
     *
     * @return                          Unsigned integer value
     * @throws      EOFException        Byte array underrun
     */
    public long getUnsignedInt() throws EOFException {
        return (long)getInt()&0xffffffffL;
    }

    /**
     * Return a variable-length integer value
     *
     * @return                          Integer value
     * @throws      EOFException        Byte array underrun
     */
    public int getVarInt() throws EOFException {
        return (int)decodeVar();
    }

    /**
     * Store a 4-byte integer value
     *
     * @param       val                 Integer value
     * @return                          This buffer
     */
    public SerializedBuffer putInt(int val) {
        if (bufferLimit-offset < 4)
            reallocateArray(4);
        bytes[offset++] = (byte)val;
        bytes[offset++] = (byte)(val>>>8);
        bytes[offset++] = (byte)(val>>>16);
        bytes[offset++] = (byte)(val>>>24);
        return this;
    }

    /**
     * Store a 4-byte unsigned integer value
     *
     * @param       val                 Unsigned integer value
     * @return                          This buffer
     */
    public SerializedBuffer putUnsignedInt(long val) {
        return putInt((int)val);
    }

    /**
     * Store a variable-length unsigned integer value
     *
     * @param       val                 Integer value
     * @return                          This buffer
     */
    public SerializedBuffer putVarInt(int val) {
        return encodeVar((long)val&0x00000000ffffffffL);
    }

    /**
     * Return an 8-byte long value
     *
     * @return                          Long value
     * @throws      EOFException        Byte array underrun
     */
    public long getLong() throws EOFException {
        if (bufferLimit-offset < 8)
            throw new EOFException("Byte array underrun");
        return ((long)bytes[offset++]&255) |
                        (((long)bytes[offset++]&255)<<8) |
                        (((long)bytes[offset++]&255)<<16) |
                        (((long)bytes[offset++]&255)<<24) |
                        (((long)bytes[offset++]&255)<<32) |
                        (((long)bytes[offset++]&255)<<40) |
                        (((long)bytes[offset++]&255)<<48) |
                        (((long)bytes[offset++]&255)<<56);
    }

    /**
     * Return a variable-length long value
     *
     * @return                          Long value
     * @throws      EOFException        Byte array underrun
     */
    public long getVarLong() throws EOFException {
        return decodeVar();
    }

    /**
     * Store an 8-byte long value
     *
     * @param       val                 Long value
     * @return                          This buffer
     */
    public SerializedBuffer putLong(long val) {
        if (bufferLimit-offset < 8)
            reallocateArray(8);
        bytes[offset++] = (byte)val;
        bytes[offset++] = (byte)(val>>>8);
        bytes[offset++] = (byte)(val>>>16);
        bytes[offset++] = (byte)(val>>>24);
        bytes[offset++] = (byte)(val>>>32);
        bytes[offset++] = (byte)(val>>>40);
        bytes[offset++] = (byte)(val>>>48);
        bytes[offset++] = (byte)(val>>>56);
        return this;
    }

    /**
     * Store a variable-length long value
     *
     * @param       val                 Long value
     * @return                          This buffer
     */
    public SerializedBuffer putVarLong(long val) {
        return encodeVar(val);
    }

    /**
     * Return a boolean value
     *
     * @return                          Boolean value
     * @throws      EOFException        End-of-data processing stream
     */
    public boolean getBoolean() throws EOFException {
        return (getByte()!=0);
    }

    /**
     * Store a boolean value
     *
     * @param       val                 Boolean value
     * @return                          This buffer
     */
    public SerializedBuffer putBoolean(boolean val) {
        return putByte(val ? (byte)1 : (byte)0);
    }

    /**
     * Return a string value.  The string length is encoded as a variable-length
     * integer followed by the UTF-8 string representation.
     *
     * @return                          String value
     * @throws      EOFException        End-of-data processing stream
     */
    public String getString() throws EOFException {
        int count = getVarInt();
        return (count!=0 ? new String(getBytes(count), utf8Charset) : "");
    }

    /**
     * Store a string value.  The string length is encoded as a variable-length
     * integer followed by the UTF-8 string representation.
     *
     * @param       string              String value
     * @return                          This buffer
     */
    public SerializedBuffer putString(String string) {
        byte[] stringBytes = string.getBytes(utf8Charset);
        return putVarInt(stringBytes.length).putBytes(stringBytes);
    }

    /**
     * Return bytes in a new buffer.  The byte array length is a variable-length integer preceding
     * the bytes.
     *
     * @return                          Byte array
     * @throws      EOFException        Byte array underrun
     */
    public byte[] getBytes() throws EOFException {
        return getBytes(getVarInt());
    }

    /**
     * Return bytes in a new buffer
     *
     * @param       length              Number of bytes to read
     * @return                          Byte array
     * @throws      EOFException        Byte array underrun
     */
    public byte[] getBytes(int length) throws EOFException {
        byte[] buffer;
        if (length < 0 || length > bufferLimit-offset)
            throw new EOFException("Byte array underrun");
        if (length > 0) {
            buffer = Arrays.copyOfRange(bytes, offset, offset+length);
            offset += length;
        } else {
            buffer = new byte[0];
        }
        return buffer;
    }

    /**
     * Return bytes in the supplied array (the array length is the number of bytes to read)
     *
     * @param       buffer              Buffer
     * @throws      EOFException        Byte array underrun
     */
    public void getBytes(byte[] buffer) throws EOFException {
        getBytes(buffer, 0, buffer.length);
    }

    /**
     * Return bytes in the supplied array starting at the specified offset and for the
     * specified length
     *
     * @param       buffer              Buffer
     * @param       bufOffset           Starting offset in the buffer
     * @param       length              Number of bytes to return
     * @throws      EOFException        Byte array underrun
     */
    public void getBytes(byte[] buffer, int bufOffset, int length) throws EOFException {
        if (bufferLimit-offset < length)
            throw new EOFException("Byte array underrun");
        if (length > 0) {
            System.arraycopy(bytes, offset, buffer, bufOffset, length);
            offset += length;
        }
    }

    /**
     * Store a byte array
     *
     * @param       buffer              Buffer
     * @return                          This buffer
     */
    public SerializedBuffer putBytes(byte[] buffer) {
        return putBytes(buffer, 0, buffer.length);
    }

    /**
     * Store a byte array
     *
     * @param       buffer              Buffer
     * @param       bufOffset           Starting offset in the buffer
     * @param       length              Number of bytes to store
     * @return                          This buffer
     */
    public SerializedBuffer putBytes(byte[] buffer, int bufOffset, int length) {
        if (bufferLimit-offset < length)
            reallocateArray(length);
        if (length > 0) {
            System.arraycopy(buffer, bufOffset, bytes, offset, length);
            offset += length;
        }
        return this;
    }

    /**
     * Store a list of byte serializable elements
     *
     * @param       byteList            List of byte serializable elements
     * @return                          This buffer
     */
    public SerializedBuffer putBytes(List<? extends ByteSerializable> byteList) {
        byteList.forEach((elem) -> elem.getBytes(this));
        return this;
    }

    /**
     * Return the bytes in the current segment as a new byte array
     *
     * @return                          Segment bytes
     */
    public byte[] getSegmentBytes() {
        return Arrays.copyOfRange(bytes, segmentOffset, offset);
    }

    /**
     * Allocate a new backing array
     *
     * @param       minLength           Minimum length
     */
    private void reallocateArray(int minLength) {
        int increment = Math.max(minLength, incrementSize);
        if (bufferLimit-bufferStart == bytes.length) {
            bytes = Arrays.copyOf(bytes, bytes.length+increment);
            bufferLimit += increment;
        } else {
            byte[] newBytes = new byte[bufferLimit-bufferStart+increment];
            System.arraycopy(bytes, bufferStart, newBytes, 0, bufferLimit-bufferStart);
            bytes = newBytes;
            offset -= bufferStart;
            segmentOffset -= bufferStart;
            bufferStart = 0;
            bufferLimit = bytes.length;
        }
    }

    /**
     * Decode a variable-length unsigned numeric value
     *
     * @return                          Decoded value
     * @throws      EOFException        Byte array underrun
     */
    private long decodeVar() throws EOFException {
        if (offset == bufferLimit)
            throw new EOFException("Byte array underrun");
        long value;
        int first = (int)bytes[offset++]&255;
        if (first < 253) {
            // 8 bits
            value = first;
        } else if (first == 253) {
            // 16 bits
            value = getUnsignedShort();
        } else if (first == 254) {
            // 32 bits
            value = getUnsignedInt();
        } else {
            // 64 bits
            value = getLong();
        }
        return value;
    }

    /**
     * Encode a variable-length unsigned numeric value
     *
     * @param       val                 Value to be encoded
     * @return                          This buffer
     */
    private SerializedBuffer encodeVar(long val) {
        if (bufferLimit-offset < 9)
            reallocateArray(9);
        if ((val&0xFFFFFFFF00000000L) != 0) {
            bytes[offset++] = (byte)255;
            putLong(val);
        } else if ((val&0x00000000FFFF0000L) != 0) {
            // 1 marker + 4 data bytes
            bytes[offset++] = (byte)254;
            putUnsignedInt(val);
        } else if (val >= 253L) {
            // 1 marker + 2 data bytes
            bytes[offset++] = (byte)253;
            putUnsignedShort((int)val);
        } else {
            // Single data byte
            bytes[offset++] = (byte)val;
        }
        return this;
    }
}
