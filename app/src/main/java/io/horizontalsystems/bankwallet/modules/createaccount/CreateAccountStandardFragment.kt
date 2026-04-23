package io.horizontalsystems.bankwallet.modules.createaccount

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.core.stats.statAccountType
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.D1
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInputPassword
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.Section
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightControlsSwitcher
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightSelectors
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonSize
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSIconButton
import io.horizontalsystems.bankwallet.uiv3.components.menu.MenuGroup
import io.horizontalsystems.bankwallet.uiv3.components.menu.MenuItemX
import io.horizontalsystems.bankwallet.uiv3.components.section.SectionHeaderColored
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.delay

class CreateAccountStandardFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<ManageAccountsModule.Input>()
        val popUpToInclusiveId = input?.popOffOnSuccess ?: R.id.createAccountFragment
        val inclusive = input?.popOffInclusive ?: true
        CreateAccountIntroScreen(
            onBackClick = { navController.popBackStack() },
            onFinish = { navController.popBackStack(popUpToInclusiveId, inclusive) },
        )
    }

}

@Composable
private fun CreateAccountIntroScreen(
    onBackClick: () -> Unit,
    onFinish: () -> Unit
) {
    val viewModel = viewModel<CreateAccountViewModel>(factory = CreateAccountModule.Factory())
    val view = LocalView.current

    var advancedOptionsEnabled by remember { mutableStateOf(false) }
    var showMnemonicSizeSelectorDialog by remember { mutableStateOf(false) }
    var hidePassphrase by remember { mutableStateOf(true) }

    LaunchedEffect(viewModel.success) {
        viewModel.success?.let { accountType ->
            HudHelper.showSuccessMessage(
                contenView = view,
                resId = R.string.Hud_Text_Created,
                icon = R.drawable.icon_add_to_wallet_24,
                iconTint = R.color.white
            )
            delay(300)

            onFinish.invoke()
            viewModel.onSuccessMessageShown()

            stat(
                page = if (advancedOptionsEnabled) StatPage.NewWalletAdvanced else StatPage.NewWallet,
                event = StatEvent.CreateWallet(accountType.statAccountType)
            )
        }
    }

    HSScaffold(
        title = stringResource(R.string.ManageAccounts_CreateNewWallet),
        onBack = onBackClick,
    ) {
        Column(
            modifier = Modifier.windowInsetsPadding(WindowInsets.ime)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .fillMaxSize()
            ) {
                SectionHeaderColored(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = ComposeAppTheme.colors.grey,
                    title = stringResource(id = R.string.ManageAccount_WalletName)
                )
                FormsInput(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    initial = viewModel.accountName,
                    pasteEnabled = false,
                    hint = viewModel.defaultAccountName,
                    onValueChange = viewModel::onChangeAccountName,
                    trailingContent = {
                        Box(modifier = Modifier.padding(end = 16.dp)) {
                            HSIconButton(
                                variant = ButtonVariant.Secondary,
                                size = ButtonSize.Small,
                                icon = painterResource(R.drawable.ic_swap_circle_24),
                                onClick = viewModel::generateRandomAccountName
                            )
                        }
                    }
                )

                VSpacer(24.dp)

                Section {
                    CellPrimary(
                        middle = {
                            CellMiddleInfo(
                                title = stringResource(R.string.CreateWallet_AdvancedOptions).hs(),
                            )
                        },
                        right = {
                            CellRightControlsSwitcher(
                                checked = advancedOptionsEnabled,
                                onCheckedChange = {
                                    advancedOptionsEnabled = it
                                    viewModel.setAdvancedOptionsEnabled(it)
                                }
                            )
                        }
                    )
                }

                if (advancedOptionsEnabled) {
                    VSpacer(24.dp)

                    Section {
                        CellPrimary(
                            middle = {
                                CellMiddleInfo(
                                    subtitle = stringResource(R.string.CreateWallet_Mnemonic).hs(),
                                )
                            },
                            right = {
                                CellRightSelectors(
                                    subtitle = viewModel.selectedKind.title.hs,
                                    icon = painterResource(id = R.drawable.arrow_s_down_24),
                                    iconTint = ComposeAppTheme.colors.leah
                                )
                            },
                            onClick = { showMnemonicSizeSelectorDialog = true }
                        )
                    }

                    SectionHeaderColored(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = ComposeAppTheme.colors.grey,
                        title = stringResource(id = R.string.CreateWallet_PassphraseOptional)
                    )

                    FormsInputPassword(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        hint = stringResource(R.string.CreateWallet_AddPassphrase),
                        state = viewModel.passphraseState,
                        onValueChange = viewModel::onChangePassphrase,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        hide = hidePassphrase,
                        onToggleHide = { hidePassphrase = !hidePassphrase }
                    )
                    VSpacer(16.dp)
                    FormsInputPassword(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        hint = stringResource(R.string.ConfirmPassphrase),
                        state = viewModel.passphraseConfirmState,
                        onValueChange = viewModel::onChangePassphraseConfirmation,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        hide = hidePassphrase,
                        onToggleHide = { hidePassphrase = !hidePassphrase }
                    )
                    VSpacer(12.dp)
                    D1(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        text = stringResource(R.string.CreateWallet_PassphraseShortDescription)
                    )
                }

                VSpacer(24.dp)
            }
            ButtonsGroupWithShade {
                HSButton(
                    title = stringResource(R.string.Button_Create),
                    variant = ButtonVariant.Primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    onClick = viewModel::createAccount,
                )
            }
        }
    }

    if (showMnemonicSizeSelectorDialog) {
        MenuGroup(
            title = stringResource(R.string.CreateWallet_Mnemonic),
            items = viewModel.mnemonicKinds.map {
                MenuItemX(it.titleLong, it == viewModel.selectedKind, it)
            },
            onDismissRequest = { showMnemonicSizeSelectorDialog = false },
            onSelectItem = { viewModel.setMnemonicKind(it) }
        )
    }
}
