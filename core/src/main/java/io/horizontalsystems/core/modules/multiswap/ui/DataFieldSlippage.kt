package io.horizontalsystems.core.modules.multiswap.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.core.R
import io.horizontalsystems.core.core.App
import io.horizontalsystems.core.modules.multiswap.QuoteInfoRow
import io.horizontalsystems.core.modules.multiswap.SwapInfoSheet
import io.horizontalsystems.core.modules.multiswap.providers.IMultiSwapProvider
import io.horizontalsystems.core.modules.nav3.HSNavigation
import io.horizontalsystems.core.ui.compose.ComposeAppTheme
import io.horizontalsystems.core.uiv3.components.cell.hs
import java.math.BigDecimal

data class DataFieldSlippage(val slippage: BigDecimal) : DataField {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
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
                navigation.slideFromBottom(
                    SwapInfoSheet(SwapInfoSheet.Input(infoTitle, infoText))
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
