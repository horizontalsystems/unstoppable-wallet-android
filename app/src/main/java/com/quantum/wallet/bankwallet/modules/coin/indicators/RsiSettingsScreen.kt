package com.quantum.wallet.bankwallet.modules.coin.indicators

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.entities.DataState
import com.quantum.wallet.bankwallet.modules.chart.ChartIndicatorSetting
import com.quantum.wallet.bankwallet.modules.evmfee.ButtonsGroupWithShade
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.ui.compose.TranslatableString
import com.quantum.wallet.bankwallet.ui.compose.components.ButtonPrimaryYellow
import com.quantum.wallet.bankwallet.ui.compose.components.FormsInput
import com.quantum.wallet.bankwallet.ui.compose.components.HeaderText
import com.quantum.wallet.bankwallet.ui.compose.components.InfoText
import com.quantum.wallet.bankwallet.ui.compose.components.MenuItem
import com.quantum.wallet.bankwallet.ui.compose.components.VSpacer
import com.quantum.wallet.bankwallet.uiv3.components.HSScaffold

@Composable
fun RsiSettingsScreen(navController: NavController, indicatorSetting: ChartIndicatorSetting) {
    val viewModel = viewModel<RsiSettingViewModel>(
        factory = RsiSettingViewModel.Factory(indicatorSetting)
    )
    val uiState = viewModel.uiState

    if (uiState.finish) {
        LaunchedEffect(uiState.finish) {
            navController.popBackStack()
        }
    }

    HSScaffold(
        title = viewModel.name,
        onBack = navController::popBackStack,
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Reset),
                enabled = uiState.resetEnabled,
                tint = ComposeAppTheme.colors.jacob,
                onClick = {
                    viewModel.reset()
                }
            )
        )
    ) {
        Column {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                InfoText(
                    text = stringResource(R.string.CoinPage_RsiSettingsDescription)
                )
                VSpacer(12.dp)
                HeaderText(
                    text = stringResource(R.string.CoinPage_RsiLength).uppercase()
                )
                FormsInput(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    hint = viewModel.defaultPeriod ?: "",
                    initial = uiState.period,
                    state = uiState.periodError?.let {
                        DataState.Error(it)
                    },
                    pasteEnabled = false,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    onValueChange = {
                        viewModel.onEnterPeriod(it)
                    }
                )
                VSpacer(32.dp)
            }
            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp),
                    title = stringResource(R.string.SwapSettings_Apply),
                    onClick = {
                        viewModel.save()
                    },
                    enabled = uiState.applyEnabled
                )
            }
        }
    }
}
