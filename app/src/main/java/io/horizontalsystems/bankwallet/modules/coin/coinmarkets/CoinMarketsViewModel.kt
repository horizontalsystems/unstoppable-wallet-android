package io.horizontalsystems.bankwallet.modules.coin.coinmarkets

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.coin.MarketTickerViewItem
import io.horizontalsystems.bankwallet.ui.compose.components.ToggleIndicator
import io.horizontalsystems.bankwallet.ui.extensions.MarketListHeaderView
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable

class CoinMarketsViewModel(private val service: CoinMarketsService) : ViewModel() {
    val tickersLiveData = MutableLiveData<List<MarketTickerViewItem>>()
    val topMenuLiveData = MutableLiveData<Pair<MarketListHeaderView.SortMenu, MarketListHeaderView.ToggleButton>>()

    private val disposables = CompositeDisposable()

    init {
        service.itemsObservable
            .subscribeIO {
                tickersLiveData.postValue(it.map { createViewItem(it) })
            }
            .let {
                disposables.add(it)
            }

        Observable
            .combineLatest(
                service.sortTypeObservable,
                service.volumeTypeObservable,
                { sortType, volumeType ->
                    Pair(sortType, volumeType)
                }
            )
            .subscribeIO { (sortType, volumeType) ->
                val direction = when (sortType) {
                    SortType.HighestVolume -> MarketListHeaderView.Direction.Down
                    SortType.LowestVolume -> MarketListHeaderView.Direction.Up
                }

                val title = when (volumeType) {
                    VolumeType.Coin -> service.fullCoin.coin.code
                    VolumeType.Currency -> service.currency.code
                }
                val indicators = listOf(ToggleIndicator(volumeType == VolumeType.Coin), ToggleIndicator(volumeType == VolumeType.Currency))

                topMenuLiveData.postValue(Pair(
                    MarketListHeaderView.SortMenu.DuoOption(direction),
                    MarketListHeaderView.ToggleButton(title, indicators)
                ))
            }
            .let {
                disposables.add(it)
            }

        service.start()
    }

    fun onSwitchSortType() {
        service.setSortType(when (service.sortType) {
            SortType.HighestVolume -> SortType.LowestVolume
            SortType.LowestVolume -> SortType.HighestVolume
        })
    }

    fun onSwitchVolumeType() {
        service.setVolumeType(when (service.volumeType) {
            VolumeType.Coin -> VolumeType.Currency
            VolumeType.Currency -> VolumeType.Coin
        })
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
            VolumeType.Coin -> {
                "$shortenValue $suffix ${item.baseCoinCode}"
            }
            VolumeType.Currency -> {
                App.numberFormatter.formatFiat(shortenValue,
                    service.currency.symbol,
                    0,
                    2) + " " + suffix
            }
        }
    }

    override fun onCleared() {
        disposables.clear()
        service.stop()
    }
}
