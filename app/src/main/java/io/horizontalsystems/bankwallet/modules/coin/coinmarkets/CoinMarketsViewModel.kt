package io.horizontalsystems.bankwallet.modules.coin.coinmarkets

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.MarketTickerViewItem
import io.horizontalsystems.bankwallet.modules.coin.coinmarkets.CoinMarketsModule.VolumeMenuType
import io.reactivex.disposables.CompositeDisposable

class CoinMarketsViewModel(private val service: CoinMarketsService) : ViewModel() {
    val volumeMenu by service::volumeMenu
    val sortingType by service::sortType
    val viewStateLiveData = MutableLiveData<ViewState>(ViewState.Loading)
    val viewItemsLiveData = MutableLiveData<List<MarketTickerViewItem>>()

    private val disposables = CompositeDisposable()

    init {
        service.stateObservable
            .subscribeIO {
                syncState(it)
            }
            .let {
                disposables.add(it)
            }

        service.start()
    }

    private fun syncState(state: DataState<List<MarketTickerItem>>) {
        viewStateLiveData.postValue(state.viewState)

        state.dataOrNull?.let { data ->
            viewItemsLiveData.postValue(data.map { createViewItem(it) })
        }
    }

    private fun createViewItem(item: MarketTickerItem): MarketTickerViewItem {
        return MarketTickerViewItem(
            item.market,
            item.marketImageUrl,
            "${item.baseCoinCode}/${item.targetCoinCode}",
            App.numberFormatter.formatCoin(item.rate, item.targetCoinCode, 0, 8),
            getVolume(item)
        )
    }

    private fun getVolume(item: MarketTickerItem): String {
        val (shortenValue, suffix) = App.numberFormatter.shortenValue(item.volume)

        return when (item.volumeType) {
            is VolumeMenuType.Coin -> {
                "$shortenValue $suffix ${item.baseCoinCode}"
            }
            is VolumeMenuType.Currency -> {
                App.numberFormatter.formatFiat(
                    shortenValue,
                    service.currency.symbol,
                    0,
                    2
                ) + " " + suffix
            }
        }
    }

    override fun onCleared() {
        disposables.clear()
        service.stop()
    }

    fun onErrorClick() {
        service.start()
    }

    fun toggleSortType(sortType: SortType) {
        service.setSortType(sortType)
    }

    fun toggleVolumeType(volumeMenuType: VolumeMenuType) {
        service.setVolumeType(volumeMenuType)
    }
}
