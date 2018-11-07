package io.horizontalsystems.bankwallet.entities

sealed class TransactionStatus {
    object Completed : TransactionStatus()
    class Processing(val progress: Byte) : TransactionStatus() //progress in 0..100%
    object Pending : TransactionStatus()
}

open class TransactionRecord {

    var transactionHash = ""

    var coinCode = ""

    var from: List<String> = listOf()
    var to: List<String> = listOf()

    var amount: Double = 0.0
    var fee: Double = 0.0

    var blockHeight: Long? = null

    var timestamp: Long? = null

    var status: TransactionStatus = TransactionStatus.Pending

}
