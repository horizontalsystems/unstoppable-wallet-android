package io.horizontalsystems.bankwallet.modules.market

import androidx.compose.runtime.Immutable
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.alternativeImageUrl
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.marketkit.models.Analytics.TechnicalAdvice.Advice
import io.horizontalsystems.marketkit.models.FullCoin

@Immutable
data class MarketViewItem(
    val fullCoin: FullCoin,
    val subtitle: String,
    val value: String,
    val marketDataValue: MarketDataValue,
    val rank: String?,
    val favorited: Boolean,
    val signal: Advice? = null
) {

    val coinUid: String
        get() = fullCoin.coin.uid

    val coinCode: String
        get() = fullCoin.coin.code

    val coinName: String
        get() = fullCoin.coin.name

    val iconUrl: String
        get() = fullCoin.coin.imageUrl

    val alternativeIconUrl: String?
        get() = fullCoin.coin.alternativeImageUrl

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
            favorited: Boolean = false,
            advice: Advice? = null
        ): MarketViewItem {
            return MarketViewItem(
                marketItem.fullCoin,
                App.numberFormatter.formatFiatShort(
                    marketItem.marketCap.value,
                    marketItem.marketCap.currency.symbol,
                    2
                ),
                App.numberFormatter.formatFiatFull(
                    marketItem.rate.value,
                    marketItem.rate.currency.symbol
                ),
                MarketDataValue.Diff(marketItem.diff),
                marketItem.rank?.toString(),
                favorited,
                advice
            )
        }
    }
}