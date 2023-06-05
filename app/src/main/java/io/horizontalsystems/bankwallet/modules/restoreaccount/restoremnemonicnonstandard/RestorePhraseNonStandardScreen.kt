package io.horizontalsystems.bankwallet.modules.restoreaccount.restoremnemonicnonstandard

import android.app.Activity
import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.displayNameStringRes
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.core.utils.Utils
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.createaccount.MnemonicLanguageCell
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.bankwallet.modules.restoreaccount.RestoreViewModel
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoremnemonic.SuggestionsBar
import io.horizontalsystems.bankwallet.ui.compose.*
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RestorePhraseNonStandard(
    mainViewModel: RestoreViewModel,
    openSelectCoinsScreen: () -> Unit,
    onBackClick: () -> Unit,
) {
    val viewModel =
        viewModel<RestoreMnemonicNonStandardViewModel>(factory = RestoreMnemonicNonStandardModule.Factory())
    val uiState = viewModel.uiState
    val context = LocalContext.current

    var textState by rememberSaveable("", stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var showCustomKeyboardDialog by remember { mutableStateOf(false) }
    var isMnemonicPhraseInputFocused by remember { mutableStateOf(false) }
    val keyboardState by observeKeyboardState()

    val qrScannerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val scannedText = result.data?.getStringExtra(ModuleField.SCAN_ADDRESS) ?: ""

                textState =
                    textState.copy(text = scannedText, selection = TextRange(scannedText.length))
                viewModel.onEnterMnemonicPhrase(scannedText, scannedText.length)
            }
        }

    val borderColor = if (uiState.error != null) {
        ComposeAppTheme.colors.red50
    } else {
        ComposeAppTheme.colors.steel20
    }

    val coroutineScope = rememberCoroutineScope()
    Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
        AppBar(
            title = TranslatableString.ResString(R.string.Restore_NonStandardRestore),
            navigationIcon = {
                HsBackButton(onClick = onBackClick)
            },
            menuItems = listOf(
                MenuItem(
                    title = TranslatableString.ResString(R.string.Button_Next),
                    onClick = viewModel::onProceed
                )
            )
        )
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(12.dp))

                InfoText(text = stringResource(R.string.Restore_NonStandard_Description))
                Spacer(Modifier.height(12.dp))

                HeaderText(stringResource(id = R.string.ManageAccount_Name))
                FormsInput(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    initial = viewModel.accountName,
                    pasteEnabled = false,
                    hint = viewModel.defaultName,
                    onValueChange = viewModel::onEnterName
                )
                Spacer(Modifier.height(32.dp))

                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                        .background(ComposeAppTheme.colors.lawrence),
                ) {

                    val style = SpanStyle(
                        color = ComposeAppTheme.colors.lucian,
                        fontWeight = FontWeight.Normal,
                        fontSize = 16.sp,
                        letterSpacing = 0.sp
                    )

                    BasicTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged {
                                isMnemonicPhraseInputFocused = it.isFocused
                            }
                            .defaultMinSize(minHeight = 68.dp)
                            .padding(start = 16.dp, end = 16.dp, top = 12.dp),
                        enabled = true,
                        value = textState,
                        onValueChange = {
                            textState = it

                            viewModel.onEnterMnemonicPhrase(it.text, it.selection.max)

                            showCustomKeyboardDialog =
                                !viewModel.isThirdPartyKeyboardAllowed && Utils.isUsingCustomKeyboard(
                                    context
                                )
                        },
                        textStyle = ColoredTextStyle(
                            color = ComposeAppTheme.colors.leah,
                            textStyle = ComposeAppTheme.typography.body
                        ),
                        maxLines = 6,
                        cursorBrush = SolidColor(ComposeAppTheme.colors.jacob),
                        visualTransformation = {
                            try {
                                val annotatedString = buildAnnotatedString {
                                    append(it.text)

                                    uiState.invalidWordRanges.forEach { range ->
                                        addStyle(style = style, range.first, range.last + 1)
                                    }
                                }
                                TransformedText(annotatedString, OffsetMapping.Identity)
                            } catch (error: Throwable) {
                                error.printStackTrace()
                                TransformedText(it, OffsetMapping.Identity)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        decorationBox = { innerTextField ->
                            if (textState.text.isEmpty()) {
                                body_grey50(
                                    stringResource(R.string.Restore_PhraseHint),
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            innerTextField()
                        },
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        if (textState.text.isNotEmpty()) {
                            ButtonSecondaryCircle(
                                modifier = Modifier.padding(end = 16.dp),
                                icon = R.drawable.ic_delete_20,
                                onClick = {
                                    textState = textState.copy(text = "", selection = TextRange(0))
                                    viewModel.onEnterMnemonicPhrase("", "".length)
                                }
                            )
                        } else {
                            ButtonSecondaryCircle(
                                modifier = Modifier.padding(end = 8.dp),
                                icon = R.drawable.ic_qr_scan_20,
                                onClick = {
                                    qrScannerLauncher.launch(
                                        QRScannerActivity.getScanQrIntent(context)
                                    )
                                }
                            )

                            val clipboardManager = LocalClipboardManager.current
                            ButtonSecondaryDefault(
                                modifier = Modifier.padding(end = 16.dp),
                                title = stringResource(id = R.string.Send_Button_Paste),
                                onClick = {
                                    clipboardManager.getText()?.text?.let { textInClipboard ->
                                        textState = textState.copy(
                                            text = textInClipboard,
                                            selection = TextRange(textInClipboard.length)
                                        )
                                        viewModel.onEnterMnemonicPhrase(
                                            textInClipboard,
                                            textInClipboard.length
                                        )
                                    }
                                },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                }

                Spacer(Modifier.height(8.dp))

                uiState.error?.let { errorText ->
                    caption_lucian(
                        modifier = Modifier.padding(horizontal = 32.dp),
                        text = errorText
                    )
                }

                Spacer(Modifier.height(24.dp))

                BottomSection(viewModel, uiState, coroutineScope)

                Spacer(Modifier.height(44.dp))
            }

            if (isMnemonicPhraseInputFocused && keyboardState == Keyboard.Opened) {
                SuggestionsBar(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    wordSuggestions = uiState.wordSuggestions
                ) { wordItem, suggestion ->
                    HudHelper.vibrate(context)

                    val cursorIndex = wordItem.range.first + suggestion.length + 1
                    var text = textState.text.replaceRange(wordItem.range, suggestion)

                    if (text.length < cursorIndex) {
                        text = "$text "
                    }

                    textState = TextFieldValue(
                        text = text,
                        selection = TextRange(cursorIndex)
                    )

                    viewModel.onEnterMnemonicPhrase(text, cursorIndex)
                }
            }
        }
    }

    uiState.accountType?.let { accountType ->
        mainViewModel.setAccountData(accountType, viewModel.accountName, true, false)
        openSelectCoinsScreen.invoke()
        viewModel.onSelectCoinsShown()
    }

    if (showCustomKeyboardDialog) {
        CustomKeyboardWarningDialog(
            onSelect = {
                val imeManager =
                    context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imeManager.showInputMethodPicker()
                showCustomKeyboardDialog = false
            },
            onSkip = {
                viewModel.onAllowThirdPartyKeyboard()
                showCustomKeyboardDialog = false
            },
            onCancel = {
                showCustomKeyboardDialog = false
            }
        )
    }

}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun BottomSection(
    viewModel: RestoreMnemonicNonStandardViewModel,
    uiState: RestoreMnemonicNonStandardModule.UiState,
    coroutineScope: CoroutineScope,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var showLanguageSelectorDialog by remember { mutableStateOf(false) }
    var hidePassphrase by remember { mutableStateOf(true) }

    if (showLanguageSelectorDialog) {
        SelectorDialogCompose(
            title = stringResource(R.string.CreateWallet_Wordlist),
            items = viewModel.mnemonicLanguages.map {
                TabItem(
                    stringResource(it.displayNameStringRes),
                    it == uiState.language,
                    it
                )
            },
            onDismissRequest = {
                coroutineScope.launch {
                    showLanguageSelectorDialog = false
                    delay(300)
                    keyboardController?.show()
                }
            },
            onSelectItem = {
                viewModel.setMnemonicLanguage(it)
            }
        )
    }

    CellSingleLineLawrenceSection(
        listOf({
            MnemonicLanguageCell(
                language = uiState.language,
                showLanguageSelectorDialog = {
                    showLanguageSelectorDialog = true
                }
            )
        },
            {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_key_phrase_20),
                        tint = ComposeAppTheme.colors.grey,
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(16.dp))
                    body_leah(
                        text = stringResource(R.string.Passphrase),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                    HsSwitch(
                        checked = uiState.passphraseEnabled,
                        onCheckedChange = viewModel::onTogglePassphrase
                    )
                }
            }
        )
    )

    if (uiState.passphraseEnabled) {
        Spacer(modifier = Modifier.height(24.dp))
        FormsInputPassword(
            modifier = Modifier.padding(horizontal = 16.dp),
            hint = stringResource(R.string.Passphrase),
            state = uiState.passphraseError?.let { DataState.Error(Exception(it)) },
            onValueChange = viewModel::onEnterPassphrase,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            hide = hidePassphrase,
            onToggleHide = {
                hidePassphrase = !hidePassphrase
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextImportantWarning(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(R.string.Restore_PassphraseDescription)
        )
    }

}