package cash.p.terminal.modules.multiswap.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.modules.multiswap.QuoteInfoRow
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.components.subhead2_leah

data class DataFieldNonce(val nonce: Long) : DataField {
    @Composable
    override fun GetContent(navController: NavController, borderTop: Boolean) {
        QuoteInfoRow(
            borderTop = borderTop,
            title = {
                subhead2_grey(text = stringResource(R.string.Send_Confirmation_Nonce))
            },
            value = {
                subhead2_leah(text = nonce.toString())
            }
        )
    }
}
