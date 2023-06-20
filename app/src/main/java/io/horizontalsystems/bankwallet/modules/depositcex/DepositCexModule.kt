package cash.p.terminal.modules.depositcex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.modules.market.ImageSource

object DepositCexModule {
    class Factory(private val coinId: String?) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DepositViewModel(coinId) as T
        }
    }

    data class CexCoinViewItem(
        val title: String,
        val subtitle: String,
        val coinIconUrl: String?,
        val coinIconPlaceholder: Int,
    )

    data class NetworkViewItem(
        val title: String,
        val imageSource: ImageSource,
    )

}
