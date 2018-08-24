package bitcoin.wallet.kit.messages

import bitcoin.wallet.kit.models.InvVect
import bitcoin.walllet.kit.common.io.BitcoinInput
import bitcoin.walllet.kit.common.io.BitcoinOutput
import java.io.ByteArrayInputStream
import java.io.IOException

/**
 * GetData Message
 *
 *  Size        Field       Definition
 *  ====        =====       ==========
 *  VarInt      Count       Number of inventory items
 *  Variable    InvVect     One or more inventory items
 */
class GetDataMessage : Message {

    lateinit var inventory: Array<InvVect> // byte[36]

    constructor(inventory: Array<InvVect>) : super("getdata") {
        this.inventory = inventory
    }

    constructor(type: Int, hashes: Array<ByteArray>) : super("getdata") {
        inventory = Array(hashes.size) { i ->
            val iv = InvVect()
            iv.type = type
            iv.hash = hashes[i]
            iv
        }
    }

    @Throws(IOException::class)
    constructor(payload: ByteArray) : super("getdata") {
        BitcoinInput(ByteArrayInputStream(payload)).use { input ->
            val count = input.readVarInt() // do not store count
            inventory = Array(count.toInt()) {
                InvVect(input)
            }
        }
    }

    override fun getPayload(): ByteArray {
        val output = BitcoinOutput().writeVarInt(inventory.size.toLong())
        for (i in inventory.indices) {
            output.write(inventory[i].toByteArray())
        }
        return output.toByteArray()
    }

    override fun toString(): String {
        return "GetDataMessage(count=${inventory.size})"
    }
}
