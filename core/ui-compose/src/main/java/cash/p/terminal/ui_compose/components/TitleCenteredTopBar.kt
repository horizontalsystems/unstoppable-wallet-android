package cash.p.terminal.ui_compose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import cash.p.terminal.ui_compose.R
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleCenteredTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onCloseClick: () -> Unit
) {
    Box(
        modifier = modifier
            .windowInsetsPadding(TopAppBarDefaults.windowInsets)
            .height(64.dp)
            .fillMaxWidth(),
    ) {
        headline1_leah(
            text = title,
            modifier = Modifier.align(Alignment.Center)
        )
        HsIconButton(
            modifier = Modifier.align(Alignment.CenterEnd),
            onClick = onCloseClick
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_close_24),
                contentDescription = "close button",
                tint = ComposeAppTheme.colors.grey,
            )
        }
    }
}
