package io.horizontalsystems.walletkit.uiv3.components.cell

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.designkit.theme.ComposeAppTheme
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.uiv3.components.controls.FilledCheckMark

@Composable
fun CellLeftSelectors(
    selected: Boolean
) {
    if (selected) {
        FilledCheckMark(size = 24.dp)
    } else {
        Image(
            modifier = Modifier.size(24.dp),
            painter = painterResource(R.drawable.selector_unchecked_20),
            contentDescription = null,
        )
    }
}

@Preview
@Composable
fun Prev_CellLeftSelectors() {
    ComposeAppTheme {
        CellLeftSelectors(
           selected = true
        )
    }
}
