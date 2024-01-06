package cash.p.terminal.modules.coin.indicators

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.modules.chart.ChartIndicatorSetting
import io.horizontalsystems.core.helpers.HudHelper

class IndicatorSettingsFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val indicatorSetting = arguments?.getString("indicatorId")?.let {
            App.chartIndicatorManager.getChartIndicatorSetting(it)
        }

        if (indicatorSetting == null) {
            HudHelper.showErrorMessage(LocalView.current, R.string.Error_ParameterNotSet)
            navController.popBackStack()
        } else {
            when (indicatorSetting.type) {
                ChartIndicatorSetting.IndicatorType.MA -> {
                    EmaSettingsScreen(
                        navController = navController,
                        indicatorSetting = indicatorSetting
                    )
                }

                ChartIndicatorSetting.IndicatorType.RSI -> {
                    RsiSettingsScreen(
                        navController = navController,
                        indicatorSetting = indicatorSetting
                    )
                }

                ChartIndicatorSetting.IndicatorType.MACD -> {
                    MacdSettingsScreen(
                        navController = navController,
                        indicatorSetting = indicatorSetting
                    )
                }
            }
        }
    }

    companion object {
        fun params(indicatorId: String) = bundleOf("indicatorId" to indicatorId)
    }
}
