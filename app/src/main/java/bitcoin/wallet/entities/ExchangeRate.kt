package bitcoin.wallet.entities

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class ExchangeRate : RealmObject() {

    @PrimaryKey
    var code: String = ""

    var value: Double = 0.0

}
