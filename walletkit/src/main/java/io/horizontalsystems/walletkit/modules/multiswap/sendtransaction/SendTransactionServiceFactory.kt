package io.horizontalsystems.walletkit.modules.multiswap.sendtransaction

import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.UnsupportedException
import io.horizontalsystems.walletkit.core.adapters.ThorchainAdapter
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
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

            BlockchainType.Ton -> {
                SendTransactionServiceTon(token)
            }



            BlockchainType.Thorchain,
            BlockchainType.Mayachain -> {
                val adapter = App.adapterManager.getAdapterForToken<ThorchainAdapter>(token)
                    ?: throw IllegalStateException("ThorchainAdapter is null")
                SendTransactionServiceThorchain(adapter, blockchainType)
            }

            else -> ChainRegistry[blockchainType]?.sendTransactionService(token)
                ?: throw UnsupportedException("")
        }
}
