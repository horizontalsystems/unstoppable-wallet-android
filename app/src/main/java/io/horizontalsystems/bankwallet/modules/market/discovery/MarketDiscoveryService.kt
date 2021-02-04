package io.horizontalsystems.bankwallet.modules.market.discovery

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.Score
import io.horizontalsystems.core.entities.Currency
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject

class MarketDiscoveryService(
        private val marketCategoryProvider: MarketCategoryProvider,
        val currency: Currency,
        private val xRateManager: IRateManager
) : Clearable {

    sealed class State {
        object Loading : State()
        object Loaded : State()
        data class Error(val error: Throwable) : State()
    }

    val stateObservable: BehaviorSubject<State> = BehaviorSubject.createDefault(State.Loading)

    var marketItems: List<MarketItem> = listOf()

    var marketCategory: MarketCategory? = null
        set(value) {
            if (field != value) {
                field = value

                marketItems = listOf()
                fetch()
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

    private var itemsDisposable: Disposable? = null

    init {
        fetch()
    }

    fun refresh() {
        fetch()
    }

    override fun clear() {
        itemsDisposable?.dispose()
    }

    private fun fetch() {
        itemsDisposable?.dispose()
        itemsDisposable = null

        stateObservable.onNext(State.Loading)

        val marketItemsSingle = when (val category = marketCategory) {
            null -> getAllMarketItemsAsync()
            MarketCategory.Rated -> getRatedMarketItemsAsync()
            else -> getMarketItemsByCategoryAsync(category)
        }

        itemsDisposable = marketItemsSingle
                .subscribeIO({
                    marketItems = it
                    stateObservable.onNext(State.Loaded)
                }, {
                    stateObservable.onNext(State.Error(it))
                })
    }

    private fun getAllMarketItemsAsync(): Single<List<MarketItem>> {
        return xRateManager.getTopMarketList(currency.code, 250)
                .map { coinMarkets ->
                    coinMarkets.mapIndexed { index, coinMarket ->
                        MarketItem.createFromCoinMarket(coinMarket, Score.Rank(index + 1))
                    }
                }
    }

    private fun getRatedMarketItemsAsync(): Single<List<MarketItem>> {
        return marketCategoryProvider.getCoinRatingsAsync()
                .flatMap { coinRatingsMap ->
                    val coinCodes = coinRatingsMap.keys.map { it }
                    xRateManager.getCoinMarketList(coinCodes, currency.code)
                            .map { coinMarkets ->
                                coinMarkets.mapNotNull { coinMarket ->
                                    coinRatingsMap[coinMarket.coin.code]?.let { rating ->
                                        MarketItem.createFromCoinMarket(coinMarket, Score.Rating(rating))
                                    }
                                }
                            }
                }
    }

    private fun getMarketItemsByCategoryAsync(category: MarketCategory): Single<List<MarketItem>> {
        return marketCategoryProvider.getCoinCodesByCategoryAsync(category.id)
                .flatMap { coinCodes ->
                    xRateManager.getCoinMarketList(coinCodes, currency.code)
                            .map { coinMarkets ->
                                coinMarkets.map { coinMarket ->
                                    MarketItem.createFromCoinMarket(coinMarket, null)
                                }
                            }
                }
    }

}
