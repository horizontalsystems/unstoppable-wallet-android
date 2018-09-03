package bitcoin.wallet.kit.network

import bitcoin.wallet.kit.crypto.BloomFilter
import bitcoin.wallet.kit.messages.*
import bitcoin.wallet.kit.models.Header
import bitcoin.wallet.kit.models.InventoryItem
import bitcoin.wallet.kit.models.MerkleBlock
import bitcoin.wallet.kit.models.Transaction
import org.slf4j.LoggerFactory
import java.lang.Exception

class Peer(val host: String, private val network: NetworkParameters, private val listener: Listener) : PeerInteraction, PeerConnection.Listener {

    private val log = LoggerFactory.getLogger(Peer::class.java)

    interface Listener {
        fun connected(peer: Peer)
        fun disconnected(peer: Peer, e: Exception?, incompleteMerkleBlocks: Array<ByteArray>)
        fun onReceiveHeaders(headers: Array<Header>)
        fun onReceiveMerkleBlock(merkleBlock: MerkleBlock)
        fun onReceiveTransaction(transaction: Transaction)
        fun shouldRequest(inventory: InventoryItem): Boolean
    }

    var isFree = true

    private val peerConnection = PeerConnection(host, network, this)
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
        peerConnection.sendMessage(GetHeadersMessage(headerHashes, network))
    }

    override fun requestMerkleBlocks(headerHashes: Array<ByteArray>) {
        requestedMerkleBlocks.plusAssign(headerHashes.map { it to null }.toMap())

        peerConnection.sendMessage(GetDataMessage(InventoryItem.MSG_FILTERED_BLOCK, headerHashes))
        isFree = false
    }

    override fun relay(transaction: Transaction) {
        relayedTransactions[transaction.txHash] = transaction

        peerConnection.sendMessage(InvMessage(InventoryItem.MSG_TX, transaction.txHash))
    }

    override fun onMessage(message: Message) {
        when (message) {
            is PingMessage -> peerConnection.sendMessage(PongMessage(message.nonce))
            is VersionMessage -> {
                val reason = reasonToClosePeer(message)
                if (reason.isEmpty()) {
                    log.info("SENDING VerAckMessage")
                    peerConnection.sendMessage(VerAckMessage())
                } else {
                    //close with reason
                    log.info("Closing Peer with reason: $reason")
                    close()
                }
            }
            is VerAckMessage -> listener.connected(this)
            is HeadersMessage -> listener.onReceiveHeaders(message.headers)
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
                            if (it.type == InventoryItem.MSG_BLOCK) {
                                InventoryItem().apply {
                                    type = InventoryItem.MSG_FILTERED_BLOCK
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
                message.inventory.filter { it.type == InventoryItem.MSG_TX }.forEach {
                    relayedTransactions[it.hash]?.let { tx ->
                        peerConnection.sendMessage(TransactionMessage(tx))
                        relayedTransactions.remove(tx.txHash)
                    }
                }
            }
        }
    }

    private fun reasonToClosePeer(message: VersionMessage): String {
        var reason = ""
        if (message.lastBlock <= 0) {
            reason = "Peer last block is not greater than 0."
        } else if (!message.hasBlockChain(network)) {
            reason = "Peer does not have a copy of the block chain."
        } else if (!message.supportsBloomFilter(network)) {
            reason = "Peer does not support Bloom Filter."
        }
        return reason
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
