package bitcoin.walllet.kit.network;

import bitcoin.walllet.kit.network.message.Message;

public interface MessageListener {

    void onMessage(MessageSender sender, Message message);

}
