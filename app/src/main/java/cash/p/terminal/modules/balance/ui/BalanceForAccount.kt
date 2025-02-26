package cash.p.terminal.modules.balance.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.Caution
import cash.p.terminal.core.slideFromBottom
import cash.p.terminal.core.stats.StatEvent
import cash.p.terminal.core.stats.StatPage
import cash.p.terminal.core.stats.stat
import cash.p.terminal.core.utils.ModuleField
import io.horizontalsystems.core.entities.ViewState
import cash.p.terminal.modules.backupalert.BackupAlert
import cash.p.terminal.modules.balance.AccountViewItem
import cash.p.terminal.modules.balance.BalanceModule
import cash.p.terminal.modules.balance.BalanceViewModel
import cash.p.terminal.modules.contacts.screen.ConfirmationBottomSheet
import cash.p.terminal.modules.manageaccount.dialogs.BackupRequiredDialog
import cash.p.terminal.modules.manageaccounts.ManageAccountsModule
import cash.p.terminal.modules.qrscanner.QRScannerActivity
import cash.p.terminal.modules.walletconnect.WCAccountTypeNotSupportedDialog
import cash.p.terminal.modules.walletconnect.WCManager
import cash.p.terminal.modules.walletconnect.list.WalletConnectListViewModel
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui_compose.components.title3_leah
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BalanceForAccount(navController: NavController, accountViewItem: AccountViewItem) {
    val viewModel = viewModel<BalanceViewModel>(factory = BalanceModule.Factory())

    val context = LocalContext.current
    val invalidUrlBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val coroutineScope = rememberCoroutineScope()
    val qrScannerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.handleScannedData(
                    result.data?.getStringExtra(ModuleField.SCAN_ADDRESS) ?: ""
                )
            }
        }

    viewModel.uiState.errorMessage?.let { message ->
        val view = LocalView.current
        HudHelper.showErrorMessage(view, text = message)
        viewModel.errorShown()
    }

    when (viewModel.connectionResult) {
        WalletConnectListViewModel.ConnectionResult.Error -> {
            LaunchedEffect(viewModel.connectionResult) {
                coroutineScope.launch {
                    delay(300)
                    invalidUrlBottomSheetState.show()
                }
            }
            viewModel.onHandleRoute()
        }

        else -> Unit
    }


    BackupAlert(navController)
    ModalBottomSheetLayout(
        sheetState = invalidUrlBottomSheetState,
        sheetBackgroundColor = cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.transparent,
        sheetContent = {
            ConfirmationBottomSheet(
                title = stringResource(R.string.WalletConnect_Title),
                text = stringResource(R.string.WalletConnect_Error_InvalidUrl),
                iconPainter = painterResource(R.drawable.ic_wallet_connect_24),
                iconTint = ColorFilter.tint(cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.jacob),
                confirmText = stringResource(R.string.Button_TryAgain),
                cautionType = Caution.Type.Warning,
                cancelText = stringResource(R.string.Button_Cancel),
                onConfirm = {
                    coroutineScope.launch {
                        invalidUrlBottomSheetState.hide()
                        qrScannerLauncher.launch(QRScannerActivity.getScanQrIntent(context, true))
                    }
                },
                onClose = {
                    coroutineScope.launch { invalidUrlBottomSheetState.hide() }
                }
            )
        }
    ) {
        Scaffold(
            backgroundColor = cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = {
                        BalanceTitleRow(navController, accountViewItem.name)
                    },
                    menuItems = buildList {
                        if (accountViewItem.type.supportsWalletConnect) {
                            add(
                                MenuItem(
                                    title = TranslatableString.ResString(R.string.WalletConnect_NewConnect),
                                    icon = R.drawable.ic_qr_scan_20,
                                    onClick = {
                                        when (val state =
                                            viewModel.getWalletConnectSupportState()) {
                                            WCManager.SupportState.Supported -> {
                                                qrScannerLauncher.launch(
                                                    QRScannerActivity.getScanQrIntent(context, true)
                                                )

                                                stat(
                                                    page = StatPage.Balance,
                                                    event = StatEvent.Open(StatPage.ScanQrCode)
                                                )
                                            }

                                            WCManager.SupportState.NotSupportedDueToNoActiveAccount -> {
                                                navController.slideFromBottom(R.id.wcErrorNoAccountFragment)
                                            }

                                            is WCManager.SupportState.NotSupportedDueToNonBackedUpAccount -> {
                                                val text =
                                                    cash.p.terminal.strings.helpers.Translator.getString(
                                                        R.string.WalletConnect_Error_NeedBackup
                                                    )
                                                navController.slideFromBottom(
                                                    R.id.backupRequiredDialog,
                                                    BackupRequiredDialog.Input(state.account, text)
                                                )

                                                stat(
                                                    page = StatPage.Balance,
                                                    event = StatEvent.Open(StatPage.BackupRequired)
                                                )
                                            }

                                            is WCManager.SupportState.NotSupported -> {
                                                navController.slideFromBottom(
                                                    R.id.wcAccountTypeNotSupportedDialog,
                                                    WCAccountTypeNotSupportedDialog.Input(state.accountTypeDescription)
                                                )
                                            }
                                        }
                                    }
                                )
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            val uiState = viewModel.uiState

            Crossfade(
                targetState = uiState.viewState,
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                label = ""
            ) { viewState ->
                when (viewState) {
                    ViewState.Success -> {
                        val balanceViewItems = uiState.balanceViewItems
                        BalanceItems(
                            balanceViewItems,
                            viewModel,
                            accountViewItem,
                            navController,
                            uiState,
                            viewModel.totalUiState,
                        )
                    }

                    ViewState.Loading,
                    is ViewState.Error,
                    null -> {
                    }
                }
            }
        }
    }
}

@Composable
fun BalanceTitleRow(
    navController: NavController,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        title3_leah(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(weight = 1f, fill = false)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            painter = painterResource(id = R.drawable.ic_down_24),
            contentDescription = null,
            tint = ComposeAppTheme.colors.yellowD,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    navController.slideFromBottom(
                        R.id.manageAccountsFragment,
                        ManageAccountsModule.Mode.Switcher
                    )

                    stat(page = StatPage.Balance, event = StatEvent.Open(StatPage.ManageWallets))
                },
        )
    }
}