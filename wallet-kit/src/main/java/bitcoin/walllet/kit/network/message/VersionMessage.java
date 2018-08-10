package bitcoin.walllet.kit.network.message;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;

import bitcoin.walllet.kit.common.constant.BitcoinConstants;
import bitcoin.walllet.kit.common.io.BitcoinInput;
import bitcoin.walllet.kit.common.io.BitcoinOutput;
import bitcoin.walllet.kit.common.util.NetworkUtils;
import bitcoin.walllet.kit.struct.NetworkAddress;

/**
 * VersionMessage sends version info.
 *
 * @author liaoxuefeng
 */
public class VersionMessage extends Message {

    public int protocolVersion;
    public long services;
    public long timestamp;

    public NetworkAddress recipientAddress;
    public NetworkAddress senderAddress;

    public long nonce;
    public String subVersion;

    public int lastBlock;
    public boolean relay;

    public VersionMessage(byte[] payload) throws IOException {
        super("version");
        try (BitcoinInput input = new BitcoinInput(new ByteArrayInputStream(payload))) {
            this.protocolVersion = input.readInt();
            this.services = input.readLong();
            this.timestamp = input.readLong();
            this.recipientAddress = NetworkAddress.parse(input, true);
            if (this.protocolVersion >= 106) {
                this.senderAddress = NetworkAddress.parse(input, true);
                this.nonce = input.readLong();
                this.subVersion = input.readString();
                this.lastBlock = input.readInt();
                if (this.protocolVersion >= 70001) {
                    this.relay = input.readByte() != 0;
                }
            }
        }
    }

    public VersionMessage() {
        super("version");
    }

    public VersionMessage(int lastBlock, InetAddress recipientAddr) {
        super("version");
        this.protocolVersion = BitcoinConstants.PROTOCOL_VERSION;
        this.services = BitcoinConstants.NETWORK_SERVICES;
        this.timestamp = System.currentTimeMillis() / 1000;
        this.recipientAddress = new NetworkAddress(recipientAddr);
        this.senderAddress = new NetworkAddress(NetworkUtils.getLocalInetAddress());
        this.nonce = BitcoinConstants.NODE_ID;
        this.subVersion = BitcoinConstants.SUB_VERSION;
        this.lastBlock = lastBlock;
        this.relay = true;
    }

    @Override
    protected byte[] getPayload() {
        BitcoinOutput output = new BitcoinOutput();
        output.writeInt(this.protocolVersion) // protocol
                .writeLong(this.services) // services
                .writeLong(timestamp) // timestamp
                .write(this.recipientAddress.toByteArray(true)); // recipient-address
        if (this.protocolVersion >= 106) {
            output.write(this.senderAddress.toByteArray(true)) // sender-address
                    .writeLong(this.nonce) // nodeId
                    .writeString(this.subVersion) // sub-version-string
                    .writeInt(this.lastBlock); // # of last block
            if (this.protocolVersion >= 70001) {
                output.writeByte(1);
            }
        }
        return output.toByteArray();
    }

    @Override
    public String toString() {
        return "VersionMessage(lastBlock=" + this.lastBlock + ", protocol=" + this.protocolVersion + ", timestamp="
                + this.timestamp + ")";
    }
}
