package bitcoin.walllet.kit.network;

import bitcoin.walllet.kit.network.message.Message;

public interface PeerListener {

    void onMessage(MessageSender sender, Message message);

    void connected(String ip);

    void disconnected(String ip, Exception e);
}
