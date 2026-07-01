package io.horizontalsystems.bankwallet.modules.metricchart

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.core.R
import io.horizontalsystems.bankwallet.modules.chart.ChartViewModel
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Chart
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.bankwallet.ui.extensions.HSBottomSheet
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
data class ProChartSheet(val input: Input) : HSBottomSheet() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
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
            onCloseClick = { navigation.removeLastOrNull() }
        ) {
            Chart(chartViewModel = chartViewModel)
            VSpacer(32.dp)
        }
    }

    @Serializable
    @Parcelize
    data class Input(
        val coinUid: String,
        val title: String,
        val chartType: Int,
    ) : Parcelable
}

