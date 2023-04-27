package io.horizontalsystems.bankwallet.modules.manageaccount.privatekeys

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.authorizedAction
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.manageaccount.evmprivatekey.EvmPrivateKeyFragment
import io.horizontalsystems.bankwallet.modules.manageaccount.publickeys.PublicKeysModule.ACCOUNT_KEY
import io.horizontalsystems.bankwallet.modules.manageaccount.showextendedkey.ShowExtendedKeyModule
import io.horizontalsystems.bankwallet.modules.manageaccount.ui.KeyActionItem
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.core.findNavController

class PrivateKeysFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            try {
                setContent {
                    val account: Account = arguments?.getParcelable(ACCOUNT_KEY)
                        ?: throw IllegalArgumentException("Account parameter is missing")
                    ManageAccountScreen(findNavController(), account)
                }
            } catch (t: Throwable) {
                Toast.makeText(App.instance, t.message ?: t.javaClass.simpleName, Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }
    }
}

@Composable
fun ManageAccountScreen(navController: NavController, account: Account) {
    val viewModel = viewModel<PrivateKeysViewModel>(factory = PrivateKeysModule.Factory(account))

    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = TranslatableString.ResString(R.string.PrivateKeys_Title),
                    navigationIcon = {
                        HsBackButton(onClick = { navController.popBackStack() })
                    }
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .padding(it)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(12.dp))
                viewModel.viewState.evmPrivateKey?.let { key ->
                    KeyActionItem(
                        title = stringResource(id = R.string.PrivateKeys_EvmPrivateKey),
                        description = stringResource(R.string.PrivateKeys_EvmPrivateKeyDescription)
                    ) {
                        navController.authorizedAction {
                            navController.slideFromRight(
                                R.id.evmPrivateKeyFragment,
                                EvmPrivateKeyFragment.prepareParams(key)
                            )
                        }
                    }
                }
                viewModel.viewState.bip32RootKey?.let { key ->
                    KeyActionItem(
                        title = stringResource(id = R.string.PrivateKeys_Bip32RootKey),
                        description = stringResource(id = R.string.PrivateKeys_Bip32RootKeyDescription),
                    ) {
                        navController.authorizedAction {
                            navController.slideFromRight(
                                R.id.showExtendedKeyFragment,
                                ShowExtendedKeyModule.prepareParams(
                                    key.hdKey,
                                    key.displayKeyType
                                )
                            )
                        }
                    }
                }
                viewModel.viewState.accountExtendedPrivateKey?.let { key ->
                    KeyActionItem(
                        title = stringResource(id = R.string.PrivateKeys_AccountExtendedPrivateKey),
                        description = stringResource(id = R.string.PrivateKeys_AccountExtendedPrivateKeyDescription),
                    ) {
                        navController.authorizedAction {
                            navController.slideFromRight(
                                R.id.showExtendedKeyFragment,
                                ShowExtendedKeyModule.prepareParams(key.hdKey, key.displayKeyType)
                            )
                        }
                    }
                }
            }
        }
    }
}
