package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.bankwallet.modules.transactions.TransactionStatus

open class TransactionRecord {

    var transactionHash = ""
    var blockHeight: Long = 0
    var coin = ""
    var amount: Double = 0.0
    var timestamp: Long = 0
    var rate: Double = 0.0

    var from: List<TransactionAddress> = listOf()
    var to: List<TransactionAddress> = listOf()

    @Deprecated("use coin instead")
    var coinCode = ""

    @Deprecated("remove???")
    var fee: Double = 0.0

    @Deprecated("remove")
    var status: TransactionStatus = TransactionStatus.Pending
}

class TransactionAddress {
    var address = ""
    var mine = false
}
