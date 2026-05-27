package cash.p.terminal.modules.calculator

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import cash.p.terminal.R
import io.horizontalsystems.core.ui.dialogs.ConfirmationDialogBottomSheet

@Composable
@Suppress("ModifierMissing")
internal fun CalculatorModePushNotificationsWarningDialog(
    onCloseClick: () -> Unit,
    onDisablePushClick: () -> Unit,
    onKeepPushClick: () -> Unit,
    visible: Boolean,
) {
    if (!visible) return

    ConfirmationDialogBottomSheet(
        title = stringResource(R.string.push_notification),
        icon = R.drawable.icon_24_warning_2,
        warningTitle = null,
        warningText = stringResource(R.string.calculator_mode_push_notifications_warning),
        actionButtonTitle = stringResource(R.string.button_disable),
        transparentButtonTitle = stringResource(R.string.button_do_not_disable),
        onCloseClick = onCloseClick,
        onActionButtonClick = onDisablePushClick,
        onTransparentButtonClick = onKeepPushClick,
    )
}
