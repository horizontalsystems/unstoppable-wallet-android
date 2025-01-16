package io.horizontalsystems.bankwallet.modules.availablebalance

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.amount.AmountInputType
import io.horizontalsystems.bankwallet.ui.compose.components.HSCircularProgressIndicator
import io.horizontalsystems.bankwallet.ui.compose.components.caption_grey
import java.math.BigDecimal

@Composable
fun AvailableBalance(
    coinCode: String,
    coinDecimal: Int,
    fiatDecimal: Int,
    availableBalance: BigDecimal?,
    amountInputType: AmountInputType,
    rate: CurrencyValue?
) {
    val viewModel = viewModel<AvailableBalanceViewModel>(
        factory = AvailableBalanceModule.Factory(
            coinCode,
            coinDecimal,
            fiatDecimal
        )
    )
    val formatted = viewModel.formatted

    LaunchedEffect(availableBalance, amountInputType, rate) {
        viewModel.availableBalance = availableBalance
        viewModel.amountInputType = amountInputType
        viewModel.xRate = rate
        viewModel.refreshFormatted()
    }


    Row(
        modifier = Modifier.padding(horizontal = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        caption_grey(
            text = stringResource(R.string.Send_DialogAvailableBalance),
            modifier = Modifier.weight(1f)
        )

        if (formatted != null) {
            caption_grey(text = formatted)
        } else {
            HSCircularProgressIndicator()
        }
    }
}