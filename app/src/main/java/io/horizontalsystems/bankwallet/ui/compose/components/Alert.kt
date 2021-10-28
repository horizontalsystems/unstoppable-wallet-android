package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.WithTranslatableTitle

@Composable
fun <T : WithTranslatableTitle> AlertGroup(
    @StringRes title: Int,
    select: Select<T>,
    onSelect: (T) -> Unit,
    onDismiss: (() -> Unit)
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
                        color = if (option == select.selected) ComposeAppTheme.colors.jacob else ComposeAppTheme.colors.oz,
                        style = ComposeAppTheme.typography.body,
                    )
                }
            }
        }
    }
}

@Composable
fun AlertHeader(@StringRes title: Int) {
    Box(
        modifier = Modifier
            .height(40.dp)
            .fillMaxWidth()
            .background(ComposeAppTheme.colors.lawrence),
        contentAlignment = Alignment.Center
    ) {
        Text(
            stringResource(title),
            color = ComposeAppTheme.colors.grey,
            style = ComposeAppTheme.typography.subhead1,
        )
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
        Divider(
            thickness = 1.dp,
            color = ComposeAppTheme.colors.steel10,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        content.invoke()
    }
}
