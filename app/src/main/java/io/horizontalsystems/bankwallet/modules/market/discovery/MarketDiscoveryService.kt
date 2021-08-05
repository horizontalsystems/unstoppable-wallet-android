package io.horizontalsystems.bankwallet.modules.market.discovery

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.Score
import io.horizontalsystems.bankwallet.modules.market.list.IMarketListFetcher
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.xrateskit.entities.TimePeriod
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject

class MarketDiscoveryService(
        private val xRateManager: IRateManager,
        private val backgroundManager: BackgroundManager
) : IMarketListFetcher, BackgroundManager.Listener, Clearable {

    private val dataUpdatedSubject = PublishSubject.create<Unit>()

    override val dataUpdatedAsync: Observable<Unit>
        get() = dataUpdatedSubject

    override fun fetchAsync(currency: Currency) = when (val category = marketCategory) {
        null -> getAllMarketItemsAsync(currency)
        MarketCategory.Rated -> getRatedMarketItemsAsync(currency)
        else -> getMarketItemsByCategoryAsync(category, currency)
    }

    var marketCategory: MarketCategory? = null
        set(value) {
            if (field != value) {
                field = value

                dataUpdatedSubject.onNext(Unit)
            }
        }

    val marketCategories = listOf(
            MarketCategory.Blockchains, MarketCategory.Dexes, MarketCategory.Lending, MarketCategory.YieldAggregators,
            MarketCategory.Gaming, MarketCategory.Oracles, MarketCategory.NFT, MarketCategory.Privacy, MarketCategory.Storage,
            MarketCategory.Wallets, MarketCategory.Identity, MarketCategory.Scaling, MarketCategory.Analytics,
            MarketCategory.YieldTokens, MarketCategory.ExchangeTokens, MarketCategory.FiatStablecoins,
            MarketCategory.TokenizedBitcoin, MarketCategory.RiskManagement, MarketCategory.Synthetics,
            MarketCategory.IndexFunds, MarketCategory.Prediction, MarketCategory.FundRaising,  MarketCategory.Infrastructure,

    )

    init {
        backgroundManager.registerListener(this)
    }

    override fun willEnterForeground() {
        dataUpdatedSubject.onNext(Unit)
    }

    override fun clear() {
        backgroundManager.unregisterListener(this)
    }

    private fun getAllMarketItemsAsync(currency: Currency): Single<List<MarketItem>> {
        return xRateManager.getTopMarketList(currency.code, 250, TimePeriod.HOUR_24)
                .map { coinMarkets ->
                    coinMarkets.mapIndexed { index, coinMarket ->
                        MarketItem.createFromCoinMarket(coinMarket, currency, Score.Rank(index + 1))
                    }
                }
    }

    private fun getRatedMarketItemsAsync(currency: Currency): Single<List<MarketItem>> {
        return xRateManager.getCoinRatingsAsync()
                .flatMap { coinRatingsMap ->
                    val coinTypes = coinRatingsMap.keys.map { it }
                    xRateManager.getCoinMarketList(coinTypes, currency.code)
                            .map { coinMarkets ->
                                coinMarkets.mapNotNull { coinMarket ->
                                    coinRatingsMap[coinMarket.data.type]?.let { rating ->
                                        MarketItem.createFromCoinMarket(coinMarket, currency, Score.Rating(rating))
                                    }
                                }
                            }
                }
    }

    private fun getMarketItemsByCategoryAsync(category: MarketCategory, currency: Currency): Single<List<MarketItem>> {
        return xRateManager.getCoinMarketListByCategory(category.id, currency.code)
                .map { coinMarkets ->
                    coinMarkets.map { coinMarket ->
                        MarketItem.createFromCoinMarket(coinMarket, currency, null)
                    }
                }
    }

}
