package io.horizontalsystems.bankwallet.modules.backupkey

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.backupconfirmkey.BackupConfirmKeyModule
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.showkey.PassphraseCell
import io.horizontalsystems.bankwallet.modules.showkey.SeedPhraseList
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.getNavigationResult
import io.horizontalsystems.pin.PinInteractionType
import io.horizontalsystems.pin.PinModule

class BackupKeyFragment : BaseFragment() {
    private val viewModel by navGraphViewModels<BackupKeyViewModel>(R.id.backupKeyFragment) {
        BackupKeyModule.Factory(
            arguments?.getParcelable(BackupKeyModule.ACCOUNT)!!
        )
    }

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
                ComposeAppTheme {
                    BackupKeyScreen(
                        navController = findNavController(),
                        viewModel = viewModel,
                        subscribeForPinResult = { subscribeForPinResult() }
                    )
                }
            }
        }
    }

    private fun subscribeForPinResult() {
        getNavigationResult(PinModule.requestKey) { bundle ->
            val resultType = bundle.getParcelable<PinInteractionType>(PinModule.requestType)
            val resultCode = bundle.getInt(PinModule.requestResult)

            if (resultType == PinInteractionType.UNLOCK && resultCode == PinModule.RESULT_OK) {
                viewModel.pinUnlocked()
            }
        }
    }

}

@Composable
private fun BackupKeyScreen(
    navController: NavController,
    viewModel: BackupKeyViewModel,
    subscribeForPinResult: () -> Unit,
) {

    if (viewModel.showPinUnlock) {
        subscribeForPinResult()
        navController.slideFromBottom(R.id.pinFragment, PinModule.forUnlock())
        viewModel.pinUnlockShown()
    }

    if (viewModel.showKeyConfirmation) {
        navController.slideFromRight(
            R.id.backupConfirmationKeyFragment,
            BackupConfirmKeyModule.prepareParams(viewModel.account)
        )
        viewModel.keyConfirmationShown()
    }

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
                title = TranslatableString.ResString(R.string.BackupKey_Title),
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

            when (viewModel.viewState) {
                BackupKeyModule.ViewState.Warning -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        TextImportantWarning(
                            modifier = Modifier.padding(vertical = 12.dp),
                            text = stringResource(R.string.BackupKey_Description)
                        )
                    }
                    ButtonsGroupWithShade {
                        ButtonPrimaryYellow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                            title = stringResource(R.string.ShowKey_ButtonShow),
                            onClick = { viewModel.onClickShow() },
                        )
                    }
                }
                BackupKeyModule.ViewState.MnemonicKey -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(Modifier.height(16.dp))
                        SeedPhraseList(viewModel.wordsNumbered)
                        Spacer(Modifier.height(24.dp))
                        PassphraseCell(viewModel.passphrase)
                    }
                    ButtonsGroupWithShade {
                        ButtonPrimaryYellow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                            title = stringResource(R.string.BackupKey_ButtonBackup),
                            onClick = { viewModel.onClickBackup() },
                        )
                    }
                }
            }
        }
    }

}
