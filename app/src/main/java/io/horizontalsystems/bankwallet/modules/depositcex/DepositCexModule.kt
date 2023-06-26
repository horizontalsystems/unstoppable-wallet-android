package io.horizontalsystems.bankwallet.modules.depositcex

import io.horizontalsystems.bankwallet.core.providers.CexAsset
import io.horizontalsystems.bankwallet.modules.market.ImageSource

object DepositCexModule {

    data class CexCoinViewItem(
        val title: String,
        val subtitle: String,
        val coinIconUrl: String?,
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
