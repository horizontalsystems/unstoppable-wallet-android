package io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction

import io.horizontalsystems.bankwallet.core.UnsupportedException
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token

object SendTransactionServiceFactory {
    fun create(tokenIn: Token): ISendTransactionService = when (val blockchainType = tokenIn.blockchainType) {
        BlockchainType.Bitcoin -> TODO()
        BlockchainType.BitcoinCash -> TODO()
        BlockchainType.ECash -> TODO()
        BlockchainType.Litecoin -> TODO()
        BlockchainType.Dash -> TODO()
        BlockchainType.Zcash -> TODO()
        BlockchainType.Ethereum,
        BlockchainType.BinanceSmartChain -> SendTransactionServiceEvm(blockchainType)
        BlockchainType.BinanceChain -> TODO()
        BlockchainType.Polygon -> TODO()
        BlockchainType.Avalanche -> TODO()
        BlockchainType.Optimism -> TODO()
        BlockchainType.ArbitrumOne -> TODO()
        BlockchainType.Solana -> TODO()
        BlockchainType.Gnosis -> TODO()
        BlockchainType.Fantom -> TODO()
        BlockchainType.Tron -> TODO()
        BlockchainType.Ton -> TODO()
        is BlockchainType.Unsupported -> throw UnsupportedException("")
    }
}
