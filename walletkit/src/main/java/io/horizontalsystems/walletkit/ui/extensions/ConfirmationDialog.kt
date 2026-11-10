package io.horizontalsystems.walletkit.ui.extensions

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.components.TextImportantWarning
import io.horizontalsystems.walletkit.uiv3.components.controls.HSButton
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonStyle

@Composable
fun ConfirmationDialog(
    title: String,
    icon: Int?,
    warningTitle: String?,
    warningText: String?,
    actionButtonTitle: String?,
    transparentButtonTitle: String?,
    onClose: () -> Unit,
    actionButtonXxx: () -> Unit,
    transparentButtonXxx: () -> Unit
) {
    BottomSheetHeader(
        iconPainter = painterResource(icon ?: R.drawable.ic_attention_24),
        iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob),
        title = title,
        onCloseClick = onClose
    ) {

        warningText?.let {
            TextImportantWarning(
                title = warningTitle,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                text = it
            )
            Spacer(Modifier.height(8.dp))
        }
        actionButtonTitle?.let {
            HSButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 12.dp, end = 24.dp),
                title = actionButtonTitle,
                onClick = {
                    actionButtonXxx()
                    onClose()
                }
            )
        }
        transparentButtonTitle?.let {
            HSButton(
                variant = ButtonVariant.Secondary,
                style = ButtonStyle.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 12.dp, end = 24.dp),
                title = transparentButtonTitle,
                onClick = {
                    transparentButtonXxx()
                    onClose()
                }
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}
