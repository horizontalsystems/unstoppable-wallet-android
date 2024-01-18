package io.horizontalsystems.bankwallet.modules.swapxxx.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.swapxxx.QuoteInfoRow
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah

data class SwapFeeField(val feeAmountData: SendModule.AmountData) : SwapDataField {
    @Composable
    override fun GetContent() {
        QuoteInfoRow(
            title = {
                subhead2_grey(text = stringResource(R.string.Swap_Fee))
            },
            value = {
                val text =
                    feeAmountData.secondary?.getFormatted() ?: feeAmountData.primary.getFormatted()
                subhead2_leah(text = text)
            }
        )
    }
}
