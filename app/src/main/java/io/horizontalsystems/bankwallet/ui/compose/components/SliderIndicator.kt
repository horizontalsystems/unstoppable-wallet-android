package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun SliderIndicator(total: Int, current: Int) {
    Row(
        modifier = Modifier.height(32.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (index == current) ComposeAppTheme.colors.jacob else ComposeAppTheme.colors.blade)
                    .size(width = 20.dp, height = 4.dp),
            )
        }
    }
}

@Composable
fun DynamicSliderIndicator(total: Int, current: Int) {
    val inactiveDotWidth = 10.dp
    val spacing = 4.dp
    val activeDotWidth = inactiveDotWidth * 2 + spacing
    val dotHeight = 4.dp
    val dotsTotal = total + 1 //show one more, because active dot covers 2 dots space

    Box(
        modifier = Modifier.height(32.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(dotsTotal) {
                Box(
                    modifier = Modifier
                        .width(inactiveDotWidth)
                        .height(dotHeight)
                        .clip(RoundedCornerShape(2.dp))
                        .background(ComposeAppTheme.colors.blade)
                )
            }
        }

        val offset by animateDpAsState(
            targetValue = (inactiveDotWidth + spacing) * current,
            label = "offset"
        )
        Box(
            modifier = Modifier
                .width(activeDotWidth)
                .height(dotHeight)
                .offset(x = offset)
                .clip(RoundedCornerShape(2.dp))
                .background(ComposeAppTheme.colors.jacob)
        )
    }
}
