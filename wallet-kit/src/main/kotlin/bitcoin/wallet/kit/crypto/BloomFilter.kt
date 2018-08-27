package bitcoin.wallet.kit.crypto

import bitcoin.walllet.kit.hdwallet.Utils
import bitcoin.walllet.kit.io.BitcoinOutput
import bitcoin.walllet.kit.crypto.MurmurHash3
import java.lang.Double.valueOf

/**
 * BloomFilter
 *
 *   Size       Field           Description
 *   ====       =====           ===========
 *   VarInt     Count           Number of bytes in the filter
 *   Variable   Filter          Filter data
 *   4 bytes    nHashFuncs      Number of hash functions
 *   4 bytes    nTweak          Random value to add to the hash seed
 *   1 byte     nFlags          Filter update flags
 */
class BloomFilter(elements: Int) {

    /** Filter data  */
    private val filter: ByteArray

    /** Number of hash functions  */
    private val nHashFuncs: Int

    /** Random tweak nonce  */
    private val nTweak = valueOf(Math.random() * Long.MAX_VALUE).toLong()

    /** Filter update flags  */
    private val nFlags = UPDATE_NONE

    init {
        //
        // We will use a false-positive rate of 0.0005 (0.05%)
        //
        val falsePositiveRate = 0.0005
        //
        // Allocate the filter array
        //
        val size = Math.min((-1 / Math.pow(Math.log(2.0), 2.0) * elements.toDouble() * Math.log(falsePositiveRate)).toInt(),
                MAX_FILTER_SIZE * 8) / 8
        filter = ByteArray(if (size <= 0) 1 else size)
        //
        // Optimal number of hash functions for a given filter size and element count.
        //
        nHashFuncs = Math.min((filter.size * 8 / elements.toDouble() * Math.log(2.0)).toInt(), MAX_HASH_FUNCS)
    }

    /**
     * Inserts an bytes into the filter
     *
     * @param   bytes    Object to insert
     */
    fun insert(bytes: ByteArray) {
        for (i in 0 until nHashFuncs) {
            Utils.setBitLE(filter, MurmurHash3.hash(filter, nTweak, i, bytes))
        }
    }

    /**
     * Serialize the filter
     */
    fun toByteArray(): ByteArray {
        val output = BitcoinOutput()
        output.writeVarInt(filter.size.toLong())
        output.write(filter)
        output.writeInt(nHashFuncs)
        output.writeUnsignedInt(nTweak)
        output.writeByte(nFlags)

        return output.toByteArray()
    }

    override fun toString(): String {
        return "Bloom Filter of size ${filter.size} with $nHashFuncs hash functions."
    }

    companion object {

        /** Bloom filter - Filter is not adjusted for matching outputs  */
        val UPDATE_NONE = 0

        /** Bloom filter - Filter is adjusted for all matching outputs  */
        val UPDATE_ALL = 1

        /** Bloom filter - Filter is adjusted only for pay-to-pubkey or pay-to-multi-sig  */
        val UPDATE_P2PUBKEY_ONLY = 2

        /** Maximum filter size  */
        val MAX_FILTER_SIZE = 36000

        /** Maximum number of hash functions  */
        val MAX_HASH_FUNCS = 50
    }
}
