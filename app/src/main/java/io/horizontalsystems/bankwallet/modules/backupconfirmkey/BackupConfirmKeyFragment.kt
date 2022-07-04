package io.horizontalsystems.bankwallet.modules.backupconfirmkey

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class BackupConfirmKeyFragment : BaseFragment() {

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
                    BackupConfirmScreen(
                        arguments?.getParcelable(BackupConfirmKeyModule.ACCOUNT)!!,
                        findNavController(),
                    )
                }
            }
        }
    }
}

@Composable
private fun BackupConfirmScreen(
    account: Account,
    navController: NavController,
    viewModel: BackupConfirmKeyViewModel = viewModel(
        factory = BackupConfirmKeyModule.Factory(account)
    )
) {
    viewModel.successMessage?.let {
        HudHelper.showSuccessMessage(LocalView.current, it)
        viewModel.onSuccessMessageShown()

        Handler(Looper.getMainLooper()).postDelayed({
            navController.popBackStack(R.id.backupKeyFragment, true)
        }, 1200)
    }

    val focusRequester = remember { FocusRequester() }

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
                title = TranslatableString.ResString(R.string.Backup_Confirmation_CheckTitle),
                navigationIcon = {
                    HsIconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "back button",
                            tint = ComposeAppTheme.colors.jacob
                        )
                    }
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Done),
                        onClick = { viewModel.onClickDone() },
                    )
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(12.dp))
                FormsInput(
                    modifier = Modifier.focusRequester(focusRequester),
                    hint = "",
                    onValueChange = {
                        viewModel.onChangeFirstWord(it)
                    },
                    pasteEnabled = false,
                    prefix = viewModel.firstInputPrefix,
                    state = viewModel.firstInputErrorState
                )

                Spacer(Modifier.height(16.dp))
                FormsInput(
                    hint = "",
                    onValueChange = {
                        viewModel.onChangeSecondWord(it)
                    },
                    pasteEnabled = false,
                    prefix = viewModel.secondInputPrefix,
                    state = viewModel.secondInputErrorState
                )
                Spacer(Modifier.height(12.dp))
                subhead2_grey(
                    modifier = Modifier.padding(start = 8.dp),
                    text = stringResource(R.string.BackupConfirmKey_Description)
                )
                if (viewModel.passphraseVisible) {
                    Spacer(Modifier.height(36.dp))
                    FormsInput(
                        hint = stringResource(R.string.Passphrase),
                        pasteEnabled = false,
                        state = viewModel.passphraseErrorState,
                        onValueChange = { value ->
                            viewModel.onChangePassphrase(value)
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                    subhead2_grey(
                        modifier = Modifier.padding(start = 8.dp),
                        text = stringResource(R.string.EnterPassphrase)
                    )
                }
            }
        }
    }
    SideEffect {
        focusRequester.requestFocus()
    }
}
