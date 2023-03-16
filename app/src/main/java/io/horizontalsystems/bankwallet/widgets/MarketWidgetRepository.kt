package io.horizontalsystems.bankwallet.widgets

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.market.*
import io.horizontalsystems.bankwallet.modules.market.favorites.MarketFavoritesMenuService
import io.horizontalsystems.bankwallet.modules.market.topnftcollections.TopNftCollectionsRepository
import io.horizontalsystems.bankwallet.modules.market.topnftcollections.TopNftCollectionsViewItemFactory
import io.horizontalsystems.bankwallet.modules.market.topplatforms.TopPlatformsRepository
import kotlinx.coroutines.rx2.await
import java.math.BigDecimal

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
                getWatchlist()
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
            forceRefresh = true,
            limit = itemsLimit
        )
        return platformItems.map { item ->
            MarketWidgetItem(
                uid = item.platform.uid,
                title = item.platform.name,
                subtitle = Translator.getString(R.string.MarketTopPlatforms_Protocols, item.protocols),
                label = item.rank.toString(),
                value = App.numberFormatter.formatFiatShort(
                    item.marketCap,
                    currency.symbol,
                    2
                ),
                diff = item.changeDiff,
                marketCap = null,
                volume = null,
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
                marketCap = null,
                diff = it.volumeDiff,
                volume = null,
                blockchainTypeUid = it.blockchainType.uid,
                imageRemoteUrl = it.imageUrl ?: ""
            )
        }
    }

    private suspend fun getTopGainers(): List<MarketWidgetItem> {
        val marketItems = marketKit.marketInfosSingle(topGainers, currency.code)
            .await()
            .map { MarketItem.createFromCoinMarket(it, currency) }

        val sortedMarketItems = marketItems
            .subList(0, Integer.min(marketItems.size, topGainers))
            .sort(SortingField.TopGainers)
            .subList(0, Integer.min(marketItems.size, itemsLimit))

        return sortedMarketItems.map { marketWidgetItem(it, MarketField.PriceDiff) }
    }

    private suspend fun getWatchlist(): List<MarketWidgetItem> {
        val favoriteCoins = favoritesManager.getAll()
        var marketItems = listOf<MarketItem>()

        if (favoriteCoins.isNotEmpty()) {
            val favoriteCoinUids = favoriteCoins.map { it.coinUid }
            marketItems = marketKit.marketInfosSingle(favoriteCoinUids, currency.code)
                .await()
                .map { marketInfo ->
                    MarketItem.createFromCoinMarket(marketInfo, currency)
                }
                .sort(favoritesMenuService.sortingField)
        }

        return marketItems.map { marketWidgetItem(it, favoritesMenuService.marketField) }
    }

    private fun marketWidgetItem(
        marketItem: MarketItem,
        marketField: MarketField,
    ): MarketWidgetItem {
        var marketCap: String? = null
        var volume: String? = null
        var diff: BigDecimal? = null

        when (marketField) {
            MarketField.MarketCap -> {
                marketCap = App.numberFormatter.formatFiatShort(marketItem.marketCap.value, marketItem.marketCap.currency.symbol, 2)
            }
            MarketField.Volume -> {
                volume = App.numberFormatter.formatFiatShort(marketItem.volume.value, marketItem.volume.currency.symbol, 2)
            }
            MarketField.PriceDiff -> {
                diff = marketItem.diff
            }
        }

        return MarketWidgetItem(
            uid = marketItem.fullCoin.coin.uid,
            title = marketItem.fullCoin.coin.name,
            subtitle = marketItem.fullCoin.coin.code,
            label = marketItem.fullCoin.coin.marketCapRank?.toString() ?: "",
            value = App.numberFormatter.formatFiatFull(
                marketItem.rate.value,
                marketItem.rate.currency.symbol
            ),
            marketCap = marketCap,
            volume = volume,
            diff = diff,
            blockchainTypeUid = null,
            imageRemoteUrl = marketItem.fullCoin.coin.imageUrl
        )
    }

}
