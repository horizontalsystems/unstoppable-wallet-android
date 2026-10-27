package io.horizontalsystems.walletkit.uiv3.components.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.components.subhead_grey
import kotlin.math.roundToInt

/**
 * A slider over whole numbers from [range] in multiples of [step], with a label at either end.
 * [onValueChange] is called once per step crossed, never with a value between two steps.
 * The track is not filled on either side of the thumb: the two labels already say how the
 * whole is divided, so a filled side would wrongly single one of them out.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HSStepSlider(
    value: Int,
    range: IntRange,
    step: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    leftLabel: String? = null,
    rightLabel: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leftLabel?.let { subhead_grey(text = it) }
        Slider(
            modifier = Modifier.weight(1f),
            value = value.toFloat(),
            onValueChange = { raw ->
                val snapped = (((raw - range.first) / step).roundToInt() * step + range.first).coerceIn(range)
                if (snapped != value) onValueChange(snapped)
            },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = ((range.last - range.first) / step - 1).coerceAtLeast(0),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(ComposeAppTheme.colors.jacob),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        painter = painterResource(R.drawable.ic_minus_20),
                        tint = ComposeAppTheme.colors.lawrence,
                        contentDescription = null,
                    )
                }
            },
            track = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(ComposeAppTheme.colors.blade),
                )
            },
        )
        rightLabel?.let { subhead_grey(text = it) }
    }
}
