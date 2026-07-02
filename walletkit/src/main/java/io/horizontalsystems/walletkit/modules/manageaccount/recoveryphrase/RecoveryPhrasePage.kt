package io.horizontalsystems.walletkit.modules.manageaccount.recoveryphrase

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
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.managers.FaqManager
import io.horizontalsystems.walletkit.core.stats.StatEntity
import io.horizontalsystems.walletkit.core.stats.StatEvent
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.core.stats.stat
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.helpers.HudHelper
import io.horizontalsystems.walletkit.modules.manageaccount.ui.ActionButton
import io.horizontalsystems.walletkit.modules.manageaccount.ui.ConfirmCopyBottomSheet
import io.horizontalsystems.walletkit.modules.manageaccount.ui.PassphraseCell
import io.horizontalsystems.walletkit.modules.manageaccount.ui.SeedPhraseList
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import io.horizontalsystems.walletkit.ui.compose.components.MenuItem
import io.horizontalsystems.walletkit.ui.compose.components.TextImportantWarning
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.ui.helpers.TextHelper
import io.horizontalsystems.walletkit.uiv3.components.HSScaffold
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class RecoveryPhrasePage(val input: Account) : HSPage(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        RecoveryPhraseScreen(navigation, input)
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecoveryPhraseScreen(
    navigation: HSNavigation,
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
        onBack = navigation::removeLastOrNull,
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Info_Title),
                icon = R.drawable.ic_info_24,
                onClick = {
                    FaqManager.showFaqPage(navigation, FaqManager.faqPathPrivateKeys)
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
