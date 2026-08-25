package io.horizontalsystems.walletkit.modules.multiswap.sendtransaction

import io.horizontalsystems.walletkit.core.UnsupportedException
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.marketkit.models.Token

object SendTransactionServiceFactory {
    fun create(token: Token): AbstractSendTransactionService =
        ChainRegistry[token.blockchainType]?.sendTransactionService(token)
            ?: throw UnsupportedException("")
}
