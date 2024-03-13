package io.horizontalsystems.bankwallet.modules.swapxxx.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.modules.swapxxx.QuoteInfoRow
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah
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
