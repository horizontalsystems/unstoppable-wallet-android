package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.walletkit.core.storage.BlockchainSettingsStorage
import io.horizontalsystems.walletkit.core.storage.ZanoNodeStorage
import io.horizontalsystems.walletkit.entities.ZanoNodeRecord
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class ZanoNodeManager(
    private val blockchainSettingsStorage: BlockchainSettingsStorage,
    private val zanoNodeStorage: ZanoNodeStorage,
    private val marketKitWrapper: MarketKitWrapper,
) {
    private val _currentNodeUpdatedFlow = MutableSharedFlow<String>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val currentNodeUpdatedFlow = _currentNodeUpdatedFlow.asSharedFlow()

    private val _nodesUpdatedFlow = MutableSharedFlow<String>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val nodesUpdatedFlow = _nodesUpdatedFlow.asSharedFlow()

    val defaultNodesInitial = listOf(
        ZanoNode("https://zano.unstoppable.money:443", "zano.unstoppable.money"),
        ZanoNode("https://node.zano.org:443", "node.zano.org"),
    )

    val defaultNodes: List<ZanoNode> get() = defaultNodesInitial

    val customNodes: List<ZanoNode>
        get() {
            val defaultUrls = defaultNodesInitial.map { it.host }
            return zanoNodeStorage.getAll()
                .filterNot { defaultUrls.contains(it.url) }
                .map { ZanoNode(it.url, it.url) }
        }

    val allNodes: List<ZanoNode>
        get() = defaultNodes + customNodes

    val currentNode: ZanoNode
        get() {
            val host = blockchainSettingsStorage.zanoNodeHost()
            return allNodes.firstOrNull { it.host == host } ?: defaultNodes.first()
        }

    val blockchain: Blockchain?
        get() = marketKitWrapper.blockchain(BlockchainType.Zano.uid)

    /**
     * Whether the wallet may move to another built-in node when the selected one fails.
     *
     * Decided once and then stored: on only for a user who never touched the node (still on
     * the default one and no custom node added). Anyone who picked another node made a choice
     * that must not be overridden silently.
     */
    val autoSelectEnabled: Boolean
        get() {
            blockchainSettingsStorage.zanoAutoSelect()?.let { return it }

            val enabled = autoSelectDefault(
                storedHost = blockchainSettingsStorage.zanoNodeHost(),
                defaultHost = defaultNodes.first().host,
                hasCustomNodes = customNodes.isNotEmpty(),
            )
            blockchainSettingsStorage.saveZanoAutoSelect(enabled)
            return enabled
        }

    fun setAutoSelect(enabled: Boolean) {
        blockchainSettingsStorage.saveZanoAutoSelect(enabled)

        // Only built-in nodes take part; a custom selection gives way to the default one.
        if (enabled && currentNode !in defaultNodes) {
            select(defaultNodes.first())
        }
        _nodesUpdatedFlow.tryEmit(currentNode.host)
    }

    /**
     * Probes nodes for Auto-Select. Supplied by the Zano chain plugin; null while the module
     * is absent.
     */
    @Volatile
    var nodePinger: (suspend (List<ZanoNode>) -> List<SourceProbe<ZanoNode>>)? = null

    // Restarting the Zano wallet on another node is costly and one probe's timing is noisy,
    // so at startup the node is left only when it failed or is behind, never for speed.
    private val autoSelector = NodeAutoSelector(
        isEnabled = { autoSelectEnabled },
        current = { currentNode },
        probe = { nodePinger?.invoke(defaultNodes) ?: emptyList() },
        switchTo = ::saveAutoSelected,
        startupChoice = { current, probes -> SourceRepick.choose(current, probes, LAG_THRESHOLD, switchForSpeed = false) },
        lagThreshold = LAG_THRESHOLD,
    )

    /** Startup probe: moves off a built-in node that is down or behind. Does not wait for the kit. */
    suspend fun repickNode() = autoSelector.pickOnStartup()

    /**
     * For a node that died while the app was running: moves to the fastest built-in node
     * that answers.
     */
    suspend fun reselectNode(): NodeAutoSelector.Reselection = autoSelector.reselect()

    /**
     * Leaves the current node for the fastest other built-in one that answers, for a node that
     * failed a send although it may still answer probes.
     *
     * @return the node moved to, or null when Auto-Select is off or no other node answers.
     */
    suspend fun switchAwayFromNode(): ZanoNode? = autoSelector.switchAway()

    /**
     * Whether the current node has stopped answering, with Auto-Select on. The kit cannot be
     * asked: once connected it keeps reporting the wallet as synced when its daemon dies.
     */
    suspend fun isCurrentNodeDown(): Boolean =
        autoSelector.isCurrentDown { node -> nodePinger?.invoke(listOf(node)) ?: emptyList() }

    /**
     * Re-emits the current node so the wallet reload restarts the kit on it. For a stalled
     * sync where no better node was found, a kit restart is the recovery.
     */
    fun retryCurrentNode() {
        _currentNodeUpdatedFlow.tryEmit(currentNode.host)
    }

    /** A node the user picked: they chose where to sync from, so Auto-Select stops. */
    fun save(node: ZanoNode) {
        blockchainSettingsStorage.saveZanoAutoSelect(false)
        select(node)
        _nodesUpdatedFlow.tryEmit(node.host)
    }

    /** A node Auto-Select picked; Auto-Select stays on. */
    private fun saveAutoSelected(node: ZanoNode) {
        select(node)
        _nodesUpdatedFlow.tryEmit(node.host)
    }

    private fun select(node: ZanoNode) {
        blockchainSettingsStorage.saveZanoNode(node.host)
        _currentNodeUpdatedFlow.tryEmit(node.host)
    }

    fun addZanoNode(url: String) {
        zanoNodeStorage.save(ZanoNodeRecord(url))
        customNodes.firstOrNull { it.host == url }?.let { save(it) }
        _nodesUpdatedFlow.tryEmit(url)
    }

    fun delete(node: ZanoNode) {
        val isCurrent = node == currentNode
        zanoNodeStorage.delete(node.host)
        if (isCurrent) {
            // The selection only falls back; it is not a pick, so the setting is kept.
            select(defaultNodes.first())
        }
        _nodesUpdatedFlow.tryEmit(node.host)
    }

    data class ZanoNode(val host: String, val name: String)

    companion object {
        /**
         * How far behind the best reported height a node may be and still count as current.
         * Zano makes a block about every minute, so this is about ten minutes.
         */
        const val LAG_THRESHOLD = 10L

        fun autoSelectDefault(storedHost: String?, defaultHost: String, hasCustomNodes: Boolean): Boolean =
            (storedHost == null || storedHost == defaultHost) && !hasCustomNodes
    }
}
