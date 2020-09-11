package io.horizontalsystems.bankwallet.modules.managewallets.view

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.DerivationSetting
import io.horizontalsystems.bankwallet.modules.managewallets.ManageCoinViewItem
import io.horizontalsystems.bankwallet.modules.managewallets.ManageWalletsModule
import io.horizontalsystems.bankwallet.modules.managewallets.ManageWalletsService
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.*

class ManageWalletsViewModel(
        private val service: ManageWalletsModule.IManageWalletsService,
        private val clearables: List<Clearable>
) : ViewModel() {

    private var disposable: Disposable? = null

    val viewItemsLiveData = MutableLiveData<List<ManageCoinViewItem>>()
    val openDerivationSettingsLiveEvent = SingleLiveEvent<Pair<Coin, AccountType.Derivation>>()
    private var filter: String? = null

    init {
        service.stateObservable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    syncViewState(it)
                }
                .let { disposable = it }

        syncViewState()
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

        val viewItems = mutableListOf<ManageCoinViewItem>()

        val filteredFeatureCoins = filtered(state.featuredItems)

        if (filteredFeatureCoins.isNotEmpty()) {
            viewItems.addAll(filteredFeatureCoins.mapIndexed { index, item ->
                viewItem(item, state.featuredItems.size - 1 == index)
            })
            viewItems.add(ManageCoinViewItem.Divider)
        }

        viewItems.addAll(filtered(state.items).mapIndexed { index, item ->
            viewItem(item, state.items.size - 1 == index)
        })

        viewItemsLiveData.postValue(viewItems)
    }

    private fun viewItem(item: ManageWalletsModule.Item, last: Boolean): ManageCoinViewItem {
        return when(val itemState = item.state) {
            ManageWalletsModule.ItemState.NoAccount -> ManageCoinViewItem.ToggleHidden(item.coin, last)
            is ManageWalletsModule.ItemState.HasAccount -> ManageCoinViewItem.ToggleVisible(item.coin, itemState.hasWallet, last)
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
