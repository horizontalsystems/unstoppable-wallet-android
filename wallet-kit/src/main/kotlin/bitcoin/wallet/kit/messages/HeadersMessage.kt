package bitcoin.wallet.kit.messages

import bitcoin.wallet.kit.models.Header
import bitcoin.walllet.kit.io.BitcoinInput
import bitcoin.walllet.kit.utils.HashUtils
import java.io.ByteArrayInputStream

/**
 * Headers Message
 *
 *  Size        Field       Description
 *  ====        =====       ===========
 *  VarInt      Count       Number of headers
 *  Variable    Entries     Header entries
 */
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