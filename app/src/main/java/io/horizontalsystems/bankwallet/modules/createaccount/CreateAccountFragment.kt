package io.horizontalsystems.bankwallet.modules.createaccount

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.displayNameStringRes
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.hdwalletkit.Language
import kotlinx.coroutines.delay

class CreateAccountFragment : BaseFragment() {

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
                val popUpToInclusiveId =
                    arguments?.getInt(ManageAccountsModule.popOffOnSuccessKey, R.id.createAccountFragment) ?: R.id.createAccountFragment
                CreateAccountScreen(findNavController(), popUpToInclusiveId)
            }
        }
    }
}

@Composable
private fun CreateAccountScreen(
    navController: NavController,
    popUpToInclusiveId: Int
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
            navController.popBackStack(popUpToInclusiveId, true)
            viewModel.onSuccessMessageShown()
        }
    }

    var showMnemonicSizeSelectorDialog by remember { mutableStateOf(false) }

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
                    title = TranslatableString.ResString(R.string.CreateWallet_Title),
                    navigationIcon = {
                        HsIconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_back),
                                contentDescription = "back button",
                                tint = ComposeAppTheme.colors.jacob
                            )
                        }
                    },
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Button_Create),
                            onClick = { viewModel.createAccount() },
                        )
                    )
                )

                Column {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(Modifier.height(12.dp))
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
                        PassphraseCell(
                            enabled = viewModel.passphraseEnabled,
                            onCheckedChange = { viewModel.setPassphraseEnabledState(it) }
                        )
                        if (viewModel.passphraseEnabled) {
                            Spacer(Modifier.height(24.dp))
                            FormsInput(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                hint = stringResource(R.string.EnterPassphrase),
                                onValueChange = {
                                    viewModel.onChangePassphrase(it)
                                },
                                pasteEnabled = false,
                                state = viewModel.passphraseState,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            )
                            Spacer(Modifier.height(24.dp))
                            FormsInput(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                hint = stringResource(R.string.CreateWallet_PassphraseConfirm),
                                onValueChange = {
                                    viewModel.onChangePassphraseConfirmation(it)
                                },
                                pasteEnabled = false,
                                state = viewModel.passphraseConfirmState,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            )
                            Spacer(Modifier.height(12.dp))
                            D1(
                                modifier = Modifier.padding(horizontal = 24.dp),
                                text = stringResource(R.string.CreateWallet_PassphraseDescription)
                            )
                        }
                        Spacer(Modifier.height(32.dp))
                    }

                    ButtonsGroupWithShade {
                        ButtonPrimaryYellow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            title = stringResource(R.string.Button_Create),
                            onClick = {
                                viewModel.createAccount()
                            },
                        )
                    }
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
private fun PassphraseCell(
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    CellUniversalLawrenceSection(
        listOf {
            RowUniversal(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalPadding = 0.dp,
                onClick = { onCheckedChange(!enabled) },
            ) {
                Icon(
                    modifier = Modifier.padding(vertical = 12.dp),
                    painter = painterResource(id = R.drawable.ic_key_phrase_20),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.grey
                )
                B2(
                    text = stringResource(R.string.Passphrase),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.weight(1f))
                HsSwitch(
                    checked = enabled,
                    onCheckedChange = onCheckedChange
                )
            }
        })
}
