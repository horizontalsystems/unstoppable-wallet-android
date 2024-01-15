package io.horizontalsystems.bankwallet.modules.market.favorites

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.market.MarketViewItem
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.WithTranslatableTitle
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import javax.annotation.concurrent.Immutable

object MarketFavoritesModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val repository = MarketFavoritesRepository(App.marketKit, App.marketFavoritesManager)
            val menuService = MarketFavoritesMenuService(App.localStorage, App.marketWidgetManager)
            val service = MarketFavoritesService(repository, menuService, App.currencyManager, App.backgroundManager)
            return MarketFavoritesViewModel(service) as T
        }
    }

    @Immutable
    data class ViewItem(
        val sortingDescending: Boolean,
        val periodSelect: Select<Period>,
        val marketItems: List<MarketViewItem>
    )

    @Parcelize
    enum class Period(val titleResId: Int) : WithTranslatableTitle, Parcelable {
        OneDay(R.string.CoinPage_TimeDuration_Day),
        SevenDay(R.string.CoinPage_TimeDuration_Week),
        ThirtyDay(R.string.CoinPage_TimeDuration_Month);

        @IgnoredOnParcel
        override val title = TranslatableString.ResString(titleResId)
    }
}
