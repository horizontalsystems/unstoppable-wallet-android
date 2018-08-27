package bitcoin.walllet.kit.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

import bitcoin.walllet.kit.utils.HashUtils;

/**
 * Serialize byte[] to IPv6 address.
 *
 * @author Michael Liao
 */
public class IPv6Serializer extends JsonSerializer<byte[]> {

    static final String IP_V4_PREFIX = "00000000000000000000ffff";

    @Override
    public void serialize(byte[] value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException, JsonProcessingException {
        // the IPv4 address is 12 bytes 00 00 00 00 00 00 00 00 00 00 FF FF,
        // followed by the 4 bytes of the IPv4 address
        if (value.length != 16) {
            throw new IllegalArgumentException("Invalid IPv6 address: " + HashUtils.toHexString(value));
        }
        String s = null;
        String ip = HashUtils.toHexString(value);
        if (ip.startsWith(IP_V4_PREFIX)) {
            byte[] ipv4 = HashUtils.toBytes(ip.substring(IP_V4_PREFIX.length()));
            s = (ipv4[0] & 0xff) + "." + (ipv4[1] & 0xff) + "." + (ipv4[2] & 0xff) + "." + (ipv4[3] & 0xff);
        } else {
            s = ip.substring(0, 4) + ":" + ip.substring(4, 8) + ":" + ip.substring(8, 12) + ":" + ip.substring(12, 16)
                    + ":" + ip.substring(16, 20) + ":" + ip.substring(20, 24) + ":" + ip.substring(24, 28) + ":"
                    + ip.substring(28);
        }
        gen.writeString(s);
    }

}
