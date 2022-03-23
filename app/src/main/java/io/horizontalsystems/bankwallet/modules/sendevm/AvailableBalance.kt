package io.horizontalsystems.bankwallet.modules.sendevm

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AdditionalDataCell2
import io.horizontalsystems.marketkit.models.Coin
import java.math.BigDecimal

@Composable
fun AvailableBalance(
    coin: Coin,
    coinDecimal: Int,
    fiatDecimal: Int,
    availableBalance: BigDecimal,
    amountInputMode: AmountInputModule.InputMode
) {
    val viewModel = viewModel<AvailableBalanceViewModel>(
        factory = AvailableBalanceModule.Factory(
            coin,
            coinDecimal,
            fiatDecimal
        )
    )
    val formatted = viewModel.formatted

    LaunchedEffect(availableBalance, amountInputMode) {
        viewModel.availableBalance = availableBalance
        viewModel.amountInputMode = amountInputMode
        viewModel.refreshFormatted()
    }

    AdditionalDataCell2 {
        Text(
            text = stringResource(R.string.Send_DialogAvailableBalance),
            style = ComposeAppTheme.typography.subhead2,
            color = ComposeAppTheme.colors.grey
        )

        Spacer(modifier = Modifier.weight(1f))

        if (formatted != null) {
            Text(
                text = formatted,
                style = ComposeAppTheme.typography.subhead2,
                color = ComposeAppTheme.colors.leah
            )
        } else {
            // TODO("Circle progress")
        }
    }
}