package cash.p.terminal.modules.chart

import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.coin.ChartInfoData
import cash.p.terminal.ui.compose.components.TabItem
import io.horizontalsystems.chartview.ChartViewType
import io.horizontalsystems.marketkit.models.HsTimePeriod

data class ChartUiState(
    val tabItems: List<TabItem<HsTimePeriod?>>,
    val chartHeaderView: ChartModule.ChartHeaderView?,
    val chartInfoData: ChartInfoData?,
    val loading: Boolean,
    val viewState: ViewState,
    val hasVolumes: Boolean,
    val chartViewType: ChartViewType
)
