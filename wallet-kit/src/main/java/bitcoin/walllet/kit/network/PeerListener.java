package bitcoin.walllet.kit.network;

import bitcoin.wallet.kit.network.Peer;

public interface PeerListener {

    void connected(Peer peer);
    void disconnected(Peer peer, Exception e);

}
