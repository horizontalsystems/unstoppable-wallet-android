package bitcoin.wallet.entities

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

class Transaction {
    var inputs = listOf<TransactionInput>()
    var outputs = listOf<TransactionOutput>()
    var timestamp = 0L
}

class TransactionInput(var address: String = "", var value: Long = 0)

class TransactionOutput(var address: String = "", var value: Long = 0)

open class TransactionRecord : RealmObject() {

    @PrimaryKey
    var hash = ""

    var coinCode = ""

    var from = ""
    var to = ""

    var amount = 0L
    var fee = 0L

    var incoming = true

    var blockHeight = 0L

    var timestamp = 0L

}
