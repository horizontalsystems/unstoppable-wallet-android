package io.horizontalsystems.bankwallet.modules.market.discovery

import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.Score
import io.horizontalsystems.bankwallet.modules.market.list.IMarketListFetcher
import io.horizontalsystems.core.entities.Currency
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject

class MarketDiscoveryService(
        private val marketCategoryProvider: MarketCategoryProvider,
        private val xRateManager: IRateManager
) : IMarketListFetcher {

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
            MarketCategory.Blockchains, MarketCategory.Dexes, MarketCategory.Lending, MarketCategory.Privacy,
            MarketCategory.Scaling, MarketCategory.Oracles, MarketCategory.Prediction, MarketCategory.YieldAggregators,
            MarketCategory.FiatStablecoins, MarketCategory.AlgoStablecoins, MarketCategory.TokenizedBitcoin, MarketCategory.StablecoinIssuers,
            MarketCategory.ExchangeTokens, MarketCategory.Metals, MarketCategory.RiskManagement, MarketCategory.Wallets,
            MarketCategory.Synthetics, MarketCategory.IndexFunds, MarketCategory.NFT, MarketCategory.FundRaising,
            MarketCategory.Gaming, MarketCategory.B2B, MarketCategory.Infrastructure, MarketCategory.Staking,
            MarketCategory.Governance, MarketCategory.CrossChain, MarketCategory.Computing
    )

    private fun getAllMarketItemsAsync(currency: Currency): Single<List<MarketItem>> {
        return xRateManager.getTopMarketList(currency.code, 250)
                .map { coinMarkets ->
                    coinMarkets.mapIndexed { index, coinMarket ->
                        MarketItem.createFromCoinMarket(coinMarket, currency, Score.Rank(index + 1))
                    }
                }
    }

    private fun getRatedMarketItemsAsync(currency: Currency): Single<List<MarketItem>> {
        return marketCategoryProvider.getCoinRatingsAsync()
                .flatMap { coinRatingsMap ->
                    val coinCodes = coinRatingsMap.keys.map { it }
                    xRateManager.getCoinMarketList(coinCodes, currency.code)
                            .map { coinMarkets ->
                                coinMarkets.mapNotNull { coinMarket ->
                                    coinRatingsMap[coinMarket.coin.code]?.let { rating ->
                                        MarketItem.createFromCoinMarket(coinMarket, currency, Score.Rating(rating))
                                    }
                                }
                            }
                }
    }

    private fun getMarketItemsByCategoryAsync(category: MarketCategory, currency: Currency): Single<List<MarketItem>> {
        return marketCategoryProvider.getCoinCodesByCategoryAsync(category.id)
                .flatMap { coinCodes ->
                    xRateManager.getCoinMarketList(coinCodes, currency.code)
                            .map { coinMarkets ->
                                coinMarkets.map { coinMarket ->
                                    MarketItem.createFromCoinMarket(coinMarket, currency, null)
                                }
                            }
                }
    }

}
