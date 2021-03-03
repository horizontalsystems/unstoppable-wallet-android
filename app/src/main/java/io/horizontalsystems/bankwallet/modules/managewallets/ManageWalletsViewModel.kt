package io.horizontalsystems.bankwallet.modules.managewallets

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.modules.blockchainsettings.BlockchainSettingsService
import io.horizontalsystems.bankwallet.ui.extensions.coinlist.CoinViewItem
import io.horizontalsystems.bankwallet.ui.extensions.coinlist.CoinViewState
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.views.ListPosition
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.*

class ManageWalletsViewModel(
        private val service: ManageWalletsService,
        private val blockchainSettingsService: BlockchainSettingsService,
        private val clearables: List<Clearable>)
    : ViewModel() {

    val viewStateLiveData = MutableLiveData<CoinViewState>()

    private var disposables = CompositeDisposable()
    private var filter: String? = null

    init {
        syncViewState()

        service.stateAsync
                .subscribeOn(Schedulers.io())
                .subscribe {
                    syncViewState(it)
                }
                .let { disposables.add(it) }

        service.enableCoinAsync
                .subscribeOn(Schedulers.io())
                .subscribe {
                    syncViewState()
                }
                .let { disposables.add(it) }

        service.cancelEnableCoinAsync
                .subscribeOn(Schedulers.io())
                .subscribe {
                    syncViewState()
                }
                .let { disposables.add(it) }
    }

    override fun onCleared() {
        disposables.clear()
        clearables.forEach {
            it.clear()
        }
        super.onCleared()
    }

    fun enable(coin: Coin) {
        service.enable(coin)
    }

    fun disable(coin: Coin) {
        service.disable(coin)
    }

    fun updateFilter(newText: String?) {
        filter = newText
        syncViewState()
    }

    fun onAddAccount(coin: Coin) {
        service.storeCoinToEnable(coin)
    }

    private fun syncViewState(updatedState: ManageWalletsModule.State? = null) {
        val state = updatedState ?: service.state

        val filteredFeatureCoins = filtered(state.featuredItems)

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

    private fun viewItem(item: ManageWalletsModule.Item, listPosition: ListPosition): CoinViewItem {
        return when (val itemState = item.state) {
            ManageWalletsModule.ItemState.NoAccount -> CoinViewItem.ToggleHidden(item.coin, listPosition)
            is ManageWalletsModule.ItemState.HasAccount -> CoinViewItem.ToggleVisible(item.coin, itemState.hasWallet, listPosition)
        }
    }

    private fun filtered(items: List<ManageWalletsModule.Item>): List<ManageWalletsModule.Item> {
        val filter = filter ?: return items

        return items.filter {
            it.coin.title.toLowerCase(Locale.ENGLISH).contains(filter.toLowerCase(Locale.ENGLISH))
                    || it.coin.code.toLowerCase(Locale.ENGLISH).contains(filter.toLowerCase(Locale.ENGLISH))
        }
    }

}
