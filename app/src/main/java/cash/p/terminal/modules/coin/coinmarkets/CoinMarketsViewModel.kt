package cash.p.terminal.modules.coin.coinmarkets

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.ui_compose.entities.DataState
import cash.p.terminal.ui_compose.entities.ViewState
import cash.p.terminal.modules.coin.MarketTickerViewItem
import cash.p.terminal.strings.helpers.TranslatableString
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
            market = item.market,
            marketImageUrl = item.marketImageUrl,
            pair = "${item.baseCoinCode}/${item.targetCoinCode}",
            volumeFiat = App.numberFormatter.formatFiatShort(item.volumeFiat, service.currency.symbol, service.currency.decimal),
            volumeToken = App.numberFormatter.formatCoinShort(item.volumeToken, item.baseCoinCode, 8),
            tradeUrl = item.tradeUrl,
            badge = if (item.verified) TranslatableString.ResString(R.string.CoinPage_MarketsLabel_Verified) else null
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
