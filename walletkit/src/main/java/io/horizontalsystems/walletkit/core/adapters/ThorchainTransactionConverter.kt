package io.horizontalsystems.walletkit.core.adapters

import io.horizontalsystems.walletkit.core.ICoinManager
import io.horizontalsystems.walletkit.entities.TransactionValue
import io.horizontalsystems.walletkit.entities.transactionrecords.thorchain.ThorchainIncomingTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.thorchain.ThorchainOutgoingTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.thorchain.ThorchainTransactionRecord
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.thorchainkit.models.Asset
import io.horizontalsystems.thorchainkit.models.CoinTransfer
import io.horizontalsystems.thorchainkit.models.Denom
import io.horizontalsystems.thorchainkit.models.Transaction

class ThorchainTransactionConverter(
    private val coinManager: ICoinManager,
    private val source: TransactionSource,
    private val userAddress: String,
    private val baseToken: Token,
) {

    fun convert(transaction: Transaction): ThorchainTransactionRecord? {
        // Midgard action semantics: `incoming` transfers enter the action (spent by their
        // address), `outgoing` transfers leave it (received by their address)
        val sentByUser = transaction.incoming.any { it.address == userAddress }

        val transfer = if (sentByUser) {
            transaction.incoming.firstOrNull { it.address == userAddress }
        } else {
            transaction.outgoing.firstOrNull { it.address == userAddress }
        } ?: return null

        val failed = transaction.status == "failed"
        val blockHeight = if (transaction.isPending) null else transaction.blockHeight.toInt()
        val value = transactionValue(transfer)
        val uid = "${transaction.hash}-${transfer.asset.lowercase()}"

        return if (sentByUser) {
            val to = transaction.outgoing.firstOrNull()?.address
            ThorchainOutgoingTransactionRecord(
                uid = uid,
                transactionHash = transaction.hash,
                blockHeight = blockHeight,
                timestamp = transaction.timestamp,
                fee = null,
                failed = failed,
                value = value,
                to = to,
                sentToSelf = to == userAddress,
                memo = transaction.memo,
                source = source
            )
        } else {
            ThorchainIncomingTransactionRecord(
                uid = uid,
                transactionHash = transaction.hash,
                blockHeight = blockHeight,
                timestamp = transaction.timestamp,
                fee = null,
                failed = failed,
                value = value,
                from = transaction.incoming.firstOrNull()?.address,
                memo = transaction.memo,
                source = source
            )
        }
    }

    fun token(midgardAsset: String): Token? {
        val denom = denom(midgardAsset) ?: return null

        return if (denom == Denom.RUNE) {
            baseToken
        } else {
            coinManager.getToken(TokenQuery(BlockchainType.Thorchain, TokenType.ThorchainAsset(denom)))
        }
    }

    private fun transactionValue(transfer: CoinTransfer): TransactionValue {
        val token = token(transfer.asset)
        val amount = transfer.amount.toBigDecimal().movePointLeft(Denom.DECIMALS)

        return if (token != null) {
            TransactionValue.CoinValue(token, amount)
        } else {
            val ticker = transfer.asset.substringAfterLast('.').substringBefore('-').uppercase()
            TransactionValue.TokenValue(
                tokenName = ticker,
                tokenCode = ticker,
                tokenDecimals = Denom.DECIMALS,
                value = amount
            )
        }
    }

    private fun denom(midgardAsset: String): String? = try {
        Denom.denomFor(Asset.fromString(midgardAsset))
    } catch (_: Throwable) {
        null
    }
}
