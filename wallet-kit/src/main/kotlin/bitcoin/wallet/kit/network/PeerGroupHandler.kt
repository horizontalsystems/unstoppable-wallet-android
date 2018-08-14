package bitcoin.wallet.kit.network

import bitcoin.wallet.kit.message.HeadersMessage
import bitcoin.walllet.kit.network.MessageSender
import bitcoin.walllet.kit.network.PeerGroupListener
import bitcoin.walllet.kit.network.message.GetHeadersMessage
import bitcoin.walllet.kit.network.message.Message

class PeerGroupHandler : PeerGroupListener {

    override fun onMessage(sender: MessageSender, message: Message) {
        when (message) {
            is HeadersMessage -> {
                // todo: implement
            }
        }
    }

    override fun onReady(sender: MessageSender) {
        sender.sendMessage(GetHeadersMessage())
    }
}
