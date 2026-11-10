package io.horizontalsystems.walletkit.ui.compose.components

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.horizontalsystems.designkit.components.ButtonSize
import io.horizontalsystems.designkit.components.ButtonVariant
import io.horizontalsystems.designkit.components.HSButton
import io.horizontalsystems.designkit.components.HSIconButton
import io.horizontalsystems.designkit.components.HSInput
import io.horizontalsystems.designkit.components.InputState
import io.horizontalsystems.designkit.components.InputType
import io.horizontalsystems.designkit.theme.ComposeAppTheme
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.utils.ModuleField
import io.horizontalsystems.walletkit.entities.DataState
import io.horizontalsystems.walletkit.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.walletkit.ui.helpers.TextHelper

@Composable
fun FormsInput(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    initial: String? = null,
    hint: String,
    prefix: String? = null,
    textColor: Color = ComposeAppTheme.colors.leah,
    textStyle: TextStyle = ComposeAppTheme.typography.body,
    hintColor: Color = ComposeAppTheme.colors.andy,
    hintStyle: TextStyle = ComposeAppTheme.typography.body,
    singleLine: Boolean = false,
    state: DataState<Any>? = null,
    qrScannerEnabled: Boolean = false,
    pasteEnabled: Boolean = true,
    maxLength: Int? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    textPreprocessor: TextPreprocessor = TextPreprocessorImpl,
    onChangeFocus: ((Boolean) -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onValueChange: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    var textState by rememberSaveable(initial, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initial ?: "", TextRange(initial?.length ?: 0)))
    }

    HSInput(
        modifier = modifier,
        value = textState,
        onValueChange = { textFieldValue ->
            val textFieldValueProcessed = textFieldValue.copy(text = textPreprocessor.process(textFieldValue.text))

            val text = textFieldValueProcessed.text
            if (maxLength == null || text.length <= maxLength) {
                textState = textFieldValueProcessed
                onValueChange.invoke(text)
            } else {
                // Need to set textState to new instance of TextFieldValue with the same values
                // Otherwise it getting set to empty string
                textState = TextFieldValue(text = textState.text, selection = textState.selection)
            }
        },
        hint = hint,
        state = state.toInputState(),
        description = state?.errorOrNull?.localizedMessage,
        enabled = enabled,
        singleLine = singleLine,
        textStyle = textStyle,
        textColor = textColor,
        hintStyle = hintStyle,
        hintColor = hintColor,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        textFieldModifier = Modifier
            .focusRequester(focusRequester)
            .onFocusChanged {
                onChangeFocus?.invoke(it.isFocused)
            },
        leading = prefix?.let { { body_grey(text = it) } },
    ) {
        InputStatusIcon(state)

        if (trailingContent != null) {
            trailingContent()
        } else if (textState.text.isNotEmpty()) {
            HSIconButton(
                variant = ButtonVariant.Secondary,
                size = ButtonSize.Small,
                icon = painterResource(R.drawable.ic_delete_20),
                onClick = {
                    val text = textPreprocessor.process("")
                    textState = textState.copy(text = text, selection = TextRange(0))
                    onValueChange.invoke(text)
                    focusRequester.requestFocus()
                }
            )
        } else {
            if (qrScannerEnabled) {
                val qrScannerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        val scannedText = result.data?.getStringExtra(ModuleField.SCAN_ADDRESS) ?: ""

                        val textProcessed = textPreprocessor.process(scannedText)
                        textState = textState.copy(text = textProcessed, selection = TextRange(textProcessed.length))
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
            }

            if (pasteEnabled) {
                HSButton(
                    variant = ButtonVariant.Secondary,
                    size = ButtonSize.Small,
                    title = stringResource(id = R.string.Send_Button_Paste),
                    onClick = {
                        TextHelper.getCopiedText()?.let { textInClipboard ->
                            val textProcessed = textPreprocessor.process(textInClipboard)
                            textState = textState.copy(text = textProcessed, selection = TextRange(textProcessed.length))
                            onValueChange.invoke(textProcessed)
                        }
                    },
                )
            }
        }
    }
}

internal fun DataState<*>?.toInputState() = when {
    this !is DataState.Error -> InputState.Default
    error is FormsInputStateWarning -> InputState.Warning
    else -> InputState.Error
}

/** The progress and success marks an input shows before its buttons. */
@Composable
internal fun InputStatusIcon(state: DataState<*>?) {
    when (state) {
        is DataState.Loading -> {
            HSCircularProgressIndicator()
        }

        is DataState.Success -> {
            Icon(
                painter = painterResource(id = R.drawable.ic_check_20),
                contentDescription = null,
                tint = ComposeAppTheme.colors.remus
            )
        }

        else -> {}
    }
}

@Composable
fun FormsInputPassword(
    modifier: Modifier = Modifier,
    hint: String,
    textColor: Color = ComposeAppTheme.colors.leah,
    textStyle: TextStyle = ComposeAppTheme.typography.body,
    hintColor: Color = ComposeAppTheme.colors.andy,
    hintStyle: TextStyle = ComposeAppTheme.typography.body,
    singleLine: Boolean = true,
    state: DataState<Any>? = null,
    maxLength: Int? = null,
    hide: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    enabled: Boolean = true,
    onValueChange: (String) -> Unit,
    onToggleHide: () -> Unit
) {
    var textState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }

    HSInput(
        modifier = modifier,
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
        hint = hint,
        state = state.toInputState(),
        description = state?.errorOrNull?.localizedMessage,
        enabled = enabled,
        singleLine = singleLine,
        textStyle = textStyle,
        textColor = textColor,
        hintStyle = hintStyle,
        hintColor = hintColor,
        visualTransformation = if (hide) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = keyboardOptions,
    ) {
        Icon(
            modifier = Modifier
                .size(20.dp)
                .clickable(onClick = onToggleHide, interactionSource = MutableInteractionSource(), indication = null),
            painter = painterResource(id = if (hide) R.drawable.ic_eye_off_20 else R.drawable.ic_eye_20),
            contentDescription = null,
            tint = ComposeAppTheme.colors.grey
        )
    }
}

@Composable
fun FormsInputMultiline(
    modifier: Modifier = Modifier,
    initial: String? = null,
    enabled: Boolean = true,
    hint: String,
    textColor: Color = ComposeAppTheme.colors.leah,
    textStyle: TextStyle = ComposeAppTheme.typography.body,
    hintColor: Color = ComposeAppTheme.colors.andy,
    hintStyle: TextStyle = ComposeAppTheme.typography.body,
    state: DataState<Any>? = null,
    pasteEnabled: Boolean = true,
    qrScannerEnabled: Boolean = false,
    textPreprocessor: TextPreprocessor = TextPreprocessorImpl,
    onChangeFocus: ((Boolean) -> Unit)? = null,
    maxLength: Int? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onValueChange: (String) -> Unit,
    onClear: (() -> Unit)? = null,
    onPaste: (() -> Unit)? = null,
    onScanQR: (() -> Unit)? = null
) {
    val context = LocalContext.current

    var textState by rememberSaveable(initial, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initial ?: "", selection = TextRange((initial ?: "").length)))
    }

    HSInput(
        modifier = modifier,
        type = InputType.Area,
        value = textState,
        onValueChange = { textFieldValue ->
            val textFieldValueProcessed = textFieldValue.copy(text = textPreprocessor.process(textFieldValue.text))

            val text = textFieldValueProcessed.text
            if (maxLength == null || text.length <= maxLength) {
                textState = textFieldValueProcessed
                onValueChange.invoke(text)
            } else {
                // Need to set textState to new instance of TextFieldValue with the same values
                // Otherwise it getting set to empty string
                textState = TextFieldValue(text = textState.text, selection = textState.selection)
            }
        },
        hint = hint,
        state = state.toInputState(),
        description = state?.errorOrNull?.localizedMessage,
        enabled = enabled,
        textStyle = textStyle,
        textColor = textColor,
        hintStyle = hintStyle,
        hintColor = hintColor,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        textFieldModifier = Modifier.onFocusChanged {
            onChangeFocus?.invoke(it.isFocused)
        },
    ) {
        InputStatusIcon(state)

        if (textState.text.isNotEmpty()) {
            HSIconButton(
                variant = ButtonVariant.Secondary,
                size = ButtonSize.Small,
                icon = painterResource(R.drawable.ic_delete_20),
                onClick = {
                    val text = textPreprocessor.process("")
                    textState = textState.copy(text = text, selection = TextRange(0))
                    onValueChange.invoke(text)
                    onClear?.invoke()
                }
            )
        } else {
            if (qrScannerEnabled) {
                val qrScannerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        val scannedText = result.data?.getStringExtra(ModuleField.SCAN_ADDRESS) ?: ""

                        val textProcessed = textPreprocessor.process(scannedText)
                        textState = textState.copy(text = textProcessed, selection = TextRange(textProcessed.length))
                        onValueChange.invoke(textProcessed)
                    }
                }

                HSIconButton(
                    variant = ButtonVariant.Secondary,
                    size = ButtonSize.Small,
                    icon = painterResource(R.drawable.ic_qr_scan_20),
                    onClick = {
                        qrScannerLauncher.launch(QRScannerActivity.getScanQrIntent(context))
                        onScanQR?.invoke()
                    }
                )
            }

            if (pasteEnabled) {
                HSButton(
                    variant = ButtonVariant.Secondary,
                    size = ButtonSize.Small,
                    title = stringResource(id = R.string.Send_Button_Paste),
                    onClick = {
                        TextHelper.getCopiedText()?.let { textInClipboard ->
                            val textProcessed = textPreprocessor.process(textInClipboard)
                            textState = textState.copy(text = textProcessed, selection = TextRange(textProcessed.length))
                            onValueChange.invoke(textProcessed)
                        }
                        onPaste?.invoke()
                    },
                )
            }
        }
    }
}

class FormsInputStateWarning(override val message: String?) : Exception()

interface TextPreprocessor {
    fun process(text: String): String
}

object TextPreprocessorImpl : TextPreprocessor {
    override fun process(text: String) = text
}
