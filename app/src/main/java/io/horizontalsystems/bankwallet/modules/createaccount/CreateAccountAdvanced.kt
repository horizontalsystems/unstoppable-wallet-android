package io.horizontalsystems.bankwallet.modules.createaccount

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.displayNameStringRes
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.B2
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.D1
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInputPassword
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderText
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.HsSwitch
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.SelectorDialogCompose
import io.horizontalsystems.bankwallet.ui.compose.components.TabItem
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_grey
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.hdwalletkit.Language
import kotlinx.coroutines.delay

@Composable
fun CreateAccountAdvancedScreen(
    onBackClick: () -> Unit,
    onFinish: () -> Unit
) {
    val viewModel = viewModel<CreateAccountViewModel>(factory = CreateAccountModule.Factory())
    val view = LocalView.current

    LaunchedEffect(viewModel.successMessage) {
        viewModel.successMessage?.let {
            HudHelper.showSuccessMessage(
                contenView = view,
                resId = it,
                icon = R.drawable.icon_add_to_wallet_24,
                iconTint = R.color.white
            )
            delay(300)
            onFinish.invoke()
            viewModel.onSuccessMessageShown()
        }
    }

    var showMnemonicSizeSelectorDialog by remember { mutableStateOf(false) }
    var hidePassphrase by remember { mutableStateOf(true) }

    ComposeAppTheme {
        Surface(color = ComposeAppTheme.colors.tyler) {
            if (showMnemonicSizeSelectorDialog) {
                SelectorDialogCompose(
                    title = stringResource(R.string.CreateWallet_Mnemonic),
                    items = viewModel.mnemonicKinds.map {
                        TabItem(it.titleLong, it == viewModel.selectedKind, it)
                    },
                    onDismissRequest = {
                        showMnemonicSizeSelectorDialog = false
                    },
                    onSelectItem = {
                        viewModel.setMnemonicKind(it)
                    }
                )
            }
            Column {
                AppBar(
                    title = TranslatableString.ResString(R.string.CreateWallet_Advanced_Title),
                    navigationIcon = {
                        HsBackButton(onClick = onBackClick)
                    },
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Button_Create),
                            onClick = { viewModel.createAccount() },
                        )
                    )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(12.dp))
                    HeaderText(stringResource(id = R.string.ManageAccount_Name))
                    FormsInput(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        initial = viewModel.accountName,
                        pasteEnabled = false,
                        hint = viewModel.defaultAccountName,
                        onValueChange = viewModel::onChangeAccountName
                    )
                    Spacer(Modifier.height(32.dp))
                    CellUniversalLawrenceSection(
                        listOf {
                            MnemonicNumberCell(
                                kind = viewModel.selectedKind,
                                showMnemonicSizeSelectorDialog = {
                                    showMnemonicSizeSelectorDialog = true
                                }
                            )
                        }
                    )

                    Spacer(Modifier.height(32.dp))
                    CellUniversalLawrenceSection(listOf {
                        PassphraseCell(
                            enabled = viewModel.passphraseEnabled,
                            onCheckedChange = { viewModel.setPassphraseEnabledState(it) }
                        )
                    })

                    if (viewModel.passphraseEnabled) {
                        Spacer(Modifier.height(24.dp))
                        FormsInputPassword(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            hint = stringResource(R.string.Passphrase),
                            state = viewModel.passphraseState,
                            onValueChange = viewModel::onChangePassphrase,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            hide = hidePassphrase,
                            onToggleHide = {
                                hidePassphrase = !hidePassphrase
                            }
                        )
                        Spacer(Modifier.height(16.dp))
                        FormsInputPassword(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            hint = stringResource(R.string.ConfirmPassphrase),
                            state = viewModel.passphraseConfirmState,
                            onValueChange = viewModel::onChangePassphraseConfirmation,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            hide = hidePassphrase,
                            onToggleHide = {
                                hidePassphrase = !hidePassphrase
                            }
                        )
                        Spacer(Modifier.height(12.dp))
                        D1(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            text = stringResource(R.string.CreateWallet_PassphraseDescription)
                        )
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun MnemonicNumberCell(
    kind: CreateAccountModule.Kind,
    showMnemonicSizeSelectorDialog: () -> Unit
) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalPadding = 0.dp,
        onClick = { showMnemonicSizeSelectorDialog() }
    ) {
        Icon(
            modifier = Modifier.padding(vertical = 12.dp),
            painter = painterResource(id = R.drawable.ic_key_20),
            contentDescription = null,
            tint = ComposeAppTheme.colors.grey
        )
        B2(
            text = stringResource(R.string.CreateWallet_Mnemonic),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.weight(1f))
        subhead1_grey(
            text = kind.title,
        )
        Icon(
            modifier = Modifier.padding(start = 4.dp),
            painter = painterResource(id = R.drawable.ic_down_arrow_20),
            contentDescription = null,
            tint = ComposeAppTheme.colors.grey
        )
    }
}

@Composable
fun MnemonicLanguageCell(
    language: Language,
    showLanguageSelectorDialog: () -> Unit
) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
        onClick = showLanguageSelectorDialog
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_globe_20),
            contentDescription = null,
            tint = ComposeAppTheme.colors.grey
        )
        B2(
            text = stringResource(R.string.CreateWallet_Wordlist),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.weight(1f))
        subhead1_grey(
            text = stringResource(language.displayNameStringRes),
        )
        Icon(
            modifier = Modifier.padding(start = 4.dp),
            painter = painterResource(id = R.drawable.ic_down_arrow_20),
            contentDescription = null,
            tint = ComposeAppTheme.colors.grey
        )
    }
}

@Composable
fun PassphraseCell(enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalPadding = 0.dp,
        onClick = { onCheckedChange(!enabled) },
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_key_phrase_20),
            contentDescription = null,
            tint = ComposeAppTheme.colors.grey
        )
        body_leah(
            text = stringResource(R.string.Passphrase),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp, end = 8.dp)
        )
        HsSwitch(
            checked = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}
