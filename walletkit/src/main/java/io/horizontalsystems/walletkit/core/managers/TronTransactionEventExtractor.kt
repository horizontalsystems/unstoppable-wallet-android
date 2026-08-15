package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.walletkit.entities.TransactionValue
import io.horizontalsystems.walletkit.entities.transactionrecords.evm.TransferEvent
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
     * Extract counterparty context from a Tron FullTransaction for address-poisoning correlation.
     *
     * The counterparty is taken direction-agnostically: the recipient of a send OR the sender of a
     * receive. Correlating against incoming senders (not just addresses the user sent to) is what
     * catches the common poisoning that mimics whoever just paid the user, and it is the only
     * context a watch/receive-only wallet has at all.
     */
    fun extractCounterpartyInfo(
        fullTx: FullTransaction,
        userAddress: Address
    ): PoisoningScorer.OutgoingTxInfo? {
        val tx = fullTx.transaction
        val timestamp = tx.timestamp / 1000
        val blockHeight = tx.blockNumber?.toInt()

        return when (val decoration = fullTx.decoration) {
            is NativeTransactionDecoration -> {
                val contract = decoration.contract as? TransferContract ?: return null
                when (userAddress) {
                    contract.ownerAddress -> PoisoningScorer.OutgoingTxInfo(contract.toAddress.base58, timestamp, blockHeight)
                    contract.toAddress -> PoisoningScorer.OutgoingTxInfo(contract.ownerAddress.base58, timestamp, blockHeight)
                    else -> null
                }
            }
            is OutgoingTrc20Decoration -> {
                PoisoningScorer.OutgoingTxInfo(decoration.to.base58, timestamp, blockHeight)
            }
            is UnknownTransactionDecoration -> {
                val transfers = decoration.events.mapNotNull { it as? Trc20TransferEvent }
                val sentLeg = transfers.firstOrNull { it.from == userAddress }
                val receivedLeg = transfers.firstOrNull { it.to == userAddress }
                when {
                    sentLeg != null -> PoisoningScorer.OutgoingTxInfo(sentLeg.to.base58, timestamp, blockHeight)
                    receivedLeg != null -> PoisoningScorer.OutgoingTxInfo(receivedLeg.from.base58, timestamp, blockHeight)
                    else -> null
                }
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