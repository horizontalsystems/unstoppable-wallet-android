package io.horizontalsystems.bankwallet.modules.market

import androidx.compose.runtime.Immutable
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.marketkit.models.FullCoin

@Immutable
data class MarketViewItem(
    val fullCoin: FullCoin,
    val coinRate: String,
    val marketDataValue: MarketDataValue,
    val rank: String?,
    val favorited: Boolean,
) {

    val coinUid: String
        get() = fullCoin.coin.uid

    val coinCode: String
        get() = fullCoin.coin.code

    val coinName: String
        get() = fullCoin.coin.name

    val iconUrl: String
        get() = fullCoin.coin.imageUrl

    val iconPlaceHolder: Int
        get() = fullCoin.iconPlaceholder

    fun areItemsTheSame(other: MarketViewItem): Boolean {
        return fullCoin.coin == other.fullCoin.coin
    }

    fun areContentsTheSame(other: MarketViewItem): Boolean {
        return this == other
    }

    companion object {
        fun create(
            marketItem: MarketItem,
            favorited: Boolean = false
        ): MarketViewItem {
            return MarketViewItem(
                marketItem.fullCoin,
                App.numberFormatter.formatFiatFull(
                    marketItem.rate.value,
                    marketItem.rate.currency.symbol
                ),
                MarketDataValue.Diff(marketItem.diff),
                marketItem.rank?.toString(),
                favorited
            )
        }

        fun create(
            marketItem: MarketItem,
            marketField: MarketField,
            favorited: Boolean = false
        ): MarketViewItem {
            val marketDataValue = when (marketField) {
                MarketField.MarketCap -> {
                    val marketCapFormatted = App.numberFormatter.formatFiatShort(
                        marketItem.marketCap.value,
                        marketItem.marketCap.currency.symbol,
                        2
                    )

                    MarketDataValue.MarketCap(marketCapFormatted)
                }
                MarketField.Volume -> {
                    val volumeFormatted = App.numberFormatter.formatFiatShort(
                        marketItem.volume.value,
                        marketItem.volume.currency.symbol,
                        2
                    )

                    MarketDataValue.Volume(volumeFormatted)
                }
                MarketField.PriceDiff -> {
                    MarketDataValue.Diff(marketItem.diff)
                }
            }
            return MarketViewItem(
                marketItem.fullCoin,
                App.numberFormatter.formatFiatFull(
                    marketItem.rate.value,
                    marketItem.rate.currency.symbol
                ),
                marketDataValue,
                marketItem.rank?.toString(),
                favorited
            )
        }
    }
}