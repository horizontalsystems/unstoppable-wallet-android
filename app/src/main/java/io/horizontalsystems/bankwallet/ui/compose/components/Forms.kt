package io.horizontalsystems.bankwallet.ui.compose.components

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
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
    prefix: String? = null,
    textColor: Color = ComposeAppTheme.colors.leah,
    textStyle: TextStyle = ComposeAppTheme.typography.body,
    hintColor: Color = ComposeAppTheme.colors.grey50,
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
                mutableStateOf(TextFieldValue(initial ?: "", TextRange(initial?.length ?: 0)))
            }

            prefix?.let{
                body_grey(
                    modifier = Modifier.padding(start = 12.dp),
                    text = prefix
                )
            }

            BasicTextField(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        onChangeFocus?.invoke(it.isFocused)
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .weight(1f),
                enabled = enabled,
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
                },
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
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
                    modifier = Modifier.padding(end = 16.dp),
                    icon = R.drawable.ic_delete_20,
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

                    ButtonSecondaryCircle(
                        modifier = Modifier.padding(end = if(pasteEnabled) 8.dp else 16.dp),
                        icon = R.drawable.ic_qr_scan_20,
                        onClick = {
                            qrScannerLauncher.launch(QRScannerActivity.getScanQrIntent(context))
                        }
                    )
                }

                if (pasteEnabled) {
                    val clipboardManager = LocalClipboardManager.current
                    ButtonSecondaryDefault(
                        modifier = Modifier.padding(end = 16.dp),
                        title = stringResource(id = R.string.Send_Button_Paste),
                        onClick = {
                            clipboardManager.getText()?.text?.let { textInClipboard ->
                                val textProcessed = textPreprocessor.process(textInClipboard)
                                textState = textState.copy(text = textProcessed, selection = TextRange(textProcessed.length))
                                onValueChange.invoke(textProcessed)
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

@Composable
fun FormsInputPassword(
    modifier: Modifier = Modifier,
    hint: String,
    textColor: Color = ComposeAppTheme.colors.leah,
    textStyle: TextStyle = ComposeAppTheme.typography.body,
    hintColor: Color = ComposeAppTheme.colors.grey50,
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
            var textState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
                mutableStateOf(TextFieldValue(""))
            }

            BasicTextField(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .weight(1f),
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
                },
                visualTransformation = if (hide) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = keyboardOptions,
                enabled = enabled,
            )

            when (state) {
                is DataState.Error -> {
                    Icon(
                        modifier = Modifier.padding(end = 8.dp),
                        painter = painterResource(id = R.drawable.ic_attention_20),
                        contentDescription = null,
                        tint = cautionColor
                    )
                }
                else -> {
                    Spacer(modifier = Modifier.width(28.dp))
                }
            }

            Icon(
                modifier = Modifier
                    .size(20.dp)
                    .clickable(onClick = onToggleHide, interactionSource = MutableInteractionSource(), indication = null),
                painter = painterResource(id = if (hide) R.drawable.ic_eye_off_20 else R.drawable.ic_eye_20),
                contentDescription = null,
                tint = ComposeAppTheme.colors.grey
            )
            Spacer(Modifier.width(16.dp))
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

@Composable
fun FormsInputMultiline(
    modifier: Modifier = Modifier,
    initial: String? = null,
    enabled: Boolean = true,
    hint: String,
    textColor: Color = ComposeAppTheme.colors.leah,
    textStyle: TextStyle = ComposeAppTheme.typography.body,
    hintColor: Color = ComposeAppTheme.colors.grey50,
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                .background(ComposeAppTheme.colors.lawrence),
        ) {
            var textState by rememberSaveable(initial, stateSaver = TextFieldValue.Saver) {
                mutableStateOf(TextFieldValue(initial ?: ""))
            }

            Spacer(modifier = Modifier.height(12.dp))

            BasicTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 64.dp)
                    .onFocusChanged {
                        onChangeFocus?.invoke(it.isFocused)
                    }
                    .padding(horizontal = 16.dp),
                enabled = enabled,
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
                textStyle = ColoredTextStyle(
                    color = textColor,
                    textStyle = textStyle
                ),
                cursorBrush = SolidColor(ComposeAppTheme.colors.jacob),
                decorationBox = { innerTextField ->
                    if (textState.text.isEmpty()) {
                        Text(
                            hint,
                            overflow = TextOverflow.Ellipsis,
                            color = hintColor,
                            style = hintStyle
                        )
                    }
                    innerTextField()
                },
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
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

                    }
                }

                if (textState.text.isNotEmpty()) {
                    ButtonSecondaryCircle(
                        modifier = Modifier.padding(end = 16.dp),
                        icon = R.drawable.ic_delete_20,
                        onClick = {
                            val text = textPreprocessor.process("")
                            textState = textState.copy(text = text, selection = TextRange(0))
                            onValueChange.invoke(text)
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

                        ButtonSecondaryCircle(
                            modifier = Modifier.padding(end = if(pasteEnabled) 8.dp else 16.dp),
                            icon = R.drawable.ic_qr_scan_20,
                            onClick = {
                                qrScannerLauncher.launch(QRScannerActivity.getScanQrIntent(context))
                            }
                        )
                    }

                    if (pasteEnabled) {
                        val clipboardManager = LocalClipboardManager.current
                        ButtonSecondaryDefault(
                            modifier = Modifier.padding(end = 16.dp),
                            title = stringResource(id = R.string.Send_Button_Paste),
                            onClick = {
                                clipboardManager.getText()?.text?.let { textInClipboard ->
                                    val textProcessed = textPreprocessor.process(textInClipboard)
                                    textState = textState.copy(text = textProcessed, selection = TextRange(textProcessed.length))
                                    onValueChange.invoke(textProcessed)
                                }
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
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

interface TextPreprocessor {
    fun process(text: String): String
}

object TextPreprocessorImpl : TextPreprocessor {
    override fun process(text: String) = text
}
