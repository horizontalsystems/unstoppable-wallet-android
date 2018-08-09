package bitcoin.walllet.kit.network.message;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import bitcoin.walllet.kit.common.io.BitcoinInput;
import bitcoin.walllet.kit.common.io.BitcoinOutput;
import bitcoin.walllet.kit.struct.TimestampNetworkAddress;

/**
 * Build P2P message:
 * https://en.bitcoin.it/wiki/Protocol_documentation#Message_structure
 *
 * @author liaoxuefeng
 */
public class AddrMessage extends Message {

    TimestampNetworkAddress[] addr_list; // (uint32_t + net_addr)[]

    public AddrMessage() {
        super("addr");
        this.addr_list = new TimestampNetworkAddress[0];
    }

    public AddrMessage(byte[] payload) throws IOException {
        super("addr");
        try (BitcoinInput input = new BitcoinInput(new ByteArrayInputStream(payload))) {
            long count = input.readVarInt(); // do not store count
            this.addr_list = new TimestampNetworkAddress[(int) count];
            for (int i = 0; i < this.addr_list.length; i++) {
                addr_list[i] = new TimestampNetworkAddress(input);
            }
        }
    }

    @Override
    protected byte[] getPayload() {
        BitcoinOutput output = new BitcoinOutput();
        output.writeVarInt(this.addr_list.length);
        for (int i = 0; i < this.addr_list.length; i++) {
            TimestampNetworkAddress taddr = this.addr_list[i];
            output.writeUnsignedInt(taddr.timestamp);
            output.write(taddr.address.toByteArray(false));
        }
        return output.toByteArray();
    }

    @Override
    public String toString() {
        return "AddrMessage(count=" + this.addr_list.length + ")";
    }

}
