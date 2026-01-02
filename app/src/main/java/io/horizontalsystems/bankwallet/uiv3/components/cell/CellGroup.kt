package io.horizontalsystems.bankwallet.uiv3.components.cell

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun CellGroup(
    paddingValues: PaddingValues = PaddingValues.Zero,
    content: @Composable (ColumnScope.() -> Unit)
) {
    Column(
        modifier = Modifier
            .padding(paddingValues = paddingValues)
            .border(1.dp, ComposeAppTheme.colors.blade, RoundedCornerShape(16.dp))
            .padding(vertical = 8.dp),
        content = content
    )
}
