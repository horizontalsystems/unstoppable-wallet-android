package cash.p.terminal.modules.swapxxx.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import cash.p.terminal.R
import cash.p.terminal.modules.send.SendModule
import cash.p.terminal.modules.swapxxx.QuoteInfoRow
import cash.p.terminal.ui.compose.components.subhead2_grey
import cash.p.terminal.ui.compose.components.subhead2_leah

data class SwapDataFieldFee(val feeAmountData: SendModule.AmountData) : SwapDataField {
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
