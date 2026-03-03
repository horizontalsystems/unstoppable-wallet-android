package io.horizontalsystems.bankwallet.modules.manageaccount.privatekeys

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
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.manageaccount.ui.KeyActionItem
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import kotlinx.serialization.Serializable

@Serializable
data class PrivateKeysScreen(val account: Account) : HSScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        ManageAccountScreen(backStack, account)
    }
}

class PrivateKeysFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
//        withInput<Account>(navController) { account ->
//            ManageAccountScreen(navController, account)
//        }
    }

}

@Composable
fun ManageAccountScreen(backStack: NavBackStack<HSScreen>, account: Account) {
    val viewModel = viewModel<PrivateKeysViewModel>(factory = PrivateKeysModule.Factory(account))

    HSScaffold(
        title = stringResource(R.string.PrivateKeys_Title),
        onBack = { backStack.removeLastOrNull() },
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
//                    TODO("xxx nav3")
//                    navController.authorizedAction {
//                        navController.slideFromRight(
//                            R.id.evmPrivateKeyFragment,
//                            EvmPrivateKeyFragment.Input(key)
//                        )
//
//                        stat(
//                            page = StatPage.PrivateKeys,
//                            event = StatEvent.Open(StatPage.EvmPrivateKey)
//                        )
//                    }
                }
            }
            viewModel.viewState.stellarSecretKey?.let { key ->
                KeyActionItem(
                    title = stringResource(id = R.string.PrivateKeys_StellarSecretKey),
                    description = stringResource(R.string.PrivateKeys_StellarSecretKeyDescription)
                ) {
//                    TODO("xxx nav3")
//                    navController.authorizedAction {
//                        navController.slideFromRight(
//                            R.id.stellarSecretKeyFragment,
//                            StellarSecretKeyFragment.Input(key)
//                        )
//
//                        stat(
//                            page = StatPage.PrivateKeys,
//                            event = StatEvent.Open(StatPage.StellarSecretKey)
//                        )
//                    }
                }
            }
            viewModel.viewState.bip32RootKey?.let { key ->
                KeyActionItem(
                    title = stringResource(id = R.string.PrivateKeys_Bip32RootKey),
                    description = stringResource(id = R.string.PrivateKeys_Bip32RootKeyDescription),
                ) {
//                    TODO("xxx nav3")
//                    navController.authorizedAction {
//                        navController.slideFromRight(
//                            R.id.showExtendedKeyFragment,
//                            ShowExtendedKeyFragment.Input(
//                                key.hdKey,
//                                key.displayKeyType
//                            )
//                        )
//
//                        stat(
//                            page = StatPage.PrivateKeys,
//                            event = StatEvent.Open(StatPage.Bip32RootKey)
//                        )
//                    }
                }
            }
            viewModel.viewState.accountExtendedPrivateKey?.let { key ->
                KeyActionItem(
                    title = stringResource(id = R.string.PrivateKeys_AccountExtendedPrivateKey),
                    description = stringResource(id = R.string.PrivateKeys_AccountExtendedPrivateKeyDescription),
                ) {
//                    TODO("xxx nav3")
//                    navController.authorizedAction {
//                        navController.slideFromRight(
//                            R.id.showExtendedKeyFragment,
//                            ShowExtendedKeyFragment.Input(key.hdKey, key.displayKeyType)
//                        )
//
//                        stat(
//                            page = StatPage.PrivateKeys,
//                            event = StatEvent.Open(StatPage.AccountExtendedPrivateKey)
//                        )
//                    }
                }
            }
            viewModel.viewState.moneroKeys?.let { moneroKeys ->
                KeyActionItem(
                    title = stringResource(id = R.string.PrivateKeys_MoneroPrivateKey),
                    description = stringResource(id = R.string.PrivateKeys_MoneroPrivateKeyDescription),
                ) {
//                    TODO("xxx nav3")
//                    navController.authorizedAction {
//                        navController.slideFromRight(
//                            R.id.showMoneroKeyFragment,
//                            ShowMoneroKeyFragment.Input(moneroKeys)
//                        )
//
//                        stat(
//                            page = StatPage.PrivateKeys,
//                            event = StatEvent.Open(StatPage.MoneroPrivateKey)
//                        )
//                    }
                }
            }
        }
    }
}
