package bitcoin.wallet.kit.messages

/**
 * The 'verack' message is sent in response to the 'version' message.
 * It consists of just the message header.
 */
class VerAckMessage : Message {

    constructor() : super("verack")

    constructor(payload: ByteArray) : super("verack")

    override fun getPayload(): ByteArray {
        return ByteArray(0)
    }

    override fun toString(): String {
        return "VerAckMessage()"
    }
}
