package io.horizontalsystems.walletkit.core.factories

import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.xrpkit.XrpKit
import io.horizontalsystems.xrpkit.models.Amount
import io.horizontalsystems.xrpkit.models.Transaction
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.ICoinManager
import io.horizontalsystems.walletkit.core.adapters.XrpTransactionInfo
import io.horizontalsystems.walletkit.core.adapters.XrpTransactionRecord
import io.horizontalsystems.walletkit.core.adapters.XrpTransactionRecord.Type
import io.horizontalsystems.walletkit.core.hexStringToByteArrayOrNull
import io.horizontalsystems.walletkit.core.tokenIconPlaceholder
import io.horizontalsystems.walletkit.entities.TransactionValue
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource
import java.math.BigDecimal

class XrpTransactionConverter(
    private val source: TransactionSource,
    private val selfAddress: String,
    private val coinManager: ICoinManager,
    private val baseToken: Token,
) {
    suspend fun convert(tx: Transaction): XrpTransactionRecord {
        val type: Type = when (tx.type) {
            "Payment" -> paymentType(tx)
            "TrustSet" -> trustSetType(tx)
            else -> Type.Unsupported(tx.type)
        }

        val events = XrpTransactionRecord.eventsForPhishingCheck(type)
        val txHash = tx.hash.hexStringToByteArrayOrNull()
        val spam = if (txHash != null) {
            App.spamManager.isSpam(txHash, events, source, tx.timestamp, null, null)
        } else {
            false
        }

        val info = XrpTransactionInfo(
            hash = tx.hash,
            ledgerIndex = tx.ledgerIndex,
            timestamp = tx.timestamp,
            validated = tx.validated,
            failed = tx.failed,
            fee = Amount.Xrp.fromDrops(tx.feeDrops).xrp,
            memo = tx.memo,
            destinationTag = tx.destinationTag,
        )
        return XrpTransactionRecord(baseToken, source, info, type, spam)
    }

    private fun paymentType(tx: Transaction): Type {
        val outgoing = tx.account == selfAddress
        val incoming = tx.destination == selfAddress
        // what the destination actually received; falls back to the requested amount while pending
        val amount = tx.deliveredAmount ?: tx.amount ?: return Type.Unsupported(tx.type)

        return when {
            outgoing -> Type.Send(
                value = transactionValue(amount, negate = true),
                to = tx.destination ?: "",
                sentToSelf = incoming,
                comment = tx.memo,
            )

            incoming -> Type.Receive(
                value = transactionValue(amount, negate = false),
                from = tx.account,
                comment = tx.memo,
            )

            else -> Type.Unsupported(tx.type)
        }
    }

    private fun trustSetType(tx: Transaction): Type {
        val limit = tx.limitAmount as? Amount.Issued ?: return Type.Unsupported(tx.type)
        return Type.TrustSet(limit.issuer, transactionValue(limit, negate = false))
    }

    private fun transactionValue(amount: Amount, negate: Boolean): TransactionValue {
        val value = if (negate) amount.decimalValue.negate() else amount.decimalValue
        val token = getToken(amount)
        if (token != null) {
            return TransactionValue.CoinValue(token, value)
        }
        val code = when (amount) {
            is Amount.Xrp -> "XRP"
            is Amount.Issued -> XrpKit.displayCurrencyCode(amount.currency)
        }
        return TransactionValue.TokenValue(
            tokenName = code,
            tokenCode = code,
            tokenDecimals = if (amount is Amount.Xrp) Amount.Xrp.DECIMALS else ISSUED_TOKEN_DECIMALS,
            value = value,
            coinIconPlaceholder = BlockchainType.Xrp.tokenIconPlaceholder,
        )
    }

    private fun getToken(amount: Amount): Token? {
        val tokenType = when (amount) {
            is Amount.Xrp -> TokenType.Native
            is Amount.Issued -> TokenType.XrpAsset(amount.currency, amount.issuer)
        }
        return coinManager.getToken(TokenQuery(BlockchainType.Xrp, tokenType))
    }

    companion object {
        /** Issued currencies have no fixed scale on the ledger; this is a display precision. */
        const val ISSUED_TOKEN_DECIMALS = 8
    }
}

internal fun BigDecimal.negateIf(condition: Boolean): BigDecimal = if (condition) negate() else this
