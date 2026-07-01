package io.horizontalsystems.bankwallet.modules.coin.indicators

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import io.horizontalsystems.core.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.chart.ChartIndicatorSetting
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.serialization.Serializable

@Serializable
data class IndicatorSettingsPage(val input: Input) : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val indicatorSetting =
            App.chartIndicatorManager.getChartIndicatorSetting(input.indicatorId)

        if (indicatorSetting == null) {
            HudHelper.showErrorMessage(LocalView.current, R.string.Error_ParameterNotSet)
            navigation.removeLastOrNull()
        } else {
            when (indicatorSetting.type) {
                ChartIndicatorSetting.IndicatorType.MA -> {
                    EmaSettingsScreen(
                        navigation = navigation,
                        indicatorSetting = indicatorSetting
                    )
                }

                ChartIndicatorSetting.IndicatorType.RSI -> {
                    RsiSettingsScreen(
                        navigation = navigation,
                        indicatorSetting = indicatorSetting
                    )
                }

                ChartIndicatorSetting.IndicatorType.MACD -> {
                    MacdSettingsScreen(
                        navigation = navigation,
                        indicatorSetting = indicatorSetting
                    )
                }
            }
        }
    }

    @Serializable
    data class Input(val indicatorId: String)
}
