package io.horizontalsystems.bankwallet.uiv3.components.controls

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun HSSelector(
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: ((Boolean) -> Unit)?,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChange?.invoke(!checked) }
            )
            .size(24.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .border(
                    width = 1.dp,
                    color = ComposeAppTheme.colors.andy,
                    shape = CircleShape
                )
        )

        if (checked) {
            Image(
                painter = painterResource(id = R.drawable.checkbox_selected_24),
                contentDescription = null
            )
        }
    }
}
