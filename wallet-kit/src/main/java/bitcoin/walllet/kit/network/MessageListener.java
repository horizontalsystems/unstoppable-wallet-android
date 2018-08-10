package bitcoin.wallet.kit.network

import bitcoin.wallet.kit.network.MessageSender
import bitcoin.walllet.kit.network.message.Message

interface MessageListener {

    fun onMessage(sender: MessageSender, message: Message)

}
