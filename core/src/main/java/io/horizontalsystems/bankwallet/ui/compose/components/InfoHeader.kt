package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun InfoH3(text: String) {
    headline2_jacob(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        text = text
    )
}

@Composable
fun InfoH1(text: String) {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
    ){
        Text(
            text = text,
            style = ComposeAppTheme.typography.title2М,
            color = ComposeAppTheme.colors.leah
        )
        Spacer(Modifier.height(8.dp))
        HsDivider()
    }
}
