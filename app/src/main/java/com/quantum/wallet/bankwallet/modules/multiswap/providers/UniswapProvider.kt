package com.quantum.wallet.bankwallet.modules.multiswap.providers

import com.quantum.wallet.bankwallet.R
import io.horizontalsystems.marketkit.models.BlockchainType

object UniswapProvider : BaseUniswapProvider() {
    override val id = "uniswap"
    override val title = "Uniswap"
    override val icon = R.drawable.uniswap
    override val riskLevel = RiskLevel.LIMITED

    override fun supports(blockchainType: BlockchainType): Boolean {
        return blockchainType == BlockchainType.Ethereum
    }
}
