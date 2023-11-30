package cash.p.terminal.modules.watchaddress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.modules.address.AddressHandlerFactory
import io.horizontalsystems.marketkit.models.BlockchainType

object WatchAddressModule {

    val supportedBlockchainTypes = buildList {
        addAll(App.evmBlockchainManager.allBlockchainTypes)
        add(BlockchainType.Bitcoin)
        add(BlockchainType.BitcoinCash)
        add(BlockchainType.Litecoin)
        add(BlockchainType.Dash)
        add(BlockchainType.ECash)
        add(BlockchainType.Solana)
        add(BlockchainType.Tron)
        add(BlockchainType.Ton)
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = WatchAddressService(App.accountManager, App.walletActivator, App.accountFactory, App.marketKit, App.evmBlockchainManager)
            val addressHandlerFactory =  AddressHandlerFactory(App.appConfigProvider.udnApiKey)
            val addressParserChain = addressHandlerFactory.parserChain(supportedBlockchainTypes)
            return WatchAddressViewModel(service, addressParserChain) as T
        }
    }
}
