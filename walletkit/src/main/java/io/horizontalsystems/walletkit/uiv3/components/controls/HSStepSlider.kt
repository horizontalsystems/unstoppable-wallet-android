package io.horizontalsystems.walletkit.uiv3.components.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.components.subhead_grey
import kotlin.math.roundToInt

/**
 * A slider over whole numbers from [range] in multiples of [step], with a label at either end.
 * [onValueChange] is called once per step crossed, never with a value between two steps.
 */
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
            colors = SliderDefaults.colors(
                thumbColor = ComposeAppTheme.colors.jacob,
                activeTrackColor = ComposeAppTheme.colors.jacob,
                inactiveTrackColor = ComposeAppTheme.colors.blade,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
        )
        rightLabel?.let { subhead_grey(text = it) }
    }
}
