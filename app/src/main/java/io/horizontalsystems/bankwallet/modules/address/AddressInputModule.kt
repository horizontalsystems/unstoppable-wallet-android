package io.horizontalsystems.bankwallet.modules.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery

object AddressInputModule {

    class FactoryToken(private val tokenQuery: TokenQuery, private val coinCode: String, private val initial: Address?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val addressViewModel = AddressViewModel(tokenQuery.blockchainType, App.contactsRepository, initial)

            addressViewModel.addAddressHandler(AddressHandlerEns(tokenQuery.blockchainType, EnsResolverHolder.resolver))
            addressViewModel.addAddressHandler(AddressHandlerUdn(tokenQuery, coinCode, App.appConfigProvider.udnApiKey))

            when (tokenQuery.blockchainType) {
                BlockchainType.Bitcoin,
                BlockchainType.BitcoinCash,
                BlockchainType.ECash,
                BlockchainType.Litecoin,
                BlockchainType.Dash,
                BlockchainType.Zcash,
                BlockchainType.BinanceChain -> {
                    addressViewModel.addAddressHandler(AddressHandlerPure(tokenQuery.blockchainType))
                }
                BlockchainType.Ethereum,
                BlockchainType.BinanceSmartChain,
                BlockchainType.Polygon,
                BlockchainType.Avalanche,
                BlockchainType.Optimism,
                BlockchainType.Gnosis,
                BlockchainType.Fantom,
                BlockchainType.ArbitrumOne -> {
                    addressViewModel.addAddressHandler(AddressHandlerEvm(tokenQuery.blockchainType))
                }
                BlockchainType.Solana -> {
                    addressViewModel.addAddressHandler(AddressHandlerSolana())
                }
                BlockchainType.Tron -> {
                    addressViewModel.addAddressHandler(AddressHandlerTron())
                }
                is BlockchainType.Unsupported -> Unit
            }

            return addressViewModel as T
        }
    }

    class FactoryNft(private val blockchainType: BlockchainType) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val addressViewModel = AddressViewModel(blockchainType, App.contactsRepository, null)

            addressViewModel.addAddressHandler(AddressHandlerEns(blockchainType, EnsResolverHolder.resolver))

            when (blockchainType) {
                BlockchainType.Bitcoin,
                BlockchainType.BitcoinCash,
                BlockchainType.ECash,
                BlockchainType.Litecoin,
                BlockchainType.Dash,
                BlockchainType.Zcash,
                BlockchainType.BinanceChain -> {
                    addressViewModel.addAddressHandler(AddressHandlerPure(blockchainType))
                }
                BlockchainType.Ethereum,
                BlockchainType.BinanceSmartChain,
                BlockchainType.Polygon,
                BlockchainType.Avalanche,
                BlockchainType.Optimism,
                BlockchainType.Gnosis,
                BlockchainType.Fantom,
                BlockchainType.ArbitrumOne -> {
                    addressViewModel.addAddressHandler(AddressHandlerEvm(blockchainType))
                }
                BlockchainType.Solana,
                BlockchainType.Tron,
                is BlockchainType.Unsupported -> Unit
            }

            return addressViewModel as T
        }
    }

}
