package bitcoin.wallet.entities

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class Balance : RealmObject() {

    @PrimaryKey
    var code: String = ""

    var value: Long = 0

}
