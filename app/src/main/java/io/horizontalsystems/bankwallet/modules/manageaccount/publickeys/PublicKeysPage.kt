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
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.manageaccount.evmaddress.AddressPage
import io.horizontalsystems.bankwallet.modules.manageaccount.showextendedkey.ShowExtendedKeyPage
import io.horizontalsystems.bankwallet.modules.manageaccount.showmonerokey.ShowMoneroKeyPage
import io.horizontalsystems.bankwallet.modules.manageaccount.ui.KeyActionItem
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import kotlinx.serialization.Serializable

@Serializable
data class PublicKeysPage(val input: Account) : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        ManageAccountScreen(navigation, input)
    }

}

@Composable
fun ManageAccountScreen(navigation: HSNavigation, account: Account) {
    val viewModel = viewModel<PublicKeysViewModel>(factory = PublicKeysModule.Factory(account))

    HSScaffold(
        title = stringResource(R.string.PublicKeys_Title),
        onBack = { navigation.removeLastOrNull() },
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
                    navigation.slideFromRight(
                        AddressPage(AddressPage.Input(evmAddress, AddressPage.Type.Evm))
                    )

                    stat(page = StatPage.PublicKeys, event = StatEvent.Open(StatPage.EvmAddress))
                }
            }
            viewModel.viewState.tronAddress?.let { tronAddress ->
                KeyActionItem(
                    title = stringResource(id = R.string.PublicKeys_TronAddress),
                    description = stringResource(R.string.PublicKeys_TronAddress_Description)
                ) {
                    navigation.slideFromRight(
                        AddressPage(AddressPage.Input(tronAddress, AddressPage.Type.Tron))
                    )

                    stat(page = StatPage.PublicKeys, event = StatEvent.Open(StatPage.TronAddress))
                }
            }
            viewModel.viewState.extendedPublicKey?.let { publicKey ->
                KeyActionItem(
                    title = stringResource(id = R.string.PublicKeys_AccountExtendedPublicKey),
                    description = stringResource(id = R.string.PublicKeys_AccountExtendedPublicKeyDescription),
                ) {
                    navigation.slideFromRight(
                        ShowExtendedKeyPage(ShowExtendedKeyPage.Input(
                            publicKey.hdKey,
                            publicKey.accountPublicKey
                        ))
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
                    onClick = navigation.authorizedAction {
                        navigation.slideFromRight(
                            ShowMoneroKeyPage(ShowMoneroKeyPage.Input(moneroKeys))
                        )
                        stat(
                            page = StatPage.PublicKeys,
                            event = StatEvent.Open(StatPage.MoneroPublicKey)
                        )
                    },
                )
            }
        }
    }
}
