package io.horizontalsystems.walletkit.modules.chart

import io.horizontalsystems.chartview.ChartViewType
import io.horizontalsystems.walletkit.entities.ViewState
import io.horizontalsystems.walletkit.modules.coin.ChartInfoData
import io.horizontalsystems.walletkit.uiv3.components.tabs.TabItem
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
