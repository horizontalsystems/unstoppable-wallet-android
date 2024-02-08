package io.horizontalsystems.bankwallet.modules.swapxxx.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.swapxxx.QuoteInfoRow
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah
import java.math.BigDecimal

data class SwapDataFieldSlippage(val slippage: BigDecimal) : SwapDataField {
    @Composable
    override fun GetContent() {
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
