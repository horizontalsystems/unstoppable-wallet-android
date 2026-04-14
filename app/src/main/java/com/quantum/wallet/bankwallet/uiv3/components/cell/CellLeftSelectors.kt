package com.quantum.wallet.bankwallet.uiv3.components.cell

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun CellLeftSelectors(
    selected: Boolean
) {
    val icon = if (selected) {
        R.drawable.selector_checked_20
    } else {
        R.drawable.selector_unchecked_20
    }

    Image(
        modifier = Modifier
            .size(24.dp),
        painter = painterResource(icon),
        contentDescription = null,
    )
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
