package io.horizontalsystems.bankwallet.uiv3.components.cell

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.Bright
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CellRightControlsSwitcher(
    checked: Boolean,
    enabled: Boolean = true,
    onInfoClick: (() -> Unit)? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    var internalChecked by remember { mutableStateOf(checked) }
    LaunchedEffect(checked) {
        if (internalChecked != checked) {
            internalChecked = checked
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        onInfoClick?.let {
            Icon(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onInfoClick),
                painter = painterResource(R.drawable.info_filled_24),
                contentDescription = null,
                tint = ComposeAppTheme.colors.grey
            )
            HSpacer(12.dp)
        }
        CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
            Switch(
                checked = internalChecked,
                onCheckedChange = { newChecked ->
                    internalChecked = newChecked
                    onCheckedChange(newChecked)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Bright,
                    uncheckedThumbColor = Bright,
                    checkedTrackColor = ComposeAppTheme.colors.jacob,
                    uncheckedTrackColor = ComposeAppTheme.colors.andy,
                    checkedTrackAlpha = 1f,
                    uncheckedTrackAlpha = 1f,
                ),
                enabled = enabled
            )
        }
    }
}

@Preview
@Composable
fun Preview_CellRightControlsSwitcher() {
    ComposeAppTheme {
        CellRightControlsSwitcher(
            checked = true,
            onInfoClick = {},
            onCheckedChange = {}
        )
    }
}
