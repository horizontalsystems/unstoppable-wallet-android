package com.quantum.wallet.bankwallet.modules.manageaccount.backupkey

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.core.managers.FaqManager
import com.quantum.wallet.bankwallet.core.slideFromRight
import com.quantum.wallet.bankwallet.entities.Account
import com.quantum.wallet.bankwallet.modules.evmfee.ButtonsGroupWithShade
import com.quantum.wallet.bankwallet.modules.manageaccount.ui.PassphraseCell
import com.quantum.wallet.bankwallet.modules.manageaccount.ui.SeedPhraseList
import com.quantum.wallet.bankwallet.ui.compose.TranslatableString
import com.quantum.wallet.bankwallet.ui.compose.components.ButtonPrimaryYellow
import com.quantum.wallet.bankwallet.ui.compose.components.InfoText
import com.quantum.wallet.bankwallet.ui.compose.components.MenuItem
import com.quantum.wallet.bankwallet.uiv3.components.HSScaffold

class BackupKeyFragment : BaseComposeFragment(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Account>(navController) { account ->
            RecoveryPhraseScreen(navController, account)
        }
    }
}

@Composable
fun RecoveryPhraseScreen(
    navController: NavController,
    account: Account
) {
    val viewModel = viewModel<BackupKeyViewModel>(factory = BackupKeyModule.Factory(account))

    HSScaffold(
        title = stringResource(R.string.RecoveryPhrase_Title),
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
    ) {
        Column {
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
                        .padding(start = 16.dp, end = 16.dp),
                    title = stringResource(R.string.RecoveryPhrase_Verify),
                    onClick = {
                        navController.slideFromRight(
                            R.id.backupConfirmationKeyFragment,
                            viewModel.account
                        )
                    },
                )
            }
        }
    }
}
