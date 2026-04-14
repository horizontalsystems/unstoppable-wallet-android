package com.quantum.wallet.bankwallet.modules.manageaccount.privatekeys

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
import com.quantum.wallet.bankwallet.modules.manageaccount.evmprivatekey.PrivateKeyFragment
import com.quantum.wallet.bankwallet.modules.manageaccount.showextendedkey.ShowExtendedKeyFragment
import com.quantum.wallet.bankwallet.modules.manageaccount.showmonerokey.ShowMoneroKeyFragment
import com.quantum.wallet.bankwallet.modules.manageaccount.stellarsecretkey.StellarSecretKeyFragment
import com.quantum.wallet.bankwallet.modules.manageaccount.ui.KeyActionItem
import com.quantum.wallet.bankwallet.uiv3.components.HSScaffold

class PrivateKeysFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Account>(navController) { account ->
            ManageAccountScreen(navController, account)
        }
    }

}

@Composable
fun ManageAccountScreen(navController: NavController, account: Account) {
    val viewModel = viewModel<PrivateKeysViewModel>(factory = PrivateKeysModule.Factory(account))

    HSScaffold(
        title = stringResource(R.string.PrivateKeys_Title),
        onBack = { navController.popBackStack() },
    ) {
        Column(
            modifier = Modifier
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
                            R.id.privateKeyFragment,
                            PrivateKeyFragment.Input(key, PrivateKeyFragment.Type.Evm)
                        )

                        stat(
                            page = StatPage.PrivateKeys,
                            event = StatEvent.Open(StatPage.EvmPrivateKey)
                        )
                    }
                }
            }
            viewModel.viewState.tronPrivateKey?.let { key ->
                KeyActionItem(
                    title = stringResource(id = R.string.PrivateKeys_TronPrivateKey),
                    description = stringResource(R.string.PrivateKeys_TronPrivateKeyDescription)
                ) {
                    navController.authorizedAction {
                        navController.slideFromRight(
                            R.id.privateKeyFragment,
                            PrivateKeyFragment.Input(key, PrivateKeyFragment.Type.Tron)
                        )

                        stat(
                            page = StatPage.PrivateKeys,
                            event = StatEvent.Open(StatPage.TronPrivateKey)
                        )
                    }
                }
            }
            viewModel.viewState.stellarSecretKey?.let { key ->
                KeyActionItem(
                    title = stringResource(id = R.string.PrivateKeys_StellarSecretKey),
                    description = stringResource(R.string.PrivateKeys_StellarSecretKeyDescription)
                ) {
                    navController.authorizedAction {
                        navController.slideFromRight(
                            R.id.stellarSecretKeyFragment,
                            StellarSecretKeyFragment.Input(key)
                        )

                        stat(
                            page = StatPage.PrivateKeys,
                            event = StatEvent.Open(StatPage.StellarSecretKey)
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
                            ShowExtendedKeyFragment.Input(
                                key.hdKey,
                                key.displayKeyType
                            )
                        )

                        stat(
                            page = StatPage.PrivateKeys,
                            event = StatEvent.Open(StatPage.Bip32RootKey)
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
                            ShowExtendedKeyFragment.Input(key.hdKey, key.displayKeyType)
                        )

                        stat(
                            page = StatPage.PrivateKeys,
                            event = StatEvent.Open(StatPage.AccountExtendedPrivateKey)
                        )
                    }
                }
            }
            viewModel.viewState.moneroKeys?.let { moneroKeys ->
                KeyActionItem(
                    title = stringResource(id = R.string.PrivateKeys_MoneroPrivateKey),
                    description = stringResource(id = R.string.PrivateKeys_MoneroPrivateKeyDescription),
                ) {
                    navController.authorizedAction {
                        navController.slideFromRight(
                            R.id.showMoneroKeyFragment,
                            ShowMoneroKeyFragment.Input(moneroKeys)
                        )

                        stat(
                            page = StatPage.PrivateKeys,
                            event = StatEvent.Open(StatPage.MoneroPrivateKey)
                        )
                    }
                }
            }
        }
    }
}
