package cash.p.terminal.modules.watchaddress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.modules.address.AddressHandlerFactory
import io.horizontalsystems.core.entities.BlockchainType

object WatchAddressModule {

    val supportedBlockchainTypes = buildList {
        add(BlockchainType.Ethereum)
        add(BlockchainType.Tron)
        add(BlockchainType.Ton)
        add(BlockchainType.Bitcoin)
        add(BlockchainType.BitcoinCash)
        add(BlockchainType.Litecoin)
        add(BlockchainType.Dash)
        add(BlockchainType.ECash)
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = WatchAddressService(App.accountManager, App.walletActivator, App.accountFactory, App.marketKit, App.evmBlockchainManager)
            val addressHandlerFactory =  AddressHandlerFactory(App.appConfigProvider.udnApiKey)
            val addressParserChain = addressHandlerFactory.parserChain(
                blockchainTypes = supportedBlockchainTypes,
                blockchainTypesWithEns = listOf(BlockchainType.Ethereum)
            )
            return WatchAddressViewModel(service, addressParserChain) as T
        }
    }
}
