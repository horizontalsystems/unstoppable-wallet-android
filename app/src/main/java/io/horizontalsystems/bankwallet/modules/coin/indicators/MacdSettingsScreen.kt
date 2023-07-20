package cash.p.terminal.modules.coin.indicators

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
import cash.p.terminal.R
import cash.p.terminal.modules.chart.ChartIndicatorSetting
import cash.p.terminal.modules.evmfee.ButtonsGroupWithShade
import cash.p.terminal.modules.swap.settings.ui.InputWithButtons
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.*

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
