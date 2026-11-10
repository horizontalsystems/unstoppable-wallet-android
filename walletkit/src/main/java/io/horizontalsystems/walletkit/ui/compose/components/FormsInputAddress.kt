package io.horizontalsystems.walletkit.ui.compose.components

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.designkit.components.ButtonSize
import io.horizontalsystems.designkit.components.ButtonVariant
import io.horizontalsystems.designkit.components.HSButton
import io.horizontalsystems.designkit.components.HSIconButton
import io.horizontalsystems.designkit.components.HSInput
import io.horizontalsystems.designkit.theme.ComposeAppTheme
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.utils.ModuleField
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.entities.DataState
import io.horizontalsystems.walletkit.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.walletkit.ui.helpers.TextHelper

@Composable
fun FormsInputAddress(
    modifier: Modifier = Modifier,
    value: String,
    hint: String,
    state: DataState<Address>? = null,
    showStateIcon: Boolean = true,
    textPreprocessor: TextPreprocessor = TextPreprocessorImpl,
    onValueChange: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    HSInput(
        modifier = modifier,
        value = value,
        onValueChange = { text ->
            onValueChange.invoke(textPreprocessor.process(text))
        },
        hint = hint,
        state = state.toInputState(),
        description = state?.errorOrNull?.localizedMessage,
        textColor = when (state) {
            is DataState.Error -> ComposeAppTheme.colors.lucian
            else -> ComposeAppTheme.colors.leah
        },
        textFieldModifier = Modifier.focusRequester(focusRequester),
    ) {
        if (showStateIcon || state is DataState.Loading) {
            InputStatusIcon(state)
        }

        if (value.isNotEmpty()) {
            HSIconButton(
                variant = ButtonVariant.Secondary,
                size = ButtonSize.Small,
                icon = painterResource(R.drawable.ic_delete_20),
                onClick = {
                    val text = textPreprocessor.process("")
                    onValueChange.invoke(text)
                    focusRequester.requestFocus()
                }
            )
        } else {
            val qrScannerLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        val scannedText =
                            result.data?.getStringExtra(ModuleField.SCAN_ADDRESS) ?: ""
                        val textProcessed = textPreprocessor.process(scannedText)
                        onValueChange.invoke(textProcessed)
                    }
                }
            HSIconButton(
                variant = ButtonVariant.Secondary,
                size = ButtonSize.Small,
                icon = painterResource(R.drawable.ic_qr_scan_20),
                onClick = {
                    qrScannerLauncher.launch(QRScannerActivity.getScanQrIntent(context))
                }
            )
            HSButton(
                variant = ButtonVariant.Secondary,
                size = ButtonSize.Small,
                title = stringResource(id = R.string.Send_Button_Paste),
                onClick = {
                    TextHelper.getCopiedText()?.let { textInClipboard ->
                        val textProcessed = textPreprocessor.process(textInClipboard)
                        onValueChange.invoke(textProcessed)
                    }
                },
            )
        }
    }
}