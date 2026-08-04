package io.horizontalsystems.walletkit.modules.multiswap

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.modules.enteraddress.EnterAddressScreen
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.nav3.LocalResultEventBus
import io.horizontalsystems.marketkit.models.Token
import kotlinx.serialization.Serializable

/**
 * Mandatory recipient step for swaps whose tokenOut the active account can't hold
 * (e.g. a Monero-only account swapping XMR to TRX): the swapped funds can only be
 * delivered to an external wallet, so the user enters it before confirmation.
 */
@Serializable
data class SwapRecipientPage(val input: Input) : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val resultEventBus = LocalResultEventBus.current
        EnterAddressScreen(
            navigation = navigation,
            token = input.token,
            title = stringResource(R.string.SwapSettings_RecipientAddressTitle),
            buttonTitle = stringResource(
                R.string.Swap_RecipientAddress_EnterAddress,
                input.token.blockchain.name
            ),
            allowNull = false,
            initialAddress = input.initialAddress,
            description = stringResource(R.string.Swap_RecipientAddress_Description),
            allowOwnAddress = true,
        ) { address, _ ->
            address?.let {
                resultEventBus.sendResult(Result(it))
                navigation.removeLastOrNull()
            }
        }
    }

    @Serializable
    data class Input(val token: Token, val initialAddress: String? = null)

    data class Result(val address: Address)
}
