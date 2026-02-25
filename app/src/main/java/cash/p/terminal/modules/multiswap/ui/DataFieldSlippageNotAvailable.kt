package cash.p.terminal.modules.multiswap.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import cash.p.terminal.modules.fee.QuoteInfoRow
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.components.subhead2_leah
import cash.p.terminal.R

data object DataFieldSlippageNotAvailable : DataField {
    @Composable
    override fun GetContent(navController: NavController, borderTop: Boolean) {
        QuoteInfoRow(
            borderTop = borderTop,
            title = {
                subhead2_grey(text = stringResource(R.string.Swap_Slippage))
            },
            value = {
                subhead2_leah(text = stringResource(R.string.NotAvailable))
            }
        )
    }
}
