package io.horizontalsystems.walletkit.core

import io.horizontalsystems.walletkit.core.managers.EvmKitWrapper
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import java.math.BigDecimal

interface ISendEthereumAdapter {
    val evmKitWrapper: EvmKitWrapper
    val balanceData: BalanceData

    fun getTransactionData(amount: BigDecimal, address: Address): TransactionData
}
