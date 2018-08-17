package bitcoin.wallet.kit.network

import bitcoin.wallet.kit.blocks.MerkleBlock
import bitcoin.walllet.kit.network.PeerGroupListener
import bitcoin.walllet.kit.network.PeerListener
import bitcoin.walllet.kit.struct.Transaction
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class PeerGroup(private val peerGroupListener: PeerGroupListener, private val peerManager: PeerManager, private val peerSize: Int = 3) : Thread(), PeerListener, PeerInteraction {

    private val log = LoggerFactory.getLogger(PeerGroup::class.java)
    private val peerMap = ConcurrentHashMap<String, Peer>()
    private var syncPeer: Peer? = null

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
                Thread.sleep(5000L)
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

    override fun requestHeaders(headerHashes: Array<ByteArray>, switchPeer: Boolean) {
        syncPeer?.requestHeaders(headerHashes)
    }

    override fun requestMerkleBlocks(headerHashes: Array<ByteArray>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun relay(transaction: Transaction) {
    }

    override fun connected(peer: Peer) {
        if (syncPeer == null) {
            syncPeer = peer

            log.info("Sync Peer ready")
            peerGroupListener.onReady()
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

        peerMap.remove(peer.host)
    }

    override fun onReceiveMerkleBlock(merkleBlock: MerkleBlock?) {
        peerGroupListener.onReceiveMerkleBlock(merkleBlock)
    }

}
