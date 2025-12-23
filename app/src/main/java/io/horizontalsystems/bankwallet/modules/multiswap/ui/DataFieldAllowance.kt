package io.horizontalsystems.bankwallet.modules.multiswap.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.modules.multiswap.QuoteInfoRow
import io.horizontalsystems.bankwallet.modules.multiswap.SwapInfoDialog
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

data class DataFieldAllowance(val allowance: BigDecimal, val token: Token) : DataField {
    @Composable
    override fun GetContent(navController: NavController, borderTop: Boolean) {
        val infoTitle = stringResource(id = R.string.SwapInfo_AllowanceTitle)
        val infoText = stringResource(id = R.string.SwapInfo_AllowanceDescription)

        QuoteInfoRow(
            title = stringResource(R.string.Swap_Allowance),
            value = CoinValue(token, allowance)
                .getFormattedFull()
                .hs(color = ComposeAppTheme.colors.lucian),
            onInfoClick = {
                navController.slideFromBottom(
                    R.id.swapInfoDialog,
                    SwapInfoDialog.Input(infoTitle, infoText)
                )
            }
        )
    }
}
