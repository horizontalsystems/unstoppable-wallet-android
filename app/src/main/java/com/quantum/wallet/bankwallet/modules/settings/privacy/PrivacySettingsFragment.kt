package com.quantum.wallet.bankwallet.modules.settings.privacy

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.core.stats.StatEvent
import com.quantum.wallet.bankwallet.core.stats.StatPage
import com.quantum.wallet.bankwallet.core.stats.stat
import com.quantum.wallet.bankwallet.modules.main.MainModule
import com.quantum.wallet.bankwallet.modules.settings.privacy.tor.SecurityTorSettingsModule
import com.quantum.wallet.bankwallet.modules.settings.privacy.tor.SecurityTorSettingsViewModel
import com.quantum.wallet.bankwallet.modules.settings.security.SecurityCenterCell
import com.quantum.wallet.bankwallet.modules.settings.security.ui.TorBlock
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.ui.compose.components.HsSwitch
import com.quantum.wallet.bankwallet.ui.compose.components.InfoText
import com.quantum.wallet.bankwallet.ui.compose.components.TextImportantWarning
import com.quantum.wallet.bankwallet.ui.compose.components.VSpacer
import com.quantum.wallet.bankwallet.ui.compose.components.body_leah
import com.quantum.wallet.bankwallet.ui.compose.components.cell.SectionUniversalLawrence
import com.quantum.wallet.bankwallet.ui.extensions.ConfirmationDialog
import com.quantum.wallet.bankwallet.ui.helpers.LinkHelper
import com.quantum.wallet.bankwallet.uiv3.components.HSScaffold
import kotlin.system.exitProcess

class PrivacySettingsFragment : BaseComposeFragment() {

    private val torViewModel by viewModels<SecurityTorSettingsViewModel> {
        SecurityTorSettingsModule.Factory()
    }

    @Composable
    override fun GetContent(navController: NavController) {
        PrivacyScreen(
            navController = navController,
            torViewModel = torViewModel,
            showAppRestartAlert = { showAppRestartAlert() },
            restartApp = { restartApp() },
        )
    }

    private fun showAppRestartAlert() {
        val warningTitle = if (torViewModel.torCheckEnabled) {
            getString(R.string.Tor_Connection_Enable)
        } else {
            getString(R.string.Tor_Connection_Disable)
        }

        val actionButton = if (torViewModel.torCheckEnabled) {
            getString(R.string.Button_Enable)
        } else {
            getString(R.string.Button_Disable)
        }

        ConfirmationDialog.show(
            icon = R.drawable.ic_tor_connection_24,
            title = getString(R.string.Tor_Alert_Title),
            warningTitle = warningTitle,
            warningText = getString(R.string.SettingsSecurity_AppRestartWarning),
            actionButtonTitle = actionButton,
            transparentButtonTitle = getString(R.string.Alert_Cancel),
            fragmentManager = childFragmentManager,
            listener = object : ConfirmationDialog.Listener {
                override fun onActionButtonClick() {
                    torViewModel.setTorEnabled()
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
            exitProcess(0)
        }
    }
}

@Composable
fun PrivacyScreen(
    navController: NavController,
    torViewModel: SecurityTorSettingsViewModel,
    showAppRestartAlert: () -> Unit = {},
    restartApp: () -> Unit = {},
) {
    val viewModel = viewModel<PrivacyViewModel>(factory = PrivacyViewModel.Factory())
    val uiState = viewModel.uiState
    val context = LocalContext.current

    if (torViewModel.restartApp) {
        restartApp()
        torViewModel.appRestarted()
    }

    HSScaffold(
        title = stringResource(R.string.Settings_Privacy),
        onBack = navController::popBackStack,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            VSpacer(12.dp)
            TextImportantWarning(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringResource(R.string.Privacy_Information),
            )

            VSpacer(height = 24.dp)
            SectionUniversalLawrence {
                ShareUiDataBlock(
                    checked = uiState.uiStatsEnabled,
                    onCheckedChange = {
                        viewModel.toggleUiStats(it)

                        stat(page = StatPage.Privacy, event = StatEvent.EnableUiStats(it))
                    })
            }
            InfoText(
                text = stringResource(R.string.ShareUiDataDescription),
            )

            VSpacer(12.dp)
            SectionUniversalLawrence {
                TorBlock(torViewModel, showAppRestartAlert)
            }
            InfoText(
                text = stringResource(R.string.SettingsSecurity_TorConnectionDescription),
            )

            VSpacer(12.dp)
            SectionUniversalLawrence {
                NymVpnBlock(
                    onClick = {
                        LinkHelper.openLinkInAppBrowser(context, viewModel.nymVpnLink)
                    }
                )
            }
            InfoText(
                text = stringResource(R.string.NymVpn_Description),
            )
        }
    }
}

@Composable
fun ShareUiDataBlock(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SecurityCenterCell(
        start = {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(R.drawable.ic_share_24px),
                tint = ComposeAppTheme.colors.grey,
                contentDescription = null,
            )
        },
        center = {
            body_leah(
                text = stringResource(R.string.ShareUiData),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        end = {
            HsSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        },
        onClick = { onCheckedChange.invoke(!checked) }
    )
}

@Composable
fun NymVpnBlock(
    onClick: () -> Unit,
) {
    SecurityCenterCell(
        start = {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(R.drawable.icon_nym_vpn_24),
                tint = ComposeAppTheme.colors.grey,
                contentDescription = null,
            )
        },
        center = {
            body_leah(
                text = stringResource(R.string.NymVpn_GetNymVpn),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        end = {
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(R.drawable.ic_arrow_right),
                tint = ComposeAppTheme.colors.grey,
                contentDescription = null,
            )
        },
        onClick = onClick
    )
}