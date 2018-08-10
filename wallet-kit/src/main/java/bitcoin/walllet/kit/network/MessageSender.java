package bitcoin.wallet.kit.network

import bitcoin.walllet.kit.network.message.Message

interface MessageSender {

    fun sendMessage(message: Message)

    /**
     * Set timeout must be called periodically to keep connection alive.
     *
     * @param timeoutInMillis
     */
    fun setTimeout(timeoutInMillis: Long)

    fun close()
}
