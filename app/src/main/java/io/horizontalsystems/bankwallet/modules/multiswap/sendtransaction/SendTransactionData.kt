package io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bitcoincore.storage.UtxoFilters
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.tronkit.network.CreatedTransaction
import java.math.BigDecimal

sealed class SendTransactionData {
    data class Evm(
        val transactionData: TransactionData,
        val gasLimit: Long?,
        val feesMap: Map<FeeType, CoinValue> = mapOf()
    ): SendTransactionData()

    data class Btc(
        val address: String,
        val memo: String,
        val amount: BigDecimal,
        val recommendedGasRate: Int,
        val dustThreshold: Int?,
        val changeToFirstInput: Boolean,
        val utxoFilters: UtxoFilters,
        val feesMap: Map<FeeType, CoinValue>
    ) : SendTransactionData()

    data class Tron(val createdTransaction: CreatedTransaction) : SendTransactionData()
}

enum class FeeType(val stringResId: Int) {
    Outbound(R.string.Fee_OutboundFee),
    Liquidity(R.string.Fee_LiquidityFee);
}
