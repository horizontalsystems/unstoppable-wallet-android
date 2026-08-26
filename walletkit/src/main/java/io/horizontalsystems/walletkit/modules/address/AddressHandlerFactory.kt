package io.horizontalsystems.walletkit.modules.address

import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.core.supported
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType

class AddressHandlerFactory(
    private val udnApiKey: String,
) {

    private fun domainHandlers(blockchainType: BlockchainType): List<IAddressHandler> {
        val udnHandler = AddressHandlerUdn(TokenQuery(blockchainType, TokenType.Native), null, udnApiKey)
        val domainAddressHandlers = mutableListOf<IAddressHandler>(udnHandler)
        when (blockchainType) {
            BlockchainType.Ethereum,
            BlockchainType.BinanceSmartChain,
            BlockchainType.Polygon,
            BlockchainType.Avalanche,
            BlockchainType.Optimism,
            BlockchainType.Base,
            BlockchainType.ZkSync,
            BlockchainType.RobinhoodChain,
            BlockchainType.Gnosis,
            BlockchainType.Fantom,
            BlockchainType.ArbitrumOne -> {
                domainAddressHandlers.add(AddressHandlerEns(blockchainType, EnsResolverHolder.resolver))
            }

            else -> {}
        }
        domainAddressHandlers.addAll(ChainRegistry[blockchainType]?.domainAddressHandlers().orEmpty())
        return domainAddressHandlers
    }

    fun parserChain(blockchainType: BlockchainType?, withEns: Boolean = false): AddressParserChain {
        val addressHandlers = mutableListOf<IAddressHandler>()
        val domainHandlers = mutableListOf<IAddressHandler>()

        blockchainType?.let {
            addressHandlers.addAll(plainAddressHandlers(it))
            if (withEns) {
                domainHandlers.addAll(domainHandlers(it))
            }
        } ?: run {
            BlockchainType.supported.forEach {
                addressHandlers.addAll(plainAddressHandlers(it))
                if (withEns) {
                    domainHandlers.addAll(domainHandlers(it))
                }
            }
        }

        return AddressParserChain(addressHandlers, domainHandlers)
    }

    fun parserChain(blockchainTypes: List<BlockchainType>, blockchainTypesWithEns: List<BlockchainType>): AddressParserChain {
        val addressHandlers = mutableListOf<IAddressHandler>()
        val domainHandlers = mutableListOf<IAddressHandler>()

        for (blockchainType in blockchainTypes) {
            addressHandlers.addAll(plainAddressHandlers(blockchainType))
        }

        for (blockchainType in blockchainTypesWithEns) {
            domainHandlers.addAll(domainHandlers(blockchainType))
        }

        return AddressParserChain(addressHandlers, domainHandlers)
    }

}
