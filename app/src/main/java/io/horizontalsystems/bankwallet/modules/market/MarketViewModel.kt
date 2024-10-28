package io.horizontalsystems.bankwallet.modules.market

import android.util.Log
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.IMarketStorage
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.LaunchPage
import io.horizontalsystems.bankwallet.modules.market.MarketModule.Tab
import io.horizontalsystems.marketkit.models.MarketGlobal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await

class MarketViewModel(
    private val marketStorage: IMarketStorage,
    private val marketKit: MarketKitWrapper,
    private val currencyManager: CurrencyManager,
    localStorage: ILocalStorage
) : ViewModelUiState<MarketModule.UiState>() {

    val tabs = Tab.entries.toTypedArray()
    private var currency = currencyManager.baseCurrency

    private var marketGlobal: MarketGlobal? = null
    private var selectedTab: Tab = getInitialTab(localStorage.launchPage)

    init {
        viewModelScope.launch {
            currencyManager.baseCurrencyUpdatedFlow.collect {
                currency = currencyManager.baseCurrency
                emitState()
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                marketGlobal = marketKit.marketGlobalSingle(currency.code).await()
                emitState()
            } catch (e: Throwable) {
                Log.e("TAG", "updateMarketOverview: ", e)
            }
        }
    }

    override fun createState() = MarketModule.UiState(
        selectedTab = selectedTab,
        marketGlobal = marketGlobal,
        currency = currency
    )

    fun onSelect(tab: Tab) {
        selectedTab = tab
        marketStorage.currentMarketTab = tab
        emitState()
    }

    private fun getInitialTab(launchPage: LaunchPage?) = when (launchPage) {
        LaunchPage.Watchlist -> Tab.Watchlist
        else -> marketStorage.currentMarketTab ?: Tab.Coins
    }
}