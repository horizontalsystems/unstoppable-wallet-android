package cash.p.terminal.modules.send

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.ui.compose.components.SuggestionsBarHeight
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
fun SendScreen(
    title: String,
    proceedEnabled: Boolean,
    onCloseClick: () -> Unit,
    onSendClick: () -> Unit,
    bottomOverlay: @Composable (BoxScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
        AppBar(
            title = title,
            navigationIcon = {
                HsBackButton(onClick = onCloseClick)
            },
            menuItems = listOf(
                MenuItem(
                    title = TranslatableString.ResString(R.string.Send_DialogProceed),
                    tint = ComposeAppTheme.colors.jacob,
                    enabled = proceedEnabled,
                    onClick = onSendClick
                )
            )
        )

        Box(modifier = Modifier.weight(1f)) {
            val bottomPadding = if (bottomOverlay != null) SuggestionsBarHeight else 0.dp
            Column(
                modifier = Modifier
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = bottomPadding)
            ) {
                content.invoke(this)
            }
            bottomOverlay?.invoke(this)
        }
    }
}
