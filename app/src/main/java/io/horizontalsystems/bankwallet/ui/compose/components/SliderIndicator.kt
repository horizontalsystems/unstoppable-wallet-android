package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun SliderIndicator(total: Int, current: Int) {
    Row(
        modifier = Modifier.height(30.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (index == current) ComposeAppTheme.colors.jacob else ComposeAppTheme.colors.steel20)
                    .size(width = 20.dp, height = 4.dp),
            )
        }
    }
}
