package io.horizontalsystems.bankwallet.modules.market.earn.vault

import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.entities.ViewState

class VaultViewModel(
    viewItem: VaultModule.VaultViewItem
) : ViewModelUiState<VaultModule.UiState>() {

    private var viewState: ViewState = ViewState.Loading
    private var vaultViewItem: VaultModule.VaultViewItem = viewItem
    private var isRefreshing = false

    override fun createState(): VaultModule.UiState {
        return VaultModule.UiState(
            isRefreshing = isRefreshing,
            viewState = viewState,
            vaultViewItem = vaultViewItem
        )
    }
}

object VaultModule {

    data class VaultViewItem(
        val rank: String,
        val address: String,
        val name: String,
        val tvl: String,
        val chain: String,
        val url: String?,
        val holders: String?,
        val assetSymbol: String,
        val protocolName: String,
        val assetLogo: String?,
    )

    data class UiState(
        val isRefreshing: Boolean,
        val viewState: ViewState,
        val vaultViewItem: VaultViewItem,
    )
}