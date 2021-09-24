package io.horizontalsystems.bankwallet.modules.blockchainsettings

import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetSelectorViewItem
import io.horizontalsystems.marketkit.models.PlatformCoin

object BlockchainSettingsModule {

    data class Config(
        val platformCoin: PlatformCoin,
        val title: String,
        val subtitle: String,
        val selectedIndexes: List<Int>,
        val viewItems: List<BottomSheetSelectorViewItem>,
        val description: String
    )

}
