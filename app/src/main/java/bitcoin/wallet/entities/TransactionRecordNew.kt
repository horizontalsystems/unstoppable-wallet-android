package bitcoin.wallet.entities

open class TransactionRecordNew {

    var transactionHash = ""

    var coinCode = ""

    var from: List<String> = listOf()
    var to: List<String> = listOf()

    var amount = 0L
    var fee = 0L

    var incoming = true

    var blockHeight: Long? = null

    var timestamp: Long? = null

}
