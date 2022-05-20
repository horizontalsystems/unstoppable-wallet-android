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
import io.horizontalsystems.bankwallet.core.getEnum
import io.horizontalsystems.bankwallet.core.putEnum
import io.horizontalsystems.bankwallet.modules.chart.ChartViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader

class ProChartFragment : BaseComposableBottomSheetFragment() {

    private val coinUid by lazy {
        requireArguments().getString(coinUidKey) ?: ""
    }

    private val chartType by lazy {
        requireArguments().getEnum(chartTypeKey, ProChartModule.ChartType.DexVolume)
    }

    private val title by lazy {
        requireArguments().getString(titleKey) ?: ""
    }

    private val description by lazy {
        requireArguments().getString(descriptionKey) ?: ""
    }

    private val chartViewModel by viewModels<ChartViewModel> {
        ProChartModule.Factory(
            coinUid, chartType
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
                        title = title,
                        subtitle = getString(R.string.MarketGlobalMetrics_Chart),
                        onCloseClick = { close() }
                    ) {
                        MetricChartScreen(
                            chartViewModel = chartViewModel,
                            description = description,
                            poweredBy = stringResource(R.string.Market_PoweredByHorizontalApi)
                        )
                    }
                }
            }
        }
    }

    companion object {
        private const val coinUidKey = "coinUidKey"
        private const val chartTypeKey = "chartTypeKey"
        private const val titleKey = "titleKey"
        private const val descriptionKey = "descriptionKey"

        fun show(fragmentManager: FragmentManager, coinUid: String, chartType: ProChartModule.ChartType, title: String, description: String) {
            val fragment = ProChartFragment()
            fragment.arguments = bundleOf(coinUidKey to coinUid, titleKey to title, descriptionKey to description)
            fragment.arguments?.putEnum(chartTypeKey, chartType)
            fragment.show(fragmentManager, "metric_chart_dialog")
        }
    }
}

