package io.horizontalsystems.core.modules.manageaccount.backupkey

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
import io.horizontalsystems.core.R
import io.horizontalsystems.core.core.managers.FaqManager
import io.horizontalsystems.core.entities.Account
import io.horizontalsystems.core.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.core.modules.manageaccount.backupconfirmkey.BackupConfirmKeyPage
import io.horizontalsystems.core.modules.manageaccount.ui.PassphraseCell
import io.horizontalsystems.core.modules.manageaccount.ui.SeedPhraseList
import io.horizontalsystems.core.modules.nav3.HSNavigation
import io.horizontalsystems.core.modules.nav3.HSPage
import io.horizontalsystems.core.ui.compose.TranslatableString
import io.horizontalsystems.core.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.core.ui.compose.components.InfoText
import io.horizontalsystems.core.ui.compose.components.MenuItem
import io.horizontalsystems.core.uiv3.components.HSScaffold
import kotlinx.serialization.Serializable

@Serializable
data class BackupKeyPage(val account: Account) : HSPage(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        RecoveryPhraseScreen(navigation, account)
    }
}

@Composable
fun RecoveryPhraseScreen(
    navigation: HSNavigation,
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
                    FaqManager.showFaqPage(navigation, FaqManager.faqPathPrivateKeys)
                }
            ),
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Close),
                icon = R.drawable.ic_close,
                onClick = {
                    navigation.removeLastOrNull()
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
                        navigation.slideFromRight(
                            BackupConfirmKeyPage(viewModel.account)
                        )
                    },
                )
            }
        }
    }
}
