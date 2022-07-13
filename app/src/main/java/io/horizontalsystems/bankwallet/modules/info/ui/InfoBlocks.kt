package io.horizontalsystems.bankwallet.modules.info.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.bankwallet.ui.compose.components.InfoH1
import io.horizontalsystems.bankwallet.ui.compose.components.InfoH3
import io.horizontalsystems.bankwallet.ui.compose.components.InfoTextBody

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
