package io.horizontalsystems.bankwallet.modules.settings.security

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
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
import io.horizontalsystems.bankwallet.ui.compose.components.HsIconButton
import io.horizontalsystems.bankwallet.ui.extensions.ConfirmationDialog
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.getNavigationResult
import io.horizontalsystems.pin.PinInteractionType
import io.horizontalsystems.pin.PinModule
import io.horizontalsystems.views.AlertDialogFragment
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
                        showNotificationsNotEnabledAlert = { showNotificationsNotEnabledAlert() },
                        restartApp = { restartApp() }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subscribeFragmentResult()
    }

    private fun showAppRestartAlert() {
        ConfirmationDialog.show(
            icon = R.drawable.ic_tor_connection_24,
            title = getString(R.string.Tor_Title),
            subtitle = getString(R.string.Tor_Connection_Title),
            contentText = getString(R.string.SettingsSecurity_AppRestartWarning),
            actionButtonTitle = getString(R.string.Alert_Restart),
            cancelButtonTitle = null, // Do not show cancel button
            fragmentManager = childFragmentManager,
            listener = object : ConfirmationDialog.Listener {
                override fun onActionButtonClick() {
                    torViewModel.setTorEnabled(torViewModel.torCheckEnabled)
                }

                override fun onCancelButtonClick() {
                    torViewModel.resetSwitch()
                }
            }
        )
    }

    private fun showNotificationsNotEnabledAlert() {
        AlertDialogFragment.newInstance(
            descriptionString = getString(R.string.SettingsSecurity_NotificationsDisabledWarning),
            buttonText = R.string.Button_Enable,
            cancelButtonText = R.string.Alert_Cancel,
            cancelable = true,
            listener = object : AlertDialogFragment.Listener {
                override fun onButtonClick() {
                    openAppNotificationSettings()
                }

                override fun onCancel() {}

            }).show(childFragmentManager, "alert_dialog_notification")
    }

    private fun openAppNotificationSettings() {
        val intent = Intent()
        intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
        intent.putExtra("android.provider.extra.APP_PACKAGE", context?.packageName)
        startActivity(intent)
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

    private fun subscribeFragmentResult() {
        getNavigationResult(PinModule.requestKey)?.let { bundle ->
            val resultType = bundle.getParcelable<PinInteractionType>(PinModule.requestType)
            val resultCode = bundle.getInt(PinModule.requestResult)

            if (resultCode == PinModule.RESULT_OK) {
                when (resultType) {
                    PinInteractionType.SET_PIN -> passcodeViewModel.didSetPin()
                    PinInteractionType.UNLOCK -> passcodeViewModel.disablePin()
                    else -> Unit
                }
            }
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
    showNotificationsNotEnabledAlert: () -> Unit,
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
                contentPadding = PaddingValues(top = 12.dp, bottom = 30.dp)
            ) {

                item {
                    PasscodeBlock(passcodeViewModel, navController)
                }

                item {
                    CoinListHeader(R.string.SecurityCenter_Internet)
                    TorBlock(
                        torViewModel,
                        showAppRestartAlert,
                        showNotificationsNotEnabledAlert,
                    )
                }

                item {
                    CoinListHeader(R.string.SecurityCenter_BlockchainSettings)
                    BlockchainSettingsBlock(blockchainSettingsViewModel, navController)
                }

            }
        }

    }

}

@Composable
fun CoinListHeader(titleTextRes: Int) {
    Text(
        modifier = Modifier.padding(start = 16.dp, top = 44.dp, end = 16.dp, bottom = 13.dp),
        text = stringResource(titleTextRes),
        style = ComposeAppTheme.typography.subhead1,
        color = ComposeAppTheme.colors.grey,
    )
}
