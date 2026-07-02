package io.horizontalsystems.core.modules.market.topplatforms

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.core.R
import io.horizontalsystems.core.core.App
import io.horizontalsystems.core.core.iconUrl
import io.horizontalsystems.core.entities.ViewState
import io.horizontalsystems.core.modules.market.MarketField
import io.horizontalsystems.core.modules.market.SortingField
import io.horizontalsystems.core.modules.market.TimeDuration
import io.horizontalsystems.core.ui.compose.Select
import kotlinx.serialization.Serializable
import java.math.BigDecimal

object TopPlatformsModule {

    class Factory(private val timeDuration: TimeDuration?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val repository = TopPlatformsRepository(App.marketKit)
            return TopPlatformsViewModel(repository, App.currencyManager, timeDuration) as T
        }
    }

    data class Menu(
        val sortingFieldSelect: Select<SortingField>,
        val marketFieldSelect: Select<MarketField>
    )

    data class UiState(
        val sortingField: SortingField,
        val timePeriod: TimeDuration,
        val viewItems: List<TopPlatformViewItem>,
        val viewState: ViewState,
        val isRefreshing: Boolean
    )

}

@Serializable
data class Platform(
    val uid: String,
    val name: String,
)

data class TopPlatformItem(
    val platform: Platform,
    val rank: Int,
    val protocols: Int,
    val marketCap: BigDecimal,
    val rankDiff: Int?,
    val changeDiff: BigDecimal?
)

@Immutable
data class TopPlatformViewItem(
    val platform: Platform,
    val subtitle: String,
    val marketCap: String,
    val marketCapDiff: BigDecimal?,
    val rank: String?,
    val rankDiff: Int?,
) {


    val iconUrl: String
        get() = platform.iconUrl

    val iconPlaceHolder: Int
        get() = R.drawable.ic_platform_placeholder_24

}
