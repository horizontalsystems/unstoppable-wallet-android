package com.quantum.wallet.bankwallet.modules.coin.overview.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.ui.compose.components.TabBalance
import java.math.BigDecimal

@Composable
fun Title(rate: String?, rateDiff: BigDecimal?) {
    TabBalance(borderTop = true) {
        Text(
            modifier = Modifier.padding(end = 8.dp),
            text = rate ?: "",
            style = ComposeAppTheme.typography.headline1,
            color = ComposeAppTheme.colors.leah
        )

        rateDiff?.let { value ->
            val sign = if (value >= BigDecimal.ZERO) "+" else "-"
            val text = App.numberFormatter.format(value.abs(), 0, 2, sign, "%")

            val color = when {
                value >= BigDecimal.ZERO -> ComposeAppTheme.colors.remus
                else -> ComposeAppTheme.colors.lucian
            }

            Text(
                text = text,
                style = ComposeAppTheme.typography.subhead,
                color = color
            )
        }
    }
}
