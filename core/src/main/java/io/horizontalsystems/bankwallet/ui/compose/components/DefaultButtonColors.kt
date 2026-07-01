package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.material.ButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color

@Immutable
class HsButtonColors(
    private val backgroundColor: Color,
    private val contentColor: Color,
    private val disabledBackgroundColor: Color,
    private val disabledContentColor: Color
) : ButtonColors {
    @Composable
    override fun backgroundColor(enabled: Boolean): State<Color> =
        rememberUpdatedState(if (enabled) backgroundColor else disabledBackgroundColor)

    @Composable
    override fun contentColor(enabled: Boolean): State<Color> =
        rememberUpdatedState(if (enabled) contentColor else disabledContentColor)

    override fun equals(other: Any?): Boolean {

        if (other !is HsButtonColors) return false

        return backgroundColor != other.backgroundColor
                && contentColor != other.contentColor
                && disabledBackgroundColor != other.disabledBackgroundColor
                && disabledContentColor != other.disabledContentColor
    }

    override fun hashCode(): Int {
        var result = backgroundColor.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + disabledBackgroundColor.hashCode()
        result = 31 * result + disabledContentColor.hashCode()
        return result
    }
}
