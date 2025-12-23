package io.horizontalsystems.bankwallet.modules.multiswap.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.multiswap.QuoteInfoRow
import io.horizontalsystems.bankwallet.modules.multiswap.SwapInfoDialog
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import java.math.BigDecimal

data class DataFieldSlippage(val slippage: BigDecimal) : DataField {
    @Composable
    override fun GetContent(navController: NavController) {
        val color = if (slippage > BigDecimal(50)) {
            ComposeAppTheme.colors.lucian
        } else if (slippage > BigDecimal(5)) {
            ComposeAppTheme.colors.jacob
        } else {
            ComposeAppTheme.colors.leah
        }

        val infoTitle = stringResource(R.string.Swap_Slippage)
        val infoText = stringResource(R.string.Swap_SlippageDescription)

        QuoteInfoRow(
            title = stringResource(R.string.Swap_Slippage),
            value = App.numberFormatter.format(slippage, 0, 2, suffix = "%").hs(color = color),
            onInfoClick = {
                navController.slideFromBottom(
                    R.id.swapInfoDialog,
                    SwapInfoDialog.Input(infoTitle, infoText)
                )
            }
        )
    }
}
