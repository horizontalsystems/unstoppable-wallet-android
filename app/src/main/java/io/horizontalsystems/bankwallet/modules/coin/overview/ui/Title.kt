package io.horizontalsystems.bankwallet.modules.coin.overview.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.coin.adapters.CoinSubtitleAdapter
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import java.math.BigDecimal

@Composable
fun Title(subtitle: CoinSubtitleAdapter.ViewItemWrapper) {
    Row(
        modifier = Modifier
            .height(40.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.padding(end = 8.dp),
            text = subtitle.rate ?: "",
            style = ComposeAppTheme.typography.headline1,
            color = ComposeAppTheme.colors.leah
        )

        subtitle.rateDiff?.let { value ->
            val sign = if (value >= BigDecimal.ZERO) "+" else "-"
            val text = App.numberFormatter.format(value.abs(), 0, 2, sign, "%")

            val color = when {
                value >= BigDecimal.ZERO -> ComposeAppTheme.colors.remus
                else -> ComposeAppTheme.colors.lucian
            }

            Text(
                text = text,
                style = ComposeAppTheme.typography.subhead1,
                color = color
            )
        }
    }
}