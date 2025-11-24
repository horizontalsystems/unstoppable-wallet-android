package io.horizontalsystems.bankwallet.modules.moneronetwork

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.managers.MoneroNodeManager
import io.horizontalsystems.bankwallet.core.managers.MoneroNodeManager.MoneroNode
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MoneroNetworkViewModel(
    private val moneroNodeManager: MoneroNodeManager
) : ViewModel() {

    private val currentNode get() = moneroNodeManager.currentNode

    var viewState by mutableStateOf(ViewState(emptyList(), emptyList()))
        private set

    val title = "Monero"

    init {
        moneroNodeManager.nodesUpdatedFlow
            .onEach { syncState() }
            .launchIn(viewModelScope)

        syncState()
    }

    private fun syncState() {
        viewState = ViewState(
            defaultItems = viewItems(moneroNodeManager.defaultNodes),
            customItems = viewItems(moneroNodeManager.customNodes)
        )
    }

    private fun viewItems(nodes: List<MoneroNode>): List<ViewItem> {
        val selectedNode = currentNode

        return nodes.map { node ->
            ViewItem(
                node = node,
                id = node.host,
                name = node.name,
                url = node.host,
                selected = node == selectedNode
            )
        }
    }

    fun onSelectNode(node: MoneroNode) {
        if (currentNode == node) return
        moneroNodeManager.save(node)
        syncState()
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
