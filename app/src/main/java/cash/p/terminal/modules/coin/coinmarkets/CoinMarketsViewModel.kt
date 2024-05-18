package cash.p.terminal.modules.coin.coinmarkets

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.entities.DataState
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.coin.MarketTickerViewItem
import cash.p.terminal.ui.compose.TranslatableString
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

class CoinMarketsViewModel(private val service: CoinMarketsService) : ViewModel() {
    val verifiedMenu by service::verifiedMenu
    val viewStateLiveData = MutableLiveData<ViewState>(ViewState.Loading)
    val viewItemsLiveData = MutableLiveData<List<MarketTickerViewItem>>()

    init {
        viewModelScope.launch {
            service.stateObservable.asFlow().collect {
                syncState(it)
            }
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
            App.numberFormatter.formatFiatShort(item.volumeFiat, service.currency.symbol, service.currency.decimal),
            App.numberFormatter.formatCoinShort(item.volumeToken, item.baseCoinCode, 8),
            item.tradeUrl,
            if (item.verified) TranslatableString.ResString(R.string.CoinPage_MarketsLabel_Verified) else null
        )
    }

    override fun onCleared() {
        service.stop()
    }

    fun onErrorClick() {
        service.start()
    }

    fun toggleVerifiedType(verifiedType: VerifiedType) {
        service.setVerifiedType(verifiedType)
    }

}
