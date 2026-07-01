package io.horizontalsystems.bankwallet.modules.moneronetwork

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.MoneroNodeManager
import io.horizontalsystems.bankwallet.core.managers.MoneroNodeManager.MoneroNode
import io.horizontalsystems.monerokit.NodePingResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MoneroNetworkViewModel(
    private val moneroNodeManager: MoneroNodeManager
) : ViewModelUiState<MoneroNetworkViewModel.ViewState>() {

    val title = "Monero"

    private var pingStates: Map<String, PingState> = emptyMap()
    private var pingJob: Job? = null

    override fun createState(): ViewState {
        val selectedNode = moneroNodeManager.currentNode
        return ViewState(
            autoSelectEnabled = moneroNodeManager.autoSelectEnabled,
            defaultItems = viewItems(moneroNodeManager.defaultNodes, selectedNode),
            customItems = viewItems(moneroNodeManager.customNodes, selectedNode)
        )
    }

    init {
        viewModelScope.launch {
            try {
                moneroNodeManager.nodesUpdatedFlow.collect {
                    pingNodes()
                }
            } catch (e: Exception) {
                // nodesUpdatedFlow is a MutableSharedFlow and does not throw in normal operation
            }
        }
        pingNodes()
    }

    private fun viewItems(nodes: List<MoneroNode>, selectedNode: MoneroNode): List<ViewItem> =
        nodes.map { node ->
            ViewItem(
                node = node,
                id = node.host,
                name = node.name,
                url = node.host,
                selected = node == selectedNode,
                ping = pingStates[node.serialized] ?: PingState.Loading
            )
        }

    fun onSelectNode(node: MoneroNode) {
        if (moneroNodeManager.currentNode == node) return
        moneroNodeManager.save(node)
        emitState()
    }

    fun onToggleAutoSelect(enabled: Boolean) {
        moneroNodeManager.autoSelectEnabled = enabled
        // re-ping; when enabled the fastest reachable node is selected once results arrive
        pingNodes()
    }

    fun onRemoveCustomNode(node: MoneroNode) {
        moneroNodeManager.delete(node)
    }

    fun refresh() {
        pingNodes()
    }

    private fun pingNodes() {
        val nodes = moneroNodeManager.allNodes

        pingStates = nodes.associate { it.serialized to PingState.Loading }
        emitState()

        pingJob?.cancel()
        pingJob = viewModelScope.launch {
            pingStates = try {
                val results = moneroNodeManager.pingNodes(nodes.map { it.serialized })
                    .associateBy { it.serialized }
                nodes.associate { node ->
                    node.serialized to (results[node.serialized]?.let(::pingState) ?: PingState.Unreachable)
                }
            } catch (e: Exception) {
                nodes.associate { it.serialized to PingState.Unreachable }
            }
            if (moneroNodeManager.autoSelectEnabled) {
                selectFastestNode(nodes)
            }
            emitState()
        }
    }

    private fun selectFastestNode(nodes: List<MoneroNode>) {
        val fastest = nodes
            .mapNotNull { node ->
                (pingStates[node.serialized] as? PingState.Reachable)?.let { node to it.responseTimeMs }
            }
            .minByOrNull { it.second }
            ?.first
            ?: return

        if (fastest.host != moneroNodeManager.currentNode.host) {
            moneroNodeManager.save(fastest)
        }
    }

    private fun pingState(result: NodePingResult): PingState {
        if (!result.isValid || result.responseTime >= Double.MAX_VALUE) {
            return PingState.Unreachable
        }
        val level = when {
            result.responseTime <= NodePingResult.PING_GOOD -> PingState.Level.Good
            result.responseTime <= NodePingResult.PING_MEDIUM -> PingState.Level.Medium
            else -> PingState.Level.Slow
        }
        return PingState.Reachable(result.responseTime.toInt(), level)
    }

    sealed interface PingState {
        object Loading : PingState
        object Unreachable : PingState
        data class Reachable(val responseTimeMs: Int, val level: Level) : PingState

        enum class Level { Good, Medium, Slow }
    }

    data class ViewItem(
        val node: MoneroNode,
        val id: String,
        val name: String,
        val url: String,
        val selected: Boolean,
        val ping: PingState,
    )

    data class ViewState(
        val autoSelectEnabled: Boolean,
        val defaultItems: List<ViewItem>,
        val customItems: List<ViewItem>,
    )
}
