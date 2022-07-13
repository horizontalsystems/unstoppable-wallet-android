package io.horizontalsystems.bankwallet.modules.createaccount

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

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
                CreateAccountScreen(findNavController())
            }
        }
    }
}

@Composable
private fun CreateAccountScreen(
    navController: NavController,
    viewModel: CreateAccountViewModel = viewModel(factory = CreateAccountModule.Factory())
) {

    viewModel.successMessage?.let {
        HudHelper.showSuccessMessage(LocalView.current, it)
        viewModel.onSuccessMessageShown()

        Handler(Looper.getMainLooper()).postDelayed({
            navController.popBackStack()
        }, 1000)
    }

    var showMnemonicSizeSelectorDialog by remember { mutableStateOf(false) }

    ComposeAppTheme {
        Surface(color = ComposeAppTheme.colors.tyler) {
            if (showMnemonicSizeSelectorDialog) {
                SelectorDialogCompose(
                    title = stringResource(R.string.CreateWallet_Mnemonic),
                    items = viewModel.mnemonicKinds.map {
                        TabItem(it.title, it == viewModel.selectedKind, it)
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

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(12.dp))
                    MnemonicNumberCell(
                        kind = viewModel.selectedKind,
                        showMnemonicSizeSelectorDialog = { showMnemonicSizeSelectorDialog = true }
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
            }
        }
    }
}

@Composable
private fun MnemonicNumberCell(
    kind: CreateAccountModule.Kind,
    showMnemonicSizeSelectorDialog: () -> Unit
) {
    CellSingleLineLawrenceSection(
        listOf {
            Row(
                modifier = Modifier.padding(start = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_key_20),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.grey
                )
                D1(
                    text = stringResource(R.string.CreateWallet_Mnemonic),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.weight(1f))
                ButtonSecondaryTransparent(
                    title = kind.title,
                    iconRight = R.drawable.ic_down_arrow_20,
                    onClick = {
                        showMnemonicSizeSelectorDialog()
                    }
                )
            }
        })
}

@Composable
private fun PassphraseCell(
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    CellSingleLineLawrenceSection(
        listOf {
            Row(
                modifier = Modifier
                    .clickable {
                        onCheckedChange(!enabled)
                    }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_key_phrase_20),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.grey
                )
                D1(
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
