package bitcoin.wallet.kit.messages

import bitcoin.wallet.kit.crypto.BloomFilter

/**
 * FilterLoad Message
 *
 *   Size       Field           Description
 *   ====       =====           ===========
 *   VarInt     byteCount       Number of bytes in the filter (BloomFilter.MAX_FILTER_SIZE)
 *   Variable   filter          Bloom filter
 *   4 bytes    nHashFuncs      Number of hash functions (BloomFilter.MAX_HASH_FUNCS)
 *   4 bytes    nTweak          Random value to add to seed value
 *   1 byte     nFlags          Matching flags
 */
class FilterLoadMessage() : Message("filterload") {
    lateinit var filter: BloomFilter

    constructor(bloomFilter: BloomFilter) : this() {
        filter = bloomFilter
    }

    override fun getPayload(): ByteArray {
        return filter.toByteArray()
    }

    override fun toString(): String {
        return "FilterLoad message: " + filter.toString()
    }
}
