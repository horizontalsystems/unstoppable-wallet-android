package io.horizontalsystems.bankwallet.uiv3.components.cell

import androidx.annotation.IntDef
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@IntDef(20, 24, 32, 48)
annotation class ImageSize

@Composable
fun CellLeftImage(
    painter: Painter,
    contentDescription: String? = null,
    type: ImageType,
    @ImageSize
    size: Int
) {
    val clipShape = when (type) {
        ImageType.Ellipse -> CircleShape
        ImageType.Rectangle -> {
            val radius = when (size) {
                20 -> 4
                24 -> 4
                32 -> 6
                48 -> 8
                else -> 0
            }

            RoundedCornerShape(radius.dp)
        }
    }

    Image(
        modifier = Modifier
            .size(size.dp)
            .clip(clipShape),
        painter = painter,
        contentDescription = contentDescription
    )
}

enum class ImageType {
    Ellipse, Rectangle
}

@Preview
@Composable
fun Preview_CellLeftImage() {
    ComposeAppTheme {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val sizes = listOf(20, 24, 32, 48)

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                sizes.forEach {
                    CellLeftImage(
                        painter = painterResource(R.drawable.ic_app_logo_72),
                        type = ImageType.Ellipse,
                        size = it
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                sizes.forEach {
                    CellLeftImage(
                        painter = painterResource(R.drawable.ic_app_logo_72),
                        type = ImageType.Rectangle,
                        size = it
                    )
                }
            }
        }
    }
}
