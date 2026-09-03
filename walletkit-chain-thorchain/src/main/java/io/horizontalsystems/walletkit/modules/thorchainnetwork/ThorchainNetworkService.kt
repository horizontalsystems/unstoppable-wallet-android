package io.horizontalsystems.walletkit.modules.thorchainnetwork

import io.horizontalsystems.walletkit.core.Clearable
import io.horizontalsystems.walletkit.core.managers.ThorchainRpcSource
import io.horizontalsystems.walletkit.core.managers.ThorchainRpcSourceManager
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class ThorchainNetworkService(
    private val rpcSourceManager: ThorchainRpcSourceManager,
) : Clearable {
    private val _itemsFlow = MutableSharedFlow<List<Item>>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    var items = listOf<Item>()
        private set(value) {
            field = value

            _itemsFlow.tryEmit(value)
        }

    private val currentRpcSource: ThorchainRpcSource
        get() = rpcSourceManager.rpcSource

    init {
        syncItems()
    }

    private fun syncItems() {
        val currentRpcSourceName = currentRpcSource.name

        items = rpcSourceManager.allRpcSources.map { rpcSource ->
            Item(rpcSource, rpcSource.name == currentRpcSourceName)
        }
    }

    val itemsFlow: Flow<List<Item>>
        get() = _itemsFlow

    fun setCurrentSource(name: String) {
        if (currentRpcSource.name == name) return

        val rpcSource = items.find { it.rpcSource.name == name }?.rpcSource ?: return

        rpcSourceManager.save(rpcSource)
    }

    override fun clear() = Unit

    data class Item(val rpcSource: ThorchainRpcSource, val selected: Boolean)

}
