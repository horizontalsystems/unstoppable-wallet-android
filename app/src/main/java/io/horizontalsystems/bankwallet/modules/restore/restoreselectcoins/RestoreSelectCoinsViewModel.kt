package io.horizontalsystems.bankwallet.modules.restore.restoreselectcoins

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.restore.restoreselectcoins.RestoreSelectCoinsService.ItemState.Supported
import io.horizontalsystems.bankwallet.modules.restore.restoreselectcoins.RestoreSelectCoinsService.ItemState.Unsupported
import io.horizontalsystems.bankwallet.ui.extensions.coinlist.CoinViewItem
import io.horizontalsystems.bankwallet.ui.extensions.coinlist.CoinViewItemState
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.views.ListPosition
import io.reactivex.BackpressureStrategy
import io.reactivex.disposables.CompositeDisposable

class RestoreSelectCoinsViewModel(
    private val service: RestoreSelectCoinsService,
    private val clearables: List<Clearable>
) : ViewModel() {

    val viewItemsLiveData = MutableLiveData<List<CoinViewItem>>()
    val disableBlockchainLiveData = MutableLiveData<String>()
    val successLiveEvent = SingleLiveEvent<Unit>()
    val restoreEnabledLiveData: LiveData<Boolean>
        get() = LiveDataReactiveStreams.fromPublisher(
            service.canRestore.toFlowable(BackpressureStrategy.DROP)
        )

    private var disposables = CompositeDisposable()

    init {
        service.itemsObservable
            .subscribeIO { sync(it) }
            .let { disposables.add(it) }

        service.cancelEnableBlockchainObservable
            .subscribeIO { disableBlockchainLiveData.postValue(it.name) }
            .let { disposables.add(it) }

        sync(service.items)
    }

    private fun sync(items: List<RestoreSelectCoinsService.Item>) {
        val itemsSize = items.size
        val viewItems = items.mapIndexed { index, item ->
            viewItem(item, ListPosition.getListPosition(itemsSize, index))
        }
        viewItemsLiveData.postValue(viewItems)
    }

    private fun viewItem(
        item: RestoreSelectCoinsService.Item,
        listPosition: ListPosition
    ): CoinViewItem {
        val state = when (item.state) {
            is Supported -> CoinViewItemState.ToggleVisible(
                item.state.enabled,
                item.state.hasSettings
            )
            is Unsupported -> CoinViewItemState.ToggleHidden
        }

        return CoinViewItem(
            item.blockchain.name,
            item.blockchain.icon,
            item.blockchain.title,
            item.blockchain.description,
            state,
            listPosition
        )
    }

    fun enable(uid: String) {
        val blockchain = RestoreSelectCoinsModule.Blockchain.valueOf(uid)
        service.enable(blockchain)
    }

    fun disable(uid: String) {
        val blockchain = RestoreSelectCoinsModule.Blockchain.valueOf(uid)
        service.disable(blockchain)
    }

    fun onClickSettings(uid: String) {
        val blockchain = RestoreSelectCoinsModule.Blockchain.valueOf(uid)
        service.configure(blockchain)
    }

    fun onRestore() {
        service.restore()
        successLiveEvent.postValue(Unit)
    }

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
        disposables.clear()
    }
}
