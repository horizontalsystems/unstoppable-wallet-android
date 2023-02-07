package cash.p.terminal.modules.coin.coinmarkets

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cash.p.terminal.core.App
import cash.p.terminal.core.subscribeIO
import cash.p.terminal.entities.DataState
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.coin.MarketTickerViewItem
import cash.p.terminal.modules.coin.coinmarkets.CoinMarketsModule.VolumeMenuType
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
            App.numberFormatter.formatCoinFull(item.rate, item.targetCoinCode, 8),
            getVolume(item),
            item.tradeUrl
        )
    }

    private fun getVolume(item: MarketTickerItem) = when (item.volumeType) {
        is VolumeMenuType.Coin -> {
            App.numberFormatter.formatCoinShort(item.volume, item.baseCoinCode, 8)
        }
        is VolumeMenuType.Currency -> {
            val currency = service.currency
            App.numberFormatter.formatFiatShort(item.volume, currency.symbol, currency.decimal)
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
