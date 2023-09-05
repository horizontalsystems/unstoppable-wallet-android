package cash.p.terminal.modules.manageaccount.publickeys

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.entities.Account
import cash.p.terminal.modules.manageaccount.evmaddress.EvmAddressFragment
import cash.p.terminal.modules.manageaccount.publickeys.PublicKeysModule.ACCOUNT_KEY
import cash.p.terminal.modules.manageaccount.showextendedkey.ShowExtendedKeyModule
import cash.p.terminal.modules.manageaccount.ui.KeyActionItem
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.HsBackButton
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.parcelable

class PublicKeysFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent() {
        val account: Account? = arguments?.parcelable(ACCOUNT_KEY)
        if (account == null) {
            Toast.makeText(App.instance, "Account parameter is missing", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }
        ManageAccountScreen(findNavController(), account)
    }

}

@Composable
fun ManageAccountScreen(navController: NavController, account: Account) {
    val viewModel = viewModel<PublicKeysViewModel>(factory = PublicKeysModule.Factory(account))

    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = TranslatableString.ResString(R.string.PublicKeys_Title),
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
                viewModel.viewState.evmAddress?.let { evmAddress ->
                    KeyActionItem(
                        title = stringResource(id = R.string.PublicKeys_EvmAddress),
                        description = stringResource(R.string.PublicKeys_EvmAddress_Description)
                    ) {
                        navController.slideFromRight(
                            R.id.evmAddressFragment,
                            bundleOf(EvmAddressFragment.EVM_ADDRESS_KEY to evmAddress)
                        )
                    }
                }
                viewModel.viewState.extendedPublicKey?.let { publicKey ->
                    KeyActionItem(
                        title = stringResource(id = R.string.PublicKeys_AccountExtendedPublicKey),
                        description = stringResource(id = R.string.PublicKeys_AccountExtendedPublicKeyDescription),
                    ) {
                        navController.slideFromRight(
                            R.id.showExtendedKeyFragment,
                            ShowExtendedKeyModule.prepareParams(
                                publicKey.hdKey,
                                publicKey.accountPublicKey
                            )
                        )
                    }
                }
            }
        }
    }
}
