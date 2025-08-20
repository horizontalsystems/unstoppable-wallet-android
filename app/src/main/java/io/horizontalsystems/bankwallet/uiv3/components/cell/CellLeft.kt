package io.horizontalsystems.bankwallet.uiv3.components.cell

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

@Composable
fun CellLeftImage(painter: Painter, contentDescription: String?) {
    Image(
        painter = painter,
        contentDescription = contentDescription
    )
}
