package cash.p.terminal.ui.compose.components

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
import cash.p.terminal.ui.compose.Select
import cash.p.terminal.strings.helpers.WithTranslatableTitle
import cash.p.terminal.ui_compose.components.subhead1_grey
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

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
                        color = if (option == select.selected) cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.jacob else cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.leah,
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
        subhead1_grey(stringResource(title))
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
