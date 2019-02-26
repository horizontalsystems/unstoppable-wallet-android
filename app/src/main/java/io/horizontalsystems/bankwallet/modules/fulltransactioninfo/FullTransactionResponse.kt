package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import java.math.BigInteger
import java.util.*

interface FullTransactionResponse

abstract class BitcoinResponse : FullTransactionResponse {
    abstract val hash: String
    abstract val date: Date
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
    abstract val gasPrice: String
    abstract val gasUsed: String?
    abstract val gasLimit: String
    abstract val fee: String?
    abstract val value: BigInteger

    abstract val nonce: String
    abstract val from: String
    abstract val to: String

    companion object {
        const val ethRate: Double = 1_000_000_000_000_000_000.0
        const val gweiRate: Double = 1_000_000_000.0
    }
}
