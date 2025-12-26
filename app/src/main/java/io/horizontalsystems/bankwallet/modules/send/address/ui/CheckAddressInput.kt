package io.horizontalsystems.bankwallet.modules.send.address.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.bankwallet.ui.compose.ColoredTextStyle
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInputStateWarning

@Composable
fun CheckAddressInput(
    modifier: Modifier = Modifier,
    value: String,
    hint: String,
    state: DataState<Address>? = null,
    onValueChange: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    val borderColor = when (state) {
        is DataState.Error -> {
            if (state.error is FormsInputStateWarning) {
                ComposeAppTheme.colors.yellow50
            } else {
                ComposeAppTheme.colors.red50
            }
        }
        else -> ComposeAppTheme.colors.blade
    }

    Column(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 44.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(0.5.dp, borderColor, RoundedCornerShape(16.dp))
                .background(ComposeAppTheme.colors.lawrence),
            verticalAlignment = Alignment.CenterVertically
        ) {

            BasicTextField(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .weight(1f),
                enabled = true,
                value = value,
                onValueChange = { value ->
                    onValueChange.invoke(value.trim())
                },
                textStyle = ColoredTextStyle(
                    color = ComposeAppTheme.colors.leah,
                    textStyle = ComposeAppTheme.typography.body
                ),
                singleLine = false,
                cursorBrush = SolidColor(ComposeAppTheme.colors.leah),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            hint,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            color = ComposeAppTheme.colors.andy,
                            style = ComposeAppTheme.typography.body
                        )
                    }
                    innerTextField()
                },
                visualTransformation = VisualTransformation.None,
                keyboardOptions = KeyboardOptions.Default,
            )

            Spacer(modifier = Modifier.width(28.dp))

            if (value.isNotEmpty()) {
                ButtonSecondaryCircle(
                    modifier = Modifier.padding(end = 16.dp),
                    icon = R.drawable.ic_delete_20,
                    onClick = {
                        onValueChange.invoke("")
                        focusRequester.requestFocus()
                    }
                )
            } else {
                val qrScannerLauncher =
                    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                        if (result.resultCode == Activity.RESULT_OK) {
                            val scannedText =
                                result.data?.getStringExtra(ModuleField.SCAN_ADDRESS) ?: ""

                            onValueChange.invoke(scannedText)
                        }
                    }

                ButtonSecondaryCircle(
                    modifier = Modifier.padding(end = 8.dp),
                    icon = R.drawable.ic_qr_scan_20,
                    onClick = {
                        qrScannerLauncher.launch(QRScannerActivity.getScanQrIntent(context))
                    }
                )

                val clipboardManager = LocalClipboardManager.current
                ButtonSecondaryDefault(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .height(28.dp),
                    title = stringResource(id = R.string.Send_Button_Paste),
                    onClick = {
                        clipboardManager.getText()?.text?.let { textInClipboard ->
                            onValueChange.invoke(textInClipboard)
                        }
                    },
                )
            }
        }
    }
}