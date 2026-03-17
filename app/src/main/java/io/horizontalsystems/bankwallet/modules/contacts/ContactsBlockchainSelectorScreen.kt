package io.horizontalsystems.bankwallet.modules.contacts

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.modules.contacts.screen.BlockchainSelectorScreen
import io.horizontalsystems.bankwallet.modules.contacts.viewmodel.AddressViewModel
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import kotlinx.serialization.Serializable

@Serializable
data object ContactsBlockchainSelectorScreen : HSScreen(
    parentScreenClass = ContactsAddressScreen::class
) {
    @Composable
    override fun GetContent(backStack: NavBackStack<HSScreen>) {
        val viewModel = viewModel<AddressViewModel>()
        BlockchainSelectorScreen(
            blockchains = viewModel.uiState.availableBlockchains,
            selectedBlockchain = viewModel.uiState.blockchain,
            onSelectBlockchain = { blockchain ->
                viewModel.onEnterBlockchain(blockchain)

                backStack.removeLastOrNull()
            },
            onNavigateToBack = {
                backStack.removeLastOrNull()
            }
        )
    }
}