package com.quantum.wallet.bankwallet.modules.chart

import com.quantum.wallet.bankwallet.entities.ViewState
import com.quantum.wallet.bankwallet.modules.coin.ChartInfoData
import com.quantum.wallet.bankwallet.uiv3.components.tabs.TabItem
import com.quantum.wallet.chartview.ChartViewType
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
