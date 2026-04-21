package cash.p.terminal.modules.manageaccount.backupkey

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.fragment.navArgs
import cash.p.terminal.R
import cash.p.terminal.core.managers.FaqManager
import cash.p.terminal.modules.evmfee.ButtonsGroupWithShade
import cash.p.terminal.modules.manageaccount.ui.PassphraseCell
import cash.p.terminal.modules.manageaccount.ui.SeedPhraseList
import cash.p.terminal.navigation.slideFromRight
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.InfoText
import cash.p.terminal.ui_compose.components.HudHelper
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

class BackupKeyFragment : BaseComposeFragment(screenshotEnabled = false) {

    private val args: BackupKeyFragmentArgs by navArgs()

    @Composable
    override fun GetContent(navController: NavController) {
        val accountId = args.input?.accountId
        LaunchedEffect(accountId) {
            if (accountId == null) {
                navController.popBackStack()
            }
        }
        if (accountId == null) return
        RecoveryPhraseScreen(navController, accountId)
    }
}

@Composable
fun RecoveryPhraseScreen(
    navController: NavController,
    accountId: String
) {
    val viewModel = koinViewModel<BackupKeyViewModel> { parametersOf(accountId) }
    val account = viewModel.account
    val view = LocalView.current

    LaunchedEffect(viewModel.accountNotFound, account) {
        if (viewModel.accountNotFound || account == null) {
            HudHelper.showErrorMessage(view, R.string.error_account_not_found)
            navController.popBackStack()
        }
    }

    if (viewModel.accountNotFound || account == null) return

    Scaffold(
        containerColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = stringResource(R.string.RecoveryPhrase_Title),
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Info_Title),
                        icon = R.drawable.ic_info_24,
                        onClick = {
                            FaqManager.showFaqPage(FaqManager.faqPathPrivateKeys)
                        }
                    ),
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close_24,
                        onClick = {
                            navController.popBackStack()
                        }
                    )
                )
            )
        }
    ) {
        Column(modifier = Modifier.padding(it)) {
            var hidden by remember { mutableStateOf(true) }

            InfoText(text = stringResource(R.string.RecoveryPhrase_Description))
            Spacer(Modifier.height(12.dp))
            SeedPhraseList(
                wordsNumbered = viewModel.wordsNumbered,
                hidden = hidden
            ) {
                hidden = !hidden
            }
            if (viewModel.showPassphraseBlock) {
                Spacer(Modifier.height(24.dp))
                PassphraseCell(viewModel.passphrase, hidden)
            }
            Spacer(modifier = Modifier.weight(1f))
            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp),
                    title = stringResource(R.string.RecoveryPhrase_Verify),
                    onClick = {
                        navController.slideFromRight(
                            R.id.backupConfirmationKeyFragment,
                            account
                        )
                    },
                )
            }
        }
    }
}
