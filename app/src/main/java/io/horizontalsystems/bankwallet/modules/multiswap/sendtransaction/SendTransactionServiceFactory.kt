package io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction

import io.horizontalsystems.bankwallet.core.UnsupportedException
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token

object SendTransactionServiceFactory {
    fun create(token: Token): AbstractSendTransactionService =
        when (val blockchainType = token.blockchainType) {
            BlockchainType.Ethereum,
            BlockchainType.BinanceSmartChain,
            BlockchainType.Polygon,
            BlockchainType.Avalanche,
            BlockchainType.Optimism,
            BlockchainType.Base,
            BlockchainType.ZkSync,
            BlockchainType.ArbitrumOne,
            BlockchainType.Gnosis,
            BlockchainType.Fantom,
                -> SendTransactionServiceEvm(blockchainType)

            BlockchainType.Bitcoin,
            BlockchainType.BitcoinCash,
            BlockchainType.ECash,
            BlockchainType.Litecoin,
            BlockchainType.Dash -> {
                SendTransactionServiceBtc(token)
            }

            BlockchainType.Tron -> {
                SendTransactionServiceTron(token)
            }

            BlockchainType.Stellar -> {
                SendTransactionServiceStellar(token)
            }

            BlockchainType.Solana -> {
                SendTransactionServiceSolana(token)
            }

            BlockchainType.Zcash,
            BlockchainType.Ton,
            is BlockchainType.Unsupported,
                -> throw UnsupportedException("")
        }
}
