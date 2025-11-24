package io.horizontalsystems.bankwallet.uiv3.components.section

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.components.subheadSB_andy

@Composable
fun SectionHeader(
    title: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .padding(top = 24.dp, start = 16.dp, end = 16.dp),
    ) {
        subheadSB_andy(text = title)
    }
}