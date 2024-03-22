package io.horizontalsystems.bankwallet.modules.swapxxx.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.swapxxx.QuoteInfoRow
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah

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
