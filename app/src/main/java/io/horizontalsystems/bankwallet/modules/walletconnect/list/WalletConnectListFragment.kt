package io.horizontalsystems.bankwallet.modules.walletconnect.list

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.bankwallet.modules.walletconnect.list.ui.WCSessionList
import io.horizontalsystems.bankwallet.modules.walletconnect.list.ui.WCSessionsEmpty
import io.horizontalsystems.bankwallet.modules.walletconnect.list.v1.WalletConnectListViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.list.v2.WC2ListViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v1.WalletConnectSessionModule
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v2.WC2SessionModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.core.findNavController

class WalletConnectListFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                WalletConnectSessionsScreen(findNavController())
            }
        }
    }
}

@Composable
private fun WalletConnectSessionsScreen(navController: NavController) {
    val qrScannerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val scannedText = result.data?.getStringExtra(ModuleField.SCAN_ADDRESS) ?: ""
                val wcVersion: Int = WalletConnectListModule.getVersionFromUri(scannedText)
                if (wcVersion == 1) {
                    navController.slideFromBottom(
                        R.id.walletConnectMainFragment,
                        WalletConnectSessionModule.prepareParams(null, scannedText)
                    )
                } else if(wcVersion == 2) {
                    navController.slideFromBottom(
                        R.id.wc2SessionFragment,
                        WC2SessionModule.prepareParams(null, scannedText)
                    )
                }
            }
        }

    ComposeAppTheme {
        SessionsScreen(
            navController,
            qrScannerLauncher,
        )
    }
}

@Composable
private fun SessionsScreen(
    navController: NavController,
    qrScannerLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    viewModel: WalletConnectListViewModel = viewModel(factory = WalletConnectListModule.Factory()),
    viewModelWc2: WC2ListViewModel = viewModel(factory = WalletConnectListModule.FactoryWC2())
) {
    val context = LocalContext.current

    val sectionWc1 by viewModel.sectionLiveData.observeAsState()
    val sectionWc2 by viewModelWc2.sectionsLiveData.observeAsState()
    val noSessions = sectionWc1 == null && sectionWc2 == null

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
                sectionWc2?.let { WCSessionList(it, navController) }
                sectionWc1?.let { WCSessionList(it, navController) }
            }
        }
    }
}
