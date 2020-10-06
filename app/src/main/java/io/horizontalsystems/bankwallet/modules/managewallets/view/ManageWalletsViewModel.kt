package io.horizontalsystems.bankwallet.modules.managewallets.view

import android.os.Handler
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.DerivationSetting
import io.horizontalsystems.bankwallet.modules.managewallets.ManageWalletsModule
import io.horizontalsystems.bankwallet.modules.managewallets.ManageWalletsService
import io.horizontalsystems.bankwallet.ui.extensions.coinlist.CoinViewItem
import io.horizontalsystems.bankwallet.ui.extensions.coinlist.CoinViewState
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.*

class ManageWalletsViewModel(
        private val service: ManageWalletsModule.IManageWalletsService,
        private val clearables: List<Clearable>
) : ViewModel() {

    private var disposable: Disposable? = null

    val viewStateLiveData = MutableLiveData<CoinViewState>()
    val openDerivationSettingsLiveEvent = SingleLiveEvent<Pair<Coin, AccountType.Derivation>>()
    private var filter: String? = null

    init {
        Handler().postDelayed({
            syncViewState()

            service.stateObservable
                    .subscribeOn(Schedulers.io())
                    .subscribe {
                        syncViewState(it)
                    }
                    .let { disposable = it }
        }, 500)
    }

    override fun onCleared() {
        disposable?.dispose()
        clearables.forEach {
            it.clear()
        }
        super.onCleared()
    }

    fun enable(coin: Coin){
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

    private fun enable(coin: Coin, derivationSetting: DerivationSetting? = null){
        try {
            service.enable(coin, derivationSetting)
        } catch (exception: ManageWalletsService.EnableCoinError){
            when(val e = exception){
                is ManageWalletsService.EnableCoinError.DerivationNotConfirmed -> {
                    openDerivationSettingsLiveEvent.postValue(Pair(coin, e.currentDerivation))
                }
            }
        }
    }

    private fun syncViewState(updatedState: ManageWalletsModule.State? = null) {
        val state = updatedState ?: service.state

        val filteredFeatureCoins = filtered(state.featuredItems)

        val filteredItems = filtered(state.items)

        viewStateLiveData.postValue(CoinViewState(
                filteredFeatureCoins.mapIndexed { index, item ->
                    viewItem(item, filteredFeatureCoins.size - 1 == index)
                },
                filteredItems.mapIndexed { index, item ->
                    viewItem(item, filteredItems.size - 1 == index)
                }
        ))
    }

    private fun viewItem(item: ManageWalletsModule.Item, last: Boolean): CoinViewItem {
        return when(val itemState = item.state) {
            ManageWalletsModule.ItemState.NoAccount -> CoinViewItem.ToggleHidden(item.coin, last)
            is ManageWalletsModule.ItemState.HasAccount -> CoinViewItem.ToggleVisible(item.coin, itemState.hasWallet, last)
        }
    }

    private fun filtered(items: List<ManageWalletsModule.Item>) : List<ManageWalletsModule.Item> {
        val filter = filter ?: return items

        return items.filter {
            it.coin.title.toLowerCase(Locale.ENGLISH).contains(filter.toLowerCase(Locale.ENGLISH))
                    || it.coin.code.toLowerCase(Locale.ENGLISH).contains(filter.toLowerCase(Locale.ENGLISH))
        }
    }

}
