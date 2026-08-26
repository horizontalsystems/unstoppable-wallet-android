package io.horizontalsystems.walletkit.modules.multiswap.providers

import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.uniswapkit.models.DexType

object UniswapV3Provider : BaseUniswapV3Provider(DexType.Uniswap) {
    override val id = UNISWAP_V3_PROVIDER_ID
    override val title = "Uniswap V3"
    override val riskLevel = RiskLevel.EXCELLENT

    override fun supports(blockchainType: BlockchainType) = when (blockchainType) {
        BlockchainType.Ethereum,
        BlockchainType.ArbitrumOne,
//            BlockchainType.Optimism,
        BlockchainType.Polygon,
        BlockchainType.BinanceSmartChain,
        BlockchainType.Base,
        BlockchainType.ZkSync,
        BlockchainType.RobinhoodChain,
        -> true
        else -> false
    }
}
