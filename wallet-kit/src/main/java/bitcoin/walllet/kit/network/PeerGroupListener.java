package bitcoin.walllet.kit.network;

import bitcoin.walllet.kit.network.message.Message;

public interface PeerGroupListener {

    void onMessage(MessageSender sender, Message message);

    void onReady(MessageSender sender);

}
