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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import java.util.Objects

class MoneroNodeManager(
    private val blockchainSettingsStorage: BlockchainSettingsStorage,
    private val moneroNodeStorage: MoneroNodeStorage,
    private val marketKitWrapper: MarketKitWrapper
) {
    private val blockchainType = BlockchainType.Monero

    private val reselectMutex = Mutex()

    private val _currentNodeUpdatedFlow = MutableSharedFlow<String>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val currentNodeUpdatedFlow = _currentNodeUpdatedFlow.asSharedFlow()

    private val _nodesUpdatedFlow = MutableSharedFlow<String>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val nodesUpdatedFlow = _nodesUpdatedFlow.asSharedFlow()

    val defaultNodesInitial = listOf(
        MoneroNode("xmr.unstoppable.money:443", "unstoppable.money", "xmr.unstoppable.money:443/mainnet/unstoppable.money", trusted = true),
        MoneroNode("node.xmr.rocks:18089", "xmr.rocks", "node.xmr.rocks:18089/mainnet/xmr.rocks"),
        MoneroNode("opennode.xmr-tw.org:18089", "xmr-tw.org", "opennode.xmr-tw.org:18089/mainnet/xmr-tw.org"),
        MoneroNode("node.sethforprivacy.com:18089", "sethforprivacy.com", "node.sethforprivacy.com:18089/mainnet/sethforprivacy.com"),
        MoneroNode("nodex.monerujo.io:18081", "monerujo.io", "nodex.monerujo.io:18081/mainnet/monerujo.io"),
        MoneroNode("xmr-node.cakewallet.com:18081", "cakewallet.com", "xmr-node.cakewallet.com:18081/mainnet/cakewallet.com"),
        MoneroNode("monero.stackwallet.com:18081", "stackwallet.com", "monero.stackwallet.com:18081/mainnet/stackwallet.com"),
    )

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

    // True while the startup ping is choosing the fastest node. The Monero adapter creation is
    // deferred while this is set, so the wallet connects once to the fastest node instead of
    // connecting to the stored node and then reconnecting. Set at construction (before adapters
    // are initialized) to avoid a race.
    @Volatile
    var isResolvingFastestNode: Boolean = autoSelectEnabled
        private set

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

    suspend fun autoSelectFastestNodeOnStartup() {
        if (!autoSelectEnabled || nodePinger == null) {
            isResolvingFastestNode = false
            return
        }

        var target = currentNode
        try {
            pickFastest()?.let { target = it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // keep the stored node on any ping failure
        } finally {
            // Persist WITHOUT emitting currentNodeUpdatedFlow: emitting would replay (replay=1)
            // into WalletManager's late collector and trigger reloadWallets(Monero) → adapter
            // teardown/reconnect churn. The adapter is (re)created once by the normal wallet
            // activation / WalletManager.refreshActiveWallets() with this node already current.
            persist(target)
            isResolvingFastestNode = false
        }
    }

    /**
     * Re-runs the ping and switches to the fastest reachable node, for when the current one has
     * died while the app was running. Unlike the startup path this emits, so the adapter is
     * rebuilt on the new node.
     *
     * @return true when the node actually changed.
     */
    suspend fun reselectFastestNode(): Boolean {
        if (!autoSelectEnabled || nodePinger == null) return false
        if (!reselectMutex.tryLock()) return false // a ping is already in flight

        return try {
            // Every node failing means the device has no usable network, not that the current one
            // is bad — pickFastest returns null there, so nothing is switched.
            val fastest = pickFastest() ?: return false
            if (fastest.host == currentNode.host) return false
            save(fastest)
            true
        } finally {
            reselectMutex.unlock()
        }
    }

    /** Pings every node and returns the fastest valid one, or null if none responded. */
    private suspend fun pickFastest(): MoneroNode? {
        val nodes = allNodes
        val results = pingNodes(nodes.map { it.serialized }).associateBy { it.serialized }

        return nodes
            .mapNotNull { node ->
                results[node.serialized]
                    ?.takeIf { it.isValid && it.responseTime < Double.MAX_VALUE }
                    ?.let { node to it.responseTime }
            }
            .minByOrNull { it.second }
            ?.first
    }

    fun save(node: MoneroNode) {
        persist(node)
        _currentNodeUpdatedFlow.tryEmit(node.host)
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
            _currentNodeUpdatedFlow.tryEmit(node.host)
        }

        _nodesUpdatedFlow.tryEmit(node.host)
    }

    data class NodePingResult(
        val serialized: String,
        val isValid: Boolean,
        val responseTime: Double,
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
}