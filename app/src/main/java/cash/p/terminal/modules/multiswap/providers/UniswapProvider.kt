package cash.p.terminal.modules.multiswap.providers

import cash.p.terminal.R
import cash.p.terminal.wallet.Token
import io.horizontalsystems.core.entities.BlockchainType

object UniswapProvider : BaseUniswapProvider() {
    override val id = "uniswap"
    override val title = "Uniswap"
    override val icon = R.drawable.uniswap

    override suspend fun supports(token: Token): Boolean {
        return token.blockchainType == BlockchainType.Ethereum
    }
}
