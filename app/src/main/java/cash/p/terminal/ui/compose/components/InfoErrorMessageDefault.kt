package cash.p.terminal.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
fun InfoErrorMessageDefault(painter: Painter, text: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    color = ComposeAppTheme.colors.raina,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(48.dp),
                painter = painter,
                contentDescription = "",
                tint = ComposeAppTheme.colors.grey
            )
        }
        VSpacer(height = 32.dp)
        subhead2_grey(
            text = text,
            textAlign = TextAlign.Center
        )
    }
}

@Preview
@Composable
fun InfoErrorMessageDefaultPreview() {
    cash.p.terminal.ui_compose.theme.ComposeAppTheme(darkTheme = true) {
        InfoErrorMessageDefault(
            painter = painterResource(id = R.drawable.ic_wallet_48),
            text = "You don't have any wallets"
        )
    }
}
