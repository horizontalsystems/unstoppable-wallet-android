package io.horizontalsystems.walletkit.core

import io.horizontalsystems.nftkit.models.NftType
import io.horizontalsystems.marketkit.models.BlockchainType

val BlockchainType.supportedNftTypes: List<NftType>
    get() = when (this) {
        BlockchainType.Ethereum -> listOf(NftType.Eip721, NftType.Eip1155)
//        BlockchainType.BinanceSmartChain -> listOf(NftType.Eip721)
//        BlockchainType.Polygon -> listOf(NftType.Eip721, NftType.Eip1155)
//        BlockchainType.Avalanche -> listOf(NftType.Eip721)
//        BlockchainType.ArbitrumOne -> listOf(NftType.Eip721)
        else -> listOf()
    }
