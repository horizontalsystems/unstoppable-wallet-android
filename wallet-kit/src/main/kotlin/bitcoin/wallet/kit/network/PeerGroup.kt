package bitcoin.wallet.kit.network

import bitcoin.wallet.kit.crypto.BloomFilter
import bitcoin.wallet.kit.models.Header
import bitcoin.wallet.kit.models.InventoryItem
import bitcoin.wallet.kit.models.MerkleBlock
import bitcoin.wallet.kit.models.Transaction
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

class PeerGroup(private val peerGroupListener: Listener, private val peerManager: PeerManager, private val peerSize: Int = 3) : Thread(), Peer.Listener, PeerInteraction {

    interface Listener {
        fun onReady(peerGroup: PeerGroup)
        fun onReceiveHeaders(headers: Array<Header>)
        fun onReceiveMerkleBlock(merkleBlock: MerkleBlock)
        fun onReceiveTransaction(transaction: Transaction)
        fun shouldRequest(inventory: InventoryItem): Boolean
        fun getTransaction(hash: String): Transaction
    }

    private val log = LoggerFactory.getLogger(PeerGroup::class.java)
    private val peerMap = ConcurrentHashMap<String, Peer>()
    private var syncPeer: Peer? = null
    private val fetchingBlocksQueue = ConcurrentLinkedQueue<ByteArray>()
    private var bloomFilter: BloomFilter? = null

    @Volatile
    private var fetchingBlocks = false

    @Volatile
    private var running = false

    override fun run() {
        running = true
        // loop:
        while (running) {
            if (peerMap.size < peerSize) {
                startConnection()
            }

            try {
                Thread.sleep(2000L)
            } catch (e: InterruptedException) {
                break
            }
        }

        log.info("Closing all peer connections...")
        for (conn in peerMap.values) {
            conn.close()
        }
    }

    private fun startConnection() {
        log.info("Try open new peer connection...")
        val ip = peerManager.getPeerIp()
        if (ip != null) {
            log.info("Try open new peer connection to $ip...")
            val peer = Peer(ip, this)
            peerMap[ip] = peer
            peer.start()
        } else {
            log.info("No peers found yet.")
        }
    }

    fun close() {
        running = false
        interrupt()
        try {
            join(5000)
        } catch (e: InterruptedException) {
        }
    }

    fun setBloomFilter(filter: BloomFilter) {
        bloomFilter = filter
    }

    private fun getFreePeer(): Peer? {
        return peerMap.values.firstOrNull { it.isFree }
    }

    private fun fetchBlocks() {
        if (fetchingBlocks) return

        // only on worker should distribution tasks
        fetchingBlocks = true

        // loop:
        while (fetchingBlocksQueue.isNotEmpty()) {
            val peer = getFreePeer()
            if (peer == null) {
                Thread.sleep(1000)
            } else {
                val hashes = mutableListOf<ByteArray>()
                for (i in 1..10) {
                    val hash = fetchingBlocksQueue.poll() ?: break
                    hashes.add(hash)
                }

                peer.requestMerkleBlocks(hashes.toTypedArray())
            }
        }

        fetchingBlocks = false
    }

    override fun requestHeaders(headerHashes: Array<ByteArray>, switchPeer: Boolean) {
        if (switchPeer) {
            switchSyncPeer()
        }
        syncPeer?.requestHeaders(headerHashes)
    }

    private fun switchSyncPeer() {
        val newPeer = getFreePeer()
        newPeer?.let {
            syncPeer?.isFree = true
            setSyncPeer(it)
        }
    }

    private fun setSyncPeer(newPeer: Peer) {
        // sync peer will always busy for headers tasks
        newPeer.isFree = false
        syncPeer = newPeer
    }

    override fun requestMerkleBlocks(headerHashes: Array<ByteArray>) {
        fetchingBlocksQueue.addAll(headerHashes)
        fetchBlocks()
    }

    override fun relay(transaction: Transaction) {
        peerMap.forEach {
            it.value.relay(transaction)
        }
    }

    override fun connected(peer: Peer) {
        bloomFilter?.let {
            peer.setBloomFilter(it)
        }

        if (syncPeer == null) {
            setSyncPeer(peer)

            log.info("Sync Peer ready")
            peerGroupListener.onReady(this)
        }
    }

    override fun disconnected(peer: Peer, e: Exception?, incompleteMerkleBlocks: Array<ByteArray>) {
        if (e == null) {
            log.info("PeerAddress $peer.host disconnected.")
            peerManager.markSuccess(peer.host)
        } else {
            log.warn("PeerAddress $peer.host disconnected with error.", e.message)
            peerManager.markFailed(peer.host)
        }

        // it restores syncPeer on next connection
        if (syncPeer == peer) {
            syncPeer = null
        }

        if (incompleteMerkleBlocks.isNotEmpty()) {
            requestMerkleBlocks(incompleteMerkleBlocks)
        }

        peerMap.remove(peer.host)
    }

    override fun onReceiveHeaders(headers: Array<Header>) {
        peerGroupListener.onReceiveHeaders(headers)
    }

    override fun onReceiveMerkleBlock(merkleBlock: MerkleBlock) {
        peerGroupListener.onReceiveMerkleBlock(merkleBlock)
    }

    override fun onReceiveTransaction(transaction: Transaction) {
        peerGroupListener.onReceiveTransaction(transaction)
    }

    override fun shouldRequest(inventory: InventoryItem): Boolean {
        return peerGroupListener.shouldRequest(inventory)
    }
}
