package cash.p.terminal.modules.send

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
fun SendScreen(
    title: String,
    onCloseClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
        AppBar(
            title = title,
            navigationIcon = {
                HsBackButton(onClick = onCloseClick)
            },
            menuItems = listOf()
        )

        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            content.invoke(this)
        }
    }
}