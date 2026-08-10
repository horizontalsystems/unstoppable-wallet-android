package io.horizontalsystems.walletkit.modules.multiswap.providers

import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.uniswapkit.models.DexType

object PancakeSwapV3Provider : BaseUniswapV3Provider(DexType.PancakeSwap) {
    override val id = PANCAKE_V3_PROVIDER_ID
    override val title = "PancakeSwap V3"
    override val riskLevel = RiskLevel.EXCELLENT

    override fun supports(blockchainType: BlockchainType) = when (blockchainType) {
        BlockchainType.BinanceSmartChain,
        BlockchainType.Ethereum
        -> true
        else -> false
    }
}
