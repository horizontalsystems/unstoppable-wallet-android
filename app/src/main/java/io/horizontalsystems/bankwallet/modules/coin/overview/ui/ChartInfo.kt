package io.horizontalsystems.bankwallet.modules.coin.overview.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.horizontalsystems.bankwallet.modules.coin.adapters.CoinChartAdapter
import io.horizontalsystems.bankwallet.modules.coin.adapters.CoinChartView
import io.horizontalsystems.core.entities.Currency

@Composable
fun ChartInfo(
    chartInfo: CoinChartAdapter.ViewItemWrapper,
    currency: Currency,
    listener: CoinChartAdapter.Listener
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(), // Occupy the max size in the Compose UI tree
        factory = { context ->
            CoinChartView(context).apply {
                setCurrency(currency)
                setChartViewType(CoinChartAdapter.ChartViewType.CoinChart)
                setListener(listener)
                bindNew(chartInfo)
            }
        },
        update = { view ->
            view.bindNew(chartInfo)
        }
    )
}