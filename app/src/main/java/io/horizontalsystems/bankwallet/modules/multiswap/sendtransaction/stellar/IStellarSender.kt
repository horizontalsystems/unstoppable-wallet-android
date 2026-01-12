package io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.stellar

import io.horizontalsystems.stellarkit.StellarKit
import java.math.BigDecimal

interface IStellarSender {
    val sendable: Boolean

    fun getFee(): BigDecimal?
    fun sendTransaction()
}

class StellarSenderRegular(
    private val address: String,
    private val amount: BigDecimal,
    private val memo: String,
    private val stellarKit: StellarKit
) : IStellarSender {
    override val sendable: Boolean
        get() = TODO("Not yet implemented")

    override fun getFee() = stellarKit.sendFee

    override fun sendTransaction() {
        TODO("Not yet implemented")
    }
}

class StellarSenderTransactionEnvelope(
    private val transactionEnvelope: String,
    private val stellarKit: StellarKit
) : IStellarSender {
    override val sendable = true

    override fun getFee() = StellarKit.estimateFee(transactionEnvelope)
    override fun sendTransaction() {
        stellarKit.sendTransaction(transactionEnvelope)
    }
}