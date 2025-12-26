package io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bitcoincore.storage.UtxoFilters
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.tronkit.models.Contract
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
        val minimumSendAmount: Int?,
        val changeToFirstInput: Boolean,
        val utxoFilters: UtxoFilters,
        val feesMap: Map<FeeType, CoinValue>
    ) : SendTransactionData()

    sealed class Tron : SendTransactionData() {
        data class WithContract(val contract: Contract) : Tron()
        data class WithCreateTransaction(val transaction: CreatedTransaction) : Tron()
    }

    sealed class Solana : SendTransactionData() {
        data class WithRawTransaction(val rawTransaction: ByteArray) : Solana() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is WithRawTransaction) return false

                return rawTransaction.contentEquals(other.rawTransaction)
            }

            override fun hashCode() = rawTransaction.contentHashCode()
        }
    }

    sealed class Stellar : SendTransactionData() {
        data class Regular(
            val address: String,
            val memo: String,
            val amount: BigDecimal
        ) : Stellar()

        data class WithTransactionEnvelope(val transactionEnvelope: String) : Stellar()
    }
}

enum class FeeType(val stringResId: Int) {
    Outbound(R.string.Fee_OutboundFee),
    Liquidity(R.string.Fee_LiquidityFee);
}
