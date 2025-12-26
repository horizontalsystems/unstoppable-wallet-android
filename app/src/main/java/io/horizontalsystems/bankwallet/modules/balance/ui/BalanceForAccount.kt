package io.horizontalsystems.bankwallet.modules.balance.ui

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
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.balance.AccountViewItem
import io.horizontalsystems.bankwallet.modules.balance.BalanceModule
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewModel
import io.horizontalsystems.bankwallet.modules.manageaccount.dialogs.BackupRequiredAlert
import io.horizontalsystems.bankwallet.modules.manageaccount.dialogs.BackupRequiredDialog
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.bankwallet.modules.walletconnect.WCAccountTypeNotSupportedDialog
import io.horizontalsystems.bankwallet.modules.walletconnect.WCManager
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.list.ui.WCInvalidUrlBottomSheet
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItemLoading
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalanceForAccount(navController: NavController, accountViewItem: AccountViewItem) {
    val viewModel = viewModel<BalanceViewModel>(factory = BalanceModule.Factory())

    val context = LocalContext.current
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState()
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
                    sheetState.show()
                    isWCInvalidUrlBottomSheetVisible = true
                }
            }
            viewModel.onHandleRoute()
        }

        else -> Unit
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
                            onScanClick(
                                viewModel,
                                qrScannerLauncher,
                                context,
                                navController
                            )
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
                        viewModel.totalUiState,
                    ) {
                        onScanClick(viewModel, qrScannerLauncher, context, navController)
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
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) {
                        isWCInvalidUrlBottomSheetVisible = false
                    }
                }

                qrScannerLauncher.launch(QRScannerActivity.getScanQrIntent(context, true))
            },
            onDismiss = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) {
                        isWCInvalidUrlBottomSheetVisible = false
                    }
                }
            }
        )
    }
}

private fun onScanClick(
    viewModel: BalanceViewModel,
    qrScannerLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    context: Context,
    navController: NavController
) {
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
