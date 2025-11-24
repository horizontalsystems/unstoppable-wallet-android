package io.horizontalsystems.bankwallet.uiv3.components.cell

import androidx.compose.ui.graphics.Color
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString

data class HSString(
    val text: String,
    val colorOrigin: Color?,
    val dimmed: Boolean,
) {
    val color = if (dimmed) colorOrigin?.copy(alpha = 0.5f) else colorOrigin
}

val String.hs: HSString
    get() = HSString(this, null, false)

fun String.hs(color: Color? = null, dimmed: Boolean = false): HSString {
    return HSString(this, color, dimmed)
}

fun TranslatableString.hs(color: Color? = null, dimmed: Boolean = false): HSString {
    return HSString(this.toString(), color, dimmed)
}
