package io.horizontalsystems.bankwallet.modules.sendevm

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AdditionalDataCell2

@Composable
fun AvailableBalance(availableBalanceViewModel: SendAvailableBalanceViewModel) {
    val availableBalanceViewState by availableBalanceViewModel.viewStateLiveData.observeAsState()

    AdditionalDataCell2 {
        Text(
            text = stringResource(R.string.Send_DialogAvailableBalance),
            style = ComposeAppTheme.typography.subhead2,
            color = ComposeAppTheme.colors.grey
        )

        Spacer(modifier = Modifier.weight(1f))

        when (val tmpState = availableBalanceViewState) {
            is SendAvailableBalanceViewModel.ViewState.Loaded -> {
                Text(
                    text = tmpState.value ?: "",
                    style = ComposeAppTheme.typography.subhead2,
                    color = ComposeAppTheme.colors.leah
                )
            }
            is SendAvailableBalanceViewModel.ViewState.Loading -> {
//                TODO("Circle progress")
            }
            null -> Unit
        }
    }
}