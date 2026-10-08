package io.horizontalsystems.walletkit.core.factories

import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.nearkit.NearKit
import io.horizontalsystems.nearkit.crypto.Base58
import io.horizontalsystems.nearkit.models.NearAmount
import io.horizontalsystems.nearkit.models.NearTransfer
import io.horizontalsystems.nearkit.models.Transaction
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.ICoinManager
import io.horizontalsystems.walletkit.core.adapters.NearTransactionInfo
import io.horizontalsystems.walletkit.core.adapters.NearTransactionRecord
import io.horizontalsystems.walletkit.core.adapters.NearTransactionRecord.Type
import io.horizontalsystems.walletkit.core.tokenIconPlaceholder
import io.horizontalsystems.walletkit.entities.TransactionValue
import io.horizontalsystems.walletkit.entities.transactionrecords.evm.TransferEvent
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource
import kotlinx.coroutines.CancellationException
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Turns a nearkit transaction into a wallet record from the account's point of view: the net
 * change of each asset across the whole transaction. One asset moving is a send or a receive;
 * several (a swap, a wrap) are one contract call listing each, so every wallet the transaction
 * appears in sees the same row.
 */
class NearTransactionConverter(
    private val source: TransactionSource,
    private val kit: NearKit,
    private val coinManager: ICoinManager,
    private val baseToken: Token,
) {
    private val selfAccount = kit.accountId

    suspend fun convert(tx: Transaction): NearTransactionRecord {
        val type = type(tx)

        // what the account signed itself, such as a swap's proceeds, is not an attempt to mislead it
        val events = if (tx.isSigner(selfAccount)) emptyList() else NearTransactionRecord.eventsForPhishingCheck(type)
        val hashBytes = try {
            Base58.decode(tx.hash)
        } catch (e: IllegalArgumentException) {
            null
        }
        val spam = if (hashBytes != null) {
            App.spamManager.isSpam(hashBytes, events, source, tx.timestamp, tx.blockHeight?.toInt(), null)
        } else {
            false
        }

        val info = NearTransactionInfo(
            hash = tx.hash,
            blockHeight = tx.blockHeight,
            timestamp = tx.timestamp,
            pending = tx.isPending,
            failed = tx.isFailed,
            expired = tx.failure == EXPIRED,
        )
        val fee = tx.fee?.takeIf { tx.isSigner(selfAccount) }?.let { nearValue(it, negate = false) }
        return NearTransactionRecord(source, info, type, fee, spam)
    }

    private suspend fun type(tx: Transaction): Type {
        val signed = tx.isSigner(selfAccount)
        val onlyTransfers = tx.actions.isNotEmpty() && tx.actions.all { it.type == "Transfer" }
        val method = tx.actions.firstOrNull { it.methodName != null }?.methodName ?: tx.actions.firstOrNull()?.type

        // key null is NEAR
        val moves = netMoves(tx)

        if (moves.isEmpty()) {
            tx.ftTransfers.firstOrNull { it.from == selfAccount && it.to == selfAccount }?.let { transfer ->
                return Type.Send(ftValue(transfer.contractId, transfer.amount, negate = true), selfAccount, sentToSelf = true, memo = transfer.memo)
            }
            // a failed send moved nothing; it shows what was sent, marked failed
            if (signed && onlyTransfers) {
                val sent = tx.actions.fold(BigInteger.ZERO) { acc, action -> acc + (action.deposit ?: BigInteger.ZERO) }
                return Type.Send(nearValue(sent, negate = true), tx.receiverId, sentToSelf = tx.receiverId == selfAccount, memo = null)
            }
            return Type.ContractCall(tx.receiverId, method, incoming = emptyList(), outgoing = emptyList())
        }

        val incoming = mutableListOf<TransferEvent>()
        val outgoing = mutableListOf<TransferEvent>()
        for ((contractId, change) in moves) {
            val gained = change.signum() > 0
            val value = if (contractId == null) {
                nearValue(change.abs(), negate = !gained)
            } else {
                ftValue(contractId, change.abs(), negate = !gained)
            }
            val event = TransferEvent(counterparty(tx, contractId, gained), value)
            if (gained) incoming += event else outgoing += event
        }

        val (contractId, change) = moves.entries.singleOrNull()?.toPair()
            ?: return Type.ContractCall(tx.receiverId, method, incoming, outgoing)

        // NEAR paid into a contract call (a deposit, a fee for a service) is not a send to someone
        if (contractId == null && signed && !onlyTransfers) {
            return Type.ContractCall(tx.receiverId, method, incoming, outgoing)
        }

        val event = (incoming + outgoing).single()
        val address = event.address ?: tx.receiverId
        return if (change.signum() > 0) {
            Type.Receive(event.value, address, memo(tx, contractId, gained = true))
        } else {
            Type.Send(event.value, address, sentToSelf = false, memo = memo(tx, contractId, gained = false))
        }
    }

    /** Net change of each asset the transaction moved for the account, NEAR first (key null). */
    private fun netMoves(tx: Transaction): Map<String?, BigInteger> {
        val moves = linkedMapOf<String?, BigInteger>()
        tx.nearMoved(selfAccount).takeIf { it.signum() != 0 }?.let { moves[null] = it }
        for (transfer in tx.ftTransfers) {
            val change = when (selfAccount) {
                transfer.to -> if (transfer.from == selfAccount) BigInteger.ZERO else transfer.amount
                transfer.from -> transfer.amount.negate()
                else -> BigInteger.ZERO
            }
            moves[transfer.contractId] = (moves[transfer.contractId] ?: BigInteger.ZERO) + change
        }
        return moves.filterValues { it.signum() != 0 }
    }

    /** The other side of the account's largest movement of one asset in the given direction. */
    private fun counterparty(tx: Transaction, contractId: String?, gained: Boolean): String {
        if (contractId == null) {
            val transfers = tx.nearTransfers.filter { it.success && it.kind == NearTransfer.Kind.Transfer }
            val other = if (gained) {
                transfers.filter { it.to == selfAccount && it.from != selfAccount }.maxByOrNull { it.amount }?.from
            } else {
                transfers.filter { it.from == selfAccount && it.to != selfAccount }.maxByOrNull { it.amount }?.to
            }
            return other ?: if (gained) tx.signerId else tx.receiverId
        }
        val transfers = tx.ftTransfers.filter { it.contractId == contractId }
        val other = if (gained) {
            transfers.filter { it.to == selfAccount }.maxByOrNull { it.amount }?.from
        } else {
            transfers.filter { it.from == selfAccount }.maxByOrNull { it.amount }?.to
        }
        // null for a mint or a burn: the contract is the other side
        return other ?: contractId
    }

    private fun memo(tx: Transaction, contractId: String?, gained: Boolean): String? {
        contractId ?: return null
        return tx.ftTransfers
            .filter { it.contractId == contractId && (if (gained) it.to == selfAccount else it.from == selfAccount) }
            .maxByOrNull { it.amount }
            ?.memo
    }

    private fun nearValue(yocto: BigInteger, negate: Boolean): TransactionValue {
        val value = NearAmount.toNear(yocto)
        return TransactionValue.CoinValue(baseToken, if (negate) value.negate() else value)
    }

    private suspend fun ftValue(contractId: String, amount: BigInteger, negate: Boolean): TransactionValue {
        val token = coinManager.getToken(TokenQuery(BlockchainType.Near, TokenType.Nep141(contractId)))
        if (token != null) {
            val value = BigDecimal(amount).movePointLeft(token.decimals)
            return TransactionValue.CoinValue(token, if (negate) value.negate() else value)
        }

        // Not a known token: read its metadata from the contract (cached by the kit) so the
        // amount is shown with the right scale. Unreadable metadata shows the raw amount.
        val metadata = try {
            kit.ftMetadata(contractId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            null
        }
        val decimals = metadata?.decimals ?: 0
        val value = BigDecimal(amount).movePointLeft(decimals)
        return TransactionValue.TokenValue(
            tokenName = metadata?.name?.takeIf { it.isNotBlank() } ?: contractId,
            tokenCode = metadata?.symbol ?: contractId,
            tokenDecimals = decimals,
            value = if (negate) value.negate() else value,
            coinIconPlaceholder = BlockchainType.Near.tokenIconPlaceholder,
        )
    }

    private companion object {
        // the kit's failure value for a transaction whose block hash expired before inclusion
        const val EXPIRED = "expired"
    }
}
