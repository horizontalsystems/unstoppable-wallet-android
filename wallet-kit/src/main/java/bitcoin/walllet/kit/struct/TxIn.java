package bitcoin.walllet.kit.struct;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;

import bitcoin.walllet.kit.common.io.BitcoinInput;
import bitcoin.walllet.kit.common.io.BitcoinOutput;
import bitcoin.walllet.kit.common.serializer.HexSerializer;
import bitcoin.walllet.kit.common.util.BytesUtils;

public class TxIn {

    public OutPoint previousOutput;

    @JsonSerialize(using = HexSerializer.class)
    public byte[] sigScript;

    /**
     * uint32, Transaction version as defined by the sender. Intended for
     * "replacement" of transactions when information is updated before
     * inclusion into a block.
     */
    public long sequence;

    public TxIn(BitcoinInput input) throws IOException {
        this.previousOutput = new OutPoint(input);
        long sigScriptLength = input.readVarInt();
        this.sigScript = input.readBytes((int) sigScriptLength);
        this.sequence = input.readUnsignedInt();
    }

    public boolean isCoinbase() {
        return this.previousOutput != null && this.previousOutput.hash != null
                && BytesUtils.isZeros(this.previousOutput.hash);
    }

    public byte[] toByteArray() {
        return new BitcoinOutput().write(this.previousOutput.toByteArray()).writeVarInt(this.sigScript.length)
                .write(this.sigScript).writeUnsignedInt(this.sequence).toByteArray();
    }

    public void validate() {

        // throw new ValidateException("Verify signature failed.");
    }
}
