package bitcoin.walllet.kit.network;

import bitcoin.walllet.kit.network.message.Message;

public interface MessageSender {

    void sendMessage(Message message);

    /**
     * Set timeout must be called periodically to keep connection alive.
     *
     * @param timeoutInMillis
     */
    void setTimeout(long timeoutInMillis);

    void close();
}
