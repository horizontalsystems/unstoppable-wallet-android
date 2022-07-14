package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun HsSlider(
    value: Long,
    onValueChange: (Long) -> Unit,
    valueRange: ClosedRange<Long>,
    onValueChangeFinished: () -> Unit
) {
    var selectedValue: Float by rememberSaveable(value) { mutableStateOf(value.toFloat()) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            modifier = Modifier.clickable {
                if (selectedValue > valueRange.start) {
                    selectedValue--
                    onValueChange(selectedValue.toLong())
                    onValueChangeFinished()
                }
            },
            painter = painterResource(id = R.drawable.ic_minus_20),
            contentDescription = ""
        )
        Slider(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            value = selectedValue,
            onValueChange = {
                selectedValue = it
                onValueChange(selectedValue.toLong())
            },
            valueRange = valueRange.start.toFloat()..valueRange.endInclusive.toFloat(),
            onValueChangeFinished = onValueChangeFinished,
            steps = (valueRange.endInclusive - valueRange.start).toInt(),
            colors = SliderDefaults.colors(
                thumbColor = ComposeAppTheme.colors.grey,
                activeTickColor = ComposeAppTheme.colors.transparent,
                inactiveTickColor = ComposeAppTheme.colors.transparent,
                activeTrackColor = ComposeAppTheme.colors.steel20,
                inactiveTrackColor = ComposeAppTheme.colors.steel20,
                disabledActiveTickColor = ComposeAppTheme.colors.transparent,
                disabledInactiveTrackColor = ComposeAppTheme.colors.steel20
            )
        )
        Image(
            modifier = Modifier.clickable {
                if (selectedValue < valueRange.endInclusive) {
                    selectedValue++
                    onValueChange(selectedValue.toLong())
                    onValueChangeFinished()
                }
            },
            painter = painterResource(id = R.drawable.ic_plus_20),
            contentDescription = ""
        )
    }
}
