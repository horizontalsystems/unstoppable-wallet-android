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
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.manageaccount.evmprivatekey.PrivateKeyPage
import io.horizontalsystems.bankwallet.modules.manageaccount.showextendedkey.ShowExtendedKeyPage
import io.horizontalsystems.bankwallet.modules.manageaccount.showmonerokey.ShowMoneroKeyPage
import io.horizontalsystems.bankwallet.modules.manageaccount.stellarsecretkey.StellarSecretKeyPage
import io.horizontalsystems.bankwallet.modules.manageaccount.ui.KeyActionItem
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import kotlinx.serialization.Serializable

@Serializable
data class PrivateKeysPage(val input: Account) : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        ManageAccountScreen(navigation, input)
    }

}

@Composable
fun ManageAccountScreen(navigation: HSNavigation, account: Account) {
    val viewModel = viewModel<PrivateKeysViewModel>(factory = PrivateKeysModule.Factory(account))

    HSScaffold(
        title = stringResource(R.string.PrivateKeys_Title),
        onBack = { navigation.removeLastOrNull() },
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(12.dp))
            viewModel.viewState.evmPrivateKey?.let { key ->
                KeyActionItem(
                    title = stringResource(id = R.string.PrivateKeys_EvmPrivateKey),
                    description = stringResource(R.string.PrivateKeys_EvmPrivateKeyDescription),
                    onClick = navigation.authorizedAction {
                        navigation.slideFromRight(
                            PrivateKeyPage(PrivateKeyPage.Input(key, PrivateKeyPage.Type.Evm))
                        )

                        stat(
                            page = StatPage.PrivateKeys,
                            event = StatEvent.Open(StatPage.EvmPrivateKey)
                        )
                    }
                )
            }
            viewModel.viewState.tronPrivateKey?.let { key ->
                KeyActionItem(
                    title = stringResource(id = R.string.PrivateKeys_TronPrivateKey),
                    description = stringResource(R.string.PrivateKeys_TronPrivateKeyDescription),
                    onClick = navigation.authorizedAction {
                        navigation.slideFromRight(
                            PrivateKeyPage(PrivateKeyPage.Input(key, PrivateKeyPage.Type.Tron))
                        )

                        stat(
                            page = StatPage.PrivateKeys,
                            event = StatEvent.Open(StatPage.TronPrivateKey)
                        )
                    }
                )
            }
            viewModel.viewState.stellarSecretKey?.let { key ->
                KeyActionItem(
                    title = stringResource(id = R.string.PrivateKeys_StellarSecretKey),
                    description = stringResource(R.string.PrivateKeys_StellarSecretKeyDescription),
                    onClick = navigation.authorizedAction {
                        navigation.slideFromRight(
                            StellarSecretKeyPage(StellarSecretKeyPage.Input(key))
                        )

                        stat(
                            page = StatPage.PrivateKeys,
                            event = StatEvent.Open(StatPage.StellarSecretKey)
                        )
                    }
                )
            }
            viewModel.viewState.bip32RootKey?.let { key ->
                KeyActionItem(
                    title = stringResource(id = R.string.PrivateKeys_Bip32RootKey),
                    description = stringResource(id = R.string.PrivateKeys_Bip32RootKeyDescription),
                    onClick = navigation.authorizedAction {
                        navigation.slideFromRight(
                            ShowExtendedKeyPage(ShowExtendedKeyPage.Input(
                                key.hdKey,
                                key.displayKeyType
                            ))
                        )

                        stat(
                            page = StatPage.PrivateKeys,
                            event = StatEvent.Open(StatPage.Bip32RootKey)
                        )
                    },
                )
            }
            viewModel.viewState.accountExtendedPrivateKey?.let { key ->
                KeyActionItem(
                    title = stringResource(id = R.string.PrivateKeys_AccountExtendedPrivateKey),
                    description = stringResource(id = R.string.PrivateKeys_AccountExtendedPrivateKeyDescription),
                    onClick = navigation.authorizedAction {
                        navigation.slideFromRight(
                            ShowExtendedKeyPage(ShowExtendedKeyPage.Input(key.hdKey, key.displayKeyType))
                        )

                        stat(
                            page = StatPage.PrivateKeys,
                            event = StatEvent.Open(StatPage.AccountExtendedPrivateKey)
                        )
                    },
                )
            }
            viewModel.viewState.moneroKeys?.let { moneroKeys ->
                KeyActionItem(
                    title = stringResource(id = R.string.PrivateKeys_MoneroPrivateKey),
                    description = stringResource(id = R.string.PrivateKeys_MoneroPrivateKeyDescription),
                    onClick = navigation.authorizedAction {
                        navigation.slideFromRight(
                            ShowMoneroKeyPage(ShowMoneroKeyPage.Input(moneroKeys))
                        )

                        stat(
                            page = StatPage.PrivateKeys,
                            event = StatEvent.Open(StatPage.MoneroPrivateKey)
                        )
                    },
                )
            }
        }
    }
}
