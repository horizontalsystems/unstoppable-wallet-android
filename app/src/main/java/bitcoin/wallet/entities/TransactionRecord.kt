package bitcoin.wallet.entities

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class TransactionRecord : RealmObject() {

    @PrimaryKey
    var transactionHash = ""

    var coinCode = ""

    var from = ""
    var to = ""

    var amount = 0L
    var fee = 0L

    var incoming = true

    var blockHeight = 0L

    var timestamp = 0L

}
