package bitcoin.wallet.kit.network

import bitcoin.walllet.kit.network.message.Message

interface PeerListener {

    fun onMessage(sender: MessageSender, message: Message)

    fun connected(ip: String)

    fun disconnected(ip: String, e: Exception)
}
