package io.horizontalsystems.bankwallet.modules.zanonetwork

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.managers.ZanoNodeManager
import io.horizontalsystems.bankwallet.core.managers.ZanoNodeManager.ZanoNode
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ZanoNetworkViewModel(
    private val zanoNodeManager: ZanoNodeManager
) : ViewModel() {

    var viewState by mutableStateOf(ViewState(emptyList(), emptyList()))
        private set

    val title = "Zano"

    init {
        zanoNodeManager.nodesUpdatedFlow
            .onEach { syncState() }
            .launchIn(viewModelScope)

        syncState()
    }

    private fun syncState() {
        val selectedNode = zanoNodeManager.currentNode
        viewState = ViewState(
            defaultItems = viewItems(zanoNodeManager.defaultNodes, selectedNode),
            customItems = viewItems(zanoNodeManager.customNodes, selectedNode)
        )
    }

    private fun viewItems(nodes: List<ZanoNode>, selectedNode: ZanoNode) =
        nodes.map { node ->
            ViewItem(
                node = node,
                id = node.host,
                name = node.name,
                url = node.host,
                selected = node == selectedNode
            )
        }

    fun onSelectNode(node: ZanoNode) {
        if (zanoNodeManager.currentNode == node) return
        zanoNodeManager.save(node)
        syncState()
    }

    fun onRemoveCustomNode(node: ZanoNode) {
        zanoNodeManager.delete(node)
    }

    data class ViewItem(
        val node: ZanoNode,
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
