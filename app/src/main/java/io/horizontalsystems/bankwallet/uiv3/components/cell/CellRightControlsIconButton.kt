package io.horizontalsystems.bankwallet.uiv3.components.cell

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonSize
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSIconButtonTinted

@Composable
fun CellRightControlsIconButton(
    icon: Int,
    iconTint: Color,
    onClick: () -> Unit,
) {
    HSIconButtonTinted(
        variant = ButtonVariant.Secondary,
        size = ButtonSize.Small,
        icon = painterResource(icon),
        iconTint = iconTint,
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
