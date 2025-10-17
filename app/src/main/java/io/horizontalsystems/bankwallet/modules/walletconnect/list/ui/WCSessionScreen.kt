package io.horizontalsystems.bankwallet.modules.walletconnect.list.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Caution
import io.horizontalsystems.bankwallet.core.managers.FaqManager
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.modules.contacts.screen.ConfirmationBottomSheet
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListModule
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListUiState
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListViewModel.ConnectionResult
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WCSessionsScreen(
    navController: NavController,
    deepLinkUri: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

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
                scope.launch {
                    delay(300)
                    showBottomSheet = true
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

    HSScaffold(
        title = stringResource(R.string.WalletConnect_Title),
        onBack = navController::popBackStack,
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Info_Title),
                icon = R.drawable.ic_info_24,
                tint = ComposeAppTheme.colors.grey,
                onClick = {
                    FaqManager.showFaqPage(navController, FaqManager.faqPathDefiRisks)
                }
            )
        )
    ) {
        Column {
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
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showBottomSheet = false
                },
                sheetState = sheetState,
                containerColor = ComposeAppTheme.colors.transparent
            ) {
                ConfirmationBottomSheet(
                    title = stringResource(R.string.WalletConnect_Title),
                    text = stringResource(R.string.WalletConnect_Error_InvalidUrl),
                    iconPainter = painterResource(R.drawable.ic_wallet_connect_24),
                    iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob),
                    confirmText = stringResource(R.string.Button_TryAgain),
                    cautionType = Caution.Type.Warning,
                    cancelText = stringResource(R.string.Button_Cancel),
                    onConfirm = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showBottomSheet = false
                            }
                        }

                        qrScannerLauncher.launch(QRScannerActivity.getScanQrIntent(context, true))
                    },
                    onClose = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showBottomSheet = false
                            }
                        }
                    }
                )
            }
        }
    }
}
