package bitcoin.wallet.kit.models

import bitcoin.walllet.kit.common.io.BitcoinInput
import bitcoin.walllet.kit.common.serializer.TimestampSerializer
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import java.io.IOException

class NetworkAddressTimestamp @Throws(IOException::class) constructor(input: BitcoinInput) {

    /**
     * uint32
     */
    @JsonSerialize(using = TimestampSerializer::class)
    var timestamp: Long = 0

    var address: NetworkAddress

    init {
        timestamp = input.readUnsignedInt()
        address = NetworkAddress(input, true)
    }
}
