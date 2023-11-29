package io.horizontalsystems.bankwallet.modules.market.filters

import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.priceChangeValue
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.MarketInfo
import io.horizontalsystems.marketkit.models.Token
import io.reactivex.Single
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.math.BigDecimal


class MarketFiltersService(
    private val marketKit: MarketKitWrapper,
    private val baseCurrency: Currency
) : IMarketListFetcher {

    private val blockchainTypes = listOf(
        BlockchainType.Ethereum,
        BlockchainType.BinanceSmartChain,
        BlockchainType.BinanceChain,
        BlockchainType.ArbitrumOne,
        BlockchainType.Avalanche,
        BlockchainType.Gnosis,
        BlockchainType.Fantom,
        BlockchainType.Unsupported("harmony-shard-0"),
        BlockchainType.Unsupported("huobi-token"),
        BlockchainType.Unsupported("iotex"),
        BlockchainType.Unsupported("moonriver"),
        BlockchainType.Unsupported("okex-chain"),
        BlockchainType.Optimism,
        BlockchainType.Polygon,
        BlockchainType.Unsupported("solana"),
        BlockchainType.Unsupported("sora"),
        BlockchainType.Unsupported("tomochain"),
        BlockchainType.Unsupported("xdai"),
    )

    val blockchains = marketKit.blockchains(blockchainTypes.map { it.uid })

    val currencyCode: String
        get() = baseCurrency.code

    private val allTimeDeltaPercent = BigDecimal.TEN

    var coinCount: Int = CoinList.Top250.itemsCount

    var filterMarketCap: Pair<Long?, Long?>? = null

    var filterVolume: Pair<Long?, Long?>? = null

    var filterPeriod: TimePeriod = TimePeriod.TimePeriod_1D

    var filterPriceChange: Pair<Long?, Long?>? = null

    var filterBlockchains: List<Blockchain> = listOf()

    var filterOutperformedBtcOn: Boolean = false

    var filterOutperformedEthOn: Boolean = false

    var filterOutperformedBnbOn: Boolean = false

    var filterPriceCloseToAth: Boolean = false

    var filterPriceCloseToAtl: Boolean = false

    private val _numberOfItems =
        MutableSharedFlow<Result<Int>?>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val numberOfItems = _numberOfItems.asSharedFlow()

    private var cache: List<MarketInfo>? = null

    override fun fetchAsync(): Single<List<MarketItem>> {
        return getTopMarketList()
            .map { coinMarkets ->
                coinMarkets.map {
                    val coinMarket = it.value

                    MarketItem.createFromCoinMarket(coinMarket, baseCurrency, filterPeriod)
                }
            }
    }

    fun clearCache() {
        cache = null
    }

    suspend fun refresh() = withContext(Dispatchers.IO) {
        try {
            val items = getTopMarketList().blockingGet()
            _numberOfItems.tryEmit(Result.success(items.size))
        } catch (error: Exception) {
            _numberOfItems.tryEmit(Result.failure(error))
        }
    }

    private fun getTopMarketList(): Single<Map<Int, MarketInfo>> {
        val topMarketListAsync = if (cache != null) {
            Single.just(cache)
        } else {
            marketKit.advancedMarketInfosSingle(coinCount, baseCurrency.code, "market_advanced_search_results")
                .doOnSuccess {
                    cache = it
                }
        }

        return topMarketListAsync
            .map {
                it.mapIndexed { index, coinMarket ->
                    index to coinMarket
                }.filter {
                    filterCoinMarket(it.second)
                }.toMap()
            }
    }

    private fun filterCoinMarket(marketInfo: MarketInfo): Boolean {
        val marketCap = marketInfo.marketCap ?: return false
        val totalVolume = marketInfo.totalVolume ?: return false
        val priceChangeValue = marketInfo.priceChangeValue(filterPeriod) ?: return false

        return filterByRange(filterMarketCap, marketCap.toLong())
                && filterByRange(filterVolume, totalVolume.toLong())
                && inBlockchain(marketInfo.fullCoin.tokens)
                && filterByRange(filterPriceChange, priceChangeValue.toLong())
                && (!filterPriceCloseToAth || closeToAllTime(marketInfo.athPercentage))
                && (!filterPriceCloseToAtl || closeToAllTime(marketInfo.atlPercentage))
                && (!filterOutperformedBtcOn || outperformed(priceChangeValue, "bitcoin"))
                && (!filterOutperformedEthOn || outperformed(priceChangeValue, "ethereum"))
                && (!filterOutperformedBnbOn || outperformed(priceChangeValue, "binancecoin"))
    }

    private fun filterByRange(filter: Pair<Long?, Long?>?, value: Long?): Boolean {
        if (filter == null) return true

        filter.first?.let { min ->
            if (value == null || value < min) {
                return false
            }
        }

        filter.second?.let { max ->
            if (value == null || value > max) {
                return false
            }
        }

        return true
    }

    private fun marketInfo(coinUid: String): MarketInfo? =
        cache?.firstOrNull { it.fullCoin.coin.uid == coinUid }

    private fun outperformed(value: BigDecimal?, coinUid: String): Boolean {
        if (value == null) return false
        val coinMarket = marketInfo(coinUid) ?: return false

        return coinMarket.priceChangeValue(filterPeriod) ?: BigDecimal.ZERO < value
    }

    private fun closeToAllTime(value: BigDecimal?): Boolean {
        value ?: return false

        return value.abs() < allTimeDeltaPercent
    }

    private fun inBlockchain(tokens: List<Token>): Boolean {
        if (filterBlockchains.isEmpty()) return true

        tokens.forEach { token ->
            val inBlockchain = filterBlockchains.any { token.blockchain == it }
            if (inBlockchain) return true
        }

        return false
    }
}
