package io.horizontalsystems.bankwallet.modules.metricchart

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.modules.chart.ChartViewModel
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Chart
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey

@Composable
fun MetricChartScreen(
    chartViewModel: ChartViewModel,
    description: String,
    poweredBy: String
) {
    ComposeAppTheme {
        Column {
            Chart(chartViewModel = chartViewModel)
            BottomSheetText(
                text = description
            )
            BottomSheetText(
                text = poweredBy,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun BottomSheetText(text: String, textAlign: TextAlign? = null) {
    subhead2_grey(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 24.dp),
        text = text,
        textAlign = textAlign
    )
}