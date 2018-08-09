package bitcoin.walllet.kit.struct;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;

import bitcoin.walllet.kit.common.io.BitcoinInput;
import bitcoin.walllet.kit.common.io.BitcoinOutput;
import bitcoin.walllet.kit.common.serializer.SatoshiSerializer;

public class TxOut {

    /**
     * int64, Transaction Value
     */
    @JsonSerialize(using = SatoshiSerializer.class)
    public long value;

    /**
     * uchar[], Usually contains the public key as a Bitcoin script setting up
     * conditions to claim this output.
     */
    public byte[] pk_script;

    public TxOut(BitcoinInput input) throws IOException {
        this.value = input.readLong();
        long scriptLength = input.readVarInt();
        this.pk_script = input.readBytes((int) scriptLength);
    }

    public byte[] toByteArray() {
        return new BitcoinOutput().writeLong(value).writeVarInt(this.pk_script.length).write(pk_script).toByteArray();
    }
}
