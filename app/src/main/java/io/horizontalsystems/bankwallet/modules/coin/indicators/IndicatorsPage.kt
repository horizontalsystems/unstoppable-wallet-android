package io.horizontalsystems.bankwallet.modules.coin.indicators

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.chart.ChartIndicatorSetting
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderText
import io.horizontalsystems.bankwallet.ui.compose.components.HsIconButton
import io.horizontalsystems.bankwallet.ui.compose.components.HsSwitch
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import kotlinx.serialization.Serializable

@Serializable
data object IndicatorsPage : HSPage() {

    @Composable
    override fun GetContent(navController: HSNavigation) {
        IndicatorsScreen(
            navController = navController,
        )
    }

}

@Composable
fun IndicatorsScreen(navController: HSNavigation) {
    val chartIndicatorsViewModel =
        hiltViewModel<ChartIndicatorsViewModel>()

    val uiState = chartIndicatorsViewModel.uiState
    val toggleIndicator = { indicator: ChartIndicatorSetting, checked: Boolean ->
        if (checked) {
            chartIndicatorsViewModel.enable(indicator)
        } else {
            chartIndicatorsViewModel.disable(indicator)
        }
    }

    HSScaffold(
        title = stringResource(R.string.CoinPage_Indicators),
        onBack = navController::removeLastOrNull,
    ) {
        Column {
            HeaderText(
                stringResource(R.string.CoinPage_MovingAverages).uppercase()
            )
            CellUniversalLawrenceSection(uiState.maIndicators) { indicator: ChartIndicatorSetting ->
                val indicatorDataMa = indicator.getTypedDataMA()
                IndicatorCell(
                    title = indicator.name,
                    checked = indicator.enabled,
                    leftIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_chart_type_2_24),
                            tint = Color(indicatorDataMa.color),
                            contentDescription = null,
                        )
                    },
                    onCheckedChange = {
                        toggleIndicator.invoke(indicator, it)
                    },
                    onEditClick = {
                        navController.slideFromRight(
                            IndicatorSettingsPage(IndicatorSettingsPage.Input(indicator.id))
                        )
                    }
                )
            }
            VSpacer(24.dp)
            HeaderText(
                stringResource(R.string.CoinPage_OscillatorsSettings).uppercase()
            )
            CellUniversalLawrenceSection(uiState.oscillatorIndicators) { indicator ->
                IndicatorCell(
                    title = indicator.name,
                    checked = indicator.enabled,
                    onCheckedChange = {
                        toggleIndicator.invoke(indicator, it)
                    },
                    onEditClick = {
                        navController.slideFromRight(
                            IndicatorSettingsPage(IndicatorSettingsPage.Input(indicator.id))
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun IndicatorCell(
    title: String,
    checked: Boolean,
    leftIcon: (@Composable () -> Unit)? = null,
    onCheckedChange: (Boolean) -> Unit,
    onEditClick: () -> Unit
) {
    RowUniversal(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        leftIcon?.invoke()
        HSpacer(16.dp)
        body_leah(
            text = title,
            modifier = Modifier.weight(1f)
        )
        HsIconButton(
            modifier = Modifier.size(20.dp),
            onClick = onEditClick
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_edit_20),
                contentDescription = null,
                tint = ComposeAppTheme.colors.grey
            )
        }
        HSpacer(16.dp)
        HsSwitch(
            modifier = Modifier.padding(0.dp),
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
