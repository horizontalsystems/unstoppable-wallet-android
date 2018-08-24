package bitcoin.wallet.kit.messages

import bitcoin.wallet.kit.models.MerkleBlock
import bitcoin.walllet.kit.common.io.BitcoinInput
import java.io.ByteArrayInputStream

/**
 * MerkleBlock Message
 *
 *  Size        Field           Description
 *  ====        =====           ===========
 *  80 bytes    Header          Consists of 6 fields that are hashed to calculate the block hash
 *  VarInt      hashCount       Number of hashes
 *  Variable    hashes          Hashes in depth-first order
 *  VarInt      flagsCount      Number of bytes of flag bits
 *  Variable    flagsBits       Flag bits packed 8 per byte, least significant bit first
 */
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
