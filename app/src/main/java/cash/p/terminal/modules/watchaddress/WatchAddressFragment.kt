package cash.p.terminal.modules.watchaddress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.entities.DataState
import cash.p.terminal.modules.address.HSAddressInput
import cash.p.terminal.modules.manageaccounts.ManageAccountsModule
import cash.p.terminal.modules.restoreaccount.restoremenu.ByMenu
import cash.p.terminal.modules.watchaddress.selectblockchains.SelectBlockchainsModule
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.FormsInput
import cash.p.terminal.ui.compose.components.FormsInputMultiline
import cash.p.terminal.ui.compose.components.HeaderText
import cash.p.terminal.ui.compose.components.HsBackButton
import cash.p.terminal.ui.compose.components.MenuItem
import cash.p.terminal.ui.compose.components.SelectorItem
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.delay

class WatchAddressFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent() {
        ComposeAppTheme {
            val popUpToInclusiveId =
                arguments?.getInt(ManageAccountsModule.popOffOnSuccessKey, R.id.watchAddressFragment) ?: R.id.watchAddressFragment
            val inclusive =
                arguments?.getBoolean(ManageAccountsModule.popOffInclusiveKey) ?: true
            WatchAddressScreen(findNavController(), popUpToInclusiveId, inclusive)
        }
    }

}

@Composable
fun WatchAddressScreen(navController: NavController, popUpToInclusiveId: Int, inclusive: Boolean) {
    val view = LocalView.current

    val viewModel = viewModel<WatchAddressViewModel>(factory = WatchAddressModule.Factory())
    val uiState = viewModel.uiState
    val accountCreated = uiState.accountCreated
    val submitType = uiState.submitButtonType
    val accountType = uiState.accountType
    val accountName = uiState.accountName
    val type = uiState.type

    LaunchedEffect(accountCreated) {
        if (accountCreated) {
            HudHelper.showSuccessMessage(
                contenView = view,
                resId = R.string.Hud_Text_AddressAdded,
                icon = R.drawable.icon_binocule_24,
                iconTint = R.color.white
            )
            delay(300)
            navController.popBackStack(popUpToInclusiveId, inclusive)
        }
    }

    if (accountType != null) {
        viewModel.blockchainSelectionOpened()

        navController.slideFromRight(
            R.id.selectBlockchainsFragment,
            bundleOf(
                SelectBlockchainsModule.accountTypeKey to accountType,
                SelectBlockchainsModule.accountNameKey to accountName,
                ManageAccountsModule.popOffOnSuccessKey to popUpToInclusiveId,
                ManageAccountsModule.popOffInclusiveKey to inclusive,
            )
        )
    }

    ComposeAppTheme {
        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                title = TranslatableString.ResString(R.string.ManageAccounts_WatchAddress),
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                },
                menuItems = buildList {
                    when (submitType) {
                        is SubmitButtonType.Watch -> {
                            add(
                                MenuItem(
                                    title = TranslatableString.ResString(R.string.Watch_Address_Watch),
                                    onClick = viewModel::onClickWatch,
                                    enabled = submitType.enabled
                                )
                            )
                        }
                        is SubmitButtonType.Next -> {
                            add(
                                MenuItem(
                                    title = TranslatableString.ResString(R.string.Button_Next),
                                    onClick = viewModel::onClickNext,
                                    enabled = submitType.enabled
                                )
                            )
                        }
                    }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                HeaderText(stringResource(id = R.string.ManageAccount_Name))
                FormsInput(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    initial = viewModel.accountName,
                    pasteEnabled = false,
                    hint = viewModel.defaultAccountName,
                    onValueChange = viewModel::onEnterAccountName
                )
                Spacer(Modifier.height(32.dp))

                ByMenu(
                    menuTitle = stringResource(R.string.Watch_By),
                    menuValue = stringResource(type.titleResId),
                    selectorDialogTitle = stringResource(R.string.Watch_WatchBy),
                    selectorItems = WatchAddressViewModel.Type.values().map {
                        SelectorItem(
                            title = stringResource(it.titleResId),
                            selected = it == type,
                            item = it,
                            subtitle = stringResource(it.subtitleResId)
                        )
                    },
                    onSelectItem = {
                        viewModel.onSetType(it)
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))
                when (type) {
                    WatchAddressViewModel.Type.EvmAddress -> {
                        HSAddressInput(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            tokenQuery = TokenQuery(BlockchainType.Ethereum, TokenType.Native),
                            coinCode = "ETH",
                            navController = navController,
                            onValueChange = viewModel::onEnterAddress
                        )
                    }
                    WatchAddressViewModel.Type.SolanaAddress -> {
                        HSAddressInput(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            tokenQuery = TokenQuery(BlockchainType.Solana, TokenType.Native),
                            coinCode = "SOL",
                            navController = navController,
                            onValueChange = viewModel::onEnterAddress
                        )
                    }
                    WatchAddressViewModel.Type.TronAddress -> {
                        HSAddressInput(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            tokenQuery = TokenQuery(BlockchainType.Tron, TokenType.Native),
                            coinCode = "TRX",
                            navController = navController,
                            onValueChange = viewModel::onEnterAddress
                        )
                    }
                    WatchAddressViewModel.Type.XPubKey -> {
                        FormsInputMultiline(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            hint = stringResource(id = R.string.Watch_XPubKey_Hint),
                            qrScannerEnabled = true,
                            state = if (uiState.invalidXPubKey)
                                DataState.Error(Exception(stringResource(id = R.string.Watch_Error_InvalidXPubKey)))
                            else
                               null
                        ) {
                            viewModel.onEnterXPubKey(it)
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
