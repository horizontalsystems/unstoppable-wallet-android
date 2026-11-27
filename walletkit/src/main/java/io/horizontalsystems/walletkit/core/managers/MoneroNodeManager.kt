package io.horizontalsystems.walletkit.core.managers

import android.net.Uri
import androidx.core.net.toUri
import io.horizontalsystems.walletkit.core.storage.BlockchainSettingsStorage
import io.horizontalsystems.walletkit.core.storage.MoneroNodeStorage
import io.horizontalsystems.walletkit.entities.MoneroNodeRecord
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.Objects

class MoneroNodeManager(
    private val blockchainSettingsStorage: BlockchainSettingsStorage,
    private val moneroNodeStorage: MoneroNodeStorage,
    private val marketKitWrapper: MarketKitWrapper
) {
    private val blockchainType = BlockchainType.Monero

    private val _currentNodeUpdatedFlow = MutableSharedFlow<String>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val currentNodeUpdatedFlow = _currentNodeUpdatedFlow.asSharedFlow()

    private val _nodeChangedFlow = MutableSharedFlow<String>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /**
     * The host of the node the wallet was moved to, or whose trust was changed. The kit takes
     * it with the wallet open; [currentNodeUpdatedFlow] is for a restart of the kit on the
     * same node.
     */
    val nodeChangedFlow = _nodeChangedFlow.asSharedFlow()

    private val _nodesUpdatedFlow = MutableSharedFlow<String>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val nodesUpdatedFlow = _nodesUpdatedFlow.asSharedFlow()

    val defaultNodesInitial = builtInNodes

    val defaultNodes: List<MoneroNode>
        get() {
            val nodeRecordsMap = moneroNodeStorage.getAll().associateBy { it.url }
            return defaultNodesInitial.map {
                it.copy(trusted = nodeRecordsMap[it.host]?.trusted ?: it.trusted)
            }
        }

    val customNodes: List<MoneroNode>
        get() {
            val defaultNodesUrls = defaultNodesInitial.map { it.host }
            val customNodeRecords = moneroNodeStorage.getAll().filterNot { defaultNodesUrls.contains(it.url) }
            return try {
                customNodeRecords.map { record ->
                    val uri = record.url.toUri()
                    MoneroNode(
                        host = record.url,
                        name = uri.host ?: "",
                        username = record.username,
                        password = record.password,
                        trusted = record.trusted,
                        serialized = serializeNode(uri, record.username, record.password)
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }

    val allNodes: List<MoneroNode>
        get() = defaultNodes + customNodes

    val currentNode: MoneroNode
        get() {
            val moneroNodeHost = blockchainSettingsStorage.moneroNodeHost()
            val rpcSource = allNodes.firstOrNull { it.host == moneroNodeHost }

            return rpcSource ?: defaultNodes.first()
        }

    var autoSelectEnabled: Boolean
        get() = blockchainSettingsStorage.moneroAutoSelect()
        set(value) {
            blockchainSettingsStorage.saveMoneroAutoSelect(value)
        }

    val blockchain: Blockchain?
        get() = marketKitWrapper.blockchain(blockchainType.uid)

    /**
     * Pings node endpoints and reports reachability/latency. Supplied by the Monero chain
     * plugin (the implementation lives in monero-kit); null while the module is absent.
     */
    @Volatile
    var nodePinger: (suspend (serialized: List<String>) -> List<NodePingResult>)? = null

    suspend fun pingNodes(serialized: List<String>): List<NodePingResult> =
        nodePinger?.invoke(serialized) ?: emptyList()

    // The probe is capped because the kit's internal per-node timeout is thread-interrupt
    // based, which OkHttp calls do not reliably honor.
    private val autoSelector = NodeAutoSelector(
        isEnabled = { autoSelectEnabled && nodePinger != null },
        current = { currentNode },
        probe = ::probeNodes,
        switchTo = ::save,
        startupChoice = { _, probes -> SourceRepick.fastest(probes, LAG_THRESHOLD) },
        lagThreshold = LAG_THRESHOLD,
    )

    /**
     * The adapter starts on the stored node right away: deferring its creation until the probe
     * finished left wallet rows interactive with no adapter behind them, which crashed the send
     * screen. save() emits, so a change moves the open wallet onto the winner; when the stored
     * node is still the fastest nothing is emitted and nothing reconnects.
     */
    suspend fun autoSelectFastestNodeOnStartup() = autoSelector.pickOnStartup()

    /**
     * Re-runs the ping and switches to the fastest reachable node, for when the current one has
     * died while the app was running.
     */
    suspend fun reselectFastestNode(): NodeAutoSelector.Reselection = autoSelector.reselect()

    /**
     * Leaves the current node for the fastest other one that answers, for a node that failed
     * a send although it may still answer pings.
     *
     * @return the node moved to, or null when Auto-Select is off or no other node answers.
     */
    suspend fun switchAwayFromNode(): MoneroNode? = autoSelector.switchAway()

    /**
     * Re-emits the current node so the wallet reload rebuilds the adapter on it. For a stalled
     * sync where reselection found nothing better, a kit restart is the recovery.
     */
    fun retryCurrentNode() {
        _currentNodeUpdatedFlow.tryEmit(currentNode.host)
    }

    private suspend fun probeNodes(): List<SourceProbe<MoneroNode>> {
        val nodes = allNodes
        return probes(nodes, pingNodes(nodes.map { it.serialized }))
    }

    fun save(node: MoneroNode) {
        persist(node)
        _nodeChangedFlow.tryEmit(node.host)
    }

    /**
     * The kit moved the wallet to this node itself, for a send or a fee estimate its node
     * failed. Stored so the settings show where the wallet is; Auto-Select stays as it was.
     *
     * @param serialized the node in the form the kit is given it.
     */
    fun adoptNode(serialized: String) {
        if (serialized == currentNode.serialized) return
        allNodes.firstOrNull { it.serialized == serialized }?.let(::save)
    }

    private fun persist(node: MoneroNode) {
        val record = MoneroNodeRecord(
            url = node.host,
            username = node.username,
            password = node.password,
            trusted = node.trusted
        )
        moneroNodeStorage.save(record)

        blockchainSettingsStorage.saveMoneroNode(node.host)
    }

    private fun serializeNode(uri: Uri, username: String?, password: String?): String {
        return "$username:$password@${uri.host}:${effectivePort(uri)}/mainnet/${uri.host ?: ""}"
    }

    // nodes saved before port validation was added may have no port; url scheme is always https
    private fun effectivePort(uri: Uri) = if (uri.port == -1) 443 else uri.port

    // canonical "host:port"; default nodes store host that way already, custom nodes store a full url
    private fun endpoint(hostOrUrl: String): String {
        val uri = hostOrUrl.toUri()
        return uri.host?.let { "$it:${effectivePort(uri)}" } ?: hostOrUrl
    }

    fun hasNode(url: String): Boolean {
        val endpoint = endpoint(url)
        return allNodes.any { endpoint(it.host) == endpoint }
    }

    fun addMoneroNode(url: String, username: String?, password: String?, trusted: Boolean) {
        val record = MoneroNodeRecord(
            url = url,
            username = username,
            password = password,
            trusted = trusted
        )

        moneroNodeStorage.save(record)

        customNodes.firstOrNull { it.host == url }?.let {
            save(it)
        }

        _nodesUpdatedFlow.tryEmit(url)
    }

    fun delete(node: MoneroNode) {
        val isCurrent = node == currentNode

        moneroNodeStorage.delete(node.host)

        if (isCurrent) {
            // The selection falls back to the first built-in node
            _nodeChangedFlow.tryEmit(currentNode.host)
        }

        _nodesUpdatedFlow.tryEmit(node.host)
    }

    /** @param height the index of the node's top block; 0 when the node did not answer. */
    data class NodePingResult(
        val serialized: String,
        val isValid: Boolean,
        val responseTime: Double,
        val height: Long,
    )

    data class MoneroNode(
        val host: String,
        val name: String,
        val serialized: String,
        val username: String? = null,
        val password: String? = null,
        val trusted: Boolean = false
    ) {
        override fun equals(other: Any?): Boolean {
            return other is MoneroNode && other.host == this.host && other.trusted == this.trusted
        }

        override fun hashCode(): Int {
            return Objects.hash(host, trusted)
        }
    }

    companion object {
        /**
         * How far behind the best reported height a node may be and still be picked. Monero
         * adds a block every two minutes, and a node a block or two back is only slow to hear
         * of it. The kit goes by the same number when a send moves to another node.
         */
        const val LAG_THRESHOLD = 3L

        val builtInNodes = listOf(
            MoneroNode("xmr.unstoppable.money:443", "unstoppable.money", "xmr.unstoppable.money:443/mainnet/unstoppable.money", trusted = true),
            MoneroNode("node.xmr.rocks:18089", "xmr.rocks", "node.xmr.rocks:18089/mainnet/xmr.rocks"),
            MoneroNode("opennode.xmr-tw.org:18089", "xmr-tw.org", "opennode.xmr-tw.org:18089/mainnet/xmr-tw.org"),
            MoneroNode("node.sethforprivacy.com:18089", "sethforprivacy.com", "node.sethforprivacy.com:18089/mainnet/sethforprivacy.com"),
            MoneroNode("node.monerodevs.org:18089", "monerodevs.org", "node.monerodevs.org:18089/mainnet/monerodevs.org"),
            MoneroNode("nodex.monerujo.io:18081", "monerujo.io", "nodex.monerujo.io:18081/mainnet/monerujo.io"),
            MoneroNode("xmr-node.cakewallet.com:18081", "cakewallet.com", "xmr-node.cakewallet.com:18081/mainnet/cakewallet.com"),
            MoneroNode("monero.stackwallet.com:18081", "stackwallet.com", "monero.stackwallet.com:18081/mainnet/stackwallet.com"),
            MoneroNode("nodes.hashvault.pro:18081", "hashvault.pro", "nodes.hashvault.pro:18081/mainnet/hashvault.pro"),
        )

        /** Ping results as Auto-Select probes: a node that did not answer has neither time nor height. */
        internal fun probes(nodes: List<MoneroNode>, results: List<NodePingResult>): List<SourceProbe<MoneroNode>> {
            val bySerialized = results.associateBy { it.serialized }
            return nodes.map { node ->
                val ping = bySerialized[node.serialized]?.takeIf { it.isValid && it.responseTime < Double.MAX_VALUE }
                SourceProbe(node, ping?.responseTime, ping?.height)
            }
        }

        /** The fastest node that answered and is not behind the others, or null when none answered. */
        fun fastestNode(nodes: List<MoneroNode>, results: List<NodePingResult>): MoneroNode? =
            SourceRepick.fastest(probes(nodes, results), LAG_THRESHOLD)
    }
}
