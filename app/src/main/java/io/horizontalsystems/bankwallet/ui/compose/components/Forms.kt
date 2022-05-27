package io.horizontalsystems.bankwallet.ui.compose.components

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.bankwallet.ui.compose.ColoredTextStyle
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun FormsInput(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    initial: String? = null,
    hint: String,
    textColor: Color = ComposeAppTheme.colors.oz,
    textStyle: TextStyle = ComposeAppTheme.typography.body,
    hintColor: Color = ComposeAppTheme.colors.grey50,
    hintStyle: TextStyle = ComposeAppTheme.typography.body,
    singleLine: Boolean = false,
    state: DataState<Any>? = null,
    qrScannerEnabled: Boolean = false,
    pasteEnabled: Boolean = true,
    maxLength: Int? = null,
    onChangeFocus: ((Boolean) -> Unit)? = null,
    onValueChange: (String) -> Unit
) {
    val context = LocalContext.current

    val borderColor = when (state) {
        is DataState.Error -> {
            if (state.error is FormsInputStateWarning) {
                ComposeAppTheme.colors.yellow50
            } else {
                ComposeAppTheme.colors.red50
            }
        }
        else -> ComposeAppTheme.colors.steel20
    }

    val cautionColor = if (state?.errorOrNull is FormsInputStateWarning) {
        ComposeAppTheme.colors.jacob
    } else {
        ComposeAppTheme.colors.lucian
    }

    Column(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 44.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                .background(ComposeAppTheme.colors.lawrence),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var textState by rememberSaveable(initial, stateSaver = TextFieldValue.Saver) {
                mutableStateOf(TextFieldValue(initial ?: ""))
            }

            BasicTextField(
                modifier = Modifier
                    .onFocusChanged {
                        onChangeFocus?.invoke(it.isFocused)
                    }
                    .padding(12.dp)
                    .weight(1f),
                enabled = enabled,
                value = textState,
                onValueChange = { textFieldValue ->
                    val text = textFieldValue.text
                    if (maxLength == null || text.length <= maxLength) {
                        textState = textFieldValue
                        onValueChange.invoke(text)
                    } else {
                        // Need to set textState to new instance of TextFieldValue with the same values
                        // Otherwise it getting set to empty string
                        textState = TextFieldValue(text = textState.text, selection = textState.selection)
                    }
                },
                textStyle = ColoredTextStyle(
                    color = textColor,
                    textStyle = textStyle
                ),
                singleLine = singleLine,
                cursorBrush = SolidColor(ComposeAppTheme.colors.jacob),
                decorationBox = { innerTextField ->
                    if (textState.text.isEmpty()) {
                        Text(
                            hint,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            color = hintColor,
                            style = hintStyle
                        )
                    }
                    innerTextField()
                }
            )

            when (state) {
                is DataState.Loading -> {
                    HSCircularProgressIndicator()
                }
                is DataState.Error -> {
                    Icon(
                        modifier = Modifier.padding(end = 8.dp),
                        painter = painterResource(id = R.drawable.ic_attention_20),
                        contentDescription = null,
                        tint = cautionColor
                    )
                }
                is DataState.Success -> {
                    Icon(
                        modifier = Modifier.padding(end = 8.dp),
                        painter = painterResource(id = R.drawable.ic_check_20),
                        contentDescription = null,
                        tint = ComposeAppTheme.colors.remus
                    )
                }
                else -> {
                    Spacer(modifier = Modifier.width(28.dp))
                }
            }

            if (textState.text.isNotEmpty()) {
                ButtonSecondaryCircle(
                    modifier = Modifier.padding(end = 8.dp),
                    icon = R.drawable.ic_delete_20,
                    onClick = {
                        textState = textState.copy(text = "")
                        onValueChange.invoke("")
                    }
                )
            } else {
                if (qrScannerEnabled) {
                    val qrScannerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                        if (result.resultCode == Activity.RESULT_OK) {
                            val scannedText = result.data?.getStringExtra(ModuleField.SCAN_ADDRESS) ?: ""

                            textState = textState.copy(text = scannedText, selection = TextRange(scannedText.length))
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
                }

                if (pasteEnabled) {
                    val clipboardManager = LocalClipboardManager.current
                    ButtonSecondaryDefault(
                        modifier = Modifier.padding(end = 8.dp),
                        title = stringResource(id = R.string.Send_Button_Paste),
                        onClick = {
                            clipboardManager.getText()?.text?.let { textInClipboard ->
                                textState = textState.copy(text = textInClipboard, selection = TextRange(textInClipboard.length))
                                onValueChange.invoke(textInClipboard)
                            }
                        },
                    )
                }
            }
        }

        state?.errorOrNull?.localizedMessage?.let {
            Text(
                modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp),
                text = it,
                color = cautionColor,
                style = ComposeAppTheme.typography.caption
            )
        }
    }
}

class FormsInputStateWarning(override val message: String?) : Exception()
