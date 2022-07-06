package io.horizontalsystems.bankwallet.widgets

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.modules.market.MarketField
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.favorites.MarketFavoritesMenuService
import io.horizontalsystems.bankwallet.modules.market.sort
import kotlinx.coroutines.rx2.await
import java.math.BigDecimal

object MarketRepository {

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
            imageRemoteUrl = marketItem.fullCoin.coin.iconUrl
        )
    }

    suspend fun getMarketItems(): List<MarketWidgetItem> {
        val marketKit = App.marketKit
        val favoritesManager = App.marketFavoritesManager
        val favoritesMenuService = MarketFavoritesMenuService(App.localStorage, App.marketWidgetManager)
        val currencyManager = App.currencyManager
        val currency = currencyManager.baseCurrency

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

}
