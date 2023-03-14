package io.horizontalsystems.bankwallet.modules.info.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.components.*

@Composable
fun InfoHeader(
    text: Int,
) {
    InfoH1(stringResource(text))
}

@Composable
fun InfoSubHeader(
    text: Int,
) {
    InfoH3(stringResource(text))
}

@Composable
fun InfoBody(
    text: Int,
) {
    InfoTextBody(stringResource(text))
}

@Composable
fun BulletedText(
    text: Int,
) {
    Row(
        modifier = Modifier.padding(start = 24.dp, end = 32.dp, top = 12.dp, bottom = 12.dp)
    ) {
        body_bran(
            modifier = Modifier.width(24.dp),
            text = "•"
        )
        HSpacer(16.dp)
        body_bran(
            modifier = Modifier.weight(1f),
            text = stringResource(text)
        )
    }
}
