package io.horizontalsystems.walletkit.modules.xrpnetwork

import io.horizontalsystems.walletkit.core.Clearable
import io.horizontalsystems.walletkit.core.ServiceState
import io.horizontalsystems.walletkit.core.managers.XrpRpcSource
import io.horizontalsystems.walletkit.core.managers.XrpRpcSourceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class XrpNetworkService(
    private val rpcSourceManager: XrpRpcSourceManager,
) : ServiceState<XrpNetworkService.State>(), Clearable {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val mutex = Mutex()

    private var items = listOf<Item>()

    override fun createState() = State(items = items)

    init {
        syncItems()

        coroutineScope.launch {
            rpcSourceManager.rpcSourceUpdatedFlow.collect {
                mutex.withLock {
                    syncItems()
                }
            }
        }
    }

    private fun syncItems() {
        val currentRpcSourceName = rpcSourceManager.rpcSource.name

        items = rpcSourceManager.allRpcSources.map { rpcSource ->
            Item(rpcSource, rpcSource.name == currentRpcSourceName)
        }

        emitState()
    }

    fun setCurrentSource(name: String) {
        if (rpcSourceManager.rpcSource.name == name) return

        val rpcSource = items.find { it.rpcSource.name == name }?.rpcSource ?: return

        rpcSourceManager.save(rpcSource)
    }

    override fun clear() {
        coroutineScope.cancel()
    }

    data class State(val items: List<Item>)

    data class Item(val rpcSource: XrpRpcSource, val selected: Boolean)
}
