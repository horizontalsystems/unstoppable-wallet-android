package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.RestoreSettingsViewModel
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.ZCashConfig
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun ZCashBirthdayHeightDialogWrapper(restoreSettingsViewModel: RestoreSettingsViewModel) {
    if (restoreSettingsViewModel.openBirthdayAlertSignal != null) {
        ZCashBirthdayHeightDialog(
            onEnter = restoreSettingsViewModel::onEnter,
            onCancel = restoreSettingsViewModel::onCancelEnterBirthdayHeight
        )
    }
}

@Composable
fun ZCashBirthdayHeightDialog(
    onEnter: (ZCashConfig) -> Unit,
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
                icon = painterResource(R.drawable.logo_zcash_24),
                title = stringResource(R.string.Restore_BirthdayHeight),
                subtitle = stringResource(R.string.Restore_ZCash),
                onClickClose = onCancel
            )
            BottomSheetsElementsText(
                text = stringResource(R.string.Restore_ZCash_BirthdayHeight_Hint)
            )

            var zCashConfig by remember { mutableStateOf(ZCashConfig(null, false)) }
            BottomSheetsElementsInput {
                zCashConfig = zCashConfig.copy(birthdayHeight = it)
            }
            BottomSheetsElementsCheckbox(
                onCheckedChange = {
                    zCashConfig = zCashConfig.copy(restoreAsNew = it)
                }
            )
            BottomSheetsElementsButtons(
                buttonPrimaryText = stringResource(R.string.Button_Done),
                onClickPrimary = {
                    onEnter.invoke(zCashConfig)
                }
            )
        }
    }
}

@Preview
@Composable
fun ZCashBirthdayHeightDialogPreview() {
    ComposeAppTheme(darkTheme = false) {
        ZCashBirthdayHeightDialog(
            onEnter = {},
            onCancel = {}
        )
    }
}