package io.horizontalsystems.bankwallet.entities.transactionrecords.evm

import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.ethereumkit.models.FullTransaction
import java.math.BigDecimal

class ContractCallTransactionRecord(
    fullTransaction: FullTransaction,
    baseCoin: Coin,
    val contractAddress: String,
    val method: String?,
    value: BigDecimal,
    val incomingInternalETHs: List<Pair<String, CoinValue>>,
    val incomingEip20Events: List<Pair<String, CoinValue>>,
    val outgoingEip20Events: List<Pair<String, CoinValue>>
) : EvmTransactionRecord(fullTransaction, baseCoin) {

    val value: CoinValue = CoinValue(baseCoin, value)

}
