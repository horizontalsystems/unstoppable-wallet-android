package cash.p.terminal.core.adapters

import cash.p.terminal.core.ICoinManager
import cash.p.terminal.core.managers.EvmLabelManager
import cash.p.terminal.core.managers.TonKitWrapper
import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.tonkit.models.Event
import java.math.BigDecimal

class TonTransactionConverter(
    private val coinManager: ICoinManager,
    private val tonKitWrapper: TonKitWrapper,
    private val source: TransactionSource,
    private val baseToken: Token,
    private val evmLabelManager: EvmLabelManager
) {
    fun createTransactionRecord(event: Event): TransactionRecord {
//        val amount = transaction.amount?.toBigDecimal()?.movePointLeft(decimals)

//        val value = if (transaction.type == TransactionType.Outgoing) {
//            amount?.negate()
//        } else {
//            amount
//        } ?: BigDecimal.ZERO

//        val fee = transaction.fee?.toBigDecimal()?.movePointLeft(decimals)

//        val type = when (transaction.type) {
//            TransactionType.Incoming -> TonTransactionRecord.Type.Incoming
//            TransactionType.Outgoing -> TonTransactionRecord.Type.Outgoing
//            TransactionType.Unknown -> TonTransactionRecord.Type.Unknown
//        }
        val type = TonTransactionRecord.Type.Incoming

//        event.actions.forEach { action: Action ->
//
//            when (action.type) {
//                Action.Type.TonTransfer -> {
//                    action.tonTransfer?.let { tonTransfer ->
//
//                    }
//                }
//                Action.Type.JettonTransfer -> TODO()
//                Action.Type.JettonBurn -> TODO()
//                Action.Type.JettonMint -> TODO()
//                Action.Type.ContractDeploy -> TODO()
//                Action.Type.JettonSwap -> TODO()
//                Action.Type.SmartContract -> TODO()
//                Action.Type.Unknown -> TODO()
//            }
//        }

        return TonTransactionRecord(
            uid = event.id,
            transactionHash = event.id,
            logicalTime = event.lt,
            blockHeight = null,
            confirmationsThreshold = null,
            timestamp = event.timestamp,
            source = source,
            mainValue = TransactionValue.CoinValue(baseToken, BigDecimal.ZERO),
//            fee = fee?.let { TransactionValue.CoinValue(wallet.token, it) },
            fee = null,
//            memo = transaction.memo,
            memo = null,
            type = type,
//            transfers = transaction.transfers.map { createTransferRecord(it) }
            transfers = listOf()
        )
    }


    //    private fun createTransferRecord(transfer: TonTransfer): TonTransactionTransfer {
//        val amount = transfer.amount.toBigDecimal().movePointLeft(decimals)
//        return TonTransactionTransfer(
//            src = transfer.src.getNonBounceable(),
//            dest = transfer.dest.getNonBounceable(),
//            amount = TransactionValue.CoinValue(wallet.token, amount),
//        )
//    }



}