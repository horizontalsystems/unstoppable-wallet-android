package cash.p.terminal.modules.swapxxx.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.modules.swapxxx.QuoteInfoRow
import cash.p.terminal.ui.compose.components.subhead2_grey
import cash.p.terminal.ui.compose.components.subhead2_leah
import java.math.BigDecimal

data class SwapDataFieldSlippage(val slippage: BigDecimal) : SwapDataField {
    @Composable
    override fun GetContent(navController: NavController) {
        QuoteInfoRow(
            title = {
                subhead2_grey(text = stringResource(R.string.Swap_Slippage))
            },
            value = {
                subhead2_leah(text = App.numberFormatter.format(slippage, 0, 2, suffix = "%"))
            }
        )
    }
}
