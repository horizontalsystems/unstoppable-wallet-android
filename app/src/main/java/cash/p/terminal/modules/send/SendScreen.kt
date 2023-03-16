package cash.p.terminal.modules.send

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.core.iconPlaceholder
import cash.p.terminal.core.imageUrl
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.CoinImage
import cash.p.terminal.ui.compose.components.MenuItem
import io.horizontalsystems.marketkit.models.FullCoin

@Composable
fun SendScreen(
    fullCoin: FullCoin,
    onCloseClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
        AppBar(
            title = TranslatableString.ResString(R.string.Send_Title, fullCoin.coin.code),
            navigationIcon = {
                CoinImage(
                    iconUrl = fullCoin.coin.imageUrl,
                    placeholder = fullCoin.iconPlaceholder,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .size(24.dp)
                )
            },
            menuItems = listOf(
                MenuItem(
                    title = TranslatableString.ResString(R.string.Button_Close),
                    icon = R.drawable.ic_close,
                    onClick = onCloseClick
                )
            )
        )

        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            content.invoke(this)
        }
    }
}