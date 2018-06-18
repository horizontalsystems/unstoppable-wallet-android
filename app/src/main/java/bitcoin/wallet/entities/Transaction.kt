package bitcoin.wallet.entities

import com.google.gson.annotations.SerializedName

class Transaction {
    var inputs = listOf<TransactionInput>()
    var outputs = listOf<TransactionOutput>()
    var timestamp = 0L
}

class TransactionInput(var address: String = "", var value: Long = 0)

class TransactionOutput(var address: String = "", var value: Long = 0)

class UnspentOutput(
        val value: Long,

        @SerializedName("tx_output_n")
        val index: Int,

        val confirmations: Long,

        @SerializedName("tx_hash")
        val transactionHash: String,

        val script: String
)
