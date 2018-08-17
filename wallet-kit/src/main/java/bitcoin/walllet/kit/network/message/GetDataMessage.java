package bitcoin.walllet.kit.network.message;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import bitcoin.walllet.kit.common.io.BitcoinInput;
import bitcoin.walllet.kit.common.io.BitcoinOutput;
import bitcoin.walllet.kit.struct.InvVect;

/**
 * Build P2P message:
 * https://en.bitcoin.it/wiki/Protocol_documentation#Message_structure
 *
 * @author liaoxuefeng
 */
public class GetDataMessage extends Message {

    InvVect[] inventory; // byte[36]

    public GetDataMessage(int type, byte[][] hashes) {
        super("getdata");
        this.inventory = new InvVect[hashes.length];
        for (int i = 0; i < this.inventory.length; i++) {
            InvVect iv = new InvVect();
            iv.type = type;
            iv.hash = hashes[i];
            this.inventory[i] = iv;
        }
    }

    public GetDataMessage(byte[] payload) throws IOException {
        super("getdata");
        try (BitcoinInput input = new BitcoinInput(new ByteArrayInputStream(payload))) {
            long count = input.readVarInt(); // do not store count
            this.inventory = new InvVect[(int) count];
            for (int i = 0; i < this.inventory.length; i++) {
                this.inventory[i] = new InvVect(input);
            }
        }
    }

    @Override
    protected byte[] getPayload() {
        BitcoinOutput output = new BitcoinOutput().writeVarInt(this.inventory.length);
        for (int i = 0; i < this.inventory.length; i++) {
            output.write(this.inventory[i].toByteArray());
        }
        return output.toByteArray();
    }

    @Override
    public String toString() {
        return "GetDataMessage(count=" + this.inventory.length + ")";
    }

}
