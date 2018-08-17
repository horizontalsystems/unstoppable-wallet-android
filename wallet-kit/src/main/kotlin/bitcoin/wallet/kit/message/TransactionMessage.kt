package bitcoin.wallet.kit.message

import bitcoin.walllet.kit.common.io.BitcoinInput
import bitcoin.walllet.kit.network.message.Message
import bitcoin.walllet.kit.struct.Transaction
import java.io.ByteArrayInputStream

class TransactionMessage() : Message("tx") {

    lateinit var transaction: Transaction

    constructor(payload: ByteArray) : this() {
        BitcoinInput(ByteArrayInputStream(payload)).use { input ->

        }
    }

    override fun getPayload(): ByteArray {
        TODO("not implemented")
    }

}
