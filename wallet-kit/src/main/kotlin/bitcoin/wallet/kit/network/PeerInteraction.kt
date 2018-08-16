package bitcoin.wallet.kit.network

import bitcoin.walllet.kit.struct.Transaction

interface PeerInteraction {

    fun requestHeaders(headerHashes: Array<ByteArray>, switchPeer: Boolean = false)
    fun requestBlocks(headerHashes: Array<ByteArray>)
    fun relay(transaction: Transaction)

}
