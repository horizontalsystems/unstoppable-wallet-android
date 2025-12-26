package io.horizontalsystems.bankwallet.modules.restoreaccount.restoremnemonic

import android.app.Activity
import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import io.horizontalsystems.bankwallet.core.stats.StatEntity
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.core.utils.Utils
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.createaccount.MnemonicLanguageCell
import io.horizontalsystems.bankwallet.modules.createaccount.PassphraseCell
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.bankwallet.modules.restoreaccount.RestoreViewModel
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoremenu.RestoreByMenu
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoremenu.RestoreMenuViewModel
import io.horizontalsystems.bankwallet.ui.compose.ColoredTextStyle
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.Keyboard
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.BoxTyler44
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondary
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.CellSingleLineLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.CustomKeyboardWarningDialog
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInputPassword
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderText
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey50
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.captionSB_leah
import io.horizontalsystems.bankwallet.ui.compose.components.caption_lucian
import io.horizontalsystems.bankwallet.ui.compose.observeKeyboardState
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.menu.MenuGroup
import io.horizontalsystems.bankwallet.uiv3.components.menu.MenuItemX
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RestorePhrase(
    advanced: Boolean,
    restoreMenuViewModel: RestoreMenuViewModel,
    mainViewModel: RestoreViewModel,
    openRestoreAdvanced: (() -> Unit)? = null,
    openSelectCoins: () -> Unit,
    openNonStandardRestore: () -> Unit,
    onBackClick: () -> Unit,
) {
    val viewModel = viewModel<RestoreMnemonicViewModel>(factory = RestoreMnemonicModule.Factory())
    val uiState = viewModel.uiState
    val context = LocalContext.current
    val statPage =
        if (advanced) StatPage.ImportWalletFromKeyAdvanced else StatPage.ImportWalletFromKey

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
        ComposeAppTheme.colors.blade
    }

    val coroutineScope = rememberCoroutineScope()
    HSScaffold(
        title = if (advanced) stringResource(R.string.Restore_Advanced_Title) else stringResource(
            R.string.ManageAccounts_ImportWallet
        ),
        onBack = onBackClick,
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Next),
                onClick = viewModel::onProceed,
                tint = ComposeAppTheme.colors.jacob
            )
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                VSpacer(12.dp)

                HeaderText(stringResource(id = R.string.ManageAccount_Name))
                FormsInput(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    initial = viewModel.accountName,
                    pasteEnabled = false,
                    hint = viewModel.defaultName,
                    onValueChange = viewModel::onEnterName
                )
                VSpacer(32.dp)

                if (advanced) {
                    RestoreByMenu(restoreMenuViewModel)
                    VSpacer(32.dp)
                }

                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(0.5.dp, borderColor, RoundedCornerShape(16.dp))
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
                        cursorBrush = SolidColor(ComposeAppTheme.colors.leah),
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
                                    textState =
                                        textState.copy(text = "", selection = TextRange(0))
                                    viewModel.onEnterMnemonicPhrase("", "".length)

                                    stat(
                                        page = statPage,
                                        event = StatEvent.Clear(StatEntity.RecoveryPhrase)
                                    )
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

                                    stat(
                                        page = statPage,
                                        event = StatEvent.ScanQr(StatEntity.RecoveryPhrase)
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

                                    stat(
                                        page = statPage,
                                        event = StatEvent.Paste(StatEntity.RecoveryPhrase)
                                    )
                                },
                            )
                        }
                    }

                    VSpacer(4.dp)
                }

                uiState.error?.let { errorText ->
                    VSpacer(8.dp)
                    caption_lucian(
                        modifier = Modifier.padding(horizontal = 32.dp),
                        text = errorText
                    )
                }

                VSpacer(32.dp)

                if (advanced) {
                    BottomSection(
                        viewModel,
                        uiState,
                        openNonStandardRestore,
                        coroutineScope
                    )
                } else {
                    CellUniversalLawrenceSection(
                        listOf(
                            {
                                PassphraseCell(
                                    enabled = uiState.passphraseEnabled,
                                    onCheckedChange = viewModel::onTogglePassphrase
                                )
                            }
                        )
                    )
                    if (uiState.passphraseEnabled) {
                        VSpacer(16.dp)
                        PassPhraseBlock(
                            error = uiState.passphraseError,
                            onEnterPassphrase = {
                                viewModel.onEnterPassphrase(it)
                            }
                        )
                    }
                    VSpacer(32.dp)
                    CellSingleLineLawrenceSection {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    openRestoreAdvanced?.invoke()
                                }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            body_leah(text = stringResource(R.string.Button_Advanced))
                            Spacer(modifier = Modifier.weight(1f))
                            Image(
                                modifier = Modifier.size(20.dp),
                                painter = painterResource(id = R.drawable.ic_arrow_right),
                                contentDescription = null,
                            )
                        }
                    }

                    VSpacer(72.dp)
                }
            }

            if (isMnemonicPhraseInputFocused && keyboardState == Keyboard.Opened) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        // Add IME (keyboard) padding to push content above keyboard
                        .windowInsetsPadding(WindowInsets.ime)
                        .systemBarsPadding()
                ) {
                    SuggestionsBar(wordSuggestions = uiState.wordSuggestions) { wordItem, suggestion ->
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
    }

    uiState.accountType?.let { accountType ->
        mainViewModel.setAccountData(accountType, viewModel.accountName, true, false, statPage)
        openSelectCoins.invoke()
        viewModel.onSelectCoinsShown()

        stat(page = statPage, event = StatEvent.Open(StatPage.RestoreSelect))
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

@Composable
private fun BottomSection(
    viewModel: RestoreMnemonicViewModel,
    uiState: RestoreMnemonicModule.UiState,
    openNonStandardRestore: () -> Unit,
    coroutineScope: CoroutineScope,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var showLanguageSelectorDialog by remember { mutableStateOf(false) }

    if (showLanguageSelectorDialog) {
        MenuGroup(
            title = stringResource(R.string.CreateWallet_Wordlist),
            items = viewModel.mnemonicLanguages.map {
                MenuItemX(
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

    CellUniversalLawrenceSection(
        listOf(
            {
                MnemonicLanguageCell(
                    language = uiState.language,
                    showLanguageSelectorDialog = {
                        showLanguageSelectorDialog = true
                    }
                )
            },
            {
                PassphraseCell(
                    enabled = uiState.passphraseEnabled,
                    onCheckedChange = viewModel::onTogglePassphrase
                )
            }

        )
    )

    if (uiState.passphraseEnabled) {
        VSpacer(24.dp)
        PassPhraseBlock(
            error = uiState.passphraseError,
            onEnterPassphrase = {
                viewModel.onEnterPassphrase(it)
            }
        )
    }

    Spacer(Modifier.height(32.dp))

    CellSingleLineLawrenceSection {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    openNonStandardRestore.invoke()
                }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            body_leah(text = stringResource(R.string.Restore_NonStandardRestore))
            Spacer(modifier = Modifier.weight(1f))
            Image(
                modifier = Modifier.size(20.dp),
                painter = painterResource(id = R.drawable.ic_arrow_right),
                contentDescription = null,
            )
        }
    }
    Spacer(Modifier.height(62.dp))
}

@Composable
private fun PassPhraseBlock(
    error: String? = null,
    onEnterPassphrase: (String) -> Unit = {}
) {
    var hidePassphrase by remember { mutableStateOf(true) }
    FormsInputPassword(
        modifier = Modifier.padding(horizontal = 16.dp),
        hint = stringResource(R.string.Passphrase),
        state = error?.let { DataState.Error(Exception(it)) },
        onValueChange = onEnterPassphrase,
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

@Composable
fun SuggestionsBar(
    modifier: Modifier = Modifier,
    wordSuggestions: RestoreMnemonicModule.WordSuggestions?,
    onClick: (RestoreMnemonicModule.WordItem, String) -> Unit
) {
    Box(modifier = modifier) {
        BoxTyler44(borderTop = true) {
            if (wordSuggestions != null) {
                LazyRow(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(wordSuggestions.options) { suggestion ->
                        val wordItem = wordSuggestions.wordItem
                        ButtonSecondary(
                            onClick = {
                                onClick.invoke(wordItem, suggestion)
                            }
                        ) {
                            captionSB_leah(text = suggestion)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                }
            } else {
                Icon(
                    modifier = Modifier.align(Alignment.Center),
                    painter = painterResource(R.drawable.ic_more_24),
                    tint = ComposeAppTheme.colors.grey,
                    contentDescription = null
                )
            }
        }
    }
}
