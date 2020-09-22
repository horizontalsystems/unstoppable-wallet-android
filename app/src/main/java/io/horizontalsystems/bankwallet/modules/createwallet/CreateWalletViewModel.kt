package io.horizontalsystems.bankwallet.modules.createwallet

import android.os.Handler
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.ui.extensions.CoinViewItem
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.lang.Exception
import java.util.*

class CreateWalletViewModel(
        private val service: CreateWalletModule.IService,
        private val clearables: List<Clearable>
) : ViewModel() {

    val viewItemsLiveData = MutableLiveData<List<CoinViewItem>>()
    val finishLiveEvent = SingleLiveEvent<Unit>()
    val canCreateLiveData = MutableLiveData<Boolean>()
    val errorLiveData = MutableLiveData<Exception>()

    private var disposable: Disposable? = null
    private var filter: String? = null

    init {
        Handler().postDelayed({
            syncViewState()
        }, 700)

        service.canCreate
                .subscribe {
                    canCreateLiveData.postValue(it)
                }.let { disposable = it }
    }

    override fun onCleared() {
        clearables.forEach {
            it.clear()
        }
        disposable?.dispose()
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


    private fun syncViewState() {
        val state = service.state

        val viewItems = mutableListOf<CoinViewItem>()

        val filteredFeatureCoins = filtered(state.featured)

        if (filteredFeatureCoins.isNotEmpty()) {
            viewItems.addAll(filteredFeatureCoins.mapIndexed { index, item ->
                viewItem(item, filteredFeatureCoins.size - 1 == index, filteredFeatureCoins.size - 1 == index)
            })
        }

        val filteredItems = filtered(state.items)

        viewItems.addAll(filteredItems.mapIndexed { index, item ->
            viewItem(item, filteredItems.size - 1 == index)
        })

        viewItemsLiveData.postValue(viewItems)
    }

    private fun viewItem(item: CreateWalletService.Item, last: Boolean, showDivider: Boolean = false): CoinViewItem {
        return CoinViewItem.ToggleVisible(item.coin, item.enabled, last, showDivider)
    }

    private fun filtered(items: List<CreateWalletService.Item>): List<CreateWalletService.Item> {
        val filter = filter ?: return items

        return items.filter {
            it.coin.title.toLowerCase(Locale.ENGLISH).contains(filter.toLowerCase(Locale.ENGLISH))
                    || it.coin.code.toLowerCase(Locale.ENGLISH).contains(filter.toLowerCase(Locale.ENGLISH))
        }
    }

}
