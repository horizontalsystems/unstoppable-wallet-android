package cash.p.terminal.modules.watchaddress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.modules.address.AddressParserFactory
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
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = WatchAddressService(App.accountManager, App.walletActivator, App.accountFactory, App.marketKit, App.evmBlockchainManager)
            val addressParserChainFactory =  AddressParserFactory(App.appConfigProvider.udnApiKey)
            val addressParserChain = addressParserChainFactory.parserChain(supportedBlockchainTypes)
            return WatchAddressViewModel(service, addressParserChain) as T
        }
    }
}
