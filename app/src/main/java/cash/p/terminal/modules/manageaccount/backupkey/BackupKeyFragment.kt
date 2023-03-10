package cash.p.terminal.modules.manageaccount.backupkey

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.core.managers.FaqManager
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.entities.Account
import cash.p.terminal.modules.evmfee.ButtonsGroupWithShade
import cash.p.terminal.modules.manageaccount.backupconfirmkey.BackupConfirmKeyModule
import cash.p.terminal.modules.manageaccount.ui.PassphraseCell
import cash.p.terminal.modules.manageaccount.ui.SeedPhraseList
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui.compose.components.InfoText
import cash.p.terminal.ui.compose.components.MenuItem
import io.horizontalsystems.core.findNavController

class BackupKeyFragment : BaseFragment(screenshotEnabled = false) {

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
                val account = requireArguments().getParcelable<Account>(BackupKeyModule.ACCOUNT)!!
                RecoveryPhraseScreen(findNavController(), account)
            }
        }
    }

}

@Composable
fun RecoveryPhraseScreen(
    navController: NavController,
    account: Account
) {
    val viewModel = viewModel<BackupKeyViewModel>(factory = BackupKeyModule.Factory(account))

    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = TranslatableString.ResString(R.string.RecoveryPhrase_Title),
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Info_Title),
                            icon = R.drawable.ic_info_24,
                            onClick = {
                                FaqManager.showFaqPage(navController, FaqManager.faqPathPrivateKeys)
                            }
                        ),
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Button_Close),
                            icon = R.drawable.ic_close,
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
                Spacer(Modifier.height(24.dp))
                PassphraseCell(viewModel.passphrase, hidden)
                Spacer(modifier = Modifier.weight(1f))
                ButtonsGroupWithShade {
                    ButtonPrimaryYellow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                        title = stringResource(R.string.RecoveryPhrase_Verify),
                        onClick = {
                            navController.slideFromRight(
                                R.id.backupConfirmationKeyFragment,
                                BackupConfirmKeyModule.prepareParams(viewModel.account)
                            )
                        },
                    )
                }
            }
        }
    }
}