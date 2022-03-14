package io.horizontalsystems.bankwallet.modules.walletconnect.list.ui

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListModule
import io.horizontalsystems.bankwallet.modules.walletconnect.list.v1.WalletConnectListViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.list.v2.WC2ListViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v1.WCSessionModule
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v2.WC2SessionModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem

@Composable
fun WCSessionsScreen(navController: NavController) {
    val qrScannerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val scannedText = result.data?.getStringExtra(ModuleField.SCAN_ADDRESS) ?: ""
                val wcVersion: Int = WalletConnectListModule.getVersionFromUri(scannedText)
                if (wcVersion == 1) {
                    navController.slideFromBottom(
                        R.id.wcSessionFragment,
                        WCSessionModule.prepareParams(null, scannedText)
                    )
                } else if (wcVersion == 2) {
                    navController.slideFromBottom(
                        R.id.wc2SessionFragment,
                        WC2SessionModule.prepareParams(null, scannedText)
                    )
                }
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

    Column(
        modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)
    ) {
        AppBar(
            TranslatableString.ResString(R.string.WalletConnect_Title),
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
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
                WCSessionsEmpty(qrScannerLauncher)
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
}
