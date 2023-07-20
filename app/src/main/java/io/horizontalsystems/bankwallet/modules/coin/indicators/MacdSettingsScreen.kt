package io.horizontalsystems.bankwallet.modules.coin.indicators

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.chart.ChartIndicatorSetting
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.swap.settings.ui.InputWithButtons
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*

@Composable
fun MacdSettingsScreen(navController: NavController, indicatorSetting: ChartIndicatorSetting) {
    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = TranslatableString.ResString(R.string.CoinPage_IndicatorMACD),
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Reset),
                        enabled = false,
                        onClick = {

                        }
                    )
                )
            )
        }
    ) {
        Column(Modifier.padding(it)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                InfoText(
                    text = stringResource(R.string.CoinPage_MacdSettingsDescription)
                )
                VSpacer(12.dp)
                HeaderText(
                    text = stringResource(R.string.CoinPage_FastLength).uppercase()
                )
                InputWithButtons(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    hint = "14",
                    initial = null,
                    buttons = emptyList(),
                    state = null,
                    onValueChange = {

                    }
                )
                VSpacer(24.dp)
                HeaderText(
                    text = stringResource(R.string.CoinPage_SlowLength).uppercase()
                )
                InputWithButtons(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    hint = "26",
                    initial = null,
                    buttons = emptyList(),
                    state = null,
                    onValueChange = {

                    }
                )
                VSpacer(24.dp)
                HeaderText(
                    text = stringResource(R.string.CoinPage_SignalSmoothing).uppercase()
                )
                InputWithButtons(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    hint = "9",
                    initial = null,
                    buttons = emptyList(),
                    state = null,
                    onValueChange = {

                    }
                )
                VSpacer(32.dp)
            }
            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                    title = stringResource(R.string.SwapSettings_Apply),
                    onClick = {

                    },
                    enabled = false
                )
            }
        }
    }
}
