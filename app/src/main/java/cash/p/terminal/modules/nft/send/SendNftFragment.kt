package cash.p.terminal.modules.nft.send

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.entities.nft.EvmNftRecord
import cash.p.terminal.entities.nft.NftKey
import cash.p.terminal.entities.nft.NftUid
import cash.p.terminal.modules.address.AddressInputModule
import cash.p.terminal.modules.address.AddressParserViewModel
import cash.p.terminal.modules.address.AddressViewModel
import cash.p.terminal.modules.send.evm.SendEvmAddressService
import cash.p.terminal.modules.send.evm.confirmation.EvmKitWrapperHoldingViewModel
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.MenuItem
import cash.p.terminal.ui.compose.components.ScreenMessageWithAction
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.nftkit.models.NftType

class SendNftFragment : BaseFragment() {

    private val vmFactory by lazy { getFactory(requireArguments()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            val factory = vmFactory

            setContent {
                when (factory?.evmNftRecord?.nftType) {
                    NftType.Eip721 -> {
                        val evmKitWrapperViewModel by navGraphViewModels<EvmKitWrapperHoldingViewModel>(
                            R.id.nftSendFragment
                        ) { factory }
                        val initiateLazyViewModel = evmKitWrapperViewModel //needed in SendEvmConfirmationFragment

                        val eip721ViewModel by viewModels<SendEip721ViewModel> { factory }
                        val addressViewModel by viewModels<AddressViewModel> {
                            AddressInputModule.FactoryNft(factory.nftUid.blockchainType)
                        }
                        val addressParserViewModel by viewModels<AddressParserViewModel> { factory }
                        SendEip721Screen(
                            findNavController(),
                            eip721ViewModel,
                            addressViewModel,
                            addressParserViewModel,
                            R.id.nftSendFragment,
                        )
                    }
                    NftType.Eip1155 -> {
                        val evmKitWrapperViewModel by navGraphViewModels<EvmKitWrapperHoldingViewModel>(
                            R.id.nftSendFragment
                        ) { factory }
                        val initiateLazyViewModel = evmKitWrapperViewModel //needed in SendEvmConfirmationFragment

                        val eip1155ViewModel by viewModels<SendEip1155ViewModel> { factory }
                        val addressViewModel by viewModels<AddressViewModel> {
                            AddressInputModule.FactoryNft(factory.nftUid.blockchainType)
                        }
                        val addressParserViewModel by viewModels<AddressParserViewModel> { factory }
                        SendEip1155Screen(
                            findNavController(),
                            eip1155ViewModel,
                            addressViewModel,
                            addressParserViewModel,
                            R.id.nftSendFragment,
                        )
                    }
                    else -> {
                        ShowErrorMessage(findNavController())
                    }
                }
            }
        }
    }
}

private fun getFactory(requireArguments: Bundle): SendNftModule.Factory? {
    val nftUid = requireArguments.getString(SendNftModule.nftUidKey)?.let {
        NftUid.fromUid(it)
    } ?: return null

    val account = App.accountManager.activeAccount ?: return null

    if (account.isWatchAccount) return null

    val nftKey = NftKey(account, nftUid.blockchainType)

    val adapter = App.nftAdapterManager.adapter(nftKey) ?: return null

    val nftRecord = adapter.nftRecord(nftUid) ?: return null

    val evmNftRecord = (nftRecord as? EvmNftRecord) ?: return null

    val evmKitWrapper = App.evmBlockchainManager
        .getEvmKitManager(nftUid.blockchainType)
        .getEvmKitWrapper(account, nftUid.blockchainType)

    return SendNftModule.Factory(
        evmNftRecord,
        nftUid,
        nftRecord.balance,
        adapter,
        SendEvmAddressService(),
        App.nftMetadataManager,
        evmKitWrapper
    )
}

@Composable
private fun ShowErrorMessage(navController: NavController) {
    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = TranslatableString.ResString(R.string.SendNft_Title),
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
}
