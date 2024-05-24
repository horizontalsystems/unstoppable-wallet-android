package cash.p.terminal.widgets

import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.iconUrl
import cash.p.terminal.core.imageUrl
import cash.p.terminal.core.managers.CurrencyManager
import cash.p.terminal.core.managers.MarketFavoritesManager
import cash.p.terminal.core.managers.MarketKitWrapper
import cash.p.terminal.core.providers.Translator
import cash.p.terminal.modules.market.MarketItem
import cash.p.terminal.modules.market.SortingField
import cash.p.terminal.modules.market.TimeDuration
import cash.p.terminal.modules.market.favorites.MarketFavoritesMenuService
import cash.p.terminal.modules.market.favorites.WatchlistSorting
import cash.p.terminal.modules.market.favorites.period
import cash.p.terminal.modules.market.sort
import cash.p.terminal.modules.market.topnftcollections.TopNftCollectionsRepository
import cash.p.terminal.modules.market.topnftcollections.TopNftCollectionsViewItemFactory
import cash.p.terminal.modules.market.topplatforms.TopPlatformsRepository
import kotlinx.coroutines.rx2.await

class MarketWidgetRepository(
    private val marketKit: MarketKitWrapper,
    private val favoritesManager: MarketFavoritesManager,
    private val favoritesMenuService: MarketFavoritesMenuService,
    private val topNftCollectionsRepository: TopNftCollectionsRepository,
    private val topNftCollectionsViewItemFactory: TopNftCollectionsViewItemFactory,
    private val topPlatformsRepository: TopPlatformsRepository,
    private val currencyManager: CurrencyManager
) {
    companion object {
        private const val topGainers = 100
        private const val itemsLimit = 5
    }

    private val currency
        get() = currencyManager.baseCurrency

    suspend fun getMarketItems(marketWidgetType: MarketWidgetType): List<MarketWidgetItem> =
        when (marketWidgetType) {
            MarketWidgetType.Watchlist -> {
                getWatchlist(
                    favoritesMenuService.listSorting,
                    favoritesMenuService.manualSortOrder,
                    favoritesMenuService.timeDuration
                )
            }

            MarketWidgetType.TopGainers -> {
                getTopGainers()
            }

            MarketWidgetType.TopNfts -> {
                getTopNtfs()
            }

            MarketWidgetType.TopPlatforms -> {
                getTopPlatforms()
            }
        }

    private suspend fun getTopPlatforms(): List<MarketWidgetItem> {
        val platformItems = topPlatformsRepository.get(
            sortingField = SortingField.HighestCap,
            timeDuration = TimeDuration.OneDay,
            currencyCode = currency.code,
            forceRefresh = true,
            limit = itemsLimit
        )
        return platformItems.map { item ->
            MarketWidgetItem(
                uid = item.platform.uid,
                title = item.platform.name,
                subtitle = Translator.getString(
                    R.string.MarketTopPlatforms_Protocols,
                    item.protocols
                ),
                label = item.rank.toString(),
                value = App.numberFormatter.formatFiatShort(
                    item.marketCap,
                    currency.symbol,
                    2
                ),
                diff = item.changeDiff,
                blockchainTypeUid = null,
                imageRemoteUrl = item.platform.iconUrl
            )
        }
    }

    private suspend fun getTopNtfs(): List<MarketWidgetItem> {
        val nftCollectionViewItems = topNftCollectionsRepository.get(
            sortingField = SortingField.HighestVolume,
            timeDuration = TimeDuration.SevenDay,
            forceRefresh = true,
            limit = itemsLimit
        ).mapIndexed { index, item ->
            topNftCollectionsViewItemFactory.viewItem(item, TimeDuration.SevenDay, index + 1)
        }

        return nftCollectionViewItems.map {
            MarketWidgetItem(
                uid = it.uid,
                title = it.name,
                subtitle = it.floorPrice,
                label = it.order.toString(),
                value = it.volume,
                diff = it.volumeDiff,
                blockchainTypeUid = it.blockchainType.uid,
                imageRemoteUrl = it.imageUrl ?: ""
            )
        }
    }

    private suspend fun getTopGainers(): List<MarketWidgetItem> {
        val marketItems = marketKit.marketInfosSingle(topGainers, currency.code, false)
            .await()
            .map { MarketItem.createFromCoinMarket(it, currency) }

        val sortedMarketItems = marketItems
            .subList(0, Integer.min(marketItems.size, topGainers))
            .sort(SortingField.TopGainers)
            .subList(0, Integer.min(marketItems.size, itemsLimit))

        return sortedMarketItems.map { marketWidgetItem(it) }
    }

    private suspend fun getWatchlist(
        listSorting: WatchlistSorting,
        manualSortOrder: List<String>,
        timeDuration: TimeDuration
    ): List<MarketWidgetItem> {
        val favoriteCoins = favoritesManager.getAll()
        var marketItems = listOf<MarketItem>()

        if (favoriteCoins.isNotEmpty()) {
            val favoriteCoinUids = favoriteCoins.map { it.coinUid }
            marketItems = marketKit.marketInfosSingle(favoriteCoinUids, currency.code)
                .await()
                .map { marketInfo ->
                    MarketItem.createFromCoinMarket(marketInfo, currency, timeDuration.period)
                }

            if (listSorting == WatchlistSorting.Manual) {
                marketItems = marketItems.sortedBy {
                    manualSortOrder.indexOf(it.fullCoin.coin.uid)
                }
            } else {
                val sortField = when (listSorting) {
                    WatchlistSorting.HighestCap -> SortingField.HighestCap
                    WatchlistSorting.LowestCap -> SortingField.LowestCap
                    WatchlistSorting.Gainers -> SortingField.TopGainers
                    WatchlistSorting.Losers -> SortingField.TopLosers
                    else -> throw IllegalStateException("Manual sorting should be handled separately")
                }
                marketItems = marketItems.sort(sortField)
            }
        }

        return marketItems.map { marketWidgetItem(it) }
    }

    private fun marketWidgetItem(
        marketItem: MarketItem,
    ): MarketWidgetItem {

        return MarketWidgetItem(
            uid = marketItem.fullCoin.coin.uid,
            title = marketItem.fullCoin.coin.name,
            subtitle = marketItem.fullCoin.coin.code,
            label = marketItem.fullCoin.coin.marketCapRank?.toString() ?: "",
            value = App.numberFormatter.formatFiatFull(
                marketItem.rate.value,
                marketItem.rate.currency.symbol
            ),
            diff = marketItem.diff,
            blockchainTypeUid = null,
            imageRemoteUrl = marketItem.fullCoin.coin.imageUrl
        )
    }

}
