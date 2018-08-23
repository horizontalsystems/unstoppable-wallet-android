package bitcoin.walllet.kit.common.util;

/**
 * Applies the MurmurHash3 (x86_32) algorithm to the given data.
 * See this <a href="https://github.com/aappleby/smhasher/blob/master/src/MurmurHash3.cpp">C++ code for the original.</a>
 */
public final class MurmurHash3 {

    /**
     * Rotate a 32-bit value left by the specified number of bits
     *
     * @param       x               The bit value
     * @param       count           The number of bits to rotate
     * @return                      The rotated value
     */
    private static int rotateLeft32(int x, int count) {
        return (x<<count) | (x>>>(32-count));
    }

    /**
     * Performs a MurmurHash3
     *
     * @param       filter          Filter data
     * @param       nTweak          Random value to add to the hash seed
     * @param       hashNum         The hash number
     * @param       object          The byte array to hash
     * @return                      The hash of the object using the specified hash number
     */
    public static int hash(byte[] filter, long nTweak, int hashNum, byte[] object) {
        int h1 = (int)(hashNum * 0xFBA4C795L + nTweak);
        final int c1 = 0xcc9e2d51;
        final int c2 = 0x1b873593;

        int numBlocks = (object.length / 4) * 4;
        // body
        for(int i = 0; i < numBlocks; i += 4) {
            int k1 = (object[i] & 0xFF) |
                    ((object[i+1] & 0xFF) << 8) |
                    ((object[i+2] & 0xFF) << 16) |
                    ((object[i+3] & 0xFF) << 24);

            k1 *= c1;
            k1 = rotateLeft32(k1, 15);
            k1 *= c2;

            h1 ^= k1;
            h1 = rotateLeft32(h1, 13);
            h1 = h1*5+0xe6546b64;
        }

        int k1 = 0;
        switch(object.length & 3) {
            case 3:
                k1 ^= (object[numBlocks + 2] & 0xff) << 16;
                // Fall through.
            case 2:
                k1 ^= (object[numBlocks + 1] & 0xff) << 8;
                // Fall through.
            case 1:
                k1 ^= (object[numBlocks] & 0xff);
                k1 *= c1; k1 = rotateLeft32(k1, 15); k1 *= c2; h1 ^= k1;
                // Fall through.
            default:
                // Do nothing.
                break;
        }

        // finalization
        h1 ^= object.length;
        h1 ^= h1 >>> 16;
        h1 *= 0x85ebca6b;
        h1 ^= h1 >>> 13;
        h1 *= 0xc2b2ae35;
        h1 ^= h1 >>> 16;

        return (int)((h1&0xFFFFFFFFL) % (filter.length * 8));
    }
}
