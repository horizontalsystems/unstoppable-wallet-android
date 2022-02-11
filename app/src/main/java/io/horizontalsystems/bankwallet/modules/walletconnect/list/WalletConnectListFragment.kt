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
import io.horizontalsystems.bankwallet.modules.walletconnect.session.WalletConnectSessionModule
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
    val viewModel =
        viewModel<WalletConnectListViewModel>(factory = WalletConnectListModule.Factory())

    val qrScannerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val scannedText = result.data?.getStringExtra(ModuleField.SCAN_ADDRESS) ?: ""
                navController.slideFromBottom(
                    R.id.walletConnectMainFragment,
                    WalletConnectSessionModule.prepareParams(null, scannedText)
                )
            }
        }

    val sections by viewModel.sectionsLiveData.observeAsState(listOf())

    ComposeAppTheme {
        SessionsScreen(
            navController,
            qrScannerLauncher,
            sections,
        )
    }
}

@Composable
private fun SessionsScreen(
    navController: NavController,
    qrScannerLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    sections: List<WalletConnectListModule.Section>,
) {
    val context = LocalContext.current

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
            sections.isEmpty() -> {
                WCSessionsEmpty(qrScannerLauncher)
            }
            else -> {
                sections.forEach { WCSessionList(it, navController) }
            }
        }
    }
}
