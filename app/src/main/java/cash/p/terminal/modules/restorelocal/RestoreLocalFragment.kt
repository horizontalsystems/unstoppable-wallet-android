package cash.p.terminal.modules.restorelocal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.modules.evmfee.ButtonsGroupWithShade
import cash.p.terminal.modules.manageaccounts.ManageAccountsModule
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellowWithSpinner
import cash.p.terminal.ui.compose.components.FormsInputPassword
import cash.p.terminal.ui.compose.components.InfoText
import cash.p.terminal.ui.compose.components.MenuItem
import cash.p.terminal.ui.compose.components.VSpacer
import io.horizontalsystems.core.findNavController

class RestoreLocalFragment : BaseFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            val popUpToInclusiveId =
                arguments?.getInt(ManageAccountsModule.popOffOnSuccessKey, R.id.restoreAccountFragment) ?: R.id.restoreAccountFragment

            setContent {
                RestoreLocalScreen(findNavController(), popUpToInclusiveId)
            }
        }
    }
}

@Composable
private fun RestoreLocalScreen(
    navController: NavController,
    popUpToInclusiveId: Int
) {
    val viewModel = viewModel<RestoreLocalViewModel>(factory = RestoreLocalModule.Factory())
    val uiState = viewModel.uiState
    var hidePassphrase by remember { mutableStateOf(true) }

    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = TranslatableString.ResString(R.string.ImportBackupFile_EnterPassword),
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
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(
                            rememberScrollState()
                        )
                ) {

                    InfoText(text = stringResource(R.string.ImportBackupFile_EnterPassword_Description))
                    VSpacer(24.dp)
                    FormsInputPassword(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        hint = stringResource(R.string.Passphrase),
                        state = uiState.passphraseState,
                        onValueChange = viewModel::onChangePassphrase,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        hide = hidePassphrase,
                        onToggleHide = {
                            hidePassphrase = !hidePassphrase
                        }
                    )
                    VSpacer(32.dp)
                }

                ButtonsGroupWithShade {
                    ButtonPrimaryYellowWithSpinner(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                        title = stringResource(R.string.Button_Restore),
                        showSpinner = uiState.showButtonSpinner,
                        enabled = uiState.showButtonSpinner.not(),
                        onClick = {
                            viewModel.onImportClick()
                        },
                    )
                }
            }
        }
    }
}