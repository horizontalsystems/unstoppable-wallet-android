package io.horizontalsystems.walletkit.modules.coin.indicators

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.helpers.HudHelper
import io.horizontalsystems.walletkit.modules.chart.ChartIndicatorSetting
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import kotlinx.serialization.Serializable

@Serializable
data class IndicatorSettingsPage(val input: Input) : HSPage(accessibleWhileLocked = true) {

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
