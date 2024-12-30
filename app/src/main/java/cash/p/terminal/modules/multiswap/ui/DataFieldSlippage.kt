package cash.p.terminal.modules.multiswap.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.modules.multiswap.QuoteInfoRow
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.components.subhead2_leah
import java.math.BigDecimal

data class DataFieldSlippage(val slippage: BigDecimal) : DataField {
    @Composable
    override fun GetContent(navController: NavController, borderTop: Boolean) {
        QuoteInfoRow(
            borderTop = borderTop,
            title = {
                subhead2_grey(text = stringResource(R.string.Swap_Slippage))
            },
            value = {
                subhead2_leah(text = App.numberFormatter.format(slippage, 0, 2, suffix = "%"))
            }
        )
    }
}
