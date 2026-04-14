package com.quantum.wallet.bankwallet.modules.balance.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.providers.Translator
import com.quantum.wallet.bankwallet.core.slideFromBottom
import com.quantum.wallet.bankwallet.core.slideFromRight
import com.quantum.wallet.bankwallet.core.stats.StatEvent
import com.quantum.wallet.bankwallet.core.stats.StatPage
import com.quantum.wallet.bankwallet.core.stats.stat
import com.quantum.wallet.bankwallet.core.utils.ModuleField
import com.quantum.wallet.bankwallet.entities.ViewState
import com.quantum.wallet.bankwallet.modules.balance.AccountViewItem
import com.quantum.wallet.bankwallet.modules.balance.BalanceModule
import com.quantum.wallet.bankwallet.modules.balance.BalanceViewModel
import com.quantum.wallet.bankwallet.modules.manageaccount.dialogs.BackupRequiredAlert
import com.quantum.wallet.bankwallet.modules.manageaccount.dialogs.BackupRequiredDialog
import com.quantum.wallet.bankwallet.modules.manageaccounts.ManageAccountsModule
import com.quantum.wallet.bankwallet.modules.qrscanner.QRScannerActivity
import com.quantum.wallet.bankwallet.modules.walletconnect.WCAccountTypeNotSupportedDialog
import com.quantum.wallet.bankwallet.modules.walletconnect.WCManager
import com.quantum.wallet.bankwallet.modules.walletconnect.list.WalletConnectListViewModel
import com.quantum.wallet.bankwallet.modules.walletconnect.list.ui.WCInvalidUrlBottomSheet
import com.quantum.wallet.bankwallet.ui.compose.TranslatableString
import com.quantum.wallet.bankwallet.ui.compose.components.MenuItem
import com.quantum.wallet.bankwallet.ui.compose.components.MenuItemLoading
import com.quantum.wallet.bankwallet.uiv3.components.HSScaffold
import com.quantum.wallet.core.helpers.HudHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalanceForAccount(
    navController: NavController,
    accountViewItem: AccountViewItem,
) {
    val viewModel = viewModel<BalanceViewModel>(factory = BalanceModule.Factory())

    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    var isWCInvalidUrlBottomSheetVisible by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
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
                scope.launch {
                    delay(300)
                    isWCInvalidUrlBottomSheetVisible = true
                }
            }
            viewModel.onHandleRoute()
        }

        else -> Unit
    }

    LaunchedEffect(viewModel.walletConnectRequest) {
        viewModel.walletConnectRequest?.let { request ->
            when (val state = viewModel.getWalletConnectSupportState()) {
                WCManager.SupportState.Supported -> {
                    viewModel.connectWC(request)
                }

                WCManager.SupportState.NotSupportedDueToNoActiveAccount -> {
                    navController.slideFromBottom(R.id.wcErrorNoAccountFragment)
                }

                is WCManager.SupportState.NotSupportedDueToNonBackedUpAccount -> {
                    val text =
                        Translator.getString(R.string.WalletConnect_Error_NeedBackup)
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
        viewModel.onWalletConnectRequestHandled()
    }

    BackupRequiredAlert(navController)
    val uiState = viewModel.uiState

    HSScaffold(
        title = accountViewItem.name,
        menuItems = buildList {
            if (uiState.loading) {
                add(MenuItemLoading)
            }

            if (!viewModel.uiState.balanceTabButtonsEnabled && !accountViewItem.isWatchAccount) {
                add(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.WalletConnect_NewConnect),
                        icon = R.drawable.ic_scan_24,
                        onClick = {
                            onScanClick(qrScannerLauncher, context)
                        }
                    )
                )
            }
            add(
                MenuItem(
                    title = TranslatableString.ResString(R.string.ManageAccounts_Title),
                    icon = R.drawable.ic_wallet_switch_24,
                    onClick = {
                        navController.slideFromRight(
                            R.id.manageAccountsFragment,
                            ManageAccountsModule.Mode.Switcher
                        )

                        stat(
                            page = StatPage.Balance,
                            event = StatEvent.Open(StatPage.ManageWallets)
                        )
                    }
                )
            )
        }
    ) {
        Crossfade(
            targetState = uiState.viewState,
            modifier = Modifier.fillMaxSize(),
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
                    ) {
                        onScanClick(qrScannerLauncher, context)
                    }
                }

                ViewState.Loading,
                is ViewState.Error,
                null -> {
                }
            }
        }
    }
    if (isWCInvalidUrlBottomSheetVisible) {
        WCInvalidUrlBottomSheet(
            sheetState = sheetState,
            onConfirm = {
                scope.launch {
                    sheetState.hide()
                    isWCInvalidUrlBottomSheetVisible = false
                    qrScannerLauncher.launch(QRScannerActivity.getScanQrIntent(context, true))
                }
            },
            onDismiss = {
                scope.launch {
                    sheetState.hide()
                    isWCInvalidUrlBottomSheetVisible = false
                }
            }
        )
    }
}

private fun onScanClick(
    qrScannerLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    context: Context
) {
    qrScannerLauncher.launch(
        QRScannerActivity.getScanQrIntent(context, true)
    )

    stat(
        page = StatPage.Balance,
        event = StatEvent.Open(StatPage.ScanQrCode)
    )
}
