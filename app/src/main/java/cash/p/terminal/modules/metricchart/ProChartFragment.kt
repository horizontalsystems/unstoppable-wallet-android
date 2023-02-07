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

class ProChartFragment : BaseComposableBottomSheetFragment() {

    private val coinUid by lazy {
        requireArguments().getString(coinUidKey) ?: ""
    }

    private val chartType by lazy {
        val enumOrdinal = requireArguments().getInt(chartTypeKey, 0)
        enumValues<ProChartModule.ChartType>()[enumOrdinal]
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
            fragment.arguments = bundleOf(coinUidKey to coinUid, titleKey to title, descriptionKey to description, chartTypeKey to chartType.ordinal)
            fragment.show(fragmentManager, "pro_chart_dialog")
        }
    }
}

