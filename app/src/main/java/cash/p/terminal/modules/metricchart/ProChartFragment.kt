package cash.p.terminal.modules.metricchart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import cash.p.terminal.R
import cash.p.terminal.modules.chart.ChartViewModel
import cash.p.terminal.modules.coin.overview.ui.Chart
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.components.VSpacer
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
                        Chart(chartViewModel = chartViewModel)
                        VSpacer(32.dp)
                    }
                }
            }
        }
    }

    companion object {
        private const val coinUidKey = "coinUidKey"
        private const val chartTypeKey = "chartTypeKey"
        private const val titleKey = "titleKey"

        fun show(fragmentManager: FragmentManager, coinUid: String, chartType: ProChartModule.ChartType, title: String) {
            val fragment = ProChartFragment()
            fragment.arguments = bundleOf(coinUidKey to coinUid, titleKey to title, chartTypeKey to chartType.ordinal)
            fragment.show(fragmentManager, "pro_chart_dialog")
        }
    }
}

