package cash.p.terminal.modules.manageaccount.recoveryphrase

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
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
import cash.p.terminal.R
import cash.p.terminal.core.managers.FaqManager
import cash.p.terminal.core.stats.StatEntity
import cash.p.terminal.core.stats.StatEvent
import cash.p.terminal.core.stats.StatPage
import cash.p.terminal.core.stats.stat
import cash.p.terminal.modules.manageaccount.ui.ActionButton
import cash.p.terminal.modules.manageaccount.ui.ConfirmCopyBottomSheet
import cash.p.terminal.modules.manageaccount.ui.PassphraseCell
import cash.p.terminal.modules.manageaccount.ui.SeedPhraseList
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui.helpers.TextHelper
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.ui_compose.components.TextImportantWarning
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import cash.p.terminal.wallet.Account
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.requireInput
import kotlinx.coroutines.launch

class RecoveryPhraseFragment : BaseComposeFragment(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navController: NavController) {
        RecoveryPhraseScreen(
            navController = navController,
            account = navController.requireInput()
        )
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
    val coroutineScope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    if (showBottomSheet) {
        ModalBottomSheet(
            sheetState = sheetState,
            dragHandle = null,
            containerColor = ComposeAppTheme.colors.transparent,
            onDismissRequest = {
                showBottomSheet = false
            }
        ) {
            ConfirmCopyBottomSheet(
                onConfirm = {
                    coroutineScope.launch {
                        TextHelper.copyText(viewModel.words.joinToString(" "))
                        HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showBottomSheet = false
                            }
                        }

                        stat(
                            page = StatPage.RecoveryPhrase,
                            event = StatEvent.Copy(StatEntity.RecoveryPhrase)
                        )
                    }
                },
                onCancel = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            showBottomSheet = false
                        }
                    }
                }
            )
        }
    }
    Scaffold(
        containerColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = stringResource(R.string.RecoveryPhrase_Title),
                navigationIcon = {
                    HsBackButton(onClick = navController::popBackStack)
                },
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
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues),
        ) {
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
    }
}
