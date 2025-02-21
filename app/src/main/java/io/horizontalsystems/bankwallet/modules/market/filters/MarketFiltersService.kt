package io.horizontalsystems.bankwallet.modules.market.filters

import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.priceChangeValue
import io.horizontalsystems.marketkit.models.Analytics.TechnicalAdvice.Advice
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.MarketInfo
import io.horizontalsystems.marketkit.models.Token
import io.reactivex.Single
import kotlinx.coroutines.rx2.await
import java.math.BigDecimal


class MarketFiltersService(
    private val marketKit: MarketKitWrapper,
    private val baseCurrency: Currency
) : IMarketListFetcher {
    private val blockchainTypes = listOf(
        BlockchainType.Ethereum,
        BlockchainType.BinanceSmartChain,
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
        BlockchainType.Base,
        BlockchainType.ZkSync,
        BlockchainType.Polygon,
        BlockchainType.Unsupported("solana"),
        BlockchainType.Unsupported("sora"),
        BlockchainType.Unsupported("tomochain"),
        BlockchainType.Unsupported("xdai"),
    )
    private var cache: List<MarketInfo>? = null

    val blockchains = marketKit.blockchains(blockchainTypes.map { it.uid })
    val currencyCode = baseCurrency.code

    var coinCount = CoinList.Top200.itemsCount
    var filterMarketCap: Pair<Long?, Long?>? = null
    var sectorIds: List<Int> = listOf()
    var filterVolume: Pair<Long?, Long?>? = null
    var filterPeriod = TimePeriod.TimePeriod_1D
    var filterPriceChange: Pair<Long?, Long?>? = null
    var filterBlockchains = listOf<Blockchain>()
    var filterTradingSignal = listOf<Advice>()
    var filterOutperformedBtcOn = false
    var filterOutperformedEthOn = false
    var filterOutperformedBnbOn = false
    var filterPriceCloseToAth = false
    var filterPriceCloseToAtl = false
    var filterListedOnTopExchanges = false
    var filterSolidCex = false
    var filterSolidDex = false
    var filterGoodDistribution = false

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

    suspend fun fetchNumberOfItems(): Int {
        return getTopMarketList().await().size
    }

    suspend fun getSectors(): List<SectorItem> {
        return marketKit.categoriesSingle().blockingGet().map { coinCategory ->
            SectorItem(coinCategory.id, coinCategory.name)
        }
    }

    private fun getTopMarketList(): Single<Map<Int, MarketInfo>> {
        val topMarketListAsync = if (cache != null) {
            Single.just(cache)
        } else {
            marketKit.advancedMarketInfosSingle(coinCount, baseCurrency.code)
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
                && inSectors(marketInfo.categoryIds, sectorIds)
                && filterByRange(filterPriceChange, priceChangeValue.toLong())
                && (!filterPriceCloseToAth || closeToAllTime(marketInfo.athPercentage))
                && (!filterPriceCloseToAtl || closeToAllTime(marketInfo.atlPercentage))
                && (!filterOutperformedBtcOn || outperformed(priceChangeValue, "bitcoin"))
                && (!filterOutperformedEthOn || outperformed(priceChangeValue, "ethereum"))
                && (!filterOutperformedBnbOn || outperformed(priceChangeValue, "binancecoin"))
                && (!filterListedOnTopExchanges || marketInfo.listedOnTopExchanges == true)
                && (!filterSolidCex || marketInfo.solidCex == true)
                && (!filterSolidDex || marketInfo.solidDex == true)
                && (!filterGoodDistribution || marketInfo.goodDistribution == true)
                && inAdvice(marketInfo.advice)
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

        return (coinMarket.priceChangeValue(filterPeriod) ?: BigDecimal.ZERO) < value
    }

    private fun closeToAllTime(value: BigDecimal?): Boolean {
        return value != null && value.abs() < BigDecimal.TEN
    }

    private fun inAdvice(tokenAdvice: Advice?): Boolean {
        if (filterTradingSignal.isEmpty()) return true
        return filterTradingSignal.contains(tokenAdvice)
    }

    private fun inSectors(ids: List<Int>?, selectedSectorIds: List<Int>): Boolean {
        if (selectedSectorIds.isEmpty()) return true
        if (ids == null) return false
        return ids.intersect(selectedSectorIds.toSet()).isNotEmpty()
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
