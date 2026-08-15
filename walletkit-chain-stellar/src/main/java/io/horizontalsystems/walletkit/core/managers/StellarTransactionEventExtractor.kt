package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.entities.TransactionValue
import io.horizontalsystems.walletkit.entities.transactionrecords.evm.TransferEvent
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.stellarkit.room.Operation
import io.horizontalsystems.stellarkit.room.StellarAsset

/**
 * Extracts transfer events from Stellar operations for spam detection.
 */
class StellarTransactionEventExtractor {

    /**
     * Extract counterparty context from a Stellar Operation for address-poisoning correlation.
     *
     * The counterparty is taken direction-agnostically: the recipient of a send OR the sender of a
     * receive. Correlating against incoming senders (not just addresses the user sent to) is what
     * catches the common poisoning that mimics whoever just paid the user, and it is the only
     * context a watch/receive-only wallet has at all.
     */
    fun extractCounterpartyInfo(
        operation: Operation,
        selfAddress: String
    ): PoisoningScorer.OutgoingTxInfo? {
        // Check payment operations
        operation.payment?.let { payment ->
            when (selfAddress) {
                payment.from -> return PoisoningScorer.OutgoingTxInfo(payment.to, operation.timestamp, null)
                payment.to -> return PoisoningScorer.OutgoingTxInfo(payment.from, operation.timestamp, null)
            }
        }

        // Check account creation operations
        operation.accountCreated?.let { accountCreated ->
            when (selfAddress) {
                accountCreated.funder -> return PoisoningScorer.OutgoingTxInfo(accountCreated.account, operation.timestamp, null)
                accountCreated.account -> return PoisoningScorer.OutgoingTxInfo(accountCreated.funder, operation.timestamp, null)
            }
        }

        return null
    }

    /**
     * Extract incoming events from Stellar Operation.
     */
    fun extractIncomingEvents(
        operation: Operation,
        selfAddress: String,
        baseToken: Token
    ): List<TransferEvent> {
        // Check payment operations
        operation.payment?.let { payment ->
            if (payment.to == selfAddress && payment.from != selfAddress) {
                val token = getToken(payment.asset)
                val transactionValue = if (token != null) {
                    TransactionValue.CoinValue(token, payment.amount)
                } else {
                    TransactionValue.RawValue(payment.amount.unscaledValue())
                }
                return listOf(TransferEvent(payment.from, transactionValue))
            }
        }

        // Check account creation operations
        operation.accountCreated?.let { accountCreated ->
            if (accountCreated.account == selfAddress && accountCreated.funder != selfAddress) {
                val transactionValue = TransactionValue.CoinValue(baseToken, accountCreated.startingBalance)
                return listOf(TransferEvent(accountCreated.funder, transactionValue))
            }
        }

        return emptyList()
    }

    private fun getToken(asset: StellarAsset): Token? {
        val tokenType = when (asset) {
            StellarAsset.Native -> TokenType.Native
            is StellarAsset.Asset -> TokenType.Asset(asset.code, asset.issuer)
        }
        return App.coinManager.getToken(TokenQuery(BlockchainType.Stellar, tokenType))
    }
}