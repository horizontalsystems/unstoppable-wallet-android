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
fun TorEnableNotificationsDialog(onEnable: () -> Unit, onCancel: () -> Unit) {
    Dialog(onDismissRequest = onCancel) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ComposeAppTheme.colors.lawrence)
        ) {
            BottomSheetsElementsHeader(
                icon = painterResource(R.drawable.ic_tor_connection_24),
                title = stringResource(R.string.Tor_Title),
                subtitle = stringResource(R.string.Tor_Connection_Title),
                onClickClose = onCancel
            )
            BottomSheetsElementsText(
                text = stringResource(R.string.SettingsSecurity_NotificationsDisabledWarning)
            )
            BottomSheetsElementsButtons(
                buttonPrimaryText = stringResource(R.string.Button_Enable),
                onClickPrimary = onEnable
            )
        }
    }
}
