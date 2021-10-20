package io.horizontalsystems.bankwallet.modules.market.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.market.MarketField
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.TopMarket
import io.horizontalsystems.bankwallet.ui.compose.Select

object MarketCategoryModule {

    class Factory(
        private val categoryUid: String,
        private val categoryName: String,
        private val categoryDescription: String,
        private val categoryImageUrl: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val marketCategoryRepository = MarketCategoryRepository(App.marketKit)
            val service = MarketCategoryService(
                marketCategoryRepository,
                App.currencyManager,
                categoryUid,
                defaultTopMarket,
                defaultSortingField
            )
            return MarketCategoryViewModel(service, categoryName, categoryDescription, categoryImageUrl) as T
        }

        companion object {
            val defaultSortingField = SortingField.HighestCap
            val defaultTopMarket = TopMarket.Top250
        }
    }

    data class Menu(
        val sortingFieldSelect: Select<SortingField>,
        val marketFieldSelect: Select<MarketField>
    )

}
