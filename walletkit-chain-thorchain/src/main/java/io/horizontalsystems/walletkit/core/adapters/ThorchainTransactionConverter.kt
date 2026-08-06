package io.horizontalsystems.walletkit.core.adapters

import io.horizontalsystems.walletkit.core.ICoinManager
import io.horizontalsystems.walletkit.entities.TransactionValue
import io.horizontalsystems.walletkit.entities.transactionrecords.thorchain.ThorchainIncomingTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.thorchain.ThorchainOutgoingTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.thorchain.ThorchainTransactionRecord
import io.horizontalsystems.walletkit.core.managers.thorchainAssetType
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.thorchainkit.models.CoinTransfer
import io.horizontalsystems.thorchainkit.models.Transaction
import io.horizontalsystems.thorchainkit.network.Network

class ThorchainTransactionConverter(
    private val coinManager: ICoinManager,
    private val source: TransactionSource,
    private val userAddress: String,
    private val baseToken: Token,
    private val blockchainType: BlockchainType,
    private val network: Network,
) {

    // Midgard action semantics: `incoming` transfers enter the action (spent by their
    // address), `outgoing` transfers leave it (received by their address). One action
    // can carry the user on BOTH sides with different assets — a swap spends RUNE and
    // delivers TCY in the same action — so every user-side transfer becomes its own
    // record; a single-record conversion would drop the received asset entirely.
    fun convert(transaction: Transaction): List<ThorchainTransactionRecord> {
        val failed = transaction.status == "failed"
        val blockHeight = if (transaction.isPending) null else transaction.blockHeight.toInt()

        val userSpends = transaction.incoming.filter { it.address == userAddress }
        val userReceives = transaction.outgoing.filter { it.address == userAddress }
        val spentAssets = userSpends.map { it.asset }.toSet()

        val records = mutableListOf<ThorchainTransactionRecord>()

        userSpends.forEach { transfer ->
            // counterparty must be matched by asset: in a swap the first outgoing
            // entry is the user's own receive of the other asset, not the recipient
            val to = transaction.outgoing.firstOrNull { it.asset == transfer.asset }?.address
            records += ThorchainOutgoingTransactionRecord(
                uid = "${transaction.hash}-${transfer.asset.lowercase()}",
                transactionHash = transaction.hash,
                blockHeight = blockHeight,
                timestamp = transaction.timestamp,
                fee = null,
                failed = failed,
                value = transactionValue(transfer, negative = true),
                to = to,
                sentToSelf = userReceives.any { it.asset == transfer.asset },
                memo = transaction.memo,
                source = source
            )
        }

        userReceives.forEach { transfer ->
            // a same-asset receive in an action the user also spent from is change or a
            // self-transfer (or a refund) — already represented by the outgoing record,
            // and a second record would collide with its uid
            if (transfer.asset in spentAssets) return@forEach

            records += ThorchainIncomingTransactionRecord(
                uid = "${transaction.hash}-${transfer.asset.lowercase()}",
                transactionHash = transaction.hash,
                blockHeight = blockHeight,
                timestamp = transaction.timestamp,
                fee = null,
                failed = failed,
                value = transactionValue(transfer),
                from = transaction.incoming.firstOrNull { it.asset == transfer.asset }?.address,
                memo = transaction.memo,
                source = source
            )
        }

        return records
    }

    fun token(midgardAsset: String): Token? {
        val denom = denom(midgardAsset) ?: return null

        return if (denom == network.nativeDenom) {
            baseToken
        } else {
            coinManager.getToken(TokenQuery(blockchainType, thorchainAssetType(blockchainType, denom)))
        }
    }

    private fun transactionValue(transfer: CoinTransfer, negative: Boolean = false): TransactionValue {
        val token = token(transfer.asset)
        var amount = transfer.amount.toBigDecimal().movePointLeft(network.decimals)
        if (negative) {
            amount = amount.negate()
        }

        return if (token != null) {
            TransactionValue.CoinValue(token, amount)
        } else {
            val ticker = try {
                network.assetResolver.assetFor(transfer.asset).ticker
            } catch (_: Throwable) {
                transfer.asset.substringAfterLast('.').substringBefore('-').uppercase()
            }
            TransactionValue.TokenValue(
                tokenName = ticker,
                tokenCode = ticker,
                tokenDecimals = network.decimals,
                value = amount
            )
        }
    }

    // Midgard reports swap assets in full notation ("THOR.RUNE") but native token
    // sends in bank-denom notation ("TCY") — assetFor normalizes both; a plain
    // Asset.fromString throws on the delimiter-less denom form and TCY transfers
    // would silently degrade to a coin-less value that every token filter drops
    private fun denom(midgardAsset: String): String? = try {
        network.assetResolver.denomFor(network.assetResolver.assetFor(midgardAsset))
    } catch (_: Throwable) {
        null
    }
}
