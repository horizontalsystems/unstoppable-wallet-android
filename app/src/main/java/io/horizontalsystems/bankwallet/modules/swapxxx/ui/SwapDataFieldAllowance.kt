package cash.p.terminal.modules.swapxxx.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import cash.p.terminal.R
import cash.p.terminal.entities.CoinValue
import cash.p.terminal.modules.swapxxx.QuoteInfoRow
import cash.p.terminal.ui.compose.components.subhead2_grey
import cash.p.terminal.ui.compose.components.subhead2_leah
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

data class SwapDataFieldAllowance(val allowance: BigDecimal, val token: Token) : SwapDataField {
    @Composable
    override fun GetContent() {
        QuoteInfoRow(
            title = {
                subhead2_grey(text = stringResource(R.string.Swap_Allowance))
            },
            value = {
                subhead2_leah(text = CoinValue(token, allowance).getFormattedFull())
            }
        )
    }
}
