package io.horizontalsystems.bankwallet.entities.transactionrecords.evm

import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.marketkit.models.PlatformCoin
import java.math.BigDecimal

class ContractCallTransactionRecord(
    fullTransaction: FullTransaction,
    baseCoin: PlatformCoin,
    val contractAddress: String,
    val method: String?,
    value: BigDecimal,
    val incomingInternalETHs: List<AddressTransactionValue>,
    val incomingEip20Events: List<AddressTransactionValue>,
    val outgoingEip20Events: List<AddressTransactionValue>,
    source: TransactionSource
) : EvmTransactionRecord(fullTransaction, baseCoin, source) {

    val value = TransactionValue.CoinValue(baseCoin, value)

}

data class AddressTransactionValue(val address: String, val value: TransactionValue)
