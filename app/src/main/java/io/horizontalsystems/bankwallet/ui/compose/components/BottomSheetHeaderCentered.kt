package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun BottomSheetHeaderCentered(
    iconPainter: Painter,
    title: String,
    iconTint: ColorFilter? = null,
    content: @Composable (ColumnScope.() -> Unit),
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(color = ComposeAppTheme.colors.lawrence)
    ) {
        Row(Modifier.height(64.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .size(24.dp),
                painter = iconPainter,
                colorFilter = iconTint,
                contentDescription = null
            )
            Text(
                modifier = Modifier
                    .padding(end = 48.dp)
                    .weight(1f),
                text = title,
                textAlign = TextAlign.Center,
                color = ComposeAppTheme.colors.leah,
                style = ComposeAppTheme.typography.headline2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(
            content = content
        )
    }
}
