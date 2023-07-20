package cash.p.terminal.modules.coin.indicators

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.modules.chart.ChartIndicatorSetting
import cash.p.terminal.ui.compose.ComposeAppTheme
import io.horizontalsystems.core.helpers.HudHelper

class IndicatorSettingsFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                ComposeAppTheme {
                    val navController = findNavController()
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
            }
        }
    }

    companion object {
        fun params(indicatorId: String) = bundleOf("indicatorId" to indicatorId)
    }
}
