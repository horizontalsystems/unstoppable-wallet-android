package io.horizontalsystems.bankwallet.modules.multiswap.providers

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.uniswapkit.models.DexType

object UniswapV3Provider : BaseUniswapV3Provider(DexType.Uniswap) {
    override val id = "uniswap_v3"
    override val title = "Uniswap V3"
    override val icon = R.drawable.uniswap_v3
    override val priority = 0

    override fun supports(blockchainType: BlockchainType) = when (blockchainType) {
        BlockchainType.Ethereum,
        BlockchainType.ArbitrumOne,
//            BlockchainType.Optimism,
        BlockchainType.Polygon,
        BlockchainType.BinanceSmartChain,
        BlockchainType.Base,
        BlockchainType.ZkSync,
        -> true
        else -> false
    }
}
