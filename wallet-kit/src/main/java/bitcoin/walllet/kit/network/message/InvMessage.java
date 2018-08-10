package bitcoin.walllet.kit.network.message;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

import bitcoin.walllet.kit.common.io.BitcoinInput;
import bitcoin.walllet.kit.common.io.BitcoinOutput;
import bitcoin.walllet.kit.common.util.HashUtils;
import bitcoin.walllet.kit.struct.InvVect;

public class InvMessage extends Message {

    InvVect[] inventory;

    public InvMessage() {
        super("inv");
        inventory = new InvVect[0];
    }

    public InvMessage(byte[] payload) throws IOException {
        super("inv");
        try (BitcoinInput input = new BitcoinInput(new ByteArrayInputStream(payload))) {
            long count = input.readVarInt(); // do not store count
            this.inventory = new InvVect[(int) count];
            for (int i = 0; i < this.inventory.length; i++) {
                this.inventory[i] = new InvVect(input);
            }
        }
    }

    public byte[][] getBlockHashes() {
        return Arrays.stream(this.inventory).filter((iv) -> {
            return iv.type == InvVect.MSG_BLOCK;
        }).map((iv) -> {
            return iv.hash;
        }).toArray(byte[][]::new);
    }

    public String[] getBlockHashesAsString() {
        return Arrays.stream(this.inventory).filter((iv) -> {
            return iv.type == InvVect.MSG_BLOCK;
        }).map((iv) -> {
            return HashUtils.toHexStringAsLittleEndian(iv.hash);
        }).toArray(String[]::new);
    }

    @Override
    protected byte[] getPayload() {
        BitcoinOutput output = new BitcoinOutput();
        output.writeVarInt(this.inventory.length);
        for (int i = 0; i < this.inventory.length; i++) {
            output.write(this.inventory[i].toByteArray());
        }
        return output.toByteArray();
    }

    @Override
    public String toString() {
        return "InvMessage(" + this.inventory.length + ": ["
                + String.join(", ", Arrays.stream(this.inventory).map((inv) -> {
            return inv.type + ":" + HashUtils.toHexStringAsLittleEndian(inv.hash);
        }).limit(10).toArray(String[]::new)) + "])";
    }
}
