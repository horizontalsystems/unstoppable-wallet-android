package bitcoin.wallet.kit.messages

import java.io.IOException

/**
 * The 'getaddr' message is sent to a peer to request a list of known peers.
 * The 'addr' message is a response
 */
class GetAddrMessage : Message {

    constructor() : super("getaddr")

    @Throws(IOException::class)
    constructor(payload: ByteArray) : super("getaddr")

    override fun getPayload(): ByteArray {
        return ByteArray(0)
    }

    override fun toString(): String {
        return "GetAddrMessage()"
    }
}
