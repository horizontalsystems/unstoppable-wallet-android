package cash.p.terminal.modules.address

import cash.p.terminal.core.supported
import io.horizontalsystems.bitcoincash.MainNetBitcoinCash
import io.horizontalsystems.bitcoinkit.MainNet
import io.horizontalsystems.dashkit.MainNetDash
import io.horizontalsystems.ecash.MainNetECash
import io.horizontalsystems.litecoinkit.MainNetLitecoin
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType

class AddressParserFactory(
    private val udnApiKey: String,
) {

    private fun parserChainHandlers(blockchainType: BlockchainType, withEns: Boolean = true): List<IAddressHandler> {
        val udnHandler = AddressHandlerUdn(TokenQuery(blockchainType, TokenType.Native), "", udnApiKey)
        val domainAddressHandlers = mutableListOf<IAddressHandler>(udnHandler)
        val addressHandlers = mutableListOf<IAddressHandler>()
        when (blockchainType) {
            BlockchainType.Bitcoin -> {
                val network = MainNet()
                addressHandlers.add(AddressHandlerBase58(network, blockchainType))
                addressHandlers.add(AddressHandlerBech32(network, blockchainType))
            }

            BlockchainType.BitcoinCash -> {
                val network = MainNetBitcoinCash()
                addressHandlers.add(AddressHandlerBase58(network, blockchainType))
                addressHandlers.add(AddressHandlerBitcoinCash(network))
            }

            BlockchainType.ECash -> {
                val network = MainNetECash()
                addressHandlers.add(AddressHandlerBase58(network, blockchainType))
                addressHandlers.add(AddressHandlerBitcoinCash(network))
            }

            BlockchainType.Litecoin -> {
                val network = MainNetLitecoin()
                addressHandlers.add(AddressHandlerBase58(network, blockchainType))
                addressHandlers.add(AddressHandlerBech32(network, blockchainType))
            }

            BlockchainType.Dash -> {
                val network = MainNetDash()
                addressHandlers.add(AddressHandlerBase58(network, blockchainType))
            }

            BlockchainType.BinanceChain -> {
                addressHandlers.add(AddressHandlerBinanceChain())
            }

            BlockchainType.Zcash -> {
                addressHandlers.add(AddressHandlerZcash())
            }

            BlockchainType.Ethereum,
            BlockchainType.BinanceSmartChain,
            BlockchainType.Polygon,
            BlockchainType.Avalanche,
            BlockchainType.Optimism,
            BlockchainType.Gnosis,
            BlockchainType.Fantom,
            BlockchainType.ArbitrumOne -> {
                domainAddressHandlers.add(AddressHandlerEns(blockchainType, EnsResolverHolder.resolver))
                addressHandlers.add(AddressHandlerEvm(blockchainType))
            }

            BlockchainType.Solana -> {
                addressHandlers.add(AddressHandlerSolana())
            }

            BlockchainType.Tron -> {
                addressHandlers.add(AddressHandlerTron())
            }

            is BlockchainType.Unsupported -> {
            }
        }
        if (withEns) {
            addressHandlers.addAll(domainAddressHandlers)
        }
        return addressHandlers
    }

    fun parserChain(blockchainType: BlockchainType?, withEns: Boolean = true): AddressParserChain {
        blockchainType?.let {
            return AddressParserChain().apply {
                addHandlers(parserChainHandlers(it, withEns))
            }
        }

        val handlers = BlockchainType.supported.map {
            parserChainHandlers(it, withEns)
        }.flatten()

        return AddressParserChain().apply {
            addHandlers(handlers)
        }
    }
}