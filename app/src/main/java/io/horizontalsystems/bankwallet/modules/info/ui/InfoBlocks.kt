package io.horizontalsystems.bankwallet.modules.info.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun InfoSubHeader(
    text: Int,
    modifier: Modifier = Modifier
) {
    Spacer(Modifier.height(12.dp))
    Text(
        modifier = modifier,
        text = stringResource(text),
        style = ComposeAppTheme.typography.headline2,
        color = ComposeAppTheme.colors.jacob
    )
    Spacer(Modifier.height(12.dp))
}

@Composable
fun InfoBodyString(
    text: String,
    modifier: Modifier = Modifier
) {
    Spacer(Modifier.height(12.dp))
    Text(
        modifier = modifier,
        text = text,
        style = ComposeAppTheme.typography.body,
        color = ComposeAppTheme.colors.bran
    )
    Spacer(Modifier.height(24.dp))
}

@Composable
fun InfoBody(
    text: Int,
    modifier: Modifier = Modifier
) {
    InfoBodyString(stringResource(text), modifier)
}

@Composable
fun InfoHeader(
    text: Int,
    modifier: Modifier = Modifier
) {
    Spacer(Modifier.height(5.dp))
    Text(
        modifier = modifier,
        text = stringResource(text),
        style = ComposeAppTheme.typography.title2,
        color = ComposeAppTheme.colors.leah
    )
    Spacer(Modifier.height(8.dp))
    Divider(
        modifier = modifier,
        thickness = 1.dp,
        color = ComposeAppTheme.colors.grey50
    )
    Spacer(Modifier.height(5.dp))
}
