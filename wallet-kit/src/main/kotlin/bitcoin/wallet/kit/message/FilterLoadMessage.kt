package bitcoin.wallet.kit.message

import bitcoin.wallet.kit.crypto.BloomFilter
import bitcoin.walllet.kit.network.message.Message

class FilterLoadMessage() : Message("filterload") {
    lateinit var bloomFilter: BloomFilter

    constructor(filter: BloomFilter) : this() {
        bloomFilter = filter
    }

    override fun getPayload(): ByteArray {
        return bloomFilter.toByteArray()
    }

    override fun toString(): String {
        return "FilterLoad message: " + bloomFilter.toString()
    }
}
