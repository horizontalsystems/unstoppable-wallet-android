package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.WithTranslatableTitle

@Composable
fun <T : WithTranslatableTitle> AlertGroup(
    title: String,
    select: Select<T>,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            AlertHeader(title)
            select.options.forEach { option ->
                AlertItem(
                    onClick = { onSelect.invoke(option) }
                ) {
                    Text(
                        option.title.getString(),
                        color = if (option == select.selected) ComposeAppTheme.colors.jacob else ComposeAppTheme.colors.leah,
                        style = ComposeAppTheme.typography.body,
                    )
                }
            }
        }
    }
}

@Composable
fun AlertHeader(text: String) {
    Box(
        modifier = Modifier
            .height(40.dp)
            .fillMaxWidth()
            .background(ComposeAppTheme.colors.lawrence),
        contentAlignment = Alignment.Center
    ) {
        subhead1_grey(text)
    }
}

@Composable
fun AlertItem(
    onClick: (() -> Unit),
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(ComposeAppTheme.colors.lawrence)
            .clickable { onClick.invoke() },
        contentAlignment = Alignment.Center
    ) {
        HsDivider(modifier = Modifier.align(Alignment.TopCenter))

        content.invoke()
    }
}
