package io.horizontalsystems.walletkit.core

import io.horizontalsystems.tronkit.models.Contract
import io.horizontalsystems.tronkit.network.CreatedTransaction
import io.horizontalsystems.tronkit.transaction.Fee
import java.math.BigDecimal
import io.horizontalsystems.tronkit.models.Address as TronAddress

interface ISendTronAdapter {
    val balanceData: BalanceData
    val trxBalanceData: BalanceData

    suspend fun estimateFee(amount: BigDecimal, to: TronAddress): List<Fee>
    suspend fun estimateFee(transaction: CreatedTransaction): List<Fee>
    suspend fun estimateFee(contract: Contract): List<Fee>
    suspend fun send(amount: BigDecimal, to: TronAddress, feeLimit: Long?): String
    suspend fun send(contract: Contract, feeLimit: Long?): String
    suspend fun send(createdTransaction: CreatedTransaction): String
    suspend fun isAddressActive(address: TronAddress): Boolean
    fun isOwnAddress(address: TronAddress): Boolean
}
