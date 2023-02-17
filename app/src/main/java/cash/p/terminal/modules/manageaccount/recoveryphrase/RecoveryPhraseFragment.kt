package cash.p.terminal.modules.manageaccount.recoveryphrase

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.core.managers.FaqManager
import cash.p.terminal.entities.Account
import cash.p.terminal.modules.manageaccount.ui.ActionButton
import cash.p.terminal.modules.manageaccount.ui.ConfirmCopyBottomSheet
import cash.p.terminal.modules.manageaccount.ui.PassphraseCell
import cash.p.terminal.modules.manageaccount.ui.SeedPhraseList
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.*
import cash.p.terminal.ui.helpers.TextHelper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.launch

class RecoveryPhraseFragment : BaseFragment() {

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
                RecoveryPhraseScreen(
                    navController = findNavController(),
                    account = arguments?.getParcelable(RecoveryPhraseModule.ACCOUNT)!!
                )
            }
        }
    }

    override fun onDestroyView() {
        allowScreenshot()
        super.onDestroyView()
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun RecoveryPhraseScreen(
    navController: NavController,
    account: Account,
) {
    val viewModel = viewModel<RecoveryPhraseViewModel>(factory = RecoveryPhraseModule.Factory(account))

    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
    )

    ComposeAppTheme {
        ModalBottomSheetLayout(
            sheetState = sheetState,
            sheetBackgroundColor = ComposeAppTheme.colors.transparent,
            sheetContent = {
                ConfirmCopyBottomSheet(
                    onConfirm = {
                        coroutineScope.launch {
                            TextHelper.copyText(viewModel.words.joinToString(" "))
                            HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
                            sheetState.hide()
                        }
                    },
                    onCancel = {
                        coroutineScope.launch {
                            sheetState.hide()
                        }
                    }
                )
            }
        ) {
            Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
                AppBar(
                    title = TranslatableString.ResString(R.string.RecoveryPhrase_Title),
                    navigationIcon = {
                        HsBackButton(onClick = navController::popBackStack)
                    },
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Info_Title),
                            icon = R.drawable.ic_info_24,
                            onClick = {
                                FaqManager.showFaqPage(navController, FaqManager.faqPathPrivateKeys)
                            }
                        )
                    )
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Top
                ) {
                    Spacer(Modifier.height(12.dp))
                    TextImportantWarning(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = stringResource(R.string.PrivateKeys_NeverShareWarning)
                    )
                    Spacer(Modifier.height(24.dp))
                    var hidden by remember { mutableStateOf(true) }
                    SeedPhraseList(viewModel.wordsNumbered, hidden) {
                        hidden = !hidden
                    }
                    Spacer(Modifier.height(24.dp))
                    PassphraseCell(viewModel.passphrase, hidden)
                }
                ActionButton(R.string.Alert_Copy) {
                    coroutineScope.launch {
                        sheetState.show()
                    }
                }
            }
        }
    }
}
