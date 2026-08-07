package io.horizontalsystems.walletkit.modules.address

import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.walletkit.core.chain.ChainRegistry

/**
 * Plain (non-domain) address handlers per blockchain — the single source of truth used by
 * both [AddressHandlerFactory] and [AddressInputModule].
 */
fun plainAddressHandlers(blockchainType: BlockchainType): List<IAddressHandler> =
    when (blockchainType) {
        BlockchainType.Ethereum,
        BlockchainType.BinanceSmartChain,
        BlockchainType.Polygon,
        BlockchainType.Avalanche,
        BlockchainType.Optimism,
        BlockchainType.Base,
        BlockchainType.ZkSync,
        BlockchainType.Gnosis,
        BlockchainType.Fantom,
        BlockchainType.ArbitrumOne -> listOf(AddressHandlerEvm(blockchainType))

        BlockchainType.Tron -> listOf(AddressHandlerTron())

        else -> ChainRegistry[blockchainType]?.addressHandlers().orEmpty()
    }
