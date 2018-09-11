package bitcoin.wallet.entities

open class TransactionRecordNew {

    var transactionHash = ""

    var coinCode = ""

    var from: List<String> = listOf()
    var to: List<String> = listOf()

    var amount: Double = 0.0
    var fee: Double = 0.0

    var incoming = true

    var blockHeight: Long? = null

    var timestamp: Long? = null

}
