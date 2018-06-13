package bitcoin.wallet.entities

class Transaction {
    var inputs = listOf<TransactionInput>()
    var outputs = listOf<TransactionOutput>()
    var timestamp = 0L
}

class TransactionInput(var address: String = "", var value: Long = 0)

class TransactionOutput(var address: String = "", var value: Long = 0)

class UnspentOutput(var value: Long)
