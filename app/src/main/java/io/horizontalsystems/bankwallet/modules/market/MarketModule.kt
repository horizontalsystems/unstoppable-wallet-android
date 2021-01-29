package io.horizontalsystems.bankwallet.modules.market

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.market.top.SortingField
import io.horizontalsystems.bankwallet.modules.market.top.MarketField

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
