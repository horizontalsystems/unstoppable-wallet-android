package io.horizontalsystems.bankwallet.modules.market

import android.content.Context
import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.WithTranslatableTitle
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.MarketInfo
import kotlinx.android.parcel.Parcelize
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

    enum class Tab(@StringRes val titleResId: Int) {
        Overview(R.string.Market_Tab_Overview),
        Posts(R.string.Market_Tab_Posts),
        Watchlist(R.string.Market_Tab_Watchlist);

        companion object {
            private val map = values().associateBy(Tab::name)

            fun fromString(type: String?): Tab? = map[type]
        }
    }

    enum class ListType(val sortingField: SortingField, val marketField: MarketField) {
        TopGainers(SortingField.TopGainers, MarketField.PriceDiff),
        TopLosers(SortingField.TopLosers, MarketField.PriceDiff),
    }

}

data class MarketItem(
    val score: Score?,
    val fullCoin: FullCoin,
    val volume: CurrencyValue,
    val rate: CurrencyValue,
    val diff: BigDecimal?,
    val marketCap: CurrencyValue
) {
    companion object {
        fun createFromCoinMarket(marketInfo: MarketInfo, currency: Currency, score: Score?): MarketItem {
            return MarketItem(
                score,
                marketInfo.fullCoin,
                CurrencyValue(currency, marketInfo.totalVolume ?: BigDecimal.ZERO),
                CurrencyValue(currency, marketInfo.price ?: BigDecimal.ZERO),
                marketInfo.priceChange,
                CurrencyValue(currency, marketInfo.marketCap?: BigDecimal.ZERO)
            )
        }
    }
}

fun List<MarketItem>.sort(sortingField: SortingField) = when (sortingField) {
    SortingField.HighestCap -> sortedByDescendingNullLast { it.marketCap.value }
    SortingField.LowestCap -> sortedByNullLast { it.marketCap.value }
    SortingField.HighestVolume -> sortedByDescendingNullLast { it.volume.value }
    SortingField.LowestVolume -> sortedByNullLast { it.volume.value }
    SortingField.TopGainers -> sortedByDescendingNullLast { it.diff }
    SortingField.TopLosers -> sortedByNullLast { it.diff }
}

@Parcelize
enum class SortingField(@StringRes val titleResId: Int): WithTranslatableTitle, Parcelable {
    HighestCap(R.string.Market_Field_HighestCap), LowestCap(R.string.Market_Field_LowestCap),
    HighestVolume(R.string.Market_Field_HighestVolume), LowestVolume(R.string.Market_Field_LowestVolume),
    TopGainers(R.string.RateList_TopGainers), TopLosers(R.string.RateList_TopLosers);

    override val title: TranslatableString
        get() = TranslatableString.ResString(titleResId)
}

@Parcelize
enum class MarketField(@StringRes val titleResId: Int): WithTranslatableTitle, Parcelable {
    MarketCap(R.string.Market_Field_MarketCap),
    Volume(R.string.Market_Field_Volume),
    PriceDiff(R.string.Market_Field_PriceDiff);

    fun next() = values()[if (ordinal == values().size - 1) 0 else ordinal + 1]

    override val title: TranslatableString
        get() = TranslatableString.ResString(titleResId)

    companion object {
        val map = values().associateBy(MarketField::ordinal)
        fun fromIndex(id: Int): MarketField? = map[id]
    }
}

@Parcelize
enum class TopMarket(val value: Int): WithTranslatableTitle, Parcelable {
    Top250(250), Top500(500), Top1000(1000);

    fun next() = values()[if (ordinal == values().size - 1) 0 else ordinal + 1]

    override val title: TranslatableString
        get() = TranslatableString.PlainString(value.toString())
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
    val color = when (this) {
        is Score.Rating -> {
            when (rating.toUpperCase(Locale.ENGLISH)) {
                "A" -> R.color.jacob
                "B" -> R.color.issyk_blue
                "C" -> R.color.grey
                else -> R.color.light_grey
            }
        }
        is Score.Rank -> {
            R.color.jeremy
        }
    }

    return context.getColor(color)
}

data class MarketViewItem(
    val score: Score?,
    val fullCoin: FullCoin,
    val rate: String,
    val diff: BigDecimal?,
    val marketDataValue: MarketDataValue
) {
    val coinUid: String
        get() = fullCoin.coin.uid

    val coinCode: String
        get() = fullCoin.coin.code

    val coinName: String
        get() = fullCoin.coin.name

    val iconUrl: String
        get() = fullCoin.coin.iconUrl

    val iconPlaceHolder: Int
        get() = fullCoin.iconPlaceholder

    sealed class MarketDataValue {
        class MarketCap(val value: String) : MarketDataValue()
        class Volume(val value: String) : MarketDataValue()
        class Diff(val value: BigDecimal?) : MarketDataValue()
    }

    fun areItemsTheSame(other: MarketViewItem): Boolean {
        return fullCoin.coin == other.fullCoin.coin
    }

    fun areContentsTheSame(other: MarketViewItem): Boolean {
        return this == other
    }

    companion object {
        fun create(marketItem: MarketItem, marketField: MarketField): MarketViewItem {
            val formattedRate = App.numberFormatter.formatFiat(marketItem.rate.value, marketItem.rate.currency.symbol, 0, 6)

            val marketDataValue = when (marketField) {
                MarketField.MarketCap -> {
                    val marketCapFormatted = marketItem.marketCap?.let { marketCap ->
                        val (shortenValue, suffix) = App.numberFormatter.shortenValue(marketCap.value)
                        App.numberFormatter.formatFiat(shortenValue, marketCap.currency.symbol, 0, 2) + " $suffix"
                    }

                    MarketDataValue.MarketCap(marketCapFormatted ?: Translator.getString(R.string.NotAvailable))
                }
                MarketField.Volume -> {
                    val (shortenValue, suffix) = App.numberFormatter.shortenValue(marketItem.volume.value)
                    val volumeFormatted = App.numberFormatter.formatFiat(shortenValue, marketItem.volume.currency.symbol, 0, 2) + " $suffix"

                    MarketDataValue.Volume(volumeFormatted)
                }
                MarketField.PriceDiff -> MarketDataValue.Diff(marketItem.diff)
            }

            return MarketViewItem(
                marketItem.score,
                marketItem.fullCoin,
                formattedRate,
                marketItem.diff,
                marketDataValue
            )
        }
    }
}

inline fun <T, R : Comparable<R>> Iterable<T>.sortedByDescendingNullLast(crossinline selector: (T) -> R?): List<T> {
    return sortedWith(compareBy(nullsFirst(), selector)).sortedByDescending(selector)
}

inline fun <T, R : Comparable<R>> Iterable<T>.sortedByNullLast(crossinline selector: (T) -> R?): List<T> {
    return sortedWith(compareBy(nullsLast(), selector))
}
