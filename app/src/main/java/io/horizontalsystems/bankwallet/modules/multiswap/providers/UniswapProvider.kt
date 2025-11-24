package io.horizontalsystems.bankwallet.modules.multiswap.providers

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.marketkit.models.BlockchainType

object UniswapProvider : BaseUniswapProvider() {
    override val id = "uniswap"
    override val title = "Uniswap"
    override val icon = R.drawable.uniswap
    override val priority = 0

    override fun supports(blockchainType: BlockchainType): Boolean {
        return blockchainType == BlockchainType.Ethereum
    }
}
