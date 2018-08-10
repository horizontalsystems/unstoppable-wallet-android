package bitcoin.walllet.kit.struct;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;
import java.net.InetAddress;

import bitcoin.walllet.kit.common.constant.BitcoinConstants;
import bitcoin.walllet.kit.common.io.BitcoinInput;
import bitcoin.walllet.kit.common.io.BitcoinOutput;
import bitcoin.walllet.kit.common.serializer.IPv6Serializer;
import bitcoin.walllet.kit.common.serializer.TimestampSerializer;
import bitcoin.walllet.kit.common.util.NetworkUtils;


public class NetworkAddress {

    /**
     * uint32, the Time (version >= 31402). Not present in version message.
     */
    @JsonSerialize(using = TimestampSerializer.class)
    public long time;

    /**
     * uint64, same service(s) listed in version
     */
    public long services;

    /**
     * 16 bytes IPv6 address. Network byte order. the IPv4 address is 12 bytes
     * 00 00 00 00 00 00 00 00 00 00 FF FF, followed by the 4 bytes of the IPv4
     * address
     */
    @JsonSerialize(using = IPv6Serializer.class)
    public byte[] ipv6;

    /**
     * uint16, port number
     */
    public int port;

    public NetworkAddress() {
    }

    public NetworkAddress(BitcoinInput input, boolean excludeTime) throws IOException {
        if (!excludeTime) {
            this.time = input.readUnsignedInt();
        }
        this.services = input.readLong();
        this.ipv6 = input.readBytes(16);
        this.port = input.readUnsignedShort();
    }

    public NetworkAddress(InetAddress addr) {
        this.time = System.currentTimeMillis() / 1000;
        this.services = 1;
        this.ipv6 = NetworkUtils.getIPv6(addr);
        this.port = BitcoinConstants.PORT;
    }

    public static NetworkAddress parse(BitcoinInput input, boolean excludeTime) throws IOException {
        NetworkAddress addr = new NetworkAddress();
        if (!excludeTime) {
            addr.time = input.readUnsignedInt();
        }
        addr.services = input.readLong();
        addr.ipv6 = input.readBytes(16);
        addr.port = input.readUnsignedShort();
        return addr;
    }

    public byte[] toByteArray(boolean excludeTime) {
        BitcoinOutput output = new BitcoinOutput();
        if (!excludeTime) {
            output.writeUnsignedInt(this.time); // time
        }
        output.writeLong(this.services) // service
                .write(this.ipv6) // ipv6
                .writeUnsignedShort(this.port); // port
        return output.toByteArray();
    }

}
