package io.horizontalsystems.bankwallet.modules.market.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.modules.market.discovery.MarketCategory
import io.horizontalsystems.marketkit.models.FullCoin
import io.reactivex.disposables.CompositeDisposable

class MarketSearchViewModel(
    private val service: MarketSearchService,
    private val clearables: List<Clearable>
) : ViewModel() {

    val marketCategories = listOf(
        MarketCategory.TopCoins,
        MarketCategory.Blockchains,
        MarketCategory.Dexes,
        MarketCategory.Lending,
        MarketCategory.YieldAggregators,
        MarketCategory.Gaming,
        MarketCategory.Oracles,
        MarketCategory.NFT,
        MarketCategory.Privacy,
        MarketCategory.Storage,
        MarketCategory.Wallets,
        MarketCategory.Identity,
        MarketCategory.Scaling,
        MarketCategory.Analytics,
        MarketCategory.YieldTokens,
        MarketCategory.ExchangeTokens,
        MarketCategory.Stablecoins,
        MarketCategory.TokenizedBitcoin,
        MarketCategory.RiskManagement,
        MarketCategory.Synthetics,
        MarketCategory.IndexFunds,
        MarketCategory.Prediction,
        MarketCategory.FundRaising,
        MarketCategory.Infrastructure,
        )

    private val disposable = CompositeDisposable()

    private val _coinResult = MutableLiveData<List<FullCoin>>()
    val coinResult: LiveData<List<FullCoin>> = _coinResult

    fun searchByQuery(query: String){
        val queryTrimmed = query.trim()
        if (queryTrimmed.count() >= 2){
            _coinResult.value = service.getCoinsByQuery(queryTrimmed)
        } else {
            _coinResult.value = listOf()
        }
    }

    override fun onCleared() {
        disposable.clear()
        clearables.forEach(Clearable::clear)
    }
}
