package bitcoin.wallet.kit.network

import bitcoin.wallet.kit.blocks.MerkleBlock
import bitcoin.wallet.kit.crypto.BloomFilter
import bitcoin.wallet.kit.message.FilterLoadMessage
import bitcoin.wallet.kit.message.MerkleBlockMessage
import bitcoin.wallet.kit.message.TransactionMessage
import bitcoin.walllet.kit.network.message.*
import bitcoin.walllet.kit.struct.InvVect
import bitcoin.walllet.kit.struct.Transaction
import java.lang.Exception

class Peer(val host: String, private val listener: Listener) : PeerInteraction, PeerConnection.Listener {

    interface Listener {
        fun connected(peer: Peer)
        fun disconnected(peer: Peer, e: Exception?, incompleteMerkleBlocks: Array<ByteArray>)
        fun onReceiveMerkleBlock(merkleBlock: MerkleBlock)
        fun onReceiveTransaction(transaction: Transaction)
        fun shouldRequest(invVect: InvVect): Boolean
    }

    var isFree = true

    private val peerConnection = PeerConnection(host, this)
    private var requestedMerkleBlocks: MutableMap<ByteArray, MerkleBlock?> = mutableMapOf()
    private var relayedTransactions: MutableMap<ByteArray, Transaction> = mutableMapOf()

    fun start() {
        peerConnection.start()
    }

    fun close() {
        peerConnection.close()
    }

    // Sets a Bloom filter on this connection
    fun setBloomFilter(filter: BloomFilter) {
        peerConnection.sendMessage(FilterLoadMessage(filter))
    }

    override fun requestHeaders(headerHashes: Array<ByteArray>, switchPeer: Boolean) {
        peerConnection.sendMessage(GetHeadersMessage(headerHashes))
    }

    override fun requestMerkleBlocks(headerHashes: Array<ByteArray>) {
        requestedMerkleBlocks.plusAssign(headerHashes.map { it to null }.toMap())

        peerConnection.sendMessage(GetDataMessage(InvVect.MSG_FILTERED_BLOCK, headerHashes))
        isFree = false
    }

    override fun relay(transaction: Transaction) {
        relayedTransactions[transaction.txHash] = transaction

        peerConnection.sendMessage(InvMessage(InvVect.MSG_TX, transaction.txHash))
    }

    override fun onMessage(message: Message) {
        when (message) {
            is PingMessage -> peerConnection.sendMessage(PongMessage(message.nonce))
            is VersionMessage -> peerConnection.sendMessage(VerAckMessage())
            is VerAckMessage -> listener.connected(this)
            is MerkleBlockMessage -> {
                val merkleBlock = message.merkleBlock
                requestedMerkleBlocks[merkleBlock.blockHash] = merkleBlock

                if (merkleBlock.associatedTransactionHashes.isEmpty()) {
                    merkleBlockCompleted(merkleBlock)
                }
            }
            is TransactionMessage -> {
                val transaction = message.transaction

                val merkleBlock = requestedMerkleBlocks.values.filterNotNull().firstOrNull { it.associatedTransactionHashes.contains(transaction.txHash) }
                if (merkleBlock != null) {
                    merkleBlock.addTransaction(transaction)
                    if (merkleBlock.associatedTransactionHashes.size == merkleBlock.associatedTransactions.size) {
                        merkleBlockCompleted(merkleBlock)
                    }
                } else {
                    listener.onReceiveTransaction(transaction)
                }
            }
            is InvMessage -> {
                val inventoryToRequest = message.inventory
                        .filter { listener.shouldRequest(it) }
                        .map {
                            if (it.type == InvVect.MSG_BLOCK) {
                                InvVect().apply {
                                    type = InvVect.MSG_FILTERED_BLOCK
                                    hash = it.hash
                                }
                            } else {
                                it
                            }
                        }
                        .toTypedArray()

                peerConnection.sendMessage(GetDataMessage(inventoryToRequest))
            }

            is GetDataMessage -> {

                //handle relayed transactions
                message.inventory.filter { it.type == InvVect.MSG_TX }.forEach {
                    relayedTransactions[it.hash]?.let { tx ->
                        peerConnection.sendMessage(TransactionMessage(tx))
                        relayedTransactions.remove(tx.txHash)
                    }
                }
            }
        }
    }

    private fun merkleBlockCompleted(merkleBlock: MerkleBlock) {
        listener.onReceiveMerkleBlock(merkleBlock)
        requestedMerkleBlocks.minusAssign(merkleBlock.blockHash)
        if (requestedMerkleBlocks.isEmpty()) {
            isFree = true
        }
    }

    override fun disconnected(e: Exception?) {
        listener.disconnected(this, e, requestedMerkleBlocks.keys.toTypedArray())
    }

}
