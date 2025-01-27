package cash.p.terminal.modules.metricchart

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewmodel.compose.viewModel
import cash.p.terminal.R
import io.horizontalsystems.core.getInputX
import io.horizontalsystems.chartview.chart.ChartViewModel
import io.horizontalsystems.chartview.ui.Chart
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui.extensions.BaseComposableBottomSheetFragment
import cash.p.terminal.ui.extensions.BottomSheetHeader
import kotlinx.parcelize.Parcelize

class ProChartFragment : BaseComposableBottomSheetFragment() {

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
                val input = requireArguments().getInputX<Input>()!!
                val chartViewModel = viewModel<ChartViewModel>(
                    factory = ProChartModule.Factory(
                        input.coinUid,
                        enumValues<ProChartModule.ChartType>()[input.chartType]
                    )
                )

                cash.p.terminal.ui_compose.theme.ComposeAppTheme {
                    BottomSheetHeader(
                        iconPainter = painterResource(R.drawable.ic_chart_24),
                        iconTint = ColorFilter.tint(cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.jacob),
                        title = input.title,
                        onCloseClick = { close() }
                    ) {
                        Chart(
                            uiState = chartViewModel.uiState,
                            getSelectedPointCallback = chartViewModel::getSelectedPoint,
                            onSelectChartInterval = chartViewModel::onSelectChartInterval)
                        VSpacer(32.dp)
                    }
                }
            }
        }
    }

    @Parcelize
    data class Input(
        val coinUid: String,
        val title: String,
        val chartType: Int,
    ) : Parcelable

    companion object {
        fun show(
            fragmentManager: FragmentManager,
            coinUid: String,
            title: String,
            chartType: ProChartModule.ChartType,
        ) {
            val fragment = ProChartFragment()
            fragment.arguments = bundleOf(
                "input" to Input(coinUid, title, chartType.ordinal)
            )
            fragment.show(fragmentManager, "pro_chart_dialog")
        }
    }
}

