package io.horizontalsystems.bankwallet.modules.zanonetwork

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.ZanoNodeManager
import io.horizontalsystems.bankwallet.core.managers.ZanoNodeManager.ZanoNode
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ZanoNetworkViewModel @Inject constructor(
    private val zanoNodeManager: ZanoNodeManager
) : ViewModelUiState<ZanoNetworkViewModel.ViewState>() {

    val title = "Zano"

    override fun createState(): ViewState {
        val selectedNode = zanoNodeManager.currentNode
        return ViewState(
            defaultItems = viewItems(zanoNodeManager.defaultNodes, selectedNode),
            customItems = viewItems(zanoNodeManager.customNodes, selectedNode)
        )
    }

    init {
        viewModelScope.launch {
            try {
                zanoNodeManager.nodesUpdatedFlow.collect {
                    emitState()
                }
            } catch (e: Exception) {
                // nodesUpdatedFlow is a MutableSharedFlow and does not throw in normal operation
            }
        }
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
        emitState()
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
