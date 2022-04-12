package io.horizontalsystems.bankwallet.modules.market.search

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.WithTranslatableTitle
import io.horizontalsystems.marketkit.models.CoinCategory
import io.horizontalsystems.marketkit.models.FullCoin
import java.math.BigDecimal
import javax.annotation.concurrent.Immutable

object MarketSearchModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = MarketSearchService(
                App.marketKit,
                App.marketFavoritesManager,
                App.currencyManager.baseCurrency
            )
            return MarketSearchViewModel(service) as T
        }
    }

    sealed class DataState {
        class Discovery(val discoveryItems: List<DiscoveryItem>) : DataState()
        class SearchResult(val coinItems: List<CoinItem>) : DataState()
    }

    sealed class DiscoveryItem {
        object TopCoins : DiscoveryItem()

        class Category(
            val coinCategory: CoinCategory,
            val marketData: CategoryMarketData? = null
        ) : DiscoveryItem()
    }

    @Immutable
    class CoinItem(val fullCoin: FullCoin, val favourited: Boolean)

    enum class TimePeriod(@StringRes val titleResId: Int): WithTranslatableTitle {
        TimePeriod_1D(R.string.Market_Search_TimePeriodShort_1D),
        TimePeriod_1W(R.string.Market_Search_TimePeriodShort_1W),
        TimePeriod_1M(R.string.Market_Search_TimePeriodShort_1M);

        override val title: TranslatableString
            get() = TranslatableString.ResString(titleResId)

    }

    data class CategoryMarketData(
        val marketCap: String? = null,
        val diff: BigDecimal? = null
    )

}
