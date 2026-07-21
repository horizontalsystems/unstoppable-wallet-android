package io.horizontalsystems.walletkit.modules.multiswap.sendtransaction

import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.UnsupportedException
import io.horizontalsystems.walletkit.core.adapters.MoneroAdapter
import io.horizontalsystems.walletkit.core.adapters.ThorchainAdapter
import io.horizontalsystems.walletkit.core.adapters.ZanoAdapter
import io.horizontalsystems.walletkit.core.adapters.zcash.ZcashAdapter
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
                val activeAccount = App.accountManager.activeAccount!!
                val stellarKitWrapper = App.stellarKitManager.getStellarKitWrapper(activeAccount)
                SendTransactionServiceStellar(stellarKitWrapper.stellarKit, token)
            }

            BlockchainType.Solana -> {
                SendTransactionServiceSolana(token)
            }

            BlockchainType.Ton -> {
                SendTransactionServiceTon(token)
            }

            BlockchainType.Zcash -> {
                val adapter = App.adapterManager.getAdapterForToken<ZcashAdapter>(token)
                    ?: throw IllegalStateException("ZcashAdapter is null")
                SendTransactionServiceZcash(adapter)
            }

            BlockchainType.Monero -> {
                val adapter = App.adapterManager.getAdapterForToken<MoneroAdapter>(token)
                    ?: throw IllegalStateException("MoneroAdapter is null")
                SendTransactionServiceMonero(adapter)
            }

            BlockchainType.Zano -> {
                val adapter = App.adapterManager.getAdapterForToken<ZanoAdapter>(token)
                    ?: throw IllegalStateException("ZanoAdapter is null")
                SendTransactionServiceZano(adapter)
            }

            BlockchainType.Thorchain -> {
                val adapter = App.adapterManager.getAdapterForToken<ThorchainAdapter>(token)
                    ?: throw IllegalStateException("ThorchainAdapter is null")
                SendTransactionServiceThorchain(adapter)
            }

            is BlockchainType.Unsupported,
                -> throw UnsupportedException("")
        }
}
