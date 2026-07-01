package io.horizontalsystems.bankwallet.modules.solananetwork

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.managers.SolanaRpcSourceManager
import io.horizontalsystems.solanakit.models.RpcSource
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

class SolanaNetworkService(
        private val rpcSourceManager: SolanaRpcSourceManager,
) : Clearable {
    private val disposables = CompositeDisposable()

    private val itemsSubject = BehaviorSubject.create<List<Item>>()
    var items = listOf<Item>()
        private set(value) {
            field = value

            itemsSubject.onNext(value)
        }

    private val currentRpcSource: RpcSource
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

    val itemsObservable: Observable<List<Item>>
        get() = itemsSubject

    fun setCurrentSource(name: String) {
        if (currentRpcSource.name == name) return

        val rpcSource = items.find { it.rpcSource.name == name }?.rpcSource ?: return

        rpcSourceManager.save(rpcSource)
    }

    override fun clear() {
        disposables.clear()
    }

    data class Item(val rpcSource: RpcSource, val selected: Boolean)

}
