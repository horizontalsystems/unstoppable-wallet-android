package io.horizontalsystems.bankwallet.modules.btcblockchainsettings

object BtcBlockchainSettingsModule {

    data class ViewItem(
        val id: String,
        val title: String,
        val subtitle: String,
        val selected: Boolean,
        val icon: BlockchainSettingsIcon
    )

    sealed class BlockchainSettingsIcon {
        data class ApiIcon(val resId: Int): BlockchainSettingsIcon()
        data class BlockchainIcon(val url: String): BlockchainSettingsIcon()
    }
}