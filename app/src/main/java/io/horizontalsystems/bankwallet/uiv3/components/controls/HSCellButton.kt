package io.horizontalsystems.bankwallet.uiv3.components.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun HSCellButton(
    modifier: Modifier = Modifier,
    icon: Painter,
    iconTint: Color = ComposeAppTheme.colors.leah,
    backgroundColor: Color = ComposeAppTheme.colors.blade,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(backgroundColor)
            .padding(horizontal = 38.dp)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = iconTint
        )
    }
}

@Preview
@Composable
fun Preview_HSCellButton() {
    ComposeAppTheme {
        Box(modifier = Modifier.height(73.dp)) {
            HSCellButton(icon = painterResource(R.drawable.trash_24)) {}
        }
    }
}
