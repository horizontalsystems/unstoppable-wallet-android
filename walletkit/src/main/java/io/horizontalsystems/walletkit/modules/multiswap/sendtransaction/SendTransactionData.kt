package io.horizontalsystems.walletkit.modules.multiswap.sendtransaction

import io.horizontalsystems.bitcoincore.storage.UtxoFilters
import io.horizontalsystems.tronkit.models.Contract
import io.horizontalsystems.tronkit.network.CreatedTransaction
import org.json.JSONObject
import java.math.BigDecimal

sealed class SendTransactionData {
    data class Evm(
        val transactionData: EvmTransactionData,
        val gasLimit: Long?,
    ): SendTransactionData()

    data class Btc(
        val address: String,
        val memo: String?,
        val amount: BigDecimal,
        val recommendedGasRate: Int?,
        val minimumSendAmount: Int?,
        val changeToFirstInput: Boolean,
        val utxoFilters: UtxoFilters,
    ) : SendTransactionData()

    sealed class Tron : SendTransactionData() {
        data class WithContract(val contract: Contract) : Tron()
        data class WithCreateTransaction(val transaction: CreatedTransaction) : Tron()
        data class Simple(val address: String, val amount: BigDecimal) : Tron()
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
        data class Simple(val address: String, val amount: BigDecimal) : Solana()
    }

    sealed class Stellar : SendTransactionData() {
        data class Regular(
            val address: String,
            val memo: String,
            val amount: BigDecimal
        ) : Stellar()

        data class WithTransactionEnvelope(val transactionEnvelope: String) : Stellar()
    }

    sealed class Ton : SendTransactionData() {
        data class Regular(
            val address: String,
            val amount: BigDecimal,
            val memo: String?
        ) : Ton()

        data class SendRequest(val requestJson: JSONObject) : Ton()
    }

    sealed class Zcash : SendTransactionData() {
        data class Regular(
            val address: String,
            val amount: BigDecimal,
            val memo: String,
        ) : Zcash()

        data class ShieldedMemo(
            val address: String,
            val amount: BigDecimal,
            val memo: String,
            val memoShieldedAddress: String,
        ) : Zcash()
    }

    sealed class Thorchain : SendTransactionData() {
        // MsgDeposit to the protocol (no inbound address), used by THORChain itself
        data class Deposit(
            val asset: String,
            val amount: BigDecimal,
            val memo: String,
        ) : Thorchain()

        // MsgSend to an inbound vault address, used by protocols with a THORChain inbound (e.g. Maya)
        data class Send(
            val address: String,
            val amount: BigDecimal,
            val memo: String,
        ) : Thorchain()
    }

    data class Monero(
        val address: String,
        val amount: BigDecimal,
        val memo: String?,
    ) : SendTransactionData()

    data class Zano(
        val address: String,
        val amount: BigDecimal,
        val memo: String?,
    ) : SendTransactionData()

}

/** Kit-free projection of ethereum-kit's TransactionData. */
data class EvmTransactionData(
    val to: String,
    val value: java.math.BigInteger,
    val input: ByteArray,
)
