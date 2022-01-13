package io.horizontalsystems.bankwallet.modules.metricchart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.chart.ChartViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader

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
                    BottomSheetHeader(
                        iconPainter = painterResource(R.drawable.ic_chart_24),
                        title = getString(R.string.CoinPage_TotalVolume),
                        subtitle = getString(R.string.MarketGlobalMetrics_Chart),
                        onCloseClick = { close() }
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
            }
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

