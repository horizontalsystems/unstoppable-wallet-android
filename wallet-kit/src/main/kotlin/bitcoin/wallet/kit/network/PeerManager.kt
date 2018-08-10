package bitcoin.wallet.kit.network

import bitcoin.walllet.kit.common.util.JsonUtils
import org.slf4j.LoggerFactory
import java.io.*
import java.util.*

class PeerManager(private val cached: File? = null) {

    private val log = LoggerFactory.getLogger(PeerManager::class.java)
    private val peers: MutableList<Peer> = ArrayList()

    init {
        // add cached peers:
        addPeers(loadPeers())

        if (peers.size < 5) {
            // lookup from DNS:
            val thread = object : Thread() {
                override fun run() {
                    try {
                        addPeers(PeerDiscover.lookup())
                    } catch (e: Exception) {
                        log.warn("Could not discover peers.", e)
                    }

                }
            }

            thread.isDaemon = true
            thread.start()
        }
    }

    /**
     * Return a peer ip to connect.
     *
     * @return Ip or null if no peer available.
     */
    @Synchronized
    fun getPeerIp(): String? {
        log.info("Try get an unused peer from " + peers.size + " peers...")
        peers.sortWith(Comparator { p1, p2 ->
            if (p1.score > p2.score) -1 else 1
        })

        for (p in peers) {
            if (!p.using) {
                p.using = true

                if (p.ip == "") {
                    throw Exception()
                }

                return p.ip
            }
        }

        return null
    }

    @Synchronized
    fun markFailed(peerIp: String) {
        peers.firstOrNull { it.ip == peerIp }?.let { peer ->
            peers.remove(peer)
            storePeers()
        }
    }

    @Synchronized
    fun markSuccess(peerIp: String) {
        peers.firstOrNull { it.ip == peerIp }?.let { peer ->
            peer.using = false
            peer.score += 3

            storePeers()
        }
    }

    @Synchronized
    fun peerCount(): Int {
        return peers.size
    }

    @Synchronized
    fun addPeers(ips: Array<String>) {
        addPeers(ips.map { Peer(it) }.toTypedArray())
        storePeers()
    }

    @Synchronized
    private fun addPeers(ps: Array<Peer>) {
        log.info("Add discovered " + ps.size + " peers...")
        for (p in ps) {
            if (!peers.contains(p)) {
                peers.add(p)
            }
        }
        log.info("Total peers: " + peers.size)
        storePeers()
    }

    @Synchronized
    fun close() {
        storePeers()
    }

    private fun loadPeers(): Array<Peer> {
        if (cached != null) {
            try {
                val inputStream = FileInputStream(cached)
                BufferedInputStream(inputStream).use { input ->
                    return JsonUtils.fromJson(Array<Peer>::class.java, input)
                }
            } catch (e: Exception) {
                log.warn("Load cached peers from cached file failed: " + cached.absolutePath)
            }

        }

        return arrayOf()
    }

    private fun storePeers() {
        if (cached != null) {
            try {
                val streamWriter = OutputStreamWriter(FileOutputStream(cached), "UTF-8")
                BufferedWriter(streamWriter).use { writer ->
                    val peerArray = peers.toTypedArray()
                    writer.write(JsonUtils.toJson(peerArray))
                }
            } catch (e: Exception) {
                log.warn("Write peers to cached file failed: " + cached.absolutePath, e)
            }

        }
    }
}
