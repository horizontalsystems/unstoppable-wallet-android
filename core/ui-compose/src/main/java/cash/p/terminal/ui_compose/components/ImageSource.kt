package cash.p.terminal.ui_compose.components

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import cash.p.terminal.ui_compose.R
import coil.compose.rememberAsyncImagePainter

sealed class ImageSource {
    class Local(@DrawableRes val resId: Int) : ImageSource()
    class Remote(
        val url: String,
        @DrawableRes
        val placeholder: Int = R.drawable.ic_placeholder,
        val alternativeUrl: String? = null
    ) : ImageSource()

    @Composable
    fun painter(): Painter = when (this) {
        is Local -> painterResource(resId)
        is Remote -> rememberAsyncImagePainter(
            model = url,
            error = alternativeUrl?.let {
                rememberAsyncImagePainter(
                    model = alternativeUrl,
                    error = painterResource(placeholder)
                )
            } ?: painterResource(placeholder)
        )
    }
}