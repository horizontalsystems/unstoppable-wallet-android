package io.horizontalsystems.bankwallet.modules.coin.coinmarkets

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.MarketTickerViewItem
import io.horizontalsystems.bankwallet.modules.coin.coinmarkets.CoinMarketsModule.VolumeMenuType
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.reactivex.disposables.CompositeDisposable

class CoinMarketsViewModel(private val service: CoinMarketsService) : ViewModel() {
    val volumeMenu by service::volumeMenu
    val verifiedMenu by service::verifiedMenu
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
            item.tradeUrl,
            if (item.verified) TranslatableString.ResString(R.string.CoinPage_MarketsLabel_Verified) else null
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

    fun toggleVerifiedType(verifiedType: VerifiedType) {
        service.setVerifiedType(verifiedType)
    }

    fun toggleVolumeType(volumeMenuType: VolumeMenuType) {
        service.setVolumeType(volumeMenuType)
    }
}
