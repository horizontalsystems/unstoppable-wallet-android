package io.horizontalsystems.walletkit.uiv3.components.cell

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonSize
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.walletkit.uiv3.components.controls.HSIconButtonTinted

@Composable
fun CellRightControlsIconButton(
    icon: Int,
    iconTint: Color,
    contentDescription: String? = null,
    onClick: () -> Unit,
) {
    HSIconButtonTinted(
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
