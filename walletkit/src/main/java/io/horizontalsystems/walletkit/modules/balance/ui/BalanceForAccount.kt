package io.horizontalsystems.walletkit.modules.balance.ui

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
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.stats.StatEvent
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.core.stats.stat
import io.horizontalsystems.walletkit.core.utils.ModuleField
import io.horizontalsystems.walletkit.entities.ViewState
import io.horizontalsystems.walletkit.helpers.HudHelper
import io.horizontalsystems.walletkit.modules.balance.AccountViewItem
import io.horizontalsystems.walletkit.modules.balance.BalanceModule
import io.horizontalsystems.walletkit.modules.balance.BalanceViewModel
import io.horizontalsystems.walletkit.modules.manageaccount.dialogs.BackupRequiredAlert
import io.horizontalsystems.walletkit.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.walletkit.modules.manageaccounts.ManageAccountsPage
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.opencryptopay.OpenCryptoPayPage
import io.horizontalsystems.walletkit.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.walletkit.modules.transactions.TransactionsPage
import io.horizontalsystems.walletkit.modules.walletconnect.WCAccountTypeNotSupportedSheet
import io.horizontalsystems.walletkit.modules.walletconnect.WCErrorNoAccountSheet
import io.horizontalsystems.walletkit.modules.walletconnect.WCManager
import io.horizontalsystems.walletkit.modules.walletconnect.list.WalletConnectListViewModel
import io.horizontalsystems.walletkit.modules.walletconnect.list.ui.WCInvalidUrlBottomSheet
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import io.horizontalsystems.walletkit.ui.compose.components.MenuItem
import io.horizontalsystems.walletkit.uiv3.components.HSScaffold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalanceForAccount(
    navigation: HSNavigation,
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
                    navigation.slideFromBottom(WCErrorNoAccountSheet)
                }

                is WCManager.SupportState.NotSupported -> {
                    navigation.slideFromBottom(
                        WCAccountTypeNotSupportedSheet(WCAccountTypeNotSupportedSheet.Input(state.accountTypeDescription))
                    )
                }
            }
        }
        viewModel.onWalletConnectRequestHandled()
    }

    val uiState = viewModel.uiState

    LaunchedEffect(uiState.openOcpPayment) {
        uiState.openOcpPayment?.let { lnurl ->
            navigation.slideFromRight(
                OpenCryptoPayPage(OpenCryptoPayPage.Input(lnurl))
            )
            viewModel.onOcpPaymentOpened()
        }
    }

    BackupRequiredAlert(navigation)

    HSScaffold(
        title = accountViewItem.name,
        menuItems = buildList {
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
                    title = TranslatableString.ResString(R.string.Transactions_Title),
                    icon = R.drawable.ic_circle_clock_24,
                    onClick = {
                        navigation.slideFromRight(TransactionsPage)

                        stat(
                            page = StatPage.Balance,
                            event = StatEvent.Open(StatPage.Transactions)
                        )
                    }
                )
            )
            add(
                MenuItem(
                    title = TranslatableString.ResString(R.string.ManageAccounts_Title),
                    icon = R.drawable.ic_wallet_switch_24,
                    onClick = {
                        navigation.slideFromRight(
                            ManageAccountsPage(ManageAccountsModule.Mode.Switcher)
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
                        navigation,
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
