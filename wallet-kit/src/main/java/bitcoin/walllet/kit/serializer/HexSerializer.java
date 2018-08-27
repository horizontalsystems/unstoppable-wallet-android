package bitcoin.walllet.kit.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

import bitcoin.walllet.kit.utils.HashUtils;

/**
 * Serialize byte[] to hex string.
 *
 * @author Michael Liao
 */
public class HexSerializer extends JsonSerializer<byte[]> {

    @Override
    public void serialize(byte[] value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
        String s = HashUtils.toHexString(value);
        gen.writeString(s);
    }

}
