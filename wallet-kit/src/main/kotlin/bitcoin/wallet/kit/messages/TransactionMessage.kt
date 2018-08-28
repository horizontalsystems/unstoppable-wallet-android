package bitcoin.wallet.kit.messages

import bitcoin.wallet.kit.models.Transaction
import bitcoin.walllet.kit.io.BitcoinInput
import java.io.ByteArrayInputStream

/**
 * The 'tx' message contains a transaction which is not yet in a block. The transaction
 * will be held in the memory pool for a period of time to allow other peers to request
 * the transaction
 */
class TransactionMessage() : Message("tx") {

    lateinit var transaction: Transaction

    constructor(transaction: Transaction) : this() {
        this.transaction = transaction
    }

    constructor(payload: ByteArray) : this() {
        BitcoinInput(ByteArrayInputStream(payload)).use { input ->
            transaction = Transaction(input)
        }
    }

    override fun getPayload(): ByteArray {
        TODO("not implemented")
    }

}
