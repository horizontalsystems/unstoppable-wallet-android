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
        // Chain-specific name services go first: they reject foreign input without a network
        // call, whereas ENS and UDN attempt a remote lookup for any dotted name.
        val domainAddressHandlers = mutableListOf<IAddressHandler>()
        domainAddressHandlers.addAll(ChainRegistry[blockchainType]?.domainAddressHandlers().orEmpty())
        domainAddressHandlers.add(udnHandler)
        when (blockchainType) {
            BlockchainType.Ethereum,
            BlockchainType.BinanceSmartChain,
            BlockchainType.Polygon,
            BlockchainType.Avalanche,
            BlockchainType.Optimism,
            BlockchainType.Base,
            BlockchainType.ZkSync,
            BlockchainType.RobinhoodChain,
            BlockchainType.Arc,
            BlockchainType.Gnosis,
            BlockchainType.Fantom,
            BlockchainType.ArbitrumOne -> {
                domainAddressHandlers.add(AddressHandlerEns(blockchainType, EnsResolverHolder.resolver))
            }

            else -> {}
        }
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
