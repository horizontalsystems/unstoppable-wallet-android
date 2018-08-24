package bitcoin.wallet.kit.messages

import bitcoin.walllet.kit.common.io.BitcoinInput
import bitcoin.walllet.kit.common.io.BitcoinOutput
import bitcoin.walllet.kit.common.util.RandomUtils
import java.io.ByteArrayInputStream
import java.io.IOException

/**
 * Ping Message
 *
 *  Size        Field   Description
 *  ====        =====   ===========
 *  8 bytes     Nonce   Random value
 */
class PingMessage : Message {

    var nonce: Long = 0
        internal set

    constructor() : super("ping") {
        nonce = RandomUtils.randomLong()
    }

    @Throws(IOException::class)
    constructor(payload: ByteArray) : super("ping") {
        BitcoinInput(ByteArrayInputStream(payload)).use { input ->
            nonce = input.readLong()
        }
    }

    override fun getPayload(): ByteArray {
        return BitcoinOutput().writeLong(nonce).toByteArray()
    }

    override fun toString(): String {
        return "PingMessage(nonce=$nonce)"
    }
}
