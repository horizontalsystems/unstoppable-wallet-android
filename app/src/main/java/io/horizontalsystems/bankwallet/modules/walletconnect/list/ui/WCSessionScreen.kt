package io.horizontalsystems.bankwallet.modules.walletconnect.list.ui

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListModule
import io.horizontalsystems.bankwallet.modules.walletconnect.list.v1.WalletConnectListViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.list.v2.WC2ListViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import kotlinx.coroutines.delay

@Composable
fun WCSessionsScreen(
    navController: NavController,
    onAddressScanned: (String) -> Unit
) {
    val qrScannerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val scannedText = result.data?.getStringExtra(ModuleField.SCAN_ADDRESS) ?: ""
                onAddressScanned(scannedText)
            }
        }

    ComposeAppTheme {
        WCSessionsContent(
            navController,
            qrScannerLauncher,
        )
    }
}

@Composable
fun WCSessionsContent(
    navController: NavController,
    qrScannerLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    viewModel: WalletConnectListViewModel = viewModel(factory = WalletConnectListModule.Factory()),
    viewModelWc2: WC2ListViewModel = viewModel(factory = WalletConnectListModule.FactoryWC2())
) {
    val context = LocalContext.current
    val noSessions = viewModel.sectionItem == null && viewModelWc2.sectionItem == null

    LaunchedEffect(Unit) {
        if (!viewModel.initialConnectionPrompted && noSessions) {
            delay(300)
            viewModel.initialConnectionPrompted = true
            qrScannerLauncher.launch(QRScannerActivity.getScanQrIntent(context, true))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = ComposeAppTheme.colors.tyler)
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            AppBar(
                TranslatableString.ResString(R.string.WalletConnect_Title),
                navigationIcon = {
                    HsIconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            tint = ComposeAppTheme.colors.jacob,
                            contentDescription = null,
                        )
                    }
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.WalletConnect_NewConnect),
                        icon = R.drawable.ic_qr_scan_24px,
                        onClick = {
                            qrScannerLauncher.launch(QRScannerActivity.getScanQrIntent(context, true))
                        }
                    )
                )
            )
            when {
                noSessions -> {
                    ListEmptyView(
                        text = stringResource(R.string.WalletConnect_NoConnection),
                        icon = R.drawable.ic_wallet_connet_48
                    )
                }
                else -> {
                    WCSessionList(
                        viewModelWc2,
                        viewModel,
                        navController
                    )
                }
            }
        }
        ButtonsGroupWithShade {
            ButtonPrimaryYellow(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
                title = stringResource(R.string.WalletConnect_NewConnect),
                onClick = { qrScannerLauncher.launch(QRScannerActivity.getScanQrIntent(context, true)) }
            )
        }
    }
}
