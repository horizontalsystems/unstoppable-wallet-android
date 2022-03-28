package io.horizontalsystems.bankwallet.modules.restore.restoreblockchains

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
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
            .subscribeIO { disableBlockchainLiveData.postValue(it.uid) }
            .let { disposables.add(it) }

        sync(service.items)
    }

    private fun sync(items: List<RestoreBlockchainsService.Item>) {
        val viewItems = items.map { viewItem(it) }
        viewItemsLiveData.postValue(viewItems)
    }

    private fun viewItem(
        item: RestoreBlockchainsService.Item,
    ) = CoinViewItem(
        item.blockchain.uid,
        item.blockchain.icon,
        item.blockchain.title,
        item.blockchain.description,
        state = CoinViewItemState.ToggleVisible(item.enabled, item.hasSettings)
    )

    fun enable(uid: String) {
        val blockchain = RestoreBlockchainsModule.Blockchain.getBlockchainByUid(uid) ?: return
        service.enable(blockchain)
    }

    fun disable(uid: String) {
        val blockchain = RestoreBlockchainsModule.Blockchain.getBlockchainByUid(uid) ?: return
        service.disable(blockchain)
    }

    fun onClickSettings(uid: String) {
        val blockchain = RestoreBlockchainsModule.Blockchain.getBlockchainByUid(uid) ?: return
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
