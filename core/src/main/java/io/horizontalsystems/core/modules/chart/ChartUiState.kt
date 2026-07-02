package io.horizontalsystems.core.modules.chart

import io.horizontalsystems.chartview.ChartViewType
import io.horizontalsystems.core.entities.ViewState
import io.horizontalsystems.core.modules.coin.ChartInfoData
import io.horizontalsystems.core.uiv3.components.tabs.TabItem
import io.horizontalsystems.marketkit.models.HsTimePeriod

data class ChartUiState(
    val tabItems: List<TabItem<HsTimePeriod?>>,
    val chartHeaderView: ChartModule.ChartHeaderView?,
    val chartInfoData: ChartInfoData?,
    val loading: Boolean,
    val viewState: ViewState,
    val hasVolumes: Boolean,
    val chartViewType: ChartViewType,
)
