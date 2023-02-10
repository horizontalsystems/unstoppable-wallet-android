package io.horizontalsystems.bankwallet.modules.walletconnect.list.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.managers.FaqManager
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListModule
import io.horizontalsystems.bankwallet.modules.walletconnect.list.v1.WalletConnectListViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v1.WCSessionModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.DisposableLifecycleCallbacks
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.delay

@Composable
fun WCSessionsScreen(
    navController: NavController,
    deepLinkUri: String?
) {
    val context = LocalContext.current
    val view = LocalView.current

    val viewModel = viewModel<WalletConnectListViewModel>(
        factory = WalletConnectListModule.Factory()
    )
    val qrScannerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.setConnectionUri(result.data?.getStringExtra(ModuleField.SCAN_ADDRESS) ?: "")
            }
        }
    val uiState = viewModel.uiState

    when (val route = viewModel.route) {
        is WalletConnectListViewModel.Route.WC1Session -> {
            navController.slideFromBottom(
                R.id.wcSessionFragment,
                WCSessionModule.prepareParams(null, route.uri)
            )
            viewModel.onHandleRoute()
        }
        WalletConnectListViewModel.Route.Error -> {
            HudHelper.showErrorMessage(view, R.string.WalletConnect_Error_InvalidUrl)
            viewModel.onHandleRoute()
        }
        null -> Unit
    }

    LaunchedEffect(Unit) {
        if (deepLinkUri != null) {
            viewModel.setConnectionUri(deepLinkUri)
        } else if (!viewModel.initialConnectionPrompted && uiState.v1SectionItem == null && uiState.v2SectionItem == null) {
            delay(300)
            viewModel.initialConnectionPrompted = true
            qrScannerLauncher.launch(QRScannerActivity.getScanQrIntent(context, true))
        }
    }

    DisposableLifecycleCallbacks(
        onResume = {
            viewModel.refreshPairingsNumber()
        }
    )

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
            if (uiState.emptyScreen) {
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
                onClick = { qrScannerLauncher.launch(QRScannerActivity.getScanQrIntent(context, true)) }
            )
        }
    }
}
