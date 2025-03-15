package io.horizontalsystems.chartview

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultFilterQuality
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImagePainter.State
import coil.compose.rememberAsyncImagePainter

@Composable
fun rememberAsyncImagePainterWithFallback(
    model: Any?,
    placeholder: Painter? = null,
    error: Painter? = null,
    onLoading: ((State.Loading) -> Unit)? = null,
    onSuccess: ((State.Success) -> Unit)? = null,
    onError: ((State.Error) -> Unit)? = null,
    contentScale: ContentScale = ContentScale.Fit,
    filterQuality: FilterQuality = DefaultFilterQuality,
) = rememberAsyncImagePainter(
    model = model,
    placeholder = placeholder,
    error = if (model is String) {
        rememberAsyncImagePainter(
            model = model.getAlternativeImageUrl(),
            error = painterResource(R.drawable.ic_platform_placeholder_32)
        )
    } else {
        error
    },
    onLoading = onLoading,
    onSuccess = onSuccess,
    onError = onError,
    contentScale = contentScale,
    filterQuality = filterQuality,
)

private fun String.getAlternativeImageUrl(): String {
    val uid = extractUid()
    return "https://p.cash/storage/coins/$uid/image.png"
}

private fun String.extractUid(): String {
    var slashPos = -1
    var endPos = this.length

    for (i in this.length - 1 downTo 0) {
        val char = this[i]

        if (char == '@' || char == '.') {
            endPos = i
        } else if (char == '/' && endPos != this.length) {
            slashPos = i
            break
        }
    }

    return if (slashPos != -1) {
        this.substring(slashPos + 1, endPos)
    } else {
        ""
    }
}