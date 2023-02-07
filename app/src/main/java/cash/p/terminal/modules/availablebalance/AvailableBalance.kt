package cash.p.terminal.modules.availablebalance

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import cash.p.terminal.R
import cash.p.terminal.entities.CurrencyValue
import cash.p.terminal.modules.amount.AmountInputType
import cash.p.terminal.ui.compose.components.AdditionalDataCell2
import cash.p.terminal.ui.compose.components.HSCircularProgressIndicator
import cash.p.terminal.ui.compose.components.subhead2_grey
import cash.p.terminal.ui.compose.components.subhead2_leah
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

    AdditionalDataCell2 {
        subhead2_grey(text = stringResource(R.string.Send_DialogAvailableBalance))

        Spacer(modifier = Modifier.weight(1f))

        if (formatted != null) {
            subhead2_leah(text = formatted)
        } else {
            HSCircularProgressIndicator()
        }
    }
}