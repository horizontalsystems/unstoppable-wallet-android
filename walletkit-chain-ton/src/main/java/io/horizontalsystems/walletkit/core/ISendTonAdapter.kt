package io.horizontalsystems.walletkit.core

import com.tonapps.wallet.data.core.entity.SendRequestEntity
import io.horizontalsystems.tonkit.FriendlyAddress
import java.math.BigDecimal

interface ISendTonAdapter {
    val availableBalance: BigDecimal
    suspend fun sign(request: SendRequestEntity): String
    suspend fun send(amount: BigDecimal, address: FriendlyAddress, memo: String?)
    suspend fun estimateFee(amount: BigDecimal, address: FriendlyAddress, memo: String?) : BigDecimal
    suspend fun send(boc: String)
    suspend fun estimateFee(boc: String) : BigDecimal
}

