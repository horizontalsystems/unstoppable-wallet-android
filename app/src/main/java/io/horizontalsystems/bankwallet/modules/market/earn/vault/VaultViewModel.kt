package io.horizontalsystems.bankwallet.modules.market.earn.vault

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.entities.ViewState

@HiltViewModel(assistedFactory = VaultViewModel.Factory::class)
class VaultViewModel @AssistedInject constructor(
    @Assisted input: VaultPage.Input,
) : ViewModelUiState<VaultModule.UiState>() {

    private var viewState: ViewState = ViewState.Loading
    private var vaultViewItem: VaultModule.VaultViewItem = VaultModule.VaultViewItem(
        rank = "#" + input.rank.toString(),
        address = input.address,
        name = input.name,
        tvl = input.tvl,
        chain = input.chain,
        url = input.url,
        holders = input.holders,
        assetSymbol = input.assetSymbol,
        protocolName = input.protocolName,
        assetLogo = input.assetLogo
    )
    private var isRefreshing = false

    override fun createState(): VaultModule.UiState {
        return VaultModule.UiState(
            isRefreshing = isRefreshing,
            viewState = viewState,
            vaultViewItem = vaultViewItem
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(input: VaultPage.Input): VaultViewModel
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
