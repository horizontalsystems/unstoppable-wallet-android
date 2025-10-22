package io.horizontalsystems.bankwallet.core

import java.math.BigDecimal

/**
 * Interface for sending Oxyra transactions
 */
interface ISendOxyraAdapter {
    suspend fun send(amount: BigDecimal, address: String, memo: String?)
    suspend fun estimateFee(amount: BigDecimal, address: String, memo: String?): BigDecimal
}

