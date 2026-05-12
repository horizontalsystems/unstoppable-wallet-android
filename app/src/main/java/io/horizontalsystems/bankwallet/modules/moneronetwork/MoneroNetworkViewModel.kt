package io.horizontalsystems.bankwallet.modules.moneronetwork

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.MoneroNodeManager
import io.horizontalsystems.bankwallet.core.managers.MoneroNodeManager.MoneroNode
import kotlinx.coroutines.launch

class MoneroNetworkViewModel(
    private val moneroNodeManager: MoneroNodeManager
) : ViewModelUiState<MoneroNetworkViewModel.ViewState>() {

    val title = "Monero"

    override fun createState(): ViewState {
        val selectedNode = moneroNodeManager.currentNode
        return ViewState(
            defaultItems = viewItems(moneroNodeManager.defaultNodes, selectedNode),
            customItems = viewItems(moneroNodeManager.customNodes, selectedNode)
        )
    }

    init {
        viewModelScope.launch {
            try {
                moneroNodeManager.nodesUpdatedFlow.collect {
                    emitState()
                }
            } catch (e: Exception) {
                // nodesUpdatedFlow is a MutableSharedFlow and does not throw in normal operation
            }
        }
    }

    private fun viewItems(nodes: List<MoneroNode>, selectedNode: MoneroNode): List<ViewItem> =
        nodes.map { node ->
            ViewItem(
                node = node,
                id = node.host,
                name = node.name,
                url = node.host,
                selected = node == selectedNode
            )
        }

    fun onSelectNode(node: MoneroNode) {
        if (moneroNodeManager.currentNode == node) return
        moneroNodeManager.save(node)
        emitState()
    }

    fun onRemoveCustomNode(node: MoneroNode) {
        moneroNodeManager.delete(node)
    }

    data class ViewItem(
        val node: MoneroNode,
        val id: String,
        val name: String,
        val url: String,
        val selected: Boolean,
    )

    data class ViewState(
        val defaultItems: List<ViewItem>,
        val customItems: List<ViewItem>,
    )
}
