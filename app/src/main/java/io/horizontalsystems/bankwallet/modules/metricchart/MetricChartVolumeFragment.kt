package io.horizontalsystems.bankwallet.modules.metricchart

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.chart.ChartViewModel
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment

class MetricChartVolumeFragment : BaseComposableBottomSheetFragment() {

    private val coinUid by lazy {
        requireArguments().getString(coinUidKey) ?: ""
    }

    private val coinName by lazy {
        requireArguments().getString(coinNameKey) ?: ""
    }

    private val chartViewModel by viewModels<ChartViewModel> {
        MetricChartVolumeModule.Factory(
            coinUid
        )
    }

    @Composable
    override fun BottomSheetScreen() {
        BottomSheetHeader(
            iconPainter = painterResource(R.drawable.ic_chart_24),
            title = getString(R.string.CoinPage_TotalVolume),
            subtitle = getString(R.string.MarketGlobalMetrics_Chart)
        ) {
            MetricChartScreen(
                chartViewModel = chartViewModel,
                description = stringResource(
                    R.string.MarketGlobalMetrics_VolumeDescriptionCoin, coinName
                ),
                poweredBy = stringResource(R.string.Market_PoweredByApi)
            )
        }
    }

    companion object {
        private const val coinUidKey = "coinUidKey"
        private const val coinNameKey = "coinNameKey"

        fun show(fragmentManager: FragmentManager, coinUid: String, coinName: String) {
            val fragment = MetricChartVolumeFragment()
            fragment.arguments = bundleOf(coinUidKey to coinUid, coinNameKey to coinName)
            fragment.show(fragmentManager, "metric_chart_dialog")
        }
    }
}

