package io.horizontalsystems.bankwallet.modules.settings.security

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.modules.settings.security.blockchains.BlockchainSettingsModule
import io.horizontalsystems.bankwallet.modules.settings.security.blockchains.BlockchainSettingsViewModel
import io.horizontalsystems.bankwallet.modules.settings.security.passcode.SecurityPasscodeSettingsModule
import io.horizontalsystems.bankwallet.modules.settings.security.passcode.SecurityPasscodeSettingsViewModel
import io.horizontalsystems.bankwallet.modules.settings.security.tor.SecurityTorSettingsModule
import io.horizontalsystems.bankwallet.modules.settings.security.tor.SecurityTorSettingsViewModel
import io.horizontalsystems.bankwallet.modules.settings.security.ui.BlockchainSettingsBlock
import io.horizontalsystems.bankwallet.modules.settings.security.ui.PasscodeBlock
import io.horizontalsystems.bankwallet.modules.settings.security.ui.TorBlock
import io.horizontalsystems.bankwallet.modules.tor.TorConnectionActivity
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderText
import io.horizontalsystems.bankwallet.ui.compose.components.HsIconButton
import io.horizontalsystems.bankwallet.ui.extensions.ConfirmationDialog
import io.horizontalsystems.core.findNavController
import kotlin.system.exitProcess

class SecuritySettingsFragment : BaseFragment() {

    private val blockchainSettingsViewModel by viewModels<BlockchainSettingsViewModel> {
        BlockchainSettingsModule.Factory()
    }

    private val torViewModel by viewModels<SecurityTorSettingsViewModel> {
        SecurityTorSettingsModule.Factory()
    }

    private val passcodeViewModel by viewModels<SecurityPasscodeSettingsViewModel> {
        SecurityPasscodeSettingsModule.Factory()
    }

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
                ComposeAppTheme {
                    SecurityCenterScreen(
                        passcodeViewModel = passcodeViewModel,
                        torViewModel = torViewModel,
                        blockchainSettingsViewModel = blockchainSettingsViewModel,
                        navController = findNavController(),
                        showAppRestartAlert = { showAppRestartAlert() },
                        restartApp = { restartApp() },
                    )
                }
            }
        }
    }

    private fun showAppRestartAlert() {
        val warningTitle =
            if (torViewModel.torCheckEnabled)
                getString(R.string.Tor_Connection_Enable)
            else
                getString(R.string.Tor_Connection_Disable)

        ConfirmationDialog.show(
            icon = R.drawable.ic_tor_connection_24,
            title = getString(R.string.Tor_Alert_Title),
            warningTitle = warningTitle,
            warningText = getString(R.string.SettingsSecurity_AppRestartWarning),
            actionButtonTitle = getString(R.string.Alert_Restart),
            transparentButtonTitle = getString(R.string.Alert_Cancel),
            fragmentManager = childFragmentManager,
            listener = object : ConfirmationDialog.Listener {
                override fun onActionButtonClick() {
                    torViewModel.setTorEnabled(torViewModel.torCheckEnabled)
                }

                override fun onTransparentButtonClick() {
                    torViewModel.resetSwitch()
                }

                override fun onCancelButtonClick() {
                    torViewModel.resetSwitch()
                }
            }
        )
    }

    private fun restartApp() {
        activity?.let {
            MainModule.startAsNewTask(it)
            if (App.localStorage.torEnabled) {
                val intent = Intent(it, TorConnectionActivity::class.java)
                startActivity(intent)
            }
            exitProcess(0)
        }
    }
}

@Composable
private fun SecurityCenterScreen(
    passcodeViewModel: SecurityPasscodeSettingsViewModel,
    torViewModel: SecurityTorSettingsViewModel,
    blockchainSettingsViewModel: BlockchainSettingsViewModel,
    navController: NavController,
    showAppRestartAlert: () -> Unit,
    restartApp: () -> Unit,
) {

    if (torViewModel.restartApp) {
        restartApp()
        torViewModel.appRestarted()
    }

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
                TranslatableString.ResString(R.string.Settings_SecurityCenter),
                navigationIcon = {
                    HsIconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "back button",
                            tint = ComposeAppTheme.colors.jacob
                        )
                    }
                },
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
            ) {

                item {
                    PasscodeBlock(
                        passcodeViewModel,
                        navController
                    )
                }

                item {
                    Spacer(Modifier.height(24.dp))
                    HeaderText(stringResource(R.string.SecurityCenter_Internet))
                    TorBlock(
                        torViewModel,
                        showAppRestartAlert,
                    )
                }

                item {
                    Spacer(Modifier.height(24.dp))
                    HeaderText(stringResource(R.string.SecurityCenter_BlockchainSettings))
                    BlockchainSettingsBlock(blockchainSettingsViewModel, navController)
                }

            }
        }

    }

}
