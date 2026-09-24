package io.horizontalsystems.walletkit.ui.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonColors
import androidx.compose.material.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme

@Composable
fun ButtonPrimaryWrapper(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    ProvideTextStyle(
        value = ComposeAppTheme.typography.headline2
    ) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(25.dp))
                .defaultMinSize(
                    minWidth = ButtonPrimaryDefaults.MinWidth,
                    minHeight = ButtonPrimaryDefaults.MinHeight
                )
                .clickable(
                    enabled = enabled,
                    onClick = onClick,
                )
                .padding(ButtonPrimaryDefaults.ContentPadding),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

object ButtonPrimaryDefaults {
    private val ButtonHorizontalPadding = 16.dp

    val ContentPadding = PaddingValues(
        start = ButtonHorizontalPadding,
        end = ButtonHorizontalPadding
    )

    /**
     * The default min width applied for the [Button].
     * Note that you can override it by applying Modifier.widthIn directly on [Button].
     */
    val MinWidth = 56.dp

    /**
     * The default min width applied for the [Button].
     * Note that you can override it by applying Modifier.heightIn directly on [Button].
     */
    val MinHeight = 56.dp

    @Composable
    fun textButtonColors(
        backgroundColor: Color,
        contentColor: Color,
        disabledBackgroundColor: Color,
        disabledContentColor: Color,
    ): ButtonColors = HsButtonColors(
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        disabledBackgroundColor = disabledBackgroundColor,
        disabledContentColor = disabledContentColor,
    )
}
