package io.horizontalsystems.walletkit.modules.multiswap

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.modules.enteraddress.EnterAddressScreen
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.marketkit.models.Token
import kotlinx.serialization.Serializable

/**
 * Mandatory recipient step for swaps whose tokenOut the active account can't hold
 * (e.g. a Monero-only account swapping XMR to TRX): the swapped funds can only be
 * delivered to an external wallet, so the user enters it before confirmation.
 *
 * Opens the confirmation itself and stays in the back stack, so going back from
 * confirmation returns here with the entered address intact.
 */
@Serializable
data class SwapRecipientPage(val input: Input) : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val swapViewModel = navigation.viewModelForScreen<SwapViewModel>(input.parentScreenContentKey)

        val openConfirmation = navigation.slideFromRightForResult<SwapConfirmPage.Result>(
            { SwapConfirmPage(input.parentScreenContentKey) }
        ) { result ->
            if (result.success) {
                // the confirmation popped itself — drop this page too so the user
                // lands back on the swap screen rather than the address form
                navigation.removeLastOrNull()
                if (input.closeAfterSwap) {
                    navigation.removeLastOrNull()
                } else {
                    swapViewModel.onEnterAmount(null)
                }
            }
        }

        EnterAddressScreen(
            navigation = navigation,
            token = input.token,
            title = stringResource(R.string.SwapSettings_RecipientAddressTitle),
            buttonTitle = stringResource(R.string.Button_Next),
            buttonTitleEmpty = stringResource(
                R.string.Swap_RecipientAddress_EnterAddress,
                input.token.blockchain.name
            ),
            allowNull = false,
            initialAddress = input.initialAddress,
            description = stringResource(R.string.Swap_RecipientAddress_Description),
            allowOwnAddress = true,
            zcashTransparentOnly = input.zcashTransparentOnly,
        ) { address, _ ->
            address?.let {
                swapViewModel.setExternalRecipient(it)
                openConfirmation()
            }
        }
    }

    @Serializable
    data class Input(
        val token: Token,
        val parentScreenContentKey: String,
        val closeAfterSwap: Boolean,
        val initialAddress: String? = null,
        // the selected route delivers only to transparent Zcash addresses
        val zcashTransparentOnly: Boolean = false,
    )
}
