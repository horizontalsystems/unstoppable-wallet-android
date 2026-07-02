package io.horizontalsystems.core.modules.balance.ui

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
import io.horizontalsystems.core.R
import io.horizontalsystems.core.core.stats.StatEvent
import io.horizontalsystems.core.core.stats.StatPage
import io.horizontalsystems.core.core.stats.stat
import io.horizontalsystems.core.core.utils.ModuleField
import io.horizontalsystems.core.entities.ViewState
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.modules.balance.AccountViewItem
import io.horizontalsystems.core.modules.balance.BalanceModule
import io.horizontalsystems.core.modules.balance.BalanceViewModel
import io.horizontalsystems.core.modules.manageaccount.dialogs.BackupRequiredAlert
import io.horizontalsystems.core.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.core.modules.manageaccounts.ManageAccountsPage
import io.horizontalsystems.core.modules.nav3.HSNavigation
import io.horizontalsystems.core.modules.opencryptopay.OpenCryptoPayPage
import io.horizontalsystems.core.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.core.modules.transactions.TransactionsPage
import io.horizontalsystems.core.modules.walletconnect.WCAccountTypeNotSupportedSheet
import io.horizontalsystems.core.modules.walletconnect.WCErrorNoAccountSheet
import io.horizontalsystems.core.modules.walletconnect.WCManager
import io.horizontalsystems.core.modules.walletconnect.list.WalletConnectListViewModel
import io.horizontalsystems.core.modules.walletconnect.list.ui.WCInvalidUrlBottomSheet
import io.horizontalsystems.core.ui.compose.TranslatableString
import io.horizontalsystems.core.ui.compose.components.MenuItem
import io.horizontalsystems.core.uiv3.components.HSScaffold
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
