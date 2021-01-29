package io.horizontalsystems.bankwallet.modules.market.top

import android.content.Context
import androidx.annotation.StringRes
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.views.helpers.LayoutHelper
import java.math.BigDecimal
import java.util.*

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

data class MarketTopItem(
        val score: Score,
        val coinCode: String,
        val coinName: String,
        val volume: BigDecimal,
        val rate: BigDecimal,
        val diff: BigDecimal,
        val marketCap: BigDecimal?
)
