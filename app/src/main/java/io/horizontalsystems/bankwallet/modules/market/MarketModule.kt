package io.horizontalsystems.bankwallet.modules.market

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.market.filters.TimePeriod
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.WithTranslatableTitle
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.MarketInfo
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

object MarketModule {

    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
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
}

data class MarketItem(
    val fullCoin: FullCoin,
    val volume: CurrencyValue,
    val rate: CurrencyValue,
    val diff: BigDecimal?,
    val marketCap: CurrencyValue,
    val rank: Int?
) {
    companion object {
        fun createFromCoinMarket(
            marketInfo: MarketInfo,
            currency: Currency,
            pricePeriod: TimePeriod = TimePeriod.TimePeriod_1D
        ): MarketItem {
            return MarketItem(
                fullCoin = marketInfo.fullCoin,
                volume = CurrencyValue(currency, marketInfo.totalVolume ?: BigDecimal.ZERO),
                rate = CurrencyValue(currency, marketInfo.price ?: BigDecimal.ZERO),
                diff = marketInfo.priceChangeValue(pricePeriod),
                marketCap = CurrencyValue(currency, marketInfo.marketCap ?: BigDecimal.ZERO),
                rank = marketInfo.marketCapRank
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
    Top100(100), Top200(200), Top300(300);

    fun next() = values()[if (ordinal == values().size - 1) 0 else ordinal + 1]

    override val title: TranslatableString
        get() = TranslatableString.PlainString(value.toString())
}

sealed class ImageSource {
    class Local(@DrawableRes val resId: Int) : ImageSource()
    class Remote(val url: String, @DrawableRes val placeholder: Int = R.drawable.ic_placeholder) : ImageSource()

    @Composable
    fun painter(): Painter = when (this) {
        is Local -> painterResource(resId)
        is Remote -> rememberAsyncImagePainter(
            model = url,
            error = painterResource(placeholder)
        )
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

@Parcelize
enum class TimeDuration(val titleResId: Int) : WithTranslatableTitle, Parcelable {
    OneDay(R.string.CoinPage_TimeDuration_Day),
    SevenDay(R.string.CoinPage_TimeDuration_Week),
    ThirtyDay(R.string.CoinPage_TimeDuration_Month),
    ThreeMonths(R.string.CoinPage_TimeDuration_Month3);

    @IgnoredOnParcel
    override val title = TranslatableString.ResString(titleResId)
}
