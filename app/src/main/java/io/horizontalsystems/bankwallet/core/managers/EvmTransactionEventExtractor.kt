package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.TransferEvent
import io.horizontalsystems.erc20kit.decorations.OutgoingEip20Decoration
import io.horizontalsystems.erc20kit.events.TransferEventInstance
import io.horizontalsystems.ethereumkit.decorations.IncomingDecoration
import io.horizontalsystems.ethereumkit.decorations.OutgoingDecoration
import io.horizontalsystems.ethereumkit.decorations.UnknownTransactionDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.marketkit.models.Token

/**
 * Extracts transfer events from EVM transactions for spam detection.
 */
class EvmTransactionEventExtractor {

    /**
     * Extract outgoing transaction info from EVM FullTransaction.
     */
    fun extractOutgoingInfo(
        fullTx: FullTransaction,
        userAddress: Address
    ): PoisoningScorer.OutgoingTxInfo? {
        val tx = fullTx.transaction
        return when (val decoration = fullTx.decoration) {
            is OutgoingDecoration -> {
                PoisoningScorer.OutgoingTxInfo(decoration.to.eip55, tx.timestamp, tx.blockNumber?.toInt())
            }
            is OutgoingEip20Decoration -> {
                PoisoningScorer.OutgoingTxInfo(decoration.to.eip55, tx.timestamp, tx.blockNumber?.toInt())
            }
            is UnknownTransactionDecoration -> {
                if (tx.from == userAddress) {
                    decoration.eventInstances
                        .mapNotNull { it as? TransferEventInstance }
                        .firstOrNull { it.from == userAddress }?.let { transfer ->
                            PoisoningScorer.OutgoingTxInfo(transfer.to.eip55, tx.timestamp, tx.blockNumber?.toInt())
                        }
                } else null
            }
            else -> null
        }
    }

    /**
     * Extract incoming events from EVM FullTransaction.
     */
    fun extractIncomingEvents(
        fullTx: FullTransaction,
        userAddress: Address,
        baseToken: Token
    ): List<TransferEvent> {
        val tx = fullTx.transaction
        return when (val decoration = fullTx.decoration) {
            is IncomingDecoration -> {
                val value = decoration.value.toBigDecimal(baseToken.decimals)
                listOf(TransferEvent(decoration.from.eip55, TransactionValue.CoinValue(baseToken, value)))
            }
            is UnknownTransactionDecoration -> {
                decoration.eventInstances
                    .mapNotNull { it as? TransferEventInstance }
                    .filter { it.to == userAddress || it.from == userAddress }
                    .map { transfer ->
                        val tokenValue = transfer.tokenInfo?.let { info ->
                            TransactionValue.TokenValue(
                                tokenName = info.tokenName,
                                tokenCode = info.tokenSymbol,
                                tokenDecimals = info.tokenDecimal,
                                value = transfer.value.toBigDecimal(info.tokenDecimal),
                            )
                        } ?: TransactionValue.RawValue(transfer.value)
                        if (transfer.from == userAddress) {
                            TransferEvent(transfer.to.eip55, tokenValue)
                        } else {
                            TransferEvent(transfer.from.eip55, tokenValue)
                        }
                    }
            }
            else -> emptyList()
        }
    }
}