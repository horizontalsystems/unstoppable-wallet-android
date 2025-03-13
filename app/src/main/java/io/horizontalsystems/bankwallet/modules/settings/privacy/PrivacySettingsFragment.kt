package io.horizontalsystems.bankwallet.modules.settings.privacy

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.modules.settings.privacy.tor.SecurityTorSettingsModule
import io.horizontalsystems.bankwallet.modules.settings.privacy.tor.SecurityTorSettingsViewModel
import io.horizontalsystems.bankwallet.modules.settings.security.ui.TorBlock
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.HFillSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.HsSwitch
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.cell.CellUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionUniversalLawrence
import io.horizontalsystems.bankwallet.ui.extensions.ConfirmationDialog
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

    if (torViewModel.restartApp) {
        restartApp()
        torViewModel.appRestarted()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .background(ComposeAppTheme.colors.tyler)
    ) {
        AppBar(
            title = stringResource(R.string.Settings_Privacy),
            navigationIcon = {
                HsBackButton(onClick = { navController.popBackStack() })
            }
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            VSpacer(12.dp)
            TextImportantWarning(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringResource(R.string.Privacy_Information),
            )

            VSpacer(height = 24.dp)
            SectionUniversalLawrence {
                CellUniversal {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_share_24px),
                        contentDescription = "Share",
                        tint = ComposeAppTheme.colors.jacob
                    )
                    HSpacer(width = 16.dp)
                    body_leah(text = stringResource(R.string.ShareUiData))
                    HFillSpacer(minWidth = 8.dp)
                    HsSwitch(
                        checked = uiState.uiStatsEnabled,
                        onCheckedChange = {
                            viewModel.toggleUiStats(it)

                            stat(page = StatPage.Privacy, event = StatEvent.EnableUiStats(it))
                        }
                    )
                }
            }
            InfoText(
                text = stringResource(R.string.ShareUiDataDescription),
            )

            VSpacer(20.dp)
            SectionUniversalLawrence {
                TorBlock(torViewModel, showAppRestartAlert)
            }
            InfoText(
                text = stringResource(R.string.SettingsSecurity_TorConnectionDescription),
            )
        }

        Divider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = ComposeAppTheme.colors.steel10
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.FooterText, uiState.currentYear),
            style = ComposeAppTheme.typography.caption,
            color = ComposeAppTheme.colors.grey,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun BulletedText(@StringRes text: Int) {
    Row(
        modifier = Modifier.padding(start = 24.dp, top = 12.dp, end = 32.dp, bottom = 12.dp)
    ) {
        Text(
            text = "\u2022 ",
            style = ComposeAppTheme.typography.body,
            color = ComposeAppTheme.colors.bran,
            modifier = Modifier.width(15.dp),
            textAlign = TextAlign.Center
        )
        HSpacer(width = 8.dp)
        Text(
            text = stringResource(text),
            style = ComposeAppTheme.typography.body,
            color = ComposeAppTheme.colors.bran,
            modifier = Modifier.padding(end = 32.dp)
        )
    }

}