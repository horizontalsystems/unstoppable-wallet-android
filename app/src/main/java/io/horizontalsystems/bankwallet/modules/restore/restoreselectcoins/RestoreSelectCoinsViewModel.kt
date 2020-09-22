package io.horizontalsystems.bankwallet.modules.restore.restoreselectcoins

import android.os.Handler
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.DerivationSetting
import io.horizontalsystems.bankwallet.modules.managewallets.ManageWalletsService
import io.horizontalsystems.bankwallet.ui.extensions.CoinViewItem
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.Disposable
import java.util.*

class RestoreSelectCoinsViewModel(
        private val service: RestoreSelectCoinsModule.IService,
        private val clearables: List<Clearable>
) : ViewModel() {

    val viewItemsLiveData = MutableLiveData<List<CoinViewItem>>()
    val enabledCoinsLiveData = SingleLiveEvent<List<Coin>>()
    val openDerivationSettingsLiveEvent = SingleLiveEvent<Pair<Coin, AccountType.Derivation>>()
    val canRestoreLiveData = MutableLiveData<Boolean>()

    private var disposable: Disposable? = null
    private var filter: String? = null

    init {
        Handler().postDelayed({
            syncViewState()

            service.canRestore
                    .subscribe {
                        canRestoreLiveData.postValue(it)
                    }.let { disposable = it }
        }, 700)
    }

    override fun onCleared() {
        clearables.forEach {
            it.clear()
        }
        disposable?.dispose()
        super.onCleared()
    }

    fun enable(coin: Coin) {
        enable(coin, null)
    }

    fun disable(coin: Coin) {
        service.disable(coin)
    }

    fun onSelect(coin: Coin, derivation: DerivationSetting) {
        enable(coin, derivation)
    }

    fun onCancelDerivationSelection() {
        syncViewState()
    }

    fun updateFilter(newText: String?) {
        filter = newText
        syncViewState()
    }

    fun onRestore() {
        enabledCoinsLiveData.postValue(service.enabledCoins)
    }


    private fun enable(coin: Coin, derivationSetting: DerivationSetting? = null) {
        try {
            service.enable(coin, derivationSetting)
        } catch (exception: ManageWalletsService.EnableCoinError) {
            when (val e = exception) {
                is ManageWalletsService.EnableCoinError.DerivationNotConfirmed -> {
                    openDerivationSettingsLiveEvent.postValue(Pair(coin, e.currentDerivation))
                }
            }
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

    private fun viewItem(item: RestoreSelectCoinsService.Item, last: Boolean, showDivider: Boolean = false): CoinViewItem {
        return CoinViewItem.ToggleVisible(item.coin, item.enabled, last, showDivider)
    }

    private fun filtered(items: List<RestoreSelectCoinsService.Item>): List<RestoreSelectCoinsService.Item> {
        val filter = filter ?: return items

        return items.filter {
            it.coin.title.toLowerCase(Locale.ENGLISH).contains(filter.toLowerCase(Locale.ENGLISH))
                    || it.coin.code.toLowerCase(Locale.ENGLISH).contains(filter.toLowerCase(Locale.ENGLISH))
        }
    }

}
