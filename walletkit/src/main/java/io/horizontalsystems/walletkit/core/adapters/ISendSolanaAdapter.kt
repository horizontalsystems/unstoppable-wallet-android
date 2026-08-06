package io.horizontalsystems.walletkit.core

import io.horizontalsystems.solanakit.models.FullTransaction
import io.horizontalsystems.solanakit.models.Address as SolanaAddress
import java.math.BigDecimal

interface ISendSolanaAdapter {
    val availableBalance: BigDecimal
    suspend fun send(amount: BigDecimal, to: SolanaAddress): FullTransaction
    suspend fun send(rawTransaction: ByteArray): FullTransaction
    fun estimateFee(rawTransaction: ByteArray): BigDecimal
}
