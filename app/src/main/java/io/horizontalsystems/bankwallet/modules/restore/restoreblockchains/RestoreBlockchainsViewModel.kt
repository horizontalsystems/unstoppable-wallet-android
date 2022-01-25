package io.horizontalsystems.bankwallet.modules.restore.restoreblockchains

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.restore.restoreblockchains.RestoreBlockchainsService.ItemState.Supported
import io.horizontalsystems.bankwallet.modules.restore.restoreblockchains.RestoreBlockchainsService.ItemState.Unsupported
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.BackpressureStrategy
import io.reactivex.disposables.CompositeDisposable

class RestoreBlockchainsViewModel(
    private val service: RestoreBlockchainsService,
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

    private fun sync(items: List<RestoreBlockchainsService.Item>) {
        val viewItems = items.map { viewItem(it) }
        viewItemsLiveData.postValue(viewItems)
    }

    private fun viewItem(
        item: RestoreBlockchainsService.Item,
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
        )
    }

    fun enable(uid: String) {
        val blockchain = RestoreBlockchainsModule.Blockchain.valueOf(uid)
        service.enable(blockchain)
    }

    fun disable(uid: String) {
        val blockchain = RestoreBlockchainsModule.Blockchain.valueOf(uid)
        service.disable(blockchain)
    }

    fun onClickSettings(uid: String) {
        val blockchain = RestoreBlockchainsModule.Blockchain.valueOf(uid)
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
