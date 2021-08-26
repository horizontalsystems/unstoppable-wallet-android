package io.horizontalsystems.bankwallet.entities.transactionrecords.evm

import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.ethereumkit.models.FullTransaction
import java.math.BigDecimal

class EvmIncomingTransactionRecord(
    fullTransaction: FullTransaction,
    baseCoin: Coin,
    amount: BigDecimal,
    val from: String,
    token: Coin,
    override val foreignTransaction: Boolean = false,
    source: TransactionSource
) : EvmTransactionRecord(fullTransaction, baseCoin, source) {

    val value: CoinValue = CoinValue(token, amount)

    override val mainValue = value
}
