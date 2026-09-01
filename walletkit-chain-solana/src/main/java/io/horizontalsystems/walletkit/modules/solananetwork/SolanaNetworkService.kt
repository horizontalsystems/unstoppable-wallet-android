package io.horizontalsystems.walletkit.modules.solananetwork

import io.horizontalsystems.walletkit.core.Clearable
import io.horizontalsystems.walletkit.core.managers.SolanaRpcSourceManager
import io.horizontalsystems.solanakit.models.RpcSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SolanaNetworkService(
        private val rpcSourceManager: SolanaRpcSourceManager,
) : Clearable {

    private val _itemsFlow = MutableStateFlow<List<Item>>(listOf())
    val itemsFlow: StateFlow<List<Item>> = _itemsFlow.asStateFlow()

    val items: List<Item>
        get() = _itemsFlow.value

    private val currentRpcSource: RpcSource
        get() = rpcSourceManager.rpcSource

    init {
        syncItems()
    }

    private fun syncItems() {
        val currentRpcSourceName = currentRpcSource.name

        _itemsFlow.value = rpcSourceManager.allRpcSources.map { rpcSource ->
            Item(rpcSource, rpcSource.name == currentRpcSourceName)
        }
    }

    fun setCurrentSource(name: String) {
        if (currentRpcSource.name == name) return

        val rpcSource = items.find { it.rpcSource.name == name }?.rpcSource ?: return

        rpcSourceManager.save(rpcSource)
    }

    override fun clear() = Unit

    data class Item(val rpcSource: RpcSource, val selected: Boolean)

}
