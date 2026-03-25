package io.horizontalsystems.bankwallet.modules.metricchart

import android.os.Parcelable
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
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import kotlinx.parcelize.Parcelize

class ProChartFragment(val input: Input) : BaseComposableBottomSheetFragment() {

    @Composable
    override fun GetContent(navController: NavBackStack<HSScreen>) {
        val chartViewModel = viewModel<ChartViewModel>(
            factory = ProChartModule.Factory(
                input.coinUid,
                enumValues<ProChartModule.ChartType>()[input.chartType]
            )
        )

        BottomSheetHeader(
            iconPainter = painterResource(R.drawable.ic_chart_24),
            iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob),
            title = input.title,
            onCloseClick = { close() }
        ) {
            Chart(chartViewModel = chartViewModel)
            VSpacer(32.dp)
        }
    }

    @Parcelize
    data class Input(
        val coinUid: String,
        val title: String,
        val chartType: Int,
    ) : Parcelable
}

