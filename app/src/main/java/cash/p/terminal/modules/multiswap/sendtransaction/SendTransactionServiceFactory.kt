package cash.p.terminal.modules.multiswap.sendtransaction

import cash.p.terminal.core.UnsupportedException
import io.horizontalsystems.core.entities.BlockchainType

object SendTransactionServiceFactory {
    fun create(blockchainType: BlockchainType): ISendTransactionService = when (blockchainType) {
        BlockchainType.Ethereum,
        BlockchainType.BinanceSmartChain,
        BlockchainType.Polygon,
        BlockchainType.Avalanche,
        BlockchainType.Optimism,
        BlockchainType.Base,
        BlockchainType.ArbitrumOne,
        BlockchainType.Gnosis,
        BlockchainType.Fantom -> SendTransactionServiceEvm(blockchainType)

        BlockchainType.Bitcoin,
        BlockchainType.BitcoinCash,
        BlockchainType.ECash,
        BlockchainType.Litecoin,
        BlockchainType.Dash,
        BlockchainType.Zcash,
        BlockchainType.BinanceChain,
        BlockchainType.Solana,
        BlockchainType.Tron,
        BlockchainType.Ton,
        is BlockchainType.Unsupported -> throw UnsupportedException("")
    }
}
