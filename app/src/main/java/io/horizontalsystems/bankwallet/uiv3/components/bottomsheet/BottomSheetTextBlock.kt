package io.horizontalsystems.bankwallet.uiv3.components.bottomsheet

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey

@Composable
fun BottomSheetTextBlock(text: String) {
    body_grey(
        text = text,
        modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp),
        textAlign = TextAlign.Center
    )
}


@Preview
@Composable
fun Preview_BottomSheetTextBlock() {
    ComposeAppTheme {
        BottomSheetTextBlock("This action will change your receive address format for Bitcoin in Unstoppable app. After that, the app will resync itself with Bitcoin blockchain.")
    }
}
