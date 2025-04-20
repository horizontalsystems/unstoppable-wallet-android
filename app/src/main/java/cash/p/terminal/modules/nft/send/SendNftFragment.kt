package cash.p.terminal.modules.nft.send

import android.os.Parcelable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.ui_compose.requireInput
import cash.p.terminal.entities.nft.EvmNftRecord
import cash.p.terminal.entities.nft.NftKey
import cash.p.terminal.entities.nft.NftUid
import cash.p.terminal.modules.address.AddressInputModule
import cash.p.terminal.modules.address.AddressParserViewModel
import cash.p.terminal.modules.address.AddressViewModel
import cash.p.terminal.modules.send.evm.SendEvmAddressService
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.ui.compose.components.ScreenMessageWithAction
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import io.horizontalsystems.nftkit.models.NftType
import kotlinx.parcelize.Parcelize

class SendNftFragment : BaseComposeFragment() {

    @Parcelize
    data class Input(val nftUid: String) : Parcelable

    @Composable
    override fun GetContent(navController: NavController) {
        val factory = getFactory(navController.requireInput<Input>().nftUid)

        when (factory?.evmNftRecord?.nftType) {
            NftType.Eip721 -> {
                val eip721ViewModel by viewModels<SendEip721ViewModel> { factory }
                val addressViewModel by viewModels<AddressViewModel> {
                    AddressInputModule.FactoryNft(factory.nftUid.blockchainType)
                }
                val addressParserViewModel by viewModels<AddressParserViewModel> { factory }
                SendEip721Screen(
                    navController,
                    eip721ViewModel,
                    addressViewModel,
                    addressParserViewModel,
                )
            }

            NftType.Eip1155 -> {
                val eip1155ViewModel by viewModels<SendEip1155ViewModel> { factory }
                val addressViewModel by viewModels<AddressViewModel> {
                    AddressInputModule.FactoryNft(factory.nftUid.blockchainType)
                }
                val addressParserViewModel by viewModels<AddressParserViewModel> { factory }
                SendEip1155Screen(
                    navController,
                    eip1155ViewModel,
                    addressViewModel,
                    addressParserViewModel,
                )
            }

            else -> {
                ShowErrorMessage(navController)
            }
        }
    }

}

private fun getFactory(nftUidString: String): SendNftModule.Factory? {
    val nftUid = NftUid.fromUid(nftUidString)

    val account = App.accountManager.activeAccount ?: return null

    if (account.isWatchAccount) return null

    val nftKey = NftKey(account, nftUid.blockchainType)

    val adapter = App.nftAdapterManager.adapter(nftKey) ?: return null

    val nftRecord = adapter.nftRecord(nftUid) ?: return null

    val evmNftRecord = (nftRecord as? EvmNftRecord) ?: return null

    return SendNftModule.Factory(
        evmNftRecord,
        nftUid,
        nftRecord.balance,
        adapter,
        SendEvmAddressService(),
        App.nftMetadataManager
    )
}

@Composable
private fun ShowErrorMessage(navController: NavController) {
    Scaffold(
        containerColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = stringResource(R.string.SendNft_Title),
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = { navController.popBackStack() }
                    )
                )
            )
        }
    ) {
        Column(Modifier.padding(it)) {
            ScreenMessageWithAction(
                text = stringResource(R.string.Error),
                icon = R.drawable.ic_error_48
            )
        }
    }
}
