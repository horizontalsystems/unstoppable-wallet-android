package cash.p.terminal.modules.market.topplatforms

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.iconUrl
import cash.p.terminal.modules.market.MarketField
import cash.p.terminal.modules.market.SortingField
import cash.p.terminal.modules.market.TimeDuration
import cash.p.terminal.ui.compose.Select
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

object TopPlatformsModule {

    class Factory(private val timeDuration: TimeDuration?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val repository = TopPlatformsRepository(App.marketKit, App.currencyManager)
            val service = TopPlatformsService(repository, App.currencyManager)
            return TopPlatformsViewModel(service, timeDuration) as T
        }
    }

    data class Menu(
        val sortingFieldSelect: Select<SortingField>,
        val marketFieldSelect: Select<MarketField>
    )

}

@Parcelize
data class Platform(
    val uid: String,
    val name: String,
): Parcelable

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
