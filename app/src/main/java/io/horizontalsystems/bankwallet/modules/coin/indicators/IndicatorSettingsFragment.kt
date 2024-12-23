package io.horizontalsystems.bankwallet.modules.coin.indicators

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.chart.ChartIndicatorSetting
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.parcelize.Parcelize

class IndicatorSettingsFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Input>(navController) { input ->
            val indicatorSetting =
                App.chartIndicatorManager.getChartIndicatorSetting(input.indicatorId)

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
    }

    @Parcelize
    data class Input(val indicatorId: String) : Parcelable
}
