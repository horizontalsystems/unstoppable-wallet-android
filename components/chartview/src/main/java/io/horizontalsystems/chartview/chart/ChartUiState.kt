package io.horizontalsystems.chartview.chart

import cash.p.terminal.ui_compose.components.TabItem
import io.horizontalsystems.chartview.ChartViewType
import io.horizontalsystems.chartview.entity.ChartInfoData
import cash.p.terminal.ui_compose.entities.ViewState
import io.horizontalsystems.core.models.HsTimePeriod

data class ChartUiState(
    val tabItems: List<TabItem<HsTimePeriod?>>,
    val chartHeaderView: ChartModule.ChartHeaderView?,
    val chartInfoData: ChartInfoData?,
    val loading: Boolean,
    val viewState: ViewState,
    val hasVolumes: Boolean,
    val chartViewType: ChartViewType,
    val considerAlwaysPositive: Boolean,
    val titleHidden: Boolean
)
