package io.horizontalsystems.bankwallet.modules.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.utils.AddressUriParser
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bitcoincash.MainNetBitcoinCash
import io.horizontalsystems.bitcoinkit.MainNet
import io.horizontalsystems.dashkit.MainNetDash
import io.horizontalsystems.ecash.MainNetECash
import io.horizontalsystems.litecoinkit.MainNetLitecoin
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery

object AddressInputModule {

    class FactoryToken(private val tokenQuery: TokenQuery, private val coinCode: String, private val initial: Address?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val blockchainType = tokenQuery.blockchainType
            val ensHandler = AddressHandlerEns(blockchainType, EnsResolverHolder.resolver)
            val udnHandler = AddressHandlerUdn(tokenQuery, coinCode, App.appConfigProvider.udnApiKey)
            val addressParserChain = AddressParserChain(domainHandlers = listOf(ensHandler, udnHandler))

            when (blockchainType) {
                BlockchainType.Bitcoin -> {
                    val network = MainNet()
                    addressParserChain.addHandler(AddressHandlerBase58(network, blockchainType))
                    addressParserChain.addHandler(AddressHandlerBech32(network, blockchainType))
                }

                BlockchainType.BitcoinCash -> {
                    val network = MainNetBitcoinCash()
                    addressParserChain.addHandler(AddressHandlerBase58(network, blockchainType))
                    addressParserChain.addHandler(AddressHandlerBitcoinCash(network, blockchainType))
                }

                BlockchainType.ECash -> {
                    val network = MainNetECash()
                    addressParserChain.addHandler(AddressHandlerBase58(network, blockchainType))
                    addressParserChain.addHandler(AddressHandlerBitcoinCash(network, blockchainType))
                }

                BlockchainType.Litecoin -> {
                    val network = MainNetLitecoin()
                    addressParserChain.addHandler(AddressHandlerBase58(network, blockchainType))
                    addressParserChain.addHandler(AddressHandlerBech32(network, blockchainType))
                }

                BlockchainType.Dash -> {
                    val network = MainNetDash()
                    addressParserChain.addHandler(AddressHandlerBase58(network, blockchainType))
                }

                BlockchainType.Zcash -> {
                    addressParserChain.addHandler(AddressHandlerZcash())
                }
                BlockchainType.Bitcoin,
                BlockchainType.BitcoinCash,
                BlockchainType.ECash,
                BlockchainType.Litecoin,
                BlockchainType.Dash,
                BlockchainType.Zcash -> {
                    addressParserChain.addHandler(AddressHandlerPure(blockchainType))
                }
                BlockchainType.Ethereum,
                BlockchainType.BinanceSmartChain,
                BlockchainType.Polygon,
                BlockchainType.Avalanche,
                BlockchainType.Optimism,
                BlockchainType.Base,
                BlockchainType.ZkSync,
                BlockchainType.Gnosis,
                BlockchainType.Fantom,
                BlockchainType.ArbitrumOne -> {
                    addressParserChain.addHandler(AddressHandlerEvm(blockchainType))
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
                BlockchainType.Stellar -> {
                    addressParserChain.addHandler(AddressHandlerStellar())
                }
                BlockchainType.Monero -> {
                    addressParserChain.addHandler(AddressHandlerMonero())
                }
                is BlockchainType.Unsupported -> Unit
            }

            val addressUriParser = AddressUriParser(blockchainType, tokenQuery.tokenType)
            val addressViewModel = AddressViewModel(
                blockchainType,
                App.contactsRepository,
                addressUriParser,
                addressParserChain,
                initial
            )

            return addressViewModel as T
        }
    }

}
