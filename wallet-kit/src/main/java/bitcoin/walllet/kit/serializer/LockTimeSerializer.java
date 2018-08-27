package bitcoin.walllet.kit.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Serialize long to lock time.
 *
 * @author Michael Liao
 */
public class LockTimeSerializer extends JsonSerializer<Long> {

    /**
     * 0 Not locked. < 500000000 Block number at which this transaction is
     * unlocked >= 500000000 UNIX timestamp at which this transaction is
     * unlocked
     */
    @Override
    public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException, JsonProcessingException {
        long n = value;
        if (n == 0) {
            gen.writeNumber(n);
        } else if (n < 500000000L) {
            gen.writeNumber(n);
        } else {
            String s = Instant.ofEpochSecond(n).atZone(ZoneId.of("Z")).toString();
            gen.writeString(s);
        }
    }

}
