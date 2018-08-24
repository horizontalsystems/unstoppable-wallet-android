package bitcoin.wallet.kit.message

import bitcoin.walllet.kit.common.io.BitcoinInput
import bitcoin.walllet.kit.common.util.HashUtils
import bitcoin.walllet.kit.network.message.Message
import bitcoin.walllet.kit.struct.Header
import java.io.ByteArrayInputStream

class HeadersMessage() : Message("headers") {

    var headers = arrayOf<Header>()

    constructor(payload: ByteArray) : this() {
        BitcoinInput(ByteArrayInputStream(payload)).use { input ->
            val count = input.readVarInt().toInt()

            headers = Array(count) {
                val header = Header(input)
                input.readVarInt() // tx count always zero
                header
            }
        }
    }

    override fun getPayload(): ByteArray {
        TODO("not implemented")
    }

    override fun toString(): String {
        return "HeadersMessage(${headers.size}:[${headers.joinToString { HashUtils.toHexStringAsLittleEndian(it.hash) }}])"
    }
}