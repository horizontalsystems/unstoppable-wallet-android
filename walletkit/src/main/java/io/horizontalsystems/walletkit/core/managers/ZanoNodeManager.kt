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

    private val _nodeChangedFlow = MutableSharedFlow<String>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /**
     * The host of the node the wallet was moved to. The kit changes its daemon in place, with
     * the wallet open; [currentNodeUpdatedFlow] is for a restart of the kit on the same node.
     */
    val nodeChangedFlow = _nodeChangedFlow.asSharedFlow()

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

    /**
     * The nodes Auto-Select picks from, and the ones a send may go on through: the built-in
     * nodes and the custom nodes that can be asked whether they are up.
     */
    val autoSelectNodes: List<ZanoNode>
        get() = autoSelectCandidates(defaultNodes, customNodes)

    val blockchain: Blockchain?
        get() = marketKitWrapper.blockchain(BlockchainType.Zano.uid)

    /**
     * Whether the wallet may move to another node when the selected one fails.
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

        // A node that cannot be probed cannot take part: it gives way to the default one.
        if (enabled && currentNode !in autoSelectNodes) {
            select(defaultNodes.first())
        } else {
            _nodesUpdatedFlow.tryEmit(currentNode.host)
        }
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
        probe = { nodePinger?.invoke(autoSelectNodes) ?: emptyList() },
        // A node Auto-Select picked; Auto-Select stays on
        switchTo = ::select,
        startupChoice = { current, probes -> SourceRepick.choose(current, probes, LAG_THRESHOLD, switchForSpeed = false) },
        lagThreshold = LAG_THRESHOLD,
    )

    /** Startup probe: moves off a node that is down or behind. Does not wait for the kit. */
    suspend fun repickNode() = autoSelector.pickOnStartup()

    /**
     * For a node that died while the app was running: moves to the fastest node that
     * answers.
     */
    suspend fun reselectNode(): NodeAutoSelector.Reselection = autoSelector.reselect()

    /**
     * Leaves the current node for the fastest other one that answers, for a node that failed
     * a send although it may still answer probes.
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
    }

    // Stores the node, hands it to the kit and has the node list drawn again. The Auto-Select
    // setting is not touched.
    private fun select(node: ZanoNode) {
        blockchainSettingsStorage.saveZanoNode(node.host)
        _nodeChangedFlow.tryEmit(node.host)
        _nodesUpdatedFlow.tryEmit(node.host)
    }

    /**
     * The kit moved the wallet to this node itself, for a send its node failed. Stored so the
     * settings show where the wallet is; Auto-Select stays as it was.
     */
    fun adoptNode(host: String) {
        if (host == currentNode.host) return
        allNodes.firstOrNull { it.host == host }?.let(::select)
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
        } else {
            _nodesUpdatedFlow.tryEmit(node.host)
        }
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

        /**
         * The nodes that take part in Auto-Select: every built-in one, and the custom ones
         * added with an https address. The probe goes over HTTP from the app, which refuses
         * plain http to a host it does not list, so a custom node added with an http address
         * would always look dead: never picked, and left at once if it were the selected one.
         */
        internal fun autoSelectCandidates(builtIn: List<ZanoNode>, custom: List<ZanoNode>): List<ZanoNode> =
            builtIn + custom.filter { it.host.startsWith("https://", ignoreCase = true) }
    }
}
