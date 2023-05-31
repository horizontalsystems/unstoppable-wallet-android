package io.horizontalsystems.bankwallet.entities.transactionrecords.evm

import io.horizontalsystems.bankwallet.core.adapters.BaseEvmAdapter
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.ethereumkit.models.Transaction
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

open class EvmTransactionRecord(
    transaction: Transaction,
    baseToken: Token,
    source: TransactionSource,
    val foreignTransaction: Boolean = false,
    spam: Boolean = false
) :
    TransactionRecord(
        uid = transaction.hashString,
        transactionHash = transaction.hashString,
        transactionIndex = transaction.transactionIndex ?: 0,
        blockHeight = transaction.blockNumber?.toInt(),
        confirmationsThreshold = BaseEvmAdapter.confirmationsThreshold,
        timestamp = transaction.timestamp,
        failed = transaction.isFailed,
        spam = spam,
        source = source
    ) {

    val fee: TransactionValue?

    init {
        val feeAmount: Long? = transaction.gasUsed ?: transaction.gasLimit
        val gasPrice = transaction.gasPrice

        fee = if (feeAmount != null && gasPrice != null) {
            val feeDecimal = feeAmount.toBigDecimal()
                .multiply(gasPrice.toBigDecimal())
                .movePointLeft(baseToken.decimals).stripTrailingZeros()

            TransactionValue.CoinValue(baseToken, feeDecimal)
        } else {
            null
        }
    }

    companion object {
        private fun sameType(value: TransactionValue, value2: TransactionValue): Boolean =
            when {
                value is TransactionValue.CoinValue && value2 is TransactionValue.CoinValue ->
                    value.token == value2.token

                value is TransactionValue.TokenValue && value2 is TransactionValue.TokenValue ->
                    value.tokenName == value2.tokenName && value.tokenCode == value2.tokenCode && value.tokenDecimals == value2.tokenDecimals

                value is TransactionValue.NftValue && value2 is TransactionValue.NftValue ->
                    value.nftUid == value2.nftUid

                else ->
                    false
            }

        fun combined(incomingEvents: List<TransferEvent>, outgoingEvents: List<TransferEvent>): Pair<List<TransactionValue>, List<TransactionValue>> {
            val values = (incomingEvents + outgoingEvents).map { it.value }
            val resultIncoming: MutableList<TransactionValue> = mutableListOf()
            val resultOutgoing: MutableList<TransactionValue> = mutableListOf()

            for (value in values) {
                if ((resultIncoming + resultOutgoing).any { sameType(value, it) }) {
                    continue
                }

                val sameTypeValues = values.filter { sameType(value, it) }
                val totalValue = sameTypeValues.map { it.decimalValue ?: BigDecimal.ZERO }.reduce { sum, t -> sum + t }
                val resultValue = when (value) {
                    is TransactionValue.CoinValue -> TransactionValue.CoinValue(value.token, totalValue)
                    is TransactionValue.TokenValue -> TransactionValue.TokenValue(
                        tokenName = value.tokenName,
                        tokenCode = value.tokenCode,
                        tokenDecimals = value.tokenDecimals,
                        value = totalValue,
                        coinIconPlaceholder = value.coinIconPlaceholder
                    )

                    is TransactionValue.RawValue -> value
                    is TransactionValue.NftValue -> value.copy(value = totalValue)
                }

                if (totalValue > BigDecimal.ZERO) {
                    resultIncoming.add(resultValue)
                } else {
                    resultOutgoing.add(resultValue)
                }
            }

            return Pair(resultIncoming, resultOutgoing)
        }
    }

}
