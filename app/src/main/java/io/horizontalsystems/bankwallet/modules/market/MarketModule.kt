package io.horizontalsystems.bankwallet.modules.market

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.market.advancedsearch.TimePeriod
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.WithTranslatableTitle
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.MarketInfo
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

object MarketModule {

    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val service = MarketService(App.marketStorage, App.localStorage)
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

    data class Header(
        val title: String,
        val description: String,
        val icon: ImageSource,
    )

    sealed class ViewItemState {
        class Error(val error: String) : ViewItemState()
        class Data(val items: List<MarketViewItem>) : ViewItemState()
    }

}

data class MarketItem(
    val fullCoin: FullCoin,
    val volume: CurrencyValue,
    val rate: CurrencyValue,
    val diff: BigDecimal?,
    val marketCap: CurrencyValue
) {
    companion object {
        fun createFromCoinMarket(
            marketInfo: MarketInfo,
            currency: Currency,
            pricePeriod: TimePeriod = TimePeriod.TimePeriod_1D
        ): MarketItem {
            return MarketItem(
                marketInfo.fullCoin,
                CurrencyValue(currency, marketInfo.totalVolume ?: BigDecimal.ZERO),
                CurrencyValue(currency, marketInfo.price ?: BigDecimal.ZERO),
                marketInfo.priceChangeValue(pricePeriod),
                CurrencyValue(currency, marketInfo.marketCap ?: BigDecimal.ZERO)
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
enum class SortingField(@StringRes val titleResId: Int) : WithTranslatableTitle, Parcelable {
    HighestCap(R.string.Market_Field_HighestCap), LowestCap(R.string.Market_Field_LowestCap),
    HighestVolume(R.string.Market_Field_HighestVolume), LowestVolume(R.string.Market_Field_LowestVolume),
    TopGainers(R.string.RateList_TopGainers), TopLosers(R.string.RateList_TopLosers);

    override val title: TranslatableString
        get() = TranslatableString.ResString(titleResId)

    companion object {
        val map = values().associateBy(SortingField::name)
        fun fromString(type: String?): SortingField? = map[type]
    }
}

@Parcelize
enum class MarketField(@StringRes val titleResId: Int) : WithTranslatableTitle, Parcelable {
    PriceDiff(R.string.Market_Field_PriceDiff),
    MarketCap(R.string.Market_Field_MarketCap),
    Volume(R.string.Market_Field_Volume);

    fun next() = values()[if (ordinal == values().size - 1) 0 else ordinal + 1]

    override val title: TranslatableString
        get() = TranslatableString.ResString(titleResId)

    companion object {
        val map = values().associateBy(MarketField::name)
        fun fromString(type: String?): MarketField? = map[type]
    }
}

@Parcelize
enum class TopMarket(val value: Int) : WithTranslatableTitle, Parcelable {
    Top250(250), Top500(500), Top1000(1000);

    fun next() = values()[if (ordinal == values().size - 1) 0 else ordinal + 1]

    override val title: TranslatableString
        get() = TranslatableString.PlainString(value.toString())
}

sealed class ImageSource {
    class Local(@DrawableRes val resId: Int) : ImageSource()
    class Remote(val url: String, @DrawableRes val placeholder: Int = R.drawable.ic_placeholder) : ImageSource()

    @ExperimentalCoilApi
    @Composable
    fun painter(): Painter = when (this) {
        is Local -> painterResource(resId)
        is Remote -> rememberImagePainter(url, builder = { error(placeholder) })
    }
}

sealed class Value {
    class Percent(val percent: BigDecimal) : Value()
    class Currency(val currencyValue: CurrencyValue) : Value()

    fun raw() = when (this) {
        is Currency -> currencyValue.value
        is Percent -> percent
    }
}

sealed class MarketDataValue {
    class MarketCap(val value: String) : MarketDataValue()
    class Volume(val value: String) : MarketDataValue()
    class Diff(val value: BigDecimal?) : MarketDataValue()
    class DiffNew(val value: Value) : MarketDataValue()
}

@Immutable
data class MarketViewItem(
    val fullCoin: FullCoin,
    val coinRate: String,
    val marketDataValue: MarketDataValue,
    val rank: String?,
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

    fun areItemsTheSame(other: MarketViewItem): Boolean {
        return fullCoin.coin == other.fullCoin.coin
    }

    fun areContentsTheSame(other: MarketViewItem): Boolean {
        return this == other
    }

    companion object {
        fun create(
            marketItem: MarketItem,
            marketField: MarketField
        ): MarketViewItem {
            val marketDataValue = when (marketField) {
                MarketField.MarketCap -> {
                    val (shortenValue, suffix) = App.numberFormatter.shortenValue(
                        marketItem.marketCap.value
                    )
                    val marketCapFormatted = App.numberFormatter.formatFiat(
                        shortenValue,
                        marketItem.marketCap.currency.symbol,
                        0,
                        2
                    ) + " $suffix"

                    MarketDataValue.MarketCap(marketCapFormatted)
                }
                MarketField.Volume -> {
                    val (shortenValue, suffix) = App.numberFormatter.shortenValue(
                        marketItem.volume.value
                    )
                    val volumeFormatted = App.numberFormatter.formatFiat(
                        shortenValue,
                        marketItem.volume.currency.symbol,
                        0,
                        2
                    ) + " $suffix"

                    MarketDataValue.Volume(volumeFormatted)
                }
                MarketField.PriceDiff -> {
                    MarketDataValue.Diff(marketItem.diff)
                }
            }
            return MarketViewItem(
                marketItem.fullCoin,
                App.numberFormatter.formatFiat(
                    marketItem.rate.value,
                    marketItem.rate.currency.symbol,
                    0,
                    6
                ),
                marketDataValue,
                marketItem.fullCoin.coin.marketCapRank?.toString()
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

fun MarketInfo.priceChangeValue(period: TimePeriod) = when (period) {
    TimePeriod.TimePeriod_1D -> priceChange24h
    TimePeriod.TimePeriod_1W -> priceChange7d
    TimePeriod.TimePeriod_2W -> priceChange14d
    TimePeriod.TimePeriod_1M -> priceChange30d
    TimePeriod.TimePeriod_6M -> priceChange200d
    TimePeriod.TimePeriod_1Y -> priceChange1y
}