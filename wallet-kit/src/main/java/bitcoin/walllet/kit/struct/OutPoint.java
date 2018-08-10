package bitcoin.walllet.kit.struct;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;

import bitcoin.walllet.kit.common.io.BitcoinInput;
import bitcoin.walllet.kit.common.io.BitcoinOutput;
import bitcoin.walllet.kit.common.serializer.HashSerializer;

public class OutPoint {

    /**
     * 32-bytes, the hash of the referenced transaction.
     */
    @JsonSerialize(using = HashSerializer.class)
    public byte[] hash;

    /**
     * uint32, the index of the specific output in the transaction. The first
     * output is 0, etc.
     */
    public long index;

    public OutPoint(BitcoinInput input) throws IOException {
        this.hash = input.readBytes(32);
        this.index = input.readUnsignedInt();
    }

    public byte[] toByteArray() {
        return new BitcoinOutput().write(hash).writeUnsignedInt(index).toByteArray();
    }
}
