package bitcoin.walllet.kit.struct;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;

import bitcoin.walllet.kit.common.io.BitcoinInput;
import bitcoin.walllet.kit.common.serializer.TimestampSerializer;

/**
 * Timestamp and network address.
 *
 * @author Michael Liao
 */
public class TimestampNetworkAddress {

    /**
     * uint32
     */
    @JsonSerialize(using = TimestampSerializer.class)
    public long timestamp;

    public NetworkAddress address;

    public TimestampNetworkAddress(BitcoinInput input) throws IOException {
        this.timestamp = input.readUnsignedInt();
        this.address = new NetworkAddress(input, true);
    }
}
