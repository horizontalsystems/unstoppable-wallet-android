package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun StackedBarChart(
    slices: List<StackBarSlice>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(40.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        slices
            .filter { it.value >= 1f }
            .forEach {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(it.color)
                        .weight(it.value),
                )
            }
    }
}

data class StackBarSlice(
    val value: Float,
    val color: Color,
)

@Preview
@Composable
private fun StackedBarChart_Preview() {
    val slices = listOf(
        StackBarSlice(value = 60f, color = Color(0xFF6B7196)),
        StackBarSlice(value = 31f, color = Color(0xFFF3BA2F)),
        StackBarSlice(value = 8f, color = Color(0xFF8247E5)),
        StackBarSlice(value = 1f, color = Color(0xFFD74F49))
    )
    ComposeAppTheme {
        StackedBarChart(
            slices,
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}
