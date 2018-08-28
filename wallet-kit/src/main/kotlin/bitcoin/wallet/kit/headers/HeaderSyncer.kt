package bitcoin.wallet.kit.headers

import bitcoin.wallet.kit.network.PeerGroup
import bitcoin.walllet.kit.constant.BitcoinConstants

class HeaderSyncer(val peerGroup: PeerGroup) {

    fun sync() {
        val hashes = arrayOf<ByteArray>(BitcoinConstants.GENESIS_HASH_BYTES)
        peerGroup.requestHeaders(hashes)
    }
}
