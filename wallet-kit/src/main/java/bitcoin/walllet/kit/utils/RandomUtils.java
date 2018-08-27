package bitcoin.walllet.kit.utils;

import java.security.SecureRandom;
import java.util.Random;

public class RandomUtils {

    private static final Random rnd = new SecureRandom();

    public static long randomLong() {
        return (long) (rnd.nextDouble() * Long.MAX_VALUE);
    }

    public static int randomInt() {
        return (int) (rnd.nextDouble() * Integer.MAX_VALUE);
    }
}
