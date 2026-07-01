package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.TransferEvent
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
     * Extract outgoing transaction info from Stellar Operation.
     */
    fun extractOutgoingInfo(
        operation: Operation,
        selfAddress: String
    ): PoisoningScorer.OutgoingTxInfo? {
        // Check payment operations
        operation.payment?.let { payment ->
            if (payment.from == selfAddress) {
                return PoisoningScorer.OutgoingTxInfo(payment.to, operation.timestamp, null)
            }
        }

        // Check account creation operations
        operation.accountCreated?.let { accountCreated ->
            if (accountCreated.funder == selfAddress) {
                return PoisoningScorer.OutgoingTxInfo(accountCreated.account, operation.timestamp, null)
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