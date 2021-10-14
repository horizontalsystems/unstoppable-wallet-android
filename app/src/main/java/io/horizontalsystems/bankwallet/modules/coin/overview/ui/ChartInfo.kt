package io.horizontalsystems.bankwallet.modules.coin.overview.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
    // Adds view to Compose
    AndroidView(
        modifier = Modifier.fillMaxSize(), // Occupy the max size in the Compose UI tree
        factory = { context ->
            // Creates custom view
            CoinChartView(context).apply {
                // Sets up listeners for View -> Compose communication
                setCurrency(currency)
                setChartViewType(CoinChartAdapter.ChartViewType.CoinChart)
                setListener(listener)
                bind(chartInfo)
            }
        },
        update = { view ->
            // View's been inflated or state read in this block has been updated
            // Add logic here if necessary

            // As selectedItem is read here, AndroidView will recompose
            // whenever the state changes
            // Example of Compose -> View communication
//            view.coordinator.selectedItem = selectedItem.value
        }
    )
}