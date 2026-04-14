package com.quantum.wallet.bankwallet.modules.manageaccount.publickeys

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
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.core.authorizedAction
import com.quantum.wallet.bankwallet.core.slideFromRight
import com.quantum.wallet.bankwallet.core.stats.StatEvent
import com.quantum.wallet.bankwallet.core.stats.StatPage
import com.quantum.wallet.bankwallet.core.stats.stat
import com.quantum.wallet.bankwallet.entities.Account
import com.quantum.wallet.bankwallet.modules.manageaccount.evmaddress.AddressFragment
import com.quantum.wallet.bankwallet.modules.manageaccount.showextendedkey.ShowExtendedKeyFragment
import com.quantum.wallet.bankwallet.modules.manageaccount.showmonerokey.ShowMoneroKeyFragment
import com.quantum.wallet.bankwallet.modules.manageaccount.ui.KeyActionItem
import com.quantum.wallet.bankwallet.uiv3.components.HSScaffold

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
                        R.id.addressFragment,
                        AddressFragment.Input(evmAddress, AddressFragment.Type.Evm)
                    )

                    stat(page = StatPage.PublicKeys, event = StatEvent.Open(StatPage.EvmAddress))
                }
            }
            viewModel.viewState.tronAddress?.let { tronAddress ->
                KeyActionItem(
                    title = stringResource(id = R.string.PublicKeys_TronAddress),
                    description = stringResource(R.string.PublicKeys_TronAddress_Description)
                ) {
                    navController.slideFromRight(
                        R.id.addressFragment,
                        AddressFragment.Input(tronAddress, AddressFragment.Type.Tron)
                    )

                    stat(page = StatPage.PublicKeys, event = StatEvent.Open(StatPage.TronAddress))
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
