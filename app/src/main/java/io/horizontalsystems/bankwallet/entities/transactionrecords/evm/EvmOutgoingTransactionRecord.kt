package io.horizontalsystems.bankwallet.entities.transactionrecords.evm

import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.ethereumkit.models.FullTransaction
import java.math.BigDecimal

class EvmOutgoingTransactionRecord(
    fullTransaction: FullTransaction,
    baseCoin: Coin,
    amount: BigDecimal,
    val to: String,
    val token: Coin,
    val sentToSelf: Boolean
) : EvmTransactionRecord(fullTransaction, baseCoin) {

    val value: CoinValue = CoinValue(token, amount)

    override val mainValue: CoinValue = value

}
