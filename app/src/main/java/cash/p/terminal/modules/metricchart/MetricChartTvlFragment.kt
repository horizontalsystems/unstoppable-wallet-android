package cash.p.terminal.modules.metricchart

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
import cash.p.terminal.R
import cash.p.terminal.modules.chart.ChartViewModel
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.extensions.BaseComposableBottomSheetFragment
import cash.p.terminal.ui.extensions.BottomSheetHeader

class MetricChartTvlFragment : BaseComposableBottomSheetFragment() {

    private val coinUid by lazy {
        requireArguments().getString(coinUidKey) ?: ""
    }

    private val chartViewModel by viewModels<ChartViewModel> {
        MetricChartTvlModule.Factory(coinUid)
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
                        title = getString(R.string.CoinPage_Tvl),
                        onCloseClick = { close() }
                    ) {
                        MetricChartScreen(
                            chartViewModel = chartViewModel,
                            description = stringResource(R.string.CoinPage_TvlDescription),
                            poweredBy = stringResource(R.string.Market_PoweredByDefiLlamaApi)
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
            val fragment = MetricChartTvlFragment()
            fragment.arguments = bundleOf(coinUidKey to coinUid, coinNameKey to coinName)
            fragment.show(fragmentManager, "metric_chart_dialog")
        }
    }
}
