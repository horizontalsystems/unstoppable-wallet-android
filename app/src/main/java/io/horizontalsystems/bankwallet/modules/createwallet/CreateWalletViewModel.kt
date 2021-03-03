package io.horizontalsystems.bankwallet.modules.createwallet

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.ui.extensions.coinlist.CoinViewItem
import io.horizontalsystems.bankwallet.ui.extensions.coinlist.CoinViewState
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.views.ListPosition
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.*

class CreateWalletViewModel(
        private val service: CreateWalletModule.IService,
        private val clearables: List<Clearable>
) : ViewModel() {

    val viewStateLiveData = MutableLiveData<CoinViewState>()
    val finishLiveEvent = SingleLiveEvent<Unit>()
    val canCreateLiveData = MutableLiveData<Boolean>()
    val errorLiveData = MutableLiveData<Exception>()

    private var disposables = CompositeDisposable()
    private var filter: String? = null

    init {
        Handler(Looper.getMainLooper()).postDelayed({
            syncViewState()

            service.stateAsync
                    .subscribeOn(Schedulers.io())
                    .subscribe {
                        syncViewState(it)
                    }
                    .let { disposables.add(it) }
        }, 700)

        service.canCreateAsync
                .subscribe {
                    canCreateLiveData.postValue(it)
                }.let { disposables.add(it) }
    }

    override fun onCleared() {
        clearables.forEach {
            it.clear()
        }
        disposables.clear()
        super.onCleared()
    }

    fun enable(coin: Coin) {
        try {
            service.enable(coin)
        } catch (e: Exception) {
            errorLiveData.postValue(e)
            syncViewState()
        }
    }

    fun disable(coin: Coin) {
        service.disable(coin)
    }

    fun updateFilter(newText: String?) {
        filter = newText
        syncViewState()
    }

    fun onCreate() {
        try {
            service.create()
            finishLiveEvent.call()
        } catch (e: Exception) {
            errorLiveData.postValue(e)
        }
    }


    private fun syncViewState(serviceState: CreateWalletService.State? = null) {
        val state = serviceState ?: service.state

        val filteredFeatureCoins = filtered(state.featured)

        val filteredItems = filtered(state.items)

        viewStateLiveData.postValue(CoinViewState(
                filteredFeatureCoins.mapIndexed { index, item ->
                    viewItem(item, ListPosition.getListPosition(filteredFeatureCoins.size, index))
                },
                filteredItems.mapIndexed { index, item ->
                    viewItem(item, ListPosition.getListPosition(filteredItems.size, index))
                }
        ))
    }

    private fun viewItem(item: CreateWalletService.Item, listPosition: ListPosition): CoinViewItem {
        return CoinViewItem.ToggleVisible(item.coin, item.enabled, listPosition)
    }

    private fun filtered(items: List<CreateWalletService.Item>): List<CreateWalletService.Item> {
        val filter = filter ?: return items

        return items.filter {
            it.coin.title.toLowerCase(Locale.ENGLISH).contains(filter.toLowerCase(Locale.ENGLISH))
                    || it.coin.code.toLowerCase(Locale.ENGLISH).contains(filter.toLowerCase(Locale.ENGLISH))
        }
    }

}
