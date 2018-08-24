package bitcoin.wallet.kit.models

import bitcoin.walllet.kit.common.io.BitcoinInput
import bitcoin.walllet.kit.common.io.BitcoinOutput
import bitcoin.walllet.kit.common.serializer.HashSerializer
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import java.io.IOException

/**
 * InvVect
 *
 *   Size       Field   Description
 *   ====       =====   ===========
 *   4 bytes    Type    0=Error, 1=Transaction, 2=Block, 3=Filtered Block
 *  32 bytes    Hash    Object hash
 */
class InvVect {

    // Uint32
    var type: Int = 0

    // 32-bytes hash
    @JsonSerialize(using = HashSerializer::class)
    lateinit var hash: ByteArray

    constructor()

    @Throws(IOException::class)
    constructor(input: BitcoinInput) {
        this.type = input.readInt()
        this.hash = input.readBytes(32)
    }

    fun toByteArray(): ByteArray {
        return BitcoinOutput().writeInt(this.type).write(this.hash).toByteArray()
    }

    companion object {

        /**
         * Any data of with this number may be ignored.
         */
        val ERROR = 0

        /**
         * Hash is related to a transaction.
         */
        val MSG_TX = 1

        /**
         * Hash is related to a data block.
         */
        val MSG_BLOCK = 2

        /**
         * Hash of a block header; identical to MSG_BLOCK. Only to be used in
         * getdata message. Indicates the reply should be a merkleblock message
         * rather than a block message; this only works if a bloom filter has been
         * set.
         */
        val MSG_FILTERED_BLOCK = 3

        /**
         * Hash of a block header; identical to MSG_BLOCK. Only to be used in
         * getdata message. Indicates the reply should be a cmpctblock message. See
         * BIP 152 for more info.
         */
        val MSG_CMPCT_BLOCK = 4
    }
}