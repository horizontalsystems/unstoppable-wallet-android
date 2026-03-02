package io.horizontalsystems.bankwallet.modules.metricchart

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.chart.ChartViewModel
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Chart
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import kotlinx.serialization.Serializable

@Serializable
data class ProChartScreen(
    val coinUid: String,
    val title: String,
    val chartType: ProChartModule.ChartType,
) : HSScreen(bottomSheet = true) {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        val chartViewModel = viewModel<ChartViewModel>(
            factory = ProChartModule.Factory(
                coinUid,
                chartType
            )
        )

        BottomSheetHeader(
            iconPainter = painterResource(R.drawable.ic_chart_24),
            iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob),
            title = title,
            onCloseClick = { backStack.removeLastOrNull() }
        ) {
            Chart(chartViewModel = chartViewModel)
            VSpacer(32.dp)
        }
    }
}

