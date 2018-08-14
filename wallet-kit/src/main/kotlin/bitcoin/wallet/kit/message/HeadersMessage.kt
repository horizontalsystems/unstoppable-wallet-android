package bitcoin.wallet.kit.message

import bitcoin.walllet.kit.common.io.BitcoinInput
import bitcoin.walllet.kit.common.util.HashUtils
import bitcoin.walllet.kit.network.message.Message
import bitcoin.walllet.kit.struct.Block
import java.io.ByteArrayInputStream

class HeadersMessage() : Message("headers") {

    val blocks = mutableListOf<Block>()

    constructor(payload: ByteArray) : this() {
        BitcoinInput(ByteArrayInputStream(payload)).use { input ->
            val count = input.readVarInt()
            for (i in 0 until count) {
                blocks.add(Block(input))
            }
        }
    }

    override fun getPayload(): ByteArray {
        TODO("not implemented")
    }

    override fun toString(): String {
        return "HeadersMessage(${blocks.size}:[${blocks.joinToString { HashUtils.toHexStringAsLittleEndian(it.blockHash) }}])"
    }
}