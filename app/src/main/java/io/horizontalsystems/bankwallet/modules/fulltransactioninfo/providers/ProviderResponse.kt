package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers

import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

interface FullTransactionResponse

abstract class BitcoinResponse : FullTransactionResponse {
    abstract val hash: String
    abstract val date: Date?
    abstract val height: Int
    abstract val fee: Double
    abstract val size: Int?
    abstract val feePerByte: Double?
    abstract val confirmations: String?
    abstract val inputs: ArrayList<Input>
    abstract val outputs: ArrayList<Output>

    abstract class Input {
        abstract val value: Double
        abstract val address: String
    }

    abstract class Output {
        abstract val value: Double
        abstract val address: String
    }

    companion object {
        const val btcRate: Double = 100_000_000.0
    }
}

abstract class EthereumResponse : FullTransactionResponse {
    abstract val hash: String
    abstract val date: Date?
    abstract val confirmations: Int?
    abstract val height: String

    abstract val size: Int?
    abstract val gasPrice: String?
    abstract val gasUsed: String?
    abstract val gasLimit: String
    abstract val fee: String?
    abstract val value: BigInteger

    abstract val nonce: String?
    abstract val from: String
    abstract val to: String
    abstract val contractAddress: String?

    companion object {
        const val ethRate: Double = 1_000_000_000_000_000_000.0
        const val gweiRate: Double = 1_000_000_000.0
    }
}

abstract class BinanceResponse : FullTransactionResponse {
    abstract val hash: String
    abstract val blockHeight: String

    abstract var fee: BigDecimal
    abstract var value: BigDecimal
    abstract val memo: String

    abstract var from: String
    abstract var to: String
}

abstract class EosResponse : FullTransactionResponse {
    abstract val txId: String
    abstract val status: String

    abstract val blockNumber: String
    abstract val blockTimeStamp: Long?

    abstract val actions: List<EosAction>

    abstract val cpuUsage: Int
    abstract val netUsage: Int
}

data class EosAction(val account: String,
                     val from: String,
                     val to: String,
                     val amount: String,
                     val memo: String)
