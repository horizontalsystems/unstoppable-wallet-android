package io.horizontalsystems.bankwallet.modules.contacts

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.contacts.model.ContactAddress
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import kotlinx.serialization.Serializable

@Serializable
data class ContactsRouterScreen(val mode: Mode = Mode.Full) : HSScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        val screen = when (mode) {
            is Mode.AddAddressToExistingContact -> {
                val addAddress = App.Companion.marketKit.blockchain(mode.blockchainType.uid)?.let { blockchain ->
                    ContactAddress(blockchain, mode.address)
                }

                ContactsScreen(mode, addAddress)
            }
            is Mode.AddAddressToNewContact -> {
                val addAddress = App.Companion.marketKit.blockchain(mode.blockchainType.uid)?.let { blockchain ->
                    ContactAddress(blockchain, mode.address)
                }
                ContactScreen(mode, addAddress = addAddress)
            }
            Mode.Full -> {
                ContactsScreen(mode, null)
            }
        }

        screen.GetContent(backStack, resultBus)
    }
}