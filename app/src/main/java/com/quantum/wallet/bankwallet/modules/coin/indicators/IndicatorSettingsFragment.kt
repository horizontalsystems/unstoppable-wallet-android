package com.quantum.wallet.bankwallet.modules.coin.indicators

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.modules.chart.ChartIndicatorSetting
import com.quantum.wallet.core.helpers.HudHelper
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
