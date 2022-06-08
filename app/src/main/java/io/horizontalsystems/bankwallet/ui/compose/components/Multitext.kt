package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MultitextM1(title: String, subtitle: String) {
    Column {
        B2(text = title)
        Spacer(modifier = Modifier.height(1.dp))
        D1(text = subtitle)
    }
}
