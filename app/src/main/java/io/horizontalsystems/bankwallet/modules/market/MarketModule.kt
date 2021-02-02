package io.horizontalsystems.bankwallet.modules.market

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.views.helpers.LayoutHelper
import io.horizontalsystems.xrateskit.entities.CoinMarket
import java.math.BigDecimal
import java.util.*

object MarketModule {

    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val service = MarketService(App.marketStorage)
            return MarketViewModel(service) as T
        }

    }

    enum class Tab {
        Overview, Discovery, Favorites;

        companion object {
            private val map = values().associateBy(Tab::name)

            fun fromString(type: String?): Tab? = map[type]
        }
    }

    enum class ListType(val sortingField: SortingField, val marketField: MarketField) {
        TopGainers(SortingField.TopGainers, MarketField.PriceDiff),
        TopLosers(SortingField.TopLosers, MarketField.PriceDiff),
        TopByVolume(SortingField.HighestVolume, MarketField.Volume),
    }

}

data class MarketItem(
        val score: Score?,
        val coinCode: String,
        val coinName: String,
        val volume: BigDecimal,
        val rate: BigDecimal,
        val diff: BigDecimal,
        val marketCap: BigDecimal?
) {
    companion object {
        fun createFromCoinMarket(coinMarket: CoinMarket, score: Score?): MarketItem {
            return MarketItem(
                    score,
                    coinMarket.coin.code,
                    coinMarket.coin.title,
                    coinMarket.marketInfo.volume,
                    coinMarket.marketInfo.rate,
                    coinMarket.marketInfo.rateDiffPeriod,
                    coinMarket.marketInfo.marketCap
            )
        }
    }
}

enum class SortingField(@StringRes val titleResId: Int) {
    HighestCap(R.string.Market_Field_HighestCap), LowestCap(R.string.Market_Field_LowestCap),
    HighestVolume(R.string.Market_Field_HighestVolume), LowestVolume(R.string.Market_Field_LowestVolume),
    HighestPrice(R.string.Market_Field_HighestPrice), LowestPrice(R.string.Market_Field_LowestPrice),
    TopGainers(R.string.RateList_TopWinners), TopLosers(R.string.RateList_TopLosers),
}

enum class MarketField(@StringRes val titleResId: Int) {
    MarketCap(R.string.Market_Field_MarketCap),
    Volume(R.string.Market_Field_Volume),
    PriceDiff(R.string.Market_Field_PriceDiff)
}

sealed class Score {
    class Rank(val rank: Int) : Score()
    class Rating(val rating: String) : Score()
}

fun Score.getText(): String {
    return when (this) {
        is Score.Rank -> this.rank.toString()
        is Score.Rating -> this.rating
    }
}

fun Score.getTextColor(context: Context): Int {
    return when (this) {
        is Score.Rating -> context.getColor(R.color.dark)
        is Score.Rank -> context.getColor(R.color.grey)
    }
}

fun Score.getBackgroundTintColor(context: Context): Int {
    return when (this) {
        is Score.Rating -> {
            when (rating.toUpperCase(Locale.ENGLISH)) {
                "A" -> LayoutHelper.getAttr(R.attr.ColorJacob, context.theme, context.getColor(R.color.yellow_d))
                "B" -> context.getColor(R.color.issykBlue)
                "C" -> context.getColor(R.color.grey)
                else -> context.getColor(R.color.light_grey)
            }
        }
        is Score.Rank -> {
            LayoutHelper.getAttr(R.attr.ColorJeremy, context.theme, context.getColor(R.color.steel_20))
        }
    }
}

data class MarketViewItem(
        val score: Score?,
        val coinCode: String,
        val coinName: String,
        val rate: String,
        val diff: BigDecimal,
        val marketDataValue: MarketDataValue
) {
    sealed class MarketDataValue {
        class MarketCap(val value: String) : MarketDataValue()
        class Volume(val value: String) : MarketDataValue()
        class Diff(val value: BigDecimal) : MarketDataValue()
    }

    fun areItemsTheSame(other: MarketViewItem): Boolean {
        return coinCode == other.coinCode && coinName == other.coinName
    }

    fun areContentsTheSame(other: MarketViewItem): Boolean {
        return this == other
    }
}

inline fun <T, R : Comparable<R>> Iterable<T>.sortedByDescendingNullLast(crossinline selector: (T) -> R?): List<T> {
    return sortedWith(Comparator.nullsLast(compareByDescending(selector)))
}

inline fun <T, R : Comparable<R>> Iterable<T>.sortedByNullLast(crossinline selector: (T) -> R?): List<T> {
    return sortedWith(Comparator.nullsLast(compareBy(selector)))
}