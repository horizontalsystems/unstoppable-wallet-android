package bitcoin.walllet.kit.network;

import bitcoin.wallet.kit.blocks.MerkleBlock;
import bitcoin.walllet.kit.struct.Header;
import bitcoin.walllet.kit.struct.InvVect;
import bitcoin.walllet.kit.struct.Transaction;

public interface PeerGroupListener {

    void onReady();

    void onReceiveHeaders(Header[] headers);

    void onReceiveMerkleBlock(MerkleBlock merkleBlock);

    void onReceiveTransaction(Transaction transaction);

    Boolean shouldRequest(InvVect invVect);

    Transaction getTransaction(String hash);

}
