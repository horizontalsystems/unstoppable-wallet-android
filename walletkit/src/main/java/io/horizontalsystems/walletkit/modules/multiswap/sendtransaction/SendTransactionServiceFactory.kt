package io.horizontalsystems.walletkit.modules.multiswap.sendtransaction

import io.horizontalsystems.walletkit.core.UnsupportedException
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.marketkit.models.Token

object SendTransactionServiceFactory {
    /**
     * [feePriority] is [FeePriority.Priority] for transactions where confirmation time matters
     * (swaps and their approve and revoke, private send, cross pay).
     */
    fun create(
        token: Token,
        feePriority: FeePriority = FeePriority.Standard,
    ): AbstractSendTransactionService {
        val service = ChainRegistry[token.blockchainType]?.sendTransactionService(token)
            ?: throw UnsupportedException("")

        service.feePriority = feePriority

        return service
    }
}
