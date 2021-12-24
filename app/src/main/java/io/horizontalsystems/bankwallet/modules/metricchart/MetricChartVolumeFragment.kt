package io.horizontalsystems.bankwallet.modules.metricchart

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.chart.ChartViewModel
import io.horizontalsystems.bankwallet.ui.extensions.BaseBottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_market_global.*

class MetricChartVolumeFragment : BaseBottomSheetDialogFragment() {

    private val coinUid by lazy {
        requireArguments().getString(coinUidKey) ?: ""
    }

    private val coinName by lazy {
        requireArguments().getString(coinNameKey) ?: ""
    }

    private val chartViewModel by viewModels<ChartViewModel> { MetricChartVolumeModule.Factory(coinUid) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setContentView(R.layout.fragment_market_global)

        setTitle(getString(R.string.CoinPage_TotalVolume))
        setSubtitle(getString(R.string.MarketGlobalMetrics_Chart))
        setHeaderIcon(R.drawable.ic_chart_24)

        composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )

        composeView.setContent {
            MetricChartScreen(
                chartViewModel = chartViewModel,
                description = stringResource(R.string.MarketGlobalMetrics_VolumeDescriptionCoin, coinName),
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

