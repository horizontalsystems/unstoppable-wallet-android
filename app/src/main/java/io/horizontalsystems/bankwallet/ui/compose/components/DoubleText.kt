package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun DoubleText(
    title: String,
    body: String,
    dimmed: Boolean,
    onClickTitle: () -> Unit,
    onClickSubtitle: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ComposeAppTheme.colors.tyler)
            .padding(horizontal = 16.dp, vertical = 24.dp),
    ) {
        Text(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClickTitle
                ),
            text = title,
            style = ComposeAppTheme.typography.title2М,
            color = if (dimmed) ComposeAppTheme.colors.grey else ComposeAppTheme.colors.leah,
            maxLines = 1
        )
        Text(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClickSubtitle
                ),
            text = body,
            style = ComposeAppTheme.typography.body,
            color = if (dimmed) ComposeAppTheme.colors.andy else ComposeAppTheme.colors.grey,
            maxLines = 1
        )
    }

}