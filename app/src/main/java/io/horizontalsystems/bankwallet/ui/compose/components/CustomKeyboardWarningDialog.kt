package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun CustomKeyboardWarningDialog(
    onSelect: () -> Unit,
    onSkip: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(
        onDismissRequest = onCancel
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ComposeAppTheme.colors.lawrence)
        ) {
            BottomSheetsElementsHeader(
                icon = painterResource(R.drawable.icon_24_warning_2),
                title = stringResource(R.string.Alert_TitleWarning),
                subtitle = stringResource(R.string.Keyboard),
                onClickClose = onCancel
            )
            BottomSheetsElementsText(
                text = stringResource(R.string.Alert_CustomKeyboardIsUsed)
            )
            BottomSheetsElementsButtons(
                buttonPrimaryText = stringResource(id = R.string.Alert_Select),
                onClickPrimary = onSelect,
                buttonDefaultText = stringResource(id = R.string.Alert_Skip),
                onClickDefault = onSkip
            )
        }
    }

}
