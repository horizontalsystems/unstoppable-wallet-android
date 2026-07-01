package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.TransferEvent
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.tronkit.decoration.NativeTransactionDecoration
import io.horizontalsystems.tronkit.decoration.UnknownTransactionDecoration
import io.horizontalsystems.tronkit.decoration.trc20.OutgoingTrc20Decoration
import io.horizontalsystems.tronkit.decoration.trc20.Trc20TransferEvent
import io.horizontalsystems.tronkit.models.Address
import io.horizontalsystems.tronkit.models.FullTransaction
import io.horizontalsystems.tronkit.models.TransferContract

/**
 * Extracts transfer events from Tron transactions for spam detection.
 */
class TronTransactionEventExtractor {

    /**
     * Extract outgoing transaction info from Tron FullTransaction.
     */
    fun extractOutgoingInfo(
        fullTx: FullTransaction,
        userAddress: Address
    ): PoisoningScorer.OutgoingTxInfo? {
        val tx = fullTx.transaction
        val timestamp = tx.timestamp / 1000
        val blockHeight = tx.blockNumber?.toInt()

        return when (val decoration = fullTx.decoration) {
            is NativeTransactionDecoration -> {
                val contract = decoration.contract as? TransferContract ?: return null
                if (contract.ownerAddress == userAddress) {
                    PoisoningScorer.OutgoingTxInfo(contract.toAddress.base58, timestamp, blockHeight)
                } else null
            }
            is OutgoingTrc20Decoration -> {
                PoisoningScorer.OutgoingTxInfo(decoration.to.base58, timestamp, blockHeight)
            }
            is UnknownTransactionDecoration -> {
                if (decoration.fromAddress == userAddress) {
                    decoration.events
                        .mapNotNull { it as? Trc20TransferEvent }
                        .firstOrNull { it.from == userAddress }?.let { transfer ->
                            PoisoningScorer.OutgoingTxInfo(transfer.to.base58, timestamp, blockHeight)
                        }
                } else null
            }
            else -> null
        }
    }

    /**
     * Extract incoming events from Tron FullTransaction.
     */
    fun extractIncomingEvents(
        fullTx: FullTransaction,
        userAddress: Address,
        baseToken: Token
    ): List<TransferEvent> {
        return when (val decoration = fullTx.decoration) {
            is NativeTransactionDecoration -> {
                val contract = decoration.contract as? TransferContract
                if (contract != null && contract.ownerAddress != userAddress && contract.toAddress == userAddress) {
                    val value = contract.amount.toBigDecimal().movePointLeft(baseToken.decimals)
                    listOf(TransferEvent(contract.ownerAddress.base58, TransactionValue.CoinValue(baseToken, value)))
                } else emptyList()
            }
            is UnknownTransactionDecoration -> {
                if (decoration.fromAddress != userAddress && decoration.toAddress != userAddress) {
                    decoration.events
                        .mapNotNull { it as? Trc20TransferEvent }
                        .filter { it.to == userAddress && it.from != userAddress }
                        .map { transfer ->
                            val tokenValue = transfer.tokenInfo?.let { info ->
                                TransactionValue.TokenValue(
                                    tokenName = info.tokenName,
                                    tokenCode = info.tokenSymbol,
                                    tokenDecimals = info.tokenDecimal,
                                    value = transfer.value.toBigDecimal().movePointLeft(info.tokenDecimal),
                                )
                            } ?: TransactionValue.RawValue(transfer.value)
                            TransferEvent(transfer.from.base58, tokenValue)
                        }
                } else emptyList()
            }
            else -> emptyList()
        }
    }
}