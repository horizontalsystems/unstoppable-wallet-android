package io.horizontalsystems.walletkit.uiv3.components.cell

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import io.horizontalsystems.designkit.components.ButtonSize
import io.horizontalsystems.designkit.components.ButtonVariant
import io.horizontalsystems.designkit.components.HSIconButton
import io.horizontalsystems.designkit.theme.ComposeAppTheme
import io.horizontalsystems.walletkit.R

@Composable
fun CellRightControlsIconButton(
    icon: Int,
    iconTint: Color,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    HSIconButton(
        modifier = modifier,
        variant = ButtonVariant.Secondary,
        size = ButtonSize.Small,
        icon = painterResource(icon),
        iconTint = iconTint,
        contentDescription = contentDescription,
        onClick = onClick
    )
}

@Preview
@Composable
fun Prev_CellRightControlsIconButton() {
    ComposeAppTheme {
        CellRightControlsIconButton(
            icon = R.drawable.copy_filled_24,
            iconTint = ComposeAppTheme.colors.leah
        ) {}
    }
}
