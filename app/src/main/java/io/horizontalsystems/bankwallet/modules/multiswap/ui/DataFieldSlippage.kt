package io.horizontalsystems.bankwallet.modules.multiswap.ui

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.multiswap.QuoteInfoRow
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
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
                val color = if (slippage > BigDecimal(50)) {
                    ComposeAppTheme.colors.lucian
                } else if (slippage > BigDecimal(5)) {
                    ComposeAppTheme.colors.jacob
                } else {
                    ComposeAppTheme.colors.leah
                }

                Text(
                    text = App.numberFormatter.format(slippage, 0, 2, suffix = "%"),
                    style = ComposeAppTheme.typography.subheadR,
                    color = color,
                )
            }
        )
    }
}
