package io.horizontalsystems.bankwallet.modules.multiswap.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.multiswap.SwapInfoRow2
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import java.math.BigDecimal

data class DataFieldSlippage(val slippage: BigDecimal) : DataField {
    @Composable
    override fun GetContent(navController: NavController, borderTop: Boolean) {
        val color = if (slippage > BigDecimal(50)) {
            ComposeAppTheme.colors.lucian
        } else if (slippage > BigDecimal(5)) {
            ComposeAppTheme.colors.jacob
        } else {
            ComposeAppTheme.colors.leah
        }

        SwapInfoRow2(
            title = stringResource(id = R.string.Swap_Slippage).hs,
            value = App.numberFormatter.format(slippage, 0, 2, suffix = "%").hs(color = color)
        )
    }
}
