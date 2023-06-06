package io.horizontalsystems.bankwallet.ui.compose.components

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
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.contacts.ChooseContactFragment
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.bankwallet.ui.compose.ColoredTextStyle
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.core.getNavigationResult
import io.horizontalsystems.marketkit.models.BlockchainType

@Composable
fun FormsInputAddress(
    modifier: Modifier = Modifier,
    initial: String? = null,
    hint: String,
    state: DataState<Address>? = null,
    textPreprocessor: TextPreprocessor = TextPreprocessorImpl,
    onChangeFocus: (Boolean) -> Unit,
    navController: NavController,
    blockchainType: BlockchainType,
    chooseContactEnable: Boolean,
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

            BasicTextField(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        onChangeFocus.invoke(it.isFocused)
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .weight(1f),
                enabled = true,
                value = textState,
                onValueChange = { textFieldValue ->
                    val textFieldValueProcessed =
                        textFieldValue.copy(text = textPreprocessor.process(textFieldValue.text))

                    val text = textFieldValueProcessed.text
                    textState = textFieldValueProcessed
                    onValueChange.invoke(text)
                },
                textStyle = ColoredTextStyle(
                    color = ComposeAppTheme.colors.leah,
                    textStyle = ComposeAppTheme.typography.body
                ),
                singleLine = false,
                cursorBrush = SolidColor(ComposeAppTheme.colors.jacob),
                decorationBox = { innerTextField ->
                    if (textState.text.isEmpty()) {
                        Text(
                            hint,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            color = ComposeAppTheme.colors.grey50,
                            style = ComposeAppTheme.typography.body
                        )
                    }
                    innerTextField()
                },
                visualTransformation = VisualTransformation.None,
                keyboardOptions = KeyboardOptions.Default,
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
                if (chooseContactEnable) {
                    ButtonSecondaryCircle(
                        modifier = Modifier.padding(end = 8.dp),
                        icon = R.drawable.ic_user_20,
                        onClick = {
                            navController.getNavigationResult(ChooseContactFragment.resultKey) {
                                val chosenAddress = it.getString("contact") ?: ""

                                val textProcessed = textPreprocessor.process(chosenAddress)
                                textState = textState.copy(
                                    text = textProcessed,
                                    selection = TextRange(textProcessed.length)
                                )
                                onValueChange.invoke(textProcessed)
                            }
                            navController.slideFromRight(
                                R.id.chooseContact, ChooseContactFragment.prepareParams(blockchainType)
                            )
                        }
                    )
                }

                val qrScannerLauncher =
                    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                        if (result.resultCode == Activity.RESULT_OK) {
                            val scannedText =
                                result.data?.getStringExtra(ModuleField.SCAN_ADDRESS) ?: ""

                            val textProcessed = textPreprocessor.process(scannedText)
                            textState = textState.copy(
                                text = textProcessed,
                                selection = TextRange(textProcessed.length)
                            )
                            onValueChange.invoke(textProcessed)
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
                    modifier = Modifier.padding(end = 16.dp),
                    title = stringResource(id = R.string.Send_Button_Paste),
                    onClick = {
                        clipboardManager.getText()?.text?.let { textInClipboard ->
                            val textProcessed = textPreprocessor.process(textInClipboard)
                            textState = textState.copy(
                                text = textProcessed,
                                selection = TextRange(textProcessed.length)
                            )
                            onValueChange.invoke(textProcessed)
                        }
                    },
                )
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