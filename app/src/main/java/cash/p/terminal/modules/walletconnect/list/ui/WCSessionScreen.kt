package cash.p.terminal.modules.walletconnect.list.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material3.Scaffold
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.Caution
import cash.p.terminal.core.managers.FaqManager
import cash.p.terminal.core.utils.ModuleField
import cash.p.terminal.modules.contacts.screen.ConfirmationBottomSheet
import cash.p.terminal.modules.evmfee.ButtonsGroupWithShade
import cash.p.terminal.modules.qrscanner.QRScannerActivity
import cash.p.terminal.modules.walletconnect.list.WalletConnectListModule
import cash.p.terminal.modules.walletconnect.list.WalletConnectListUiState
import cash.p.terminal.modules.walletconnect.list.WalletConnectListViewModel
import cash.p.terminal.modules.walletconnect.list.WalletConnectListViewModel.ConnectionResult
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui.compose.components.ListEmptyView
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WCSessionsScreen(
    navController: NavController,
    deepLinkUri: String?,
    windowInsets: WindowInsets = NavigationBarDefaults.windowInsets,
) {
    val context = LocalContext.current
    val invalidUrlBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val coroutineScope = rememberCoroutineScope()

    val viewModel = viewModel<WalletConnectListViewModel>(
        factory = WalletConnectListModule.Factory()
    )
    val qrScannerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.setConnectionUri(
                    result.data?.getStringExtra(ModuleField.SCAN_ADDRESS) ?: ""
                )
            }
        }

    val uiState by viewModel.uiState.collectAsState(initial = WalletConnectListUiState())

    when (viewModel.connectionResult) {
        ConnectionResult.Error -> {
            LaunchedEffect(viewModel.connectionResult) {
                coroutineScope.launch {
                    delay(300)
                    invalidUrlBottomSheetState.show()
                }
            }
            viewModel.onRouteHandled()
        }

        else -> Unit
    }

    LaunchedEffect(Unit) {
        if (deepLinkUri != null) {
            viewModel.setConnectionUri(deepLinkUri)
        }
    }

    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        viewModel.refreshList()
    }

    ModalBottomSheetLayout(
        sheetState = invalidUrlBottomSheetState,
        sheetBackgroundColor = ComposeAppTheme.colors.transparent,
        sheetContent = {
            ConfirmationBottomSheet(
                title = stringResource(R.string.WalletConnect_Title),
                text = stringResource(R.string.WalletConnect_Error_InvalidUrl),
                iconPainter = painterResource(R.drawable.ic_wallet_connect_24),
                iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob),
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
            containerColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = stringResource(R.string.WalletConnect_Title),
                    navigationIcon = {
                        HsBackButton(onClick = { navController.popBackStack() })
                    },
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Info_Title),
                            icon = R.drawable.ic_info_24,
                            onClick = {
                                FaqManager.showFaqPage(navController, FaqManager.faqPathDefiRisks)
                            }
                        )
                    )
                )
            }
        ) { innerPaddings ->
            Column(
                modifier = Modifier
                    .windowInsetsPadding(windowInsets)
                    .padding(innerPaddings)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (uiState.sessionViewItems.isEmpty() && uiState.pairingsNumber == 0) {
                        ListEmptyView(
                            text = stringResource(R.string.WalletConnect_NoConnection),
                            icon = R.drawable.ic_wallet_connet_48
                        )
                    } else {
                        WCSessionList(
                            viewModel,
                            navController
                        )
                    }
                }
                ButtonsGroupWithShade {
                    ButtonPrimaryYellow(
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp)
                            .fillMaxWidth(),
                        title = stringResource(R.string.WalletConnect_NewConnect),
                        onClick = {
                            qrScannerLauncher.launch(
                                QRScannerActivity.getScanQrIntent(
                                    context,
                                    true
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}
