package cash.p.terminal.modules.multiswap.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.entities.Address
import cash.p.terminal.modules.multiswap.QuoteInfoRow
import cash.p.terminal.ui.compose.components.subhead2_grey
import cash.p.terminal.ui.compose.components.subhead2_leah

data class SwapDataFieldRecipient(val address: Address) : SwapDataField {
    @Composable
    override fun GetContent(navController: NavController) {
        QuoteInfoRow(
            title = {
                subhead2_grey(text = stringResource(R.string.Swap_Recipient))
            },
            value = {
                subhead2_leah(
                    text = address.hex,
                    textAlign = TextAlign.End
                )
            }
        )
    }
}
