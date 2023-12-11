package io.horizontalsystems.bankwallet.modules.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.utils.AddressUriParser
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery

object AddressInputModule {

    class FactoryToken(private val tokenQuery: TokenQuery, private val coinCode: String, private val initial: Address?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val ensHandler = AddressHandlerEns(tokenQuery.blockchainType, EnsResolverHolder.resolver)
            val udnHandler = AddressHandlerUdn(tokenQuery, coinCode, App.appConfigProvider.udnApiKey)
            val addressParserChain = AddressParserChain(domainHandlers = listOf(ensHandler, udnHandler))

            when (tokenQuery.blockchainType) {
                BlockchainType.Bitcoin,
                BlockchainType.BitcoinCash,
                BlockchainType.ECash,
                BlockchainType.Litecoin,
                BlockchainType.Dash,
                BlockchainType.Zcash,
                BlockchainType.BinanceChain -> {
                    addressParserChain.addHandler(AddressHandlerPure(tokenQuery.blockchainType))
                }
                BlockchainType.Ethereum,
                BlockchainType.BinanceSmartChain,
                BlockchainType.Polygon,
                BlockchainType.Avalanche,
                BlockchainType.Optimism,
                BlockchainType.Gnosis,
                BlockchainType.Fantom,
                BlockchainType.ArbitrumOne -> {
                    addressParserChain.addHandler(AddressHandlerEvm(tokenQuery.blockchainType))
                }
                BlockchainType.Solana -> {
                    addressParserChain.addHandler(AddressHandlerSolana())
                }
                BlockchainType.Tron -> {
                    addressParserChain.addHandler(AddressHandlerTron())
                }
                BlockchainType.Ton -> {
                    addressParserChain.addHandler(AddressHandlerTon())
                }
                is BlockchainType.Unsupported -> Unit
            }

            val addressUriParser = AddressUriParser(tokenQuery.blockchainType, tokenQuery.tokenType)
            val addressViewModel = AddressViewModel(
                tokenQuery.blockchainType,
                App.contactsRepository,
                addressUriParser,
                addressParserChain,
                initial
            )

            return addressViewModel as T
        }
    }

    class FactoryNft(private val blockchainType: BlockchainType) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val ensHandler = AddressHandlerEns(blockchainType, EnsResolverHolder.resolver)
            val addressParserChain = AddressParserChain(domainHandlers = listOf(ensHandler))

            when (blockchainType) {
                BlockchainType.Bitcoin,
                BlockchainType.BitcoinCash,
                BlockchainType.ECash,
                BlockchainType.Litecoin,
                BlockchainType.Dash,
                BlockchainType.Zcash,
                BlockchainType.BinanceChain -> {
                    addressParserChain.addHandler(AddressHandlerPure(blockchainType))
                }
                BlockchainType.Ethereum,
                BlockchainType.BinanceSmartChain,
                BlockchainType.Polygon,
                BlockchainType.Avalanche,
                BlockchainType.Optimism,
                BlockchainType.Gnosis,
                BlockchainType.Fantom,
                BlockchainType.ArbitrumOne -> {
                    addressParserChain.addHandler(AddressHandlerEvm(blockchainType))
                }
                BlockchainType.Solana,
                BlockchainType.Tron,
                BlockchainType.Ton,
                is BlockchainType.Unsupported -> Unit
            }

            val addressUriParser = AddressUriParser(blockchainType, null)
            val addressViewModel = AddressViewModel(
                blockchainType,
                App.contactsRepository,
                addressUriParser,
                addressParserChain,
                null
            )

            return addressViewModel as T
        }
    }

}
