package bitcoin.wallet.kit.message

import bitcoin.wallet.kit.blocks.MerkleBlock
import bitcoin.walllet.kit.common.io.BitcoinInput
import bitcoin.walllet.kit.network.message.Message
import java.io.ByteArrayInputStream

class MerkleBlockMessage() : Message("merkleblock") {

    lateinit var merkleBlock: MerkleBlock

    constructor(payload: ByteArray) : this() {
        BitcoinInput(ByteArrayInputStream(payload)).use { input ->
            merkleBlock = MerkleBlock(input)
        }
    }

    override fun getPayload(): ByteArray {
        return merkleBlock.toByteArray()
    }

    override fun toString(): String {
        TODO("not implemented")
    }
}
