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
import io.horizontalsystems.bankwallet.modules.settings.appearance.PriceChangeInterval
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.WithTranslatableTitle
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.MarketGlobal
import io.horizontalsystems.marketkit.models.MarketInfo
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

object MarketModule {

    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MarketViewModel(
                App.marketStorage,
                App.marketKit,
                App.currencyManager,
                App.localStorage
            ) as T
        }

    }

    data class UiState(
        val selectedTab: Tab,
        val marketGlobal: MarketGlobal?,
        val currency: Currency
    )

    enum class Tab(@StringRes val titleResId: Int) {
        Coins(R.string.Market_Tab_Coins),
        Watchlist(R.string.Market_Tab_Watchlist),
        Sectors(R.string.Market_Tab_Sectors),
        Posts(R.string.Market_Tab_Posts),
        Platform(R.string.Market_Tab_Platform),
        Pairs(R.string.Market_Tab_Pairs);

        companion object {
            private val map = entries.associateBy(Tab::name)

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
            period: TimePeriod = TimePeriod.TimePeriod_1D
        ): MarketItem {
            return MarketItem(
                fullCoin = marketInfo.fullCoin,
                volume = CurrencyValue(currency, marketInfo.totalVolume ?: BigDecimal.ZERO),
                rate = CurrencyValue(currency, marketInfo.price ?: BigDecimal.ZERO),
                diff = marketInfo.priceChangeValue(period),
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
enum class TopMarket(val value: Int, val titleResId: Int) : WithTranslatableTitle, Parcelable {
    Top100(100, R.string.Market_Top_100),
    Top200(200, R.string.Market_Top_200),
    Top300(300, R.string.Market_Top_300),
    Top500(500, R.string.Market_Top_500);

    fun next() = entries[if (ordinal == entries.size - 1) 0 else ordinal + 1]

    override val title: TranslatableString
        get() = TranslatableString.ResString(titleResId)
}

sealed class ImageSource {
    class Local(@DrawableRes val resId: Int) : ImageSource()
    class Remote(
        val url: String,
        @DrawableRes
        val placeholder: Int = R.drawable.ic_placeholder,
        val alternativeUrl: String? = null
    ) : ImageSource()

    @Composable
    fun painter(): Painter = when (this) {
        is Local -> painterResource(resId)
        is Remote -> rememberAsyncImagePainter(
            model = url,
            error = alternativeUrl?.let {
                rememberAsyncImagePainter(
                    model = alternativeUrl,
                    error = painterResource(placeholder)
                )
            } ?: painterResource(placeholder)
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
    TimePeriod.TimePeriod_1D -> {
        when(App.priceManager.priceChangeInterval) {
            PriceChangeInterval.LAST_24H ->  priceChange24h
            PriceChangeInterval.FROM_UTC_MIDNIGHT -> priceChange1d
        }
    }
    TimePeriod.TimePeriod_1W -> priceChange7d
    TimePeriod.TimePeriod_2W -> priceChange14d
    TimePeriod.TimePeriod_1M -> priceChange30d
    TimePeriod.TimePeriod_3M -> priceChange90d
    TimePeriod.TimePeriod_6M -> priceChange200d
    TimePeriod.TimePeriod_1Y -> priceChange1y
}

@Parcelize
enum class TimeDuration(val titleResId: Int) : WithTranslatableTitle, Parcelable {
    OneDay(R.string.Market_Filter_TimePeriod_1D),
    SevenDay(R.string.Market_Filter_TimePeriod_1W),
    ThirtyDay(R.string.Market_Filter_TimePeriod_1M),
    ThreeMonths(R.string.Market_Filter_TimePeriod_3M);

    @IgnoredOnParcel
    override val title = TranslatableString.ResString(titleResId)
}
