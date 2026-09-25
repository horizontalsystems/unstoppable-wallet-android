package io.horizontalsystems.walletkit.core.factories

import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.nearkit.NearKit
import io.horizontalsystems.nearkit.crypto.Base58
import io.horizontalsystems.nearkit.models.FtTransfer
import io.horizontalsystems.nearkit.models.NearAmount
import io.horizontalsystems.nearkit.models.Transaction
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.ICoinManager
import io.horizontalsystems.walletkit.core.adapters.NearTransactionInfo
import io.horizontalsystems.walletkit.core.adapters.NearTransactionRecord
import io.horizontalsystems.walletkit.core.adapters.NearTransactionRecord.Type
import io.horizontalsystems.walletkit.core.tokenIconPlaceholder
import io.horizontalsystems.walletkit.entities.TransactionValue
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource
import kotlinx.coroutines.CancellationException
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Turns a nearkit transaction into a wallet record from the account's point of view. A token
 * movement takes precedence over the NEAR attached to it (the 1 yoctoNEAR of `ft_transfer`).
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

        val events = NearTransactionRecord.eventsForPhishingCheck(type)
        val hashBytes = try {
            Base58.decode(tx.hash)
        } catch (e: IllegalArgumentException) {
            null
        }
        val spam = if (hashBytes != null) {
            App.spamManager.isSpam(hashBytes, events, source, tx.timestamp, null, null)
        } else {
            false
        }

        val info = NearTransactionInfo(
            hash = tx.hash,
            blockHeight = tx.blockHeight,
            timestamp = tx.timestamp,
            pending = tx.isPending,
            failed = tx.isFailed,
        )
        val fee = tx.fee?.takeIf { tx.isSigner(selfAccount) }?.let { nearValue(it, negate = false) }
        return NearTransactionRecord(source, info, type, fee, spam)
    }

    private suspend fun type(tx: Transaction): Type {
        tx.ftTransfers.firstOrNull { it.from == selfAccount || it.to == selfAccount }?.let { transfer ->
            return ftType(transfer)
        }

        val netNear = tx.nearNetChange(selfAccount)
        val onlyTransfers = tx.actions.isNotEmpty() && tx.actions.all { it.type == "Transfer" }

        if (tx.isSigner(selfAccount)) {
            if (onlyTransfers) {
                val sent = tx.actions.fold(BigInteger.ZERO) { acc, action -> acc + (action.deposit ?: BigInteger.ZERO) }
                return Type.Send(
                    value = nearValue(sent, negate = true),
                    to = tx.receiverId,
                    sentToSelf = tx.receiverId == selfAccount,
                    memo = null,
                )
            }
            return Type.ContractCall(
                contractId = tx.receiverId,
                method = tx.actions.firstOrNull { it.methodName != null }?.methodName ?: tx.actions.firstOrNull()?.type,
                value = netNear.takeIf { it.signum() != 0 }?.let { nearValue(it.abs(), negate = it.signum() < 0) },
            )
        }

        if (netNear.signum() > 0) {
            val from = tx.nearTransfers.firstOrNull { it.to == selfAccount && it.success }?.from ?: tx.signerId
            return Type.Receive(value = nearValue(netNear, negate = false), from = from, memo = null)
        }

        return Type.ContractCall(
            contractId = tx.receiverId,
            method = tx.actions.firstOrNull { it.methodName != null }?.methodName,
            value = null,
        )
    }

    private suspend fun ftType(transfer: FtTransfer): Type {
        val outgoing = transfer.from == selfAccount
        val value = ftValue(transfer, negate = outgoing)
        return if (outgoing) {
            Type.Send(
                value = value,
                to = transfer.to ?: transfer.contractId,
                sentToSelf = transfer.to == selfAccount,
                memo = transfer.memo,
            )
        } else {
            Type.Receive(value = value, from = transfer.from ?: transfer.contractId, memo = transfer.memo)
        }
    }

    private fun nearValue(yocto: BigInteger, negate: Boolean): TransactionValue {
        val value = NearAmount.toNear(yocto)
        return TransactionValue.CoinValue(baseToken, if (negate) value.negate() else value)
    }

    private suspend fun ftValue(transfer: FtTransfer, negate: Boolean): TransactionValue {
        val token = coinManager.getToken(TokenQuery(BlockchainType.Near, TokenType.Nep141(transfer.contractId)))
        if (token != null) {
            val value = BigDecimal(transfer.amount).movePointLeft(token.decimals)
            return TransactionValue.CoinValue(token, if (negate) value.negate() else value)
        }

        // Not a known token: read its metadata from the contract (cached by the kit) so the
        // amount is shown with the right scale. Unreadable metadata shows the raw amount.
        val metadata = try {
            kit.ftMetadata(transfer.contractId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            null
        }
        val decimals = metadata?.decimals ?: 0
        val value = BigDecimal(transfer.amount).movePointLeft(decimals)
        return TransactionValue.TokenValue(
            tokenName = metadata?.name?.takeIf { it.isNotBlank() } ?: transfer.contractId,
            tokenCode = metadata?.symbol ?: transfer.contractId,
            tokenDecimals = decimals,
            value = if (negate) value.negate() else value,
            coinIconPlaceholder = BlockchainType.Near.tokenIconPlaceholder,
        )
    }
}
