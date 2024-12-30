package cash.p.terminal.modules.depositcex

import cash.p.terminal.core.providers.CexAsset
import cash.p.terminal.ui_compose.components.ImageSource

object DepositCexModule {

    data class CexCoinViewItem(
        val title: String,
        val subtitle: String,
        val coinIconUrl: String?,
        val alternativeCoinUrl: String?,
        val coinIconPlaceholder: Int,
        val cexAsset: CexAsset,
        val depositEnabled: Boolean,
        val withdrawEnabled: Boolean,
    )

    data class NetworkViewItem(
        val title: String,
        val imageSource: ImageSource,
    )

}
