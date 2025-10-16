package io.horizontalsystems.bankwallet.modules.manageaccount.publickeys

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.authorizedAction
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.manageaccount.evmaddress.EvmAddressFragment
import io.horizontalsystems.bankwallet.modules.manageaccount.showextendedkey.ShowExtendedKeyFragment
import io.horizontalsystems.bankwallet.modules.manageaccount.showmonerokey.ShowMoneroKeyFragment
import io.horizontalsystems.bankwallet.modules.manageaccount.ui.KeyActionItem
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold

class PublicKeysFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Account>(navController) { account ->
            ManageAccountScreen(navController, account)
        }
    }

}

@Composable
fun ManageAccountScreen(navController: NavController, account: Account) {
    val viewModel = viewModel<PublicKeysViewModel>(factory = PublicKeysModule.Factory(account))

    HSScaffold(
        title = stringResource(R.string.PublicKeys_Title),
        onBack = { navController.popBackStack() },
    ) {
        Column(
            modifier = Modifier
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
                        EvmAddressFragment.Input(evmAddress)
                    )

                    stat(page = StatPage.PublicKeys, event = StatEvent.Open(StatPage.EvmAddress))
                }
            }
            viewModel.viewState.extendedPublicKey?.let { publicKey ->
                KeyActionItem(
                    title = stringResource(id = R.string.PublicKeys_AccountExtendedPublicKey),
                    description = stringResource(id = R.string.PublicKeys_AccountExtendedPublicKeyDescription),
                ) {
                    navController.slideFromRight(
                        R.id.showExtendedKeyFragment,
                        ShowExtendedKeyFragment.Input(
                            publicKey.hdKey,
                            publicKey.accountPublicKey
                        )
                    )

                    stat(
                        page = StatPage.PublicKeys,
                        event = StatEvent.Open(StatPage.AccountExtendedPublicKey)
                    )
                }
            }

            viewModel.viewState.moneroKeys?.let { moneroKeys ->
                KeyActionItem(
                    title = stringResource(id = R.string.PublicKeys_MoneroPublicKey),
                    description = stringResource(id = R.string.PublicKeys_MoneroPublicKeyDescription),
                ) {
                    navController.authorizedAction {
                        navController.slideFromRight(
                            R.id.showMoneroKeyFragment,
                            ShowMoneroKeyFragment.Input(moneroKeys)
                        )
                        stat(
                            page = StatPage.PublicKeys,
                            event = StatEvent.Open(StatPage.MoneroPublicKey)
                        )
                    }
                }
            }
        }
    }
}
