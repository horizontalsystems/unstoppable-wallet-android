package io.horizontalsystems.bankwallet.entities.transactionrecords.evm

import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.marketkit.models.PlatformCoin
import java.math.BigDecimal

class UnknownSwapTransactionRecord(
    fullTransaction: FullTransaction,
    baseCoin: PlatformCoin,
    value: BigDecimal,
    val exchangeAddress: String,
    val incomingInternalETHs: List<AddressTransactionValue>,
    val incomingEip20Events: List<AddressTransactionValue>,
    val outgoingEip20Events: List<AddressTransactionValue>,
    source: TransactionSource
) : EvmTransactionRecord(fullTransaction, baseCoin, source) {
    val value = TransactionValue.CoinValue(baseCoin, value)
}
