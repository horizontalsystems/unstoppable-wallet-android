package bitcoin.wallet.kit.network

import bitcoin.walllet.kit.utils.JsonUtils
import org.slf4j.LoggerFactory
import java.io.*

class PeerManager(val network: NetworkParameters, private val cached: File? = null) {

    // A PeerAddress holds an IP address representing the network location of
    // a peer in the Peer-to-Peer network.
    class PeerAddress(var ip: String, var score: Int = 0) {

        @Volatile
        var using: Boolean = false

        override fun equals(other: Any?): Boolean {
            if (other is PeerAddress) {
                return ip == other.ip
            }

            return false
        }

        override fun hashCode() = ip.hashCode()
    }

    private val log = LoggerFactory.getLogger(PeerManager::class.java)
    private val peerAddresses: MutableList<PeerAddress> = ArrayList()

    init {
        // add cached peer addresses:
        addPeers(loadPeers())

        if (peerAddresses.size < 5) {
            // lookup from DNS:
            val thread = object : Thread() {
                override fun run() {
                    try {
                        addPeers(PeerDiscover.lookup(network.dnsSeeds))
                    } catch (e: Exception) {
                        log.warn("Could not discover peerAddresses.", e)
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
        log.info("Try get an unused peer from " + peerAddresses.size + " peerAddresses...")
        peerAddresses.sortWith(Comparator { p1, p2 ->
            if (p1.score > p2.score) -1 else 1
        })

        for (p in peerAddresses) {
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
        peerAddresses.firstOrNull { it.ip == peerIp }?.let { peer ->
            peerAddresses.remove(peer)
            storePeers()
        }
    }

    @Synchronized
    fun markSuccess(peerIp: String) {
        peerAddresses.firstOrNull { it.ip == peerIp }?.let { peer ->
            peer.using = false
            peer.score += 3

            storePeers()
        }
    }

    @Synchronized
    fun peerCount(): Int {
        return peerAddresses.size
    }

    @Synchronized
    fun addPeers(ips: Array<String>) {
        addPeers(ips.map { PeerAddress(it) }.toTypedArray())
        storePeers()
    }

    @Synchronized
    private fun addPeers(ps: Array<PeerAddress>) {
        log.info("Add discovered " + ps.size + " peerAddresses...")
        for (p in ps) {
            if (!peerAddresses.contains(p)) {
                peerAddresses.add(p)
            }
        }
        log.info("Total peerAddresses: " + peerAddresses.size)
        storePeers()
    }

    @Synchronized
    fun close() {
        storePeers()
    }

    private fun loadPeers(): Array<PeerAddress> {
        if (cached != null) {
            try {
                val inputStream = FileInputStream(cached)
                BufferedInputStream(inputStream).use { input ->
                    return JsonUtils.fromJson(Array<PeerAddress>::class.java, input)
                }
            } catch (e: Exception) {
                log.warn("Load cached peerAddresses from cached file failed: " + cached.absolutePath)
            }

        }

        return arrayOf()
    }

    private fun storePeers() {
        if (cached != null) {
            try {
                val streamWriter = OutputStreamWriter(FileOutputStream(cached), "UTF-8")
                BufferedWriter(streamWriter).use { writer ->
                    val peerArray = peerAddresses.toTypedArray()
                    writer.write(JsonUtils.toJson(peerArray))
                }
            } catch (e: Exception) {
                log.warn("Write peerAddresses to cached file failed: " + cached.absolutePath, e)
            }

        }
    }
}
