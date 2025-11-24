package io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction

import io.horizontalsystems.bankwallet.core.App
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
                val activeAccount = App.accountManager.activeAccount!!
                val stellarKitWrapper = App.stellarKitManager.getStellarKitWrapper(activeAccount)
                SendTransactionServiceStellar(stellarKitWrapper.stellarKit)
            }

            BlockchainType.Solana -> {
                SendTransactionServiceSolana(token)
            }

            BlockchainType.Zcash,
            BlockchainType.Ton,
            BlockchainType.Monero,
            is BlockchainType.Unsupported,
                -> throw UnsupportedException("")
        }
}
