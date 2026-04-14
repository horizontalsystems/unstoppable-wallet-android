package com.quantum.wallet.bankwallet.modules.manageaccount.recoveryphrase

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.core.managers.FaqManager
import com.quantum.wallet.bankwallet.core.stats.StatEntity
import com.quantum.wallet.bankwallet.core.stats.StatEvent
import com.quantum.wallet.bankwallet.core.stats.StatPage
import com.quantum.wallet.bankwallet.core.stats.stat
import com.quantum.wallet.bankwallet.entities.Account
import com.quantum.wallet.bankwallet.modules.manageaccount.ui.ActionButton
import com.quantum.wallet.bankwallet.modules.manageaccount.ui.ConfirmCopyBottomSheet
import com.quantum.wallet.bankwallet.modules.manageaccount.ui.PassphraseCell
import com.quantum.wallet.bankwallet.modules.manageaccount.ui.SeedPhraseList
import com.quantum.wallet.bankwallet.ui.compose.TranslatableString
import com.quantum.wallet.bankwallet.ui.compose.components.MenuItem
import com.quantum.wallet.bankwallet.ui.compose.components.TextImportantWarning
import com.quantum.wallet.bankwallet.ui.compose.components.VSpacer
import com.quantum.wallet.bankwallet.ui.helpers.TextHelper
import com.quantum.wallet.bankwallet.uiv3.components.HSScaffold
import com.quantum.wallet.core.helpers.HudHelper
import kotlinx.coroutines.launch

class RecoveryPhraseFragment : BaseComposeFragment(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Account>(navController) { input ->
            RecoveryPhraseScreen(navController, input)
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecoveryPhraseScreen(
    navController: NavController,
    account: Account,
) {
    val viewModel =
        viewModel<RecoveryPhraseViewModel>(factory = RecoveryPhraseModule.Factory(account))

    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }

    HSScaffold(
        title = stringResource(R.string.RecoveryPhrase_Title),
        onBack = navController::popBackStack,
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Info_Title),
                icon = R.drawable.ic_info_24,
                onClick = {
                    FaqManager.showFaqPage(navController, FaqManager.faqPathPrivateKeys)
                    stat(
                        page = StatPage.RecoveryPhrase,
                        event = StatEvent.Open(StatPage.Info)
                    )
                }
            )
        )
    ) {
        Column {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                VSpacer(12.dp)
                TextImportantWarning(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(R.string.PrivateKeys_NeverShareWarning)
                )
                VSpacer(24.dp)
                var hidden by remember { mutableStateOf(true) }
                SeedPhraseList(viewModel.wordsNumbered, hidden) {
                    hidden = !hidden
                    stat(page = StatPage.RecoveryPhrase, event = StatEvent.ToggleHidden)
                }
                VSpacer(24.dp)
                PassphraseCell(viewModel.passphrase, hidden)
            }
            ActionButton(R.string.Alert_Copy) {
                showBottomSheet = true
            }
        }
        if (showBottomSheet) {
            ConfirmCopyBottomSheet(
                sheetState = sheetState,
                onConfirm = {
                    scope.launch {
                        sheetState.hide()
                        showBottomSheet = false
                    }

                    TextHelper.copyText(viewModel.words.joinToString(" "))
                    HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)

                    stat(
                        page = StatPage.RecoveryPhrase,
                        event = StatEvent.Copy(StatEntity.RecoveryPhrase)
                    )
                },
                onDismiss = {
                    scope.launch {
                        sheetState.hide()
                        showBottomSheet = false
                    }
                }
            )
        }
    }
}
