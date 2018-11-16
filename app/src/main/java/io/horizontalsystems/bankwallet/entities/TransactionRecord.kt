package io.horizontalsystems.bankwallet.entities

open class TransactionRecord {

    var transactionHash = ""
    var blockHeight: Long = 0
    var coin = ""
    var amount: Double = 0.0
    var timestamp: Long = 0
    var rate: Double = 0.0

    var from: List<TransactionAddress> = listOf()
    var to: List<TransactionAddress> = listOf()

}

class TransactionAddress {
    var address = ""
    var mine = false
}
