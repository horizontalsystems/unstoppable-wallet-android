package bitcoin.walllet.kit.network;

import bitcoin.wallet.kit.network.Peer;
import bitcoin.walllet.kit.network.message.Message;

public interface PeerListener {

    void onMessage(MessageSender sender, Message message);

    void connected(Peer peer);

    void disconnected(Peer peer, Exception e);
}
