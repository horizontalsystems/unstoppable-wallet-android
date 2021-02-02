package io.horizontalsystems.bankwallet.modules.market.discovery

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.Score
import io.horizontalsystems.core.entities.Currency
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
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
                fetch()
            }
        }

    val marketCategories = listOf(
            /*MarketCategory.Rated, */MarketCategory.Blockchains, MarketCategory.Privacy, MarketCategory.Scaling,
            MarketCategory.Infrastructure, MarketCategory.RiskManagement, MarketCategory.Oracles, MarketCategory.PredictionMarkets,
            MarketCategory.DefiAggregators, MarketCategory.Dexes, MarketCategory.Synthetics, MarketCategory.Metals,
            MarketCategory.Lending, MarketCategory.GamingAndVr, MarketCategory.FundRaising, MarketCategory.InternetOfThings,
            MarketCategory.B2B, MarketCategory.NFT, MarketCategory.Wallets, MarketCategory.Staking,
            MarketCategory.FiatStablecoins, MarketCategory.TokenizedBitcoin, MarketCategory.AlgoStablecoins, MarketCategory.ExchangeTokens
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
                .subscribeOn(Schedulers.io())
                .subscribe({
                    marketItems = it
                    stateObservable.onNext(State.Loaded)
                }, {
                    stateObservable.onNext(State.Error(it))
                })
    }

    private fun getAllMarketItemsAsync(): Single<List<MarketItem>> {
        return xRateManager.getTopMarketList(currency.code)
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
