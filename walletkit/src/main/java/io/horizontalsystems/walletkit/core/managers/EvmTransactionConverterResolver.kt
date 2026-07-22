package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.walletkit.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource

/**
 * A full replacement for the stock EVM transaction-record conversion. `token`
 * is the caller's filter scope (null = unscoped consumers): a converter may
 * shape the same transaction differently per token, e.g. one hash carrying
 * transfer legs in two tokens. A null record hides the row. Suspend so
 * implementations can consult suspend services (spam scoring), like the
 * stock converter does.
 */
interface IEvmTransactionConverter {
    suspend fun convert(fullTransaction: FullTransaction, token: Token?): TransactionRecord?
}

/**
 * Registration seam for substituting the EVM transaction-record converter.
 * Consulted once per transactions adapter; a null converter keeps the stock
 * conversion for that adapter.
 */
object EvmTransactionConverterResolver {

    interface Provider {
        fun converter(source: TransactionSource, baseToken: Token, userAddress: Address): IEvmTransactionConverter?
    }

    @Volatile
    var provider: Provider? = null
}
