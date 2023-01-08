package io.horizontalsystems.bankwallet.modules.market.filters

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.marketkit.models.Blockchain
import io.reactivex.Single

object MarketFiltersModule {
    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = MarketFiltersService(App.marketKit, App.currencyManager.baseCurrency)
            return MarketFiltersViewModel(service) as T
        }

    }

    enum class FilterDropdown(val titleResId: Int){
        CoinSet(R.string.Market_Filter_ChooseSet),
        MarketCap(R.string.Market_Filter_MarketCap),
        TradingVolume(R.string.Market_Filter_Volume),
        PriceChange(R.string.Market_Filter_PriceChange),
        PricePeriod(R.string.Market_Filter_PricePeriod),
    }

    data class BlockchainViewItem(val blockchain: Blockchain, val checked: Boolean)
}

enum class CoinList(val itemsCount: Int, @StringRes val titleResId: Int) {
    Top100(100, R.string.Market_Filter_Top_100),
    Top250(250, R.string.Market_Filter_Top_250),
    Top500(500, R.string.Market_Filter_Top_500),
    Top1000(1000, R.string.Market_Filter_Top_1000),
    Top1500(1500, R.string.Market_Filter_Top_1500),
}

enum class Range(@StringRes val titleResId: Int, val values: Pair<Long?, Long?>) {
    Range_0_5M(R.string.Market_Filter_Range_0_5M, Pair(null, 5_000_000)),
    Range_5M_20M(R.string.Market_Filter_Range_5M_20M, Pair(5_000_000, 20_000_000)),
    Range_20M_100M(R.string.Market_Filter_Range_20M_100M, Pair(20_000_000, 100_000_000)),
    Range_100M_1B(R.string.Market_Filter_Range_100M_1B, Pair(100_000_000, 1_000_000_000)),
    Range_1B_5B(R.string.Market_Filter_Range_1B_5B, Pair(1_000_000_000, 5_000_000_000)),
    Range_5B_More(R.string.Market_Filter_Range_5B_More, Pair(5_000_000_000, null)),

    Range_0_10M(R.string.Market_Filter_Range_0_10M, Pair(null, 10_000_000)),
    Range_10M_40M(R.string.Market_Filter_Range_10M_40M, Pair(10_000_000, 40_000_000)),
    Range_40M_200M(R.string.Market_Filter_Range_40M_200M, Pair(40_000_000, 200_000_000)),
    Range_200M_2B(R.string.Market_Filter_Range_200M_2B, Pair(200_000_000, 2_000_000_000)),
    Range_2B_10B(R.string.Market_Filter_Range_2B_10B, Pair(2_000_000_000, 10_000_000_000)),
    Range_10B_More(R.string.Market_Filter_Range_10B_More, Pair(10_000_000_000, null)),

    Range_0_50M(R.string.Market_Filter_Range_0_50M, Pair(null, 50_000_000)),
    Range_50M_200M(R.string.Market_Filter_Range_50M_200M, Pair(50_000_000, 200_000_000)),
    Range_200M_1B(R.string.Market_Filter_Range_200M_1B, Pair(200_000_000, 1_000_000_000)),
    Range_1B_10B(R.string.Market_Filter_Range_1B_10B, Pair(1_000_000_000, 10_000_000_000)),
    Range_10B_50B(R.string.Market_Filter_Range_10B_50B, Pair(10_000_000_000, 50_000_000_000)),
    Range_50B_More(R.string.Market_Filter_Range_50B_More, Pair(50_000_000_000, null)),

    Range_0_500M(R.string.Market_Filter_Range_0_500M, Pair(null, 500_000_000)),
    Range_500M_2B(R.string.Market_Filter_Range_500M_2B, Pair(500_000_000, 2_000_000_000)),
    Range_10B_100B(R.string.Market_Filter_Range_10B_100B, Pair(10_000_000_000, 100_000_000_000)),
    Range_100B_500B(R.string.Market_Filter_Range_100B_500B, Pair(100_000_000_000, 500_000_000_000)),
    Range_500B_More(R.string.Market_Filter_Range_500B_More, Pair(500_000_000_000, null));

    companion object {
        fun valuesByCurrency(currencyCode: String) = when (currencyCode) {
            "USD",
            "EUR",
            "GBP",
            "AUD",
            "CAD",
            "CHF",
            "SGD",
            -> listOf(
                Range_0_5M,
                Range_5M_20M,
                Range_20M_100M,
                Range_100M_1B,
                Range_1B_5B,
                Range_5B_More,
            )
            "JPY",
            -> listOf(
                Range_0_500M,
                Range_500M_2B,
                Range_2B_10B,
                Range_10B_100B,
                Range_100B_500B,
                Range_500B_More,
            )
            "BRL",
            "CNY",
            "HKD",
            -> listOf(
                Range_0_50M,
                Range_50M_200M,
                Range_200M_1B,
                Range_1B_10B,
                Range_10B_50B,
                Range_50B_More,
            )
            "ILS",
            -> listOf(
                Range_0_10M,
                Range_10M_40M,
                Range_40M_200M,
                Range_200M_2B,
                Range_2B_10B,
                Range_10B_More,
            )
            "RUB",
            -> listOf(
                Range_0_500M,
                Range_500M_2B,
                Range_2B_10B,
                Range_10B_100B,
                Range_100B_500B,
                Range_500B_More,
            )
            else -> listOf()
        }
    }
}

enum class TimePeriod(@StringRes val titleResId: Int) {
    TimePeriod_1D(R.string.Market_Filter_TimePeriod_1D),
    TimePeriod_1W(R.string.Market_Filter_TimePeriod_1W),
    TimePeriod_2W(R.string.Market_Filter_TimePeriod_2W),
    TimePeriod_1M(R.string.Market_Filter_TimePeriod_1M),
    TimePeriod_6M(R.string.Market_Filter_TimePeriod_6M),
    TimePeriod_1Y(R.string.Market_Filter_TimePeriod_1Y),
}

enum class PriceChange(
    @StringRes val titleResId: Int,
    @ColorRes val color: TextColor,
    val values: Pair<Long?, Long?>
) {
    Positive_10_plus(
        R.string.Market_Filter_PriceChange_Positive_10_plus,
        TextColor.Remus,
        Pair(10, null)
    ),
    Positive_25_plus(
        R.string.Market_Filter_PriceChange_Positive_25_plus,
        TextColor.Remus,
        Pair(25, null)
    ),
    Positive_50_plus(
        R.string.Market_Filter_PriceChange_Positive_50_plus,
        TextColor.Remus,
        Pair(50, null)
    ),
    Positive_100_plus(
        R.string.Market_Filter_PriceChange_Positive_100_plus,
        TextColor.Remus,
        Pair(100, null)
    ),
    Negative_10_minus(
        R.string.Market_Filter_PriceChange_Negative_10_minus,
        TextColor.Lucian,
        Pair(null, -10)
    ),
    Negative_25_minus(
        R.string.Market_Filter_PriceChange_Negative_25_minus,
        TextColor.Lucian,
        Pair(null, -25)
    ),
    Negative_50_minus(
        R.string.Market_Filter_PriceChange_Negative_50_minus,
        TextColor.Lucian,
        Pair(null, -50)
    ),
    Negative_75_minus(
        R.string.Market_Filter_PriceChange_Negative_75_minus,
        TextColor.Lucian,
        Pair(null, -75)
    ),
}

enum class TextColor{
    Remus, Lucian, Grey, Leah
}

class FilterViewItemWrapper<T>(val title: String?, val item: T) {
    override fun equals(other: Any?) = when {
        other !is FilterViewItemWrapper<*> -> false
        else -> item == other.item
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + (item?.hashCode() ?: 0)
        return result
    }

    companion object {
        fun <T>getAny(): FilterViewItemWrapper<T?> {
            return FilterViewItemWrapper(null, null)
        }
    }
}

interface IMarketListFetcher {
    fun fetchAsync(): Single<List<MarketItem>>
}