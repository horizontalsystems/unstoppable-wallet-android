package io.horizontalsystems.bankwallet.modules.restore.restoremnemonic

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.utils.Utils
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.restore.restoreblockchains.RestoreBlockchainsFragment
import io.horizontalsystems.bankwallet.ui.compose.ColoredTextStyle
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class RestoreMnemonicFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                ComposeAppTheme {
                    RestoreMnemonicScreen(findNavController())
                }
            }
        }
    }
}

@Composable
fun RestoreMnemonicScreen(navController: NavController) {
    val viewModel = viewModel<RestoreMnemonicViewModel>(factory = RestoreMnemonicModule.Factory())
    val uiState = viewModel.uiState
    val context = LocalContext.current

    var textState by rememberSaveable("", stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    val focusRequester = remember { FocusRequester() }
    var showCustomKeyboardDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
        AppBar(
            title = TranslatableString.ResString(R.string.Restore_Enter_Key_Title),
            navigationIcon = {
                HsIconButton(onClick = navController::popBackStack) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back),
                        contentDescription = "back",
                        tint = ComposeAppTheme.colors.jacob
                    )
                }
            },
            menuItems = listOf(
                MenuItem(
                    title = TranslatableString.ResString(R.string.Button_Next),
                    onClick = viewModel::onProceed
                )
            )
        )

        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(12.dp))
            HeaderText(text = stringResource(R.string.Restore_Key))
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(8.dp))
                    .background(ComposeAppTheme.colors.lawrence),
                verticalAlignment = Alignment.CenterVertically
            ) {

                val style = SpanStyle(
                    color = ComposeAppTheme.colors.lucian,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    letterSpacing = 0.sp
                )

                BasicTextField(
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .defaultMinSize(minHeight = 93.dp)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .weight(1f),
                    enabled = true,
                    value = textState,
                    onValueChange = {
                        textState = it

                        viewModel.onEnterMnemonicPhrase(it.text, it.selection.max)

                        showCustomKeyboardDialog = !viewModel.isThirdPartyKeyboardAllowed && Utils.isUsingCustomKeyboard(context)
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
                    keyboardOptions = KeyboardOptions.Default,
                )
            }
            InfoText(text = stringResource(R.string.Restore_Mnemonic_Description))
            Spacer(Modifier.height(24.dp))
            Passphrase(
                enabled = uiState.passphraseEnabled,
                error = uiState.passphraseError,
                onEnabled = viewModel::onEnablePassphrase,
                onEnterPassphrase = viewModel::onEnterPassphrase
            )
            Spacer(Modifier.height(24.dp))
            HeaderText(text = stringResource(R.string.Restore_Name))
            FormsInput(
                modifier = Modifier.padding(horizontal = 16.dp),
                hint = uiState.defaultName,
                singleLine = true,
                pasteEnabled = false,
                onValueChange = viewModel::onEnterName
            )
            Spacer(Modifier.height(32.dp))
        }
    }

    uiState.error?.let {
        HudHelper.showErrorMessage(LocalView.current, it)

        viewModel.onErrorShown()
    }

    uiState.accountType?.let { accountType ->
        navController.slideFromRight(
            R.id.restoreSelectCoinsFragment,
            bundleOf(
                RestoreBlockchainsFragment.ACCOUNT_NAME_KEY to viewModel.resolvedName,
                RestoreBlockchainsFragment.ACCOUNT_TYPE_KEY to accountType
            )
        )

        viewModel.onSelectCoinsShown()
    }

    if (showCustomKeyboardDialog) {
        CustomKeyboardWarningDialog(
            onSelect = {
                val imeManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
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
fun Passphrase(
    enabled: Boolean,
    error: String?,
    onEnabled: (Boolean) -> Unit,
    onEnterPassphrase: (String) -> Unit
) {
    CellSingleLineLawrenceSection {
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
                checked = enabled,
                onCheckedChange = onEnabled
            )
        }
    }

    if (enabled) {
        Spacer(modifier = Modifier.height(12.dp))
        FormsInput(
            modifier = Modifier.padding(horizontal = 16.dp),
            hint = stringResource(R.string.Passphrase),
            state = error?.let { DataState.Error(Exception(it)) },
            pasteEnabled = false,
            singleLine = true,
            onValueChange = onEnterPassphrase,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
        InfoText(text = stringResource(R.string.Restore_PassphraseDescription))
    }

}
