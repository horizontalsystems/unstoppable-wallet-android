package io.horizontalsystems.walletkit.core.adapters

import io.horizontalsystems.walletkit.core.IAdapter
import io.horizontalsystems.walletkit.core.IBalanceAdapter
import io.horizontalsystems.walletkit.core.IReceiveAdapter
import io.horizontalsystems.walletkit.core.ISendSolanaAdapter
import io.horizontalsystems.walletkit.core.managers.SolanaKitWrapper
import io.horizontalsystems.walletkit.entities.transactionrecords.solana.SolanaTransactionRecord
import io.horizontalsystems.solanakit.Signer
import io.horizontalsystems.solanakit.models.FullTransaction
import java.math.BigDecimal

abstract class BaseSolanaAdapter(
        solanaKitWrapper: SolanaKitWrapper,
        val decimal: Int
) : IAdapter, IBalanceAdapter, IReceiveAdapter, ISendSolanaAdapter {

    val solanaKit = solanaKitWrapper.solanaKit
    protected val signer: Signer? = solanaKitWrapper.signer

    override val debugInfo: String
        get() = solanaKit.debugInfo()

    val statusInfo: Map<String, Any>
        get() = solanaKit.statusInfo()

    // IReceiveAdapter

    override val receiveAddress: String
        get() = solanaKit.receiveAddress

    override val isMainNet: Boolean
        get() = solanaKit.isMainnet

    override fun estimateFee(rawTransaction: ByteArray): BigDecimal {
        return solanaKit.estimateFee(rawTransaction)
    }

    override suspend fun send(rawTransaction: ByteArray): FullTransaction {
        if (signer == null) throw Exception()

        return solanaKit.sendRawTransaction(rawTransaction, signer)
    }

    companion object {
        const val confirmationsThreshold: Int = SolanaTransactionRecord.CONFIRMATIONS_THRESHOLD
    }

}
