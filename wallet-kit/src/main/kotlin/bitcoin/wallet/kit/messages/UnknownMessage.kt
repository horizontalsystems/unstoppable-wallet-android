package bitcoin.wallet.kit.messages

class UnknownMessage(command: String, private val payload: ByteArray) : Message(command) {
    override fun getPayload(): ByteArray {
        return payload
    }
}
