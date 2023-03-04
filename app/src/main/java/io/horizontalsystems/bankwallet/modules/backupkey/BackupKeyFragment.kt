package io.horizontalsystems.bankwallet.modules.backupkey

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
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.backupconfirmkey.BackupConfirmKeyModule
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.recoveryphrase.PassphraseCell
import io.horizontalsystems.bankwallet.modules.recoveryphrase.SeedPhraseList
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.core.findNavController

class BackupKeyFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        disallowScreenshot()
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

    override fun onDestroyView() {
        allowScreenshot()
        super.onDestroyView()
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
