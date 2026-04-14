package com.quantum.wallet.bankwallet.modules.multiswap.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.slideFromBottom
import com.quantum.wallet.bankwallet.modules.multiswap.QuoteInfoRow
import com.quantum.wallet.bankwallet.modules.multiswap.SwapInfoDialog
import com.quantum.wallet.bankwallet.modules.multiswap.providers.IMultiSwapProvider
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.uiv3.components.cell.hs
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

    companion object {
        fun getField(slippage: BigDecimal?) = if (
            slippage != null &&
            slippage != IMultiSwapProvider.DEFAULT_SLIPPAGE
        ) {
            DataFieldSlippage(slippage)
        } else {
            null
        }
    }
}
